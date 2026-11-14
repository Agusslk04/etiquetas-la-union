@echo off
cd /d "%~dp0"
rem Apaga el sistema. Si esta instalado como sistema, queda en pausa hasta "Iniciar Etiquetas.bat".
schtasks /query /tn "Etiquetas La Union" >nul 2>&1
if not errorlevel 1 (
	powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\sistema.ps1" -Accion Detener
) else (
	for /f "tokens=5" %%p in ('netstat -ano ^| findstr /r /c:":8081 .*LISTENING"') do taskkill /PID %%p /F >nul 2>&1
	echo Etiquetas La Union se detuvo.
)
timeout /t 3 /nobreak >nul
