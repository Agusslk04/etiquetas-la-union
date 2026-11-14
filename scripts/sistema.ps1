# =========================================================
#  Etiquetas La Unión · instalar / quitar / iniciar / detener
#  el sistema como tarea de Windows (sin ventanas, arranca solo).
#
#  Se usa desde los .bat de la carpeta del proyecto.
# =========================================================
param(
	[ValidateSet('Instalar', 'Quitar', 'Iniciar', 'Detener')]
	[string]$Accion = 'Instalar'
)

$ErrorActionPreference = 'Stop'
$raiz     = Split-Path -Parent $PSScriptRoot
$tarea    = 'Etiquetas La Union'
$carpeta  = Join-Path $raiz 'sistema'
$jarNuevo = Join-Path $raiz 'target\etiquetas-la-union.jar'
$jar      = Join-Path $carpeta 'etiquetas-la-union.jar'
$puerto   = 8081
$reglaFirewall = 'Etiquetas La Union (8081)'

function Escribir([string]$texto, [string]$color = 'Gray') {
	Write-Host $texto -ForegroundColor $color
}

function Obtener-Tarea {
	Get-ScheduledTask -TaskName $tarea -ErrorAction SilentlyContinue
}

function Responde {
	try {
		$null = Invoke-WebRequest "http://localhost:$puerto/" -UseBasicParsing -TimeoutSec 3
		return $true
	} catch {
		return $false
	}
}

function Esperar-Arranque([int]$segundos = 120) {
	for ($i = 0; $i -lt $segundos; $i += 2) {
		if (Responde) { return $true }
		Start-Sleep -Seconds 2
	}
	return $false
}

function Cerrar-Proceso {
	$proceso = Get-NetTCPConnection -LocalPort $puerto -State Listen -ErrorAction SilentlyContinue |
		Select-Object -ExpandProperty OwningProcess -First 1
	if ($proceso) {
		Stop-Process -Id $proceso -Force -ErrorAction SilentlyContinue
		Start-Sleep -Seconds 2
	}
}

function Compilar-SiHaceFalta {
	# Maven deja el .jar con fecha fija; por eso se compara contra una marca propia
	$marca = Join-Path $raiz 'target\.compilado'
	$compilar = -not (Test-Path $jarNuevo) -or -not (Test-Path $marca)
	if (-not $compilar) {
		$ultimo = Get-ChildItem (Join-Path $raiz 'src'), (Join-Path $raiz 'pom.xml') -Recurse -File |
			Sort-Object LastWriteTime -Descending | Select-Object -First 1
		$compilar = $ultimo.LastWriteTime -gt (Get-Item $marca).LastWriteTime
	}
	if ($compilar) {
		Escribir 'Compilando el sistema (puede tardar un minuto)...' 'Yellow'
		Push-Location $raiz
		try {
			& (Join-Path $raiz 'mvnw.cmd') -q -DskipTests package
			if ($LASTEXITCODE -ne 0) { throw 'No se pudo compilar el sistema.' }
			Set-Content -Path $marca -Value (Get-Date -Format s)
		} finally {
			Pop-Location
		}
	}
}

function Direccion-Red {
	$ip = Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
		Where-Object { $_.IPAddress -notlike '127.*' -and $_.IPAddress -notlike '169.254.*' -and
			$_.InterfaceAlias -notmatch 'vEthernet|VirtualBox|VMware|Loopback|WSL|Bluetooth' } |
		Sort-Object { $_.InterfaceAlias -notmatch 'Wi-?Fi|Ethernet' } |
		Select-Object -First 1
	if ($ip) { return "http://$($ip.IPAddress):$puerto" }
	return $null
}

function Mostrar-Direcciones {
	Escribir ''
	Escribir "  En esta compu:         http://localhost:$puerto" 'Green'
	$red = Direccion-Red
	if ($red) { Escribir "  Desde el mismo WiFi:   $red" 'Green' }
	if (-not (Get-NetFirewallRule -DisplayName $reglaFirewall -ErrorAction SilentlyContinue)) {
		Escribir ''
		Escribir '  Para que entren otras compus falta abrir el Firewall (una sola vez):' 'Yellow'
		Escribir '  clic derecho en "Permitir acceso en red.bat" > Ejecutar como administrador.' 'Yellow'
	}
	Escribir ''
}

