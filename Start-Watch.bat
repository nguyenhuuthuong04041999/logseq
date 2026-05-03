@echo off
title Logseq Watch (de cua so nay mo)
cd /d "%~dp0"
set "JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot"
set "PATH=%JAVA_HOME%\bin;%USERPROFILE%\bin;%PATH%"
set "ELECTRON_RUN_AS_NODE="
set "ENABLE_PLUGINS=true"

echo ==========================================
echo  Logseq - Watch / Build nen
echo ==========================================
echo.
echo Lan dau build mat ~3 phut. De cua so nay mo trong khi
echo lam viec, no se tu rebuild khi ban sua code.
echo Khi thay "Build completed" cho :electron va :app, mo
echo "Run-Logseq.bat" de chay app.
echo.
call pnpm electron-watch
echo.
echo Watch da dung. Nhan phim bat ky de thoat...
pause >nul
