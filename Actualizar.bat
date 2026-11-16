<# : Etiquetas La Union - Actualizar (este archivo es .bat y PowerShell a la vez)
@echo off
title Etiquetas La Union - Actualizar
set "ACTUALIZAR_BAT=%~f0"
set "ACTUALIZAR_ARGS=%*"
powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-Expression ([IO.File]::ReadAllText($env:ACTUALIZAR_BAT, [Text.Encoding]::UTF8))" & (if not errorlevel 99 (echo. & pause)) & exit /b
#>

# =========================================================
#  Etiquetas La Unión · Actualizar
#
#  Baja la última versión de GitHub, respalda la base de datos,
#  recompila y reinicia el sistema. Los productos no se tocan.
#
#  Se puede ejecutar desde la carpeta del sistema o directo desde un
#  pendrive: si no está en la carpeta, la busca sola.
#
#  Opciones:  /si        responde "sí" a todas las preguntas
#             /sinadmin  no pide permiso de administrador
# =========================================================

$ErrorActionPreference = 'Continue'
[Console]::OutputEncoding = [Text.Encoding]::UTF8

$repo    = 'https://github.com/Agusslk04/etiquetas-la-union.git'
$tarea   = 'Etiquetas La Union'
$puerto  = 8081
$bat     = $env:ACTUALIZAR_BAT
$opciones = " $env:ACTUALIZAR_ARGS ".ToLower()
$siATodo  = $opciones -match '\s[/-]si\s'
$sinAdmin = $opciones -match '\s[/-]sinadmin\s'
$fecha   = Get-Date -Format 'yyyyMMdd-HHmm'

function Escribir([string]$texto, [string]$color = 'Gray') { Write-Host $texto -ForegroundColor $color }
function Paso([string]$texto) { Escribir ''; Escribir $texto 'Cyan' }
function Fallar([string]$texto) { Escribir ''; Escribir $texto 'Red'; exit 1 }
function Preguntar([string]$texto) {
	if ($siATodo) { Escribir "$texto S" 'Yellow'; return $true }
	return (Read-Host "$texto (S/N)") -match '^\s*[sS]'
}

Escribir ''
Escribir '  ETIQUETAS LA UNIÓN · ACTUALIZAR' 'Green'
Escribir '  ================================' 'Green'

# ---------- Permiso de administrador ----------
# Si el sistema arranca con la compu, la tarea de Windows solo se puede reemplazar como administrador.
$esAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole(
	[Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $esAdmin -and -not $sinAdmin) {
	Escribir ''
	Escribir 'Pidiendo permiso de administrador: aceptá el cartel de Windows.' 'Yellow'
	try {
		Start-Process -FilePath $env:ComSpec -ArgumentList ('/c ""' + $bat + '" ' + $env:ACTUALIZAR_ARGS + '"') -Verb RunAs -ErrorAction Stop
		exit 99   # sigue en la ventana nueva
	} catch {
		Escribir 'Sin permiso de administrador: se intenta igual.' 'Yellow'
	}
}

# ---------- Git ----------
$git = Get-Command git.exe -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty Source
if (-not $git) {
	foreach ($g in "$env:ProgramFiles\Git\cmd\git.exe", "${env:ProgramFiles(x86)}\Git\cmd\git.exe", "$env:LOCALAPPDATA\Programs\Git\cmd\git.exe") {
		if (Test-Path $g) { $git = $g; break }
	}
}
if (-not $git) { Fallar 'No encontré Git en esta compu. Instalalo (https://git-scm.com) y volvé a ejecutar Actualizar.bat.' }

# safe.directory=*: la carpeta puede ser de otro usuario de Windows (por ejemplo, si se instaló como administrador)
function Git { & $git -c safe.directory=* -C $script:raiz @args }

# ---------- Dónde está instalado ----------
function Es-Sistema([string]$dir) {
	if (-not $dir) { return $false }
	$pom = Join-Path $dir 'pom.xml'
	if (-not (Test-Path $pom) -or -not (Test-Path (Join-Path $dir 'scripts\sistema.ps1'))) { return $false }
	return (Get-Content $pom -Raw) -match '<finalName>etiquetas-la-union</finalName>'
}

function Buscar-Sistema {
	# 1. La carpeta de este archivo
	$aqui = Split-Path -Parent $bat
	if (Es-Sistema $aqui) { return $aqui }
	# 2. La tarea de Windows que lo arranca
	$t = Get-ScheduledTask -TaskName $tarea -ErrorAction SilentlyContinue
	if ($t) {
		$trabajo = @($t.Actions)[0].WorkingDirectory
		if ($trabajo -and (Es-Sistema (Split-Path -Parent $trabajo))) { return (Split-Path -Parent $trabajo) }
	}
	# 3. Los lugares de siempre (C:\ y escritorio/documentos de cada usuario)
	$nombres = 'EtiquetasLaUnion', 'etiquetas-la-union'
	foreach ($n in $nombres) { if (Es-Sistema "C:\$n") { return "C:\$n" } }
	$usuarios = @(Get-ChildItem 'C:\Users' -Directory -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName)
	foreach ($u in $usuarios) {
		foreach ($sub in 'Desktop', 'Escritorio', 'OneDrive\Desktop', 'OneDrive\Escritorio', 'Documents', 'Documentos', '') {
			foreach ($n in $nombres) {
				$dir = Join-Path (Join-Path $u $sub) $n
				if (Es-Sistema $dir) { return $dir }
			}
		}
	}
	return $null
}

$script:raiz = Buscar-Sistema

# ---------- Primera vez: no está instalado ----------
if (-not $raiz) {
	Escribir ''
	Escribir 'No encontré Etiquetas La Unión en esta compu.' 'Yellow'
	if (-not (Preguntar '¿Instalarlo en C:\EtiquetasLaUnion?')) { Fallar 'No se hizo nada.' }
	$script:raiz = 'C:\EtiquetasLaUnion'
	if (Test-Path $raiz) { Fallar "Ya existe la carpeta $raiz pero no es el sistema. Renombrala o borrala y volvé a ejecutar Actualizar.bat." }
	Paso 'Bajando el sistema de internet...'
	& $git clone -q $repo $raiz
	if ($LASTEXITCODE -ne 0) { Fallar 'No se pudo bajar el sistema. ¿Hay internet?' }
	Paso 'Instalando (arranca solo con Windows)...'
	& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $raiz 'scripts\sistema.ps1') -Accion Instalar
	if ($LASTEXITCODE -ne 0) { Fallar 'El sistema se bajó pero no arrancó. Revisá que PostgreSQL esté iniciado.' }
	Escribir 'Listo: Etiquetas La Unión quedó instalado.' 'Green'
	exit 0
}