switch ($Accion) {

	'Instalar' {
		Compilar-SiHaceFalta

		# Si ya estaba instalado, se apaga para reemplazar el programa
		if (Obtener-Tarea) { Stop-ScheduledTask -TaskName $tarea -ErrorAction SilentlyContinue }
		Cerrar-Proceso
		New-Item -ItemType Directory -Force $carpeta | Out-Null
		Copy-Item $jarNuevo $jar -Force

		# javaw = Java sin ventana negra
		$java = (Get-Command java -ErrorAction Stop).Source
		$javaw = Join-Path (Split-Path $java) 'javaw.exe'
		if (-not (Test-Path $javaw)) { $javaw = $java }

		$esAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole(
			[Security.Principal.WindowsBuiltInRole]::Administrator)
		if ($esAdmin) {
			# Como administrador: arranca al prender la compu, aunque nadie inicie sesión
			$disparador = New-ScheduledTaskTrigger -AtStartup
			$quien = New-ScheduledTaskPrincipal -UserId 'SYSTEM' -LogonType ServiceAccount -RunLevel Highest
			$cuando = 'al prender la compu (aunque nadie inicie sesión)'
		} else {
			# Sin administrador: arranca cuando este usuario entra a Windows
			$usuario = "$env:USERDOMAIN\$env:USERNAME"
			$disparador = New-ScheduledTaskTrigger -AtLogOn -User $usuario
			$quien = New-ScheduledTaskPrincipal -UserId $usuario -LogonType Interactive -RunLevel Limited
			$cuando = "cada vez que $env:USERNAME entra a Windows"
		}
		# Vigilante: cada 5 minutos, desde ahora y para siempre. Si el sistema se cerró, lo vuelve a abrir;
		# si está abierto, no hace nada. (La repetición de un disparador "al iniciar sesión" recién
		# empieza en el próximo inicio de sesión, por eso va como disparador aparte.)
		$vigilante = New-ScheduledTaskTrigger -Once -At (Get-Date).AddMinutes(1) -RepetitionInterval (New-TimeSpan -Minutes 5)

		$programa = New-ScheduledTaskAction -Execute $javaw -Argument "-jar `"$jar`"" -WorkingDirectory $carpeta
		$ajustes = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable `
			-MultipleInstances IgnoreNew -ExecutionTimeLimit ([TimeSpan]::Zero) `
			-RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 1)

		Register-ScheduledTask -TaskName $tarea -Force `
			-Description "Sistema de etiquetas de precio de La Unión Supermercado (puerto $puerto)" `
			-Action $programa -Trigger @($disparador, $vigilante) -Principal $quien -Settings $ajustes | Out-Null
		Start-ScheduledTask -TaskName $tarea

		Escribir "Instalado: arranca solo $cuando." 'Green'
		Escribir 'Iniciando...'
		if (Esperar-Arranque) {
			Mostrar-Direcciones
		} else {
			Escribir "El sistema no respondió. Mirá el registro: $carpeta\logs\etiquetas.log" 'Red'
			exit 1
		}
	}

	'Quitar' {
		if (Obtener-Tarea) {
			Stop-ScheduledTask -TaskName $tarea -ErrorAction SilentlyContinue
			Unregister-ScheduledTask -TaskName $tarea -Confirm:$false
		}
		Cerrar-Proceso
		Escribir 'Listo: el sistema se apagó y ya no arranca solo. Los datos siguen guardados.' 'Green'
	}

	'Iniciar' {
		if (-not (Obtener-Tarea)) {
			Escribir 'El sistema no está instalado. Usá "Instalar como sistema.bat".' 'Yellow'
			exit 2
		}
		Enable-ScheduledTask -TaskName $tarea | Out-Null
		if (-not (Responde)) {
			Start-ScheduledTask -TaskName $tarea
			Escribir 'Iniciando...'
			if (-not (Esperar-Arranque)) {
				Escribir "El sistema no respondió. Mirá el registro: $carpeta\logs\etiquetas.log" 'Red'
				exit 1
			}
		}
		Mostrar-Direcciones
	}

	'Detener' {
		if (Obtener-Tarea) {
			# Se pausa la tarea para que no lo vuelva a abrir a los 5 minutos
			Disable-ScheduledTask -TaskName $tarea | Out-Null
			Stop-ScheduledTask -TaskName $tarea -ErrorAction SilentlyContinue
		}
		Cerrar-Proceso
		Escribir 'El sistema se detuvo. Para volver a prenderlo: "Iniciar Etiquetas.bat".' 'Green'
	}
}
