@echo off
title Etiquetas La Union - Quitar del sistema
cd /d "%~dp0"
rem Apaga el sistema y hace que ya no arranque solo con Windows. Los datos no se borran.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\sistema.ps1" -Accion Quitar
pause
