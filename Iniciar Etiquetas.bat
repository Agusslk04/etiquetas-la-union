@echo off
setlocal
title Etiquetas La Union
cd /d "%~dp0"

set "JAR=target\etiquetas-la-union.jar"
set "URL=http://localhost:8081"

rem Si ya esta funcionando, solo abre el navegador
curl -s -o nul "%URL%" && (start "" "%URL%" & exit /b 0)

rem Si esta instalado como sistema, se prende la tarea de Windows
schtasks /query /tn "Etiquetas La Union" >nul 2>&1
if not errorlevel 1 (
	powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\sistema.ps1" -Accion Iniciar
	if not errorlevel 1 start "" "%URL%"
	timeout /t 5 /nobreak >nul
	exit /b 0
)

rem Sin instalar: compila si hace falta y lo abre en una ventana minimizada
powershell -NoProfile -Command "if (-not (Test-Path '%JAR%') -or -not (Test-Path 'target\.compilado')) { exit 1 }; $marca = (Get-Item 'target\.compilado').LastWriteTime; $ultimo = Get-ChildItem src, pom.xml -Recurse -File | Sort-Object LastWriteTime -Descending | Select-Object -First 1; if ($ultimo.LastWriteTime -gt $marca) { exit 1 }; exit 0"
if errorlevel 1 (
	echo Preparando el sistema, puede tardar un minuto...
	call mvnw.cmd -q -DskipTests package
	if errorlevel 1 (
		echo.
		echo No se pudo compilar. Revisa los mensajes de arriba.
		pause
		exit /b 1
	)
	echo listo> "target\.compilado"
)

echo Iniciando Etiquetas La Union...
start "Etiquetas La Union - NO CERRAR (cerrar = apagar el sistema)" /min java -jar "%JAR%"

rem Espera a que responda (hasta 90 segundos) y abre el navegador
for /l %%i in (1,1,90) do (
	curl -s -o nul "%URL%" && (start "" "%URL%" & exit /b 0)
	timeout /t 1 /nobreak >nul
)

echo.
echo El sistema no arranco. Abri la ventana minimizada "Etiquetas La Union" para ver el error.
echo Revisa que PostgreSQL este iniciado.
pause
