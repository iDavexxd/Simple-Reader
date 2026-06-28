@echo off
setlocal

:: ==========================================
:: CONFIGURACION DEL PROYECTO
:: ==========================================
set APP_VERSION=1.0.3
set APP_NAME=Simple Reader
set APP_DESC=Lector de mangas basico
set FAT_JAR=simplereader-app-%APP_VERSION%-fat.jar
:: ==========================================

cd /d "%~dp0"

echo =====================================
echo   Construyendo %APP_NAME% v%APP_VERSION%
echo =====================================

echo -^> Ejecutando Maven...
call mvn clean install

if %ERRORLEVEL% neq 0 (
    echo Error: La compilacion fallo. Abortando la creacion del paquete.
    exit /b %ERRORLEVEL%
)

echo -^> Maven termino exitosamente.
echo.
echo -^> Creando el instalador .exe para Windows...

jpackage ^
  --type exe ^
  --name "%APP_NAME%" ^
  --app-version "%APP_VERSION%" ^
  --description "%APP_DESC%" ^
  --vendor "David" ^
  --input simplereader-app/target ^
  --main-jar "%FAT_JAR%" ^
  --main-class app.simplereader.Main ^
  --icon simplereader-app/src/main/resources/icons/app_icon.ico ^
  --win-shortcut ^
  --win-menu ^
  --win-dir-chooser ^
  --dest dist

if %ERRORLEVEL% equ 0 (
    echo =====================================
    echo Paquete .exe creado exitosamente en 'dist'.
    echo =====================================
) else (
    echo Error: Ocurrio un problema al usar jpackage.
    exit /b %ERRORLEVEL%
)
endlocal