Escribir ''
Escribir "Sistema encontrado en: $raiz" 'Green'
$respaldos = Join-Path $raiz 'respaldos'
New-Item -ItemType Directory -Force $respaldos | Out-Null

# ---------- 1. Bajar la versión nueva (todavía no cambia nada) ----------
Paso '1/4  Buscando la versión nueva en internet...'
if (-not (Test-Path (Join-Path $raiz '.git'))) {
	# Carpeta copiada a mano (sin Git): se la conecta a GitHub
	Git init -q
	if ($LASTEXITCODE -ne 0) { Fallar 'No se pudo preparar la carpeta para actualizar.' }
}
Git fetch -q $repo main
if ($LASTEXITCODE -ne 0) { Fallar 'No se pudo bajar la actualización (¿hay internet?). No se cambió nada.' }
$nueva  = "$(Git rev-parse FETCH_HEAD)".Trim()
$actual = "$(Git rev-parse -q --verify HEAD 2>$null)".Trim()
$fechaNueva = "$(Git log -1 --format=%cd --date=format:%d/%m/%Y $nueva)".Trim()

if ($actual -eq $nueva) {
	Escribir "Ya tiene la última versión (del $fechaNueva)." 'Green'
	if (-not (Preguntar '¿Recompilar y reiniciar el sistema igual?')) { exit 0 }
} else {
	if ($actual) {
		# Protección: si esta carpeta tiene cambios guardados que no están en GitHub, no se pisan
		$propios = [int]"$(Git rev-list --count "$nueva..$actual")"
		if ($propios -gt 0) {
			Fallar "Esta carpeta tiene $propios cambio(s) que no están en GitHub (¿es la compu de desarrollo?). No se actualiza para no perderlos."
		}
		# Archivos cambiados a mano: se guardan en respaldos\ antes de reemplazarlos
		$cambiados = @(Git status --porcelain --untracked-files=no | Where-Object { $_ })
		if ($cambiados.Count -gt 0) {
			$parche = Join-Path $respaldos "cambios-a-mano-$fecha.patch"
			Git diff HEAD --output="$parche"
			Escribir "Hay $($cambiados.Count) archivo(s) cambiados a mano en esta compu:" 'Yellow'
			$cambiados | ForEach-Object { Escribir "   $_" 'Yellow' }
			Escribir "Se guardaron en $parche" 'Yellow'
			if (-not (Preguntar '¿Reemplazarlos por la versión nueva?')) { Fallar 'No se cambió nada.' }
		}
	}
	Escribir "Hay una versión nueva (del $fechaNueva)." 'Green'
}

