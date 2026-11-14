@echo off
title Etiquetas La Union - Instalar como sistema
cd /d "%~dp0"
rem Deja el sistema corriendo solo, sin ventanas, y que arranque con Windows.
rem Volver a ejecutarlo despues de cambiar el codigo actualiza el sistema.
rem Como administrador: arranca al prender la compu, aunque nadie inicie sesion.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\sistema.ps1" -Accion Instalar
if not errorlevel 1 start "" http://localhost:8081
pause
