@echo off
rem Abre el puerto 8081 en el Firewall de Windows para que otras compus
rem de la red puedan entrar a http://IP-DE-ESTA-COMPU:8081
rem Se corre UNA sola vez: clic derecho > Ejecutar como administrador.

net session >nul 2>&1
if errorlevel 1 (
	echo.
	echo Este archivo necesita permisos de administrador.
	echo Clic derecho sobre "Permitir acceso en red.bat" y elegi "Ejecutar como administrador".
	echo.
	pause
	exit /b 1
)

netsh advfirewall firewall delete rule name="Etiquetas La Union (8081)" >nul 2>&1
rem remoteip=localsubnet: solo pueden entrar los equipos conectados a la misma red (mismo WiFi)
netsh advfirewall firewall add rule name="Etiquetas La Union (8081)" dir=in action=allow protocol=TCP localport=8081 remoteip=localsubnet profile=any
if errorlevel 1 (
	echo No se pudo crear la regla del Firewall.
) else (
	echo.
	echo Listo: las otras compus de la red ya pueden entrar al sistema.
	echo La direccion aparece abajo de todo en el panel.
)
echo.
pause