# ---------- 2. Respaldo de la base de datos ----------
Paso '2/4  Respaldando la base de datos...'
function Respaldar-Base {
	$props = Get-Content (Join-Path $raiz 'src\main\resources\application.properties') -Raw
	$url = [regex]::Match($props, '(?m)^spring\.datasource\.url=jdbc:postgresql://([^:/\s]+)(?::(\d+))?/([^?\s]+)')
	if (-not $url.Success) { Escribir 'No encontré los datos de la base en application.properties.' 'Yellow'; return $false }
	$servidor = $url.Groups[1].Value
	$puertoBase = if ($url.Groups[2].Success) { $url.Groups[2].Value } else { '5432' }
	$base = $url.Groups[3].Value
	$usuario = [regex]::Match($props, '(?m)^spring\.datasource\.username=(.*)$').Groups[1].Value.Trim()
	$clave = [regex]::Match($props, '(?m)^spring\.datasource\.password=(.*)$').Groups[1].Value.Trim()

	$pgDump = Get-ChildItem "$env:ProgramFiles\PostgreSQL\*\bin\pg_dump.exe" -ErrorAction SilentlyContinue |
		Sort-Object { [int]($_.Directory.Parent.Name -replace '[^\d]', '') } -Descending |
		Select-Object -First 1 -ExpandProperty FullName
	if (-not $pgDump) { $pgDump = Get-Command pg_dump.exe -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty Source }
	if (-not $pgDump) { Escribir 'No encontré pg_dump (PostgreSQL).' 'Yellow'; return $false }

	$archivo = Join-Path $respaldos "$base-antes-de-actualizar-$fecha.dump"
	$env:PGPASSWORD = $clave
	& $pgDump -h $servidor -p $puertoBase -U $usuario -F c -f $archivo $base 2>$null
	$ok = $LASTEXITCODE -eq 0 -and (Test-Path $archivo) -and (Get-Item $archivo).Length -gt 0
	Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
	if (-not $ok) {
		Remove-Item $archivo -ErrorAction SilentlyContinue
		Escribir "No se pudo respaldar la base '$base' (¿PostgreSQL está iniciado?)." 'Yellow'
		return $false
	}
	Escribir "Respaldo: $archivo" 'Green'
	# Se guardan los últimos 10 respaldos de actualizaciones
	Get-ChildItem $respaldos -Filter '*-antes-de-actualizar-*.dump' | Sort-Object LastWriteTime -Descending |
		Select-Object -Skip 10 | Remove-Item -ErrorAction SilentlyContinue
	return $true
}
if (-not (Respaldar-Base)) {
	if (-not (Preguntar '¿Actualizar igual, sin respaldo?')) { Fallar 'No se cambió nada.' }
}

# ---------- 3. Poner la versión nueva ----------
Paso '3/4  Copiando la versión nueva...'
if ($actual -ne $nueva) {
	Git reset -q --hard $nueva
	if ($LASTEXITCODE -ne 0) { Fallar 'No se pudieron reemplazar los archivos. Cerrá los programas que tengan abierta la carpeta y probá de nuevo.' }
	Git branch -M main 2>$null
	if ($actual) {
		Escribir 'Novedades:' 'Green'
		Git log --format='   · %s' "$actual..$nueva"
	}
}
# Fuerza a recompilar con el código nuevo
Remove-Item (Join-Path $raiz 'target\.compilado') -ErrorAction SilentlyContinue

# ---------- 4. Recompilar y reiniciar ----------
Paso '4/4  Recompilando y reiniciando (puede tardar un par de minutos)...'
$sistema = Join-Path $raiz 'scripts\sistema.ps1'
if (Get-ScheduledTask -TaskName $tarea -ErrorAction SilentlyContinue) {
	# Instalado como sistema: "Instalar" recompila, reemplaza el programa y lo vuelve a arrancar
	& powershell -NoProfile -ExecutionPolicy Bypass -File $sistema -Accion Instalar
	if ($LASTEXITCODE -ne 0) { Fallar "El código se actualizó pero el sistema no arrancó. Mirá el registro: $raiz\sistema\logs\etiquetas.log" }
} else {
	# Sin instalar: se recompila; si estaba abierto con "Iniciar Etiquetas.bat", se reinicia
	$proceso = Get-NetTCPConnection -LocalPort $puerto -State Listen -ErrorAction SilentlyContinue |
		Select-Object -First 1 -ExpandProperty OwningProcess
	if ($proceso) {
		Stop-Process -Id $proceso -Force -ErrorAction SilentlyContinue
		Start-Sleep -Seconds 2
	}
	Push-Location $raiz
	& (Join-Path $raiz 'mvnw.cmd') -q -DskipTests package
	$compilo = $LASTEXITCODE -eq 0
	Pop-Location
	if (-not $compilo) { Fallar 'No se pudo compilar la versión nueva. Revisá los mensajes de arriba.' }
	Set-Content -Path (Join-Path $raiz 'target\.compilado') -Value 'listo'
	if ($proceso) {
		Start-Process -FilePath (Join-Path $raiz 'Iniciar Etiquetas.bat') -WorkingDirectory $raiz
		Escribir 'Se volvió a abrir con "Iniciar Etiquetas.bat".' 'Green'
	} else {
		Escribir 'Para abrirlo: "Iniciar Etiquetas.bat" (o "Instalar como sistema.bat" para que arranque solo).' 'Yellow'
	}
}

Escribir ''
Escribir "Listo: Etiquetas La Unión quedó actualizado (versión del $fechaNueva)." 'Green'
Escribir 'En las otras compus, si algo se ve raro, apretá Ctrl + F5 una vez.' 'Gray'
exit 0
