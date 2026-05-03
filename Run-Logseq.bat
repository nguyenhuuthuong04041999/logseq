@echo off
title Logseq App (dev)
cd /d "%~dp0"
set "JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot"
set "PATH=%JAVA_HOME%\bin;%USERPROFILE%\bin;%PATH%"
set "ELECTRON_RUN_AS_NODE="

echo ==========================================
echo  Logseq - Khoi dong app (che do dev)
echo ==========================================
echo.
echo Luu y: Watch (electron-watch) phai dang chay o cua so khac.
echo Neu chua chay, hay double-click "Start-Watch.bat" truoc va doi
echo den khi thay "Build completed" trong cua so do.
echo.
call pnpm dev-electron-app
echo.
echo App da dong. Nhan phim bat ky de thoat...
pause >nul
