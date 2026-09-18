@echo off
title SYNC ENGINE Desktop by Barrantes Co.
cd /d "C:\Users\joseb\.copilot\chats\12ebd589-5561-40ef-a3f7-8677604369bd\sync-android-laptop-mvp\desktop-electron"
echo ========================================================
echo   INICIANDO SYNC ENGINE - APLICACION DE ESCRITORIO
echo ========================================================
:: Liberar puerto 8123 si quedo ocupado por un proceso previo
for /f "tokens=5" %%a in ('netstat -aon 2^>nul ^| findstr ":8123" ^| findstr "LISTENING"') do taskkill /f /pid %%a >nul 2>&1

:: Iniciar la aplicacion de escritorio Electron
call npm start

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ========================================================
    echo   [MODO RESPALDO] Iniciando Servidor de Consola Node.js
    echo ========================================================
    node server.js
)
pause
