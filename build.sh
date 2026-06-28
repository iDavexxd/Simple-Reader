#!/bin/bash

# ==========================================
# CONFIGURACIÓN DEL PROYECTO
# ==========================================
APP_VERSION="1.1.0"
APP_NAME="Simple Reader"
APP_DESC="Lector de mangas básico"
PKG_NAME="simplereader"
FAT_JAR="simplereader-app-${APP_VERSION}-fat.jar"
# ==========================================

cd "$(dirname "$0")"

echo "====================================="
echo "  Construyendo $APP_NAME v$APP_VERSION"
echo "====================================="

echo "-> Ejecutando Maven..."
mvn clean install

if [ $? -ne 0 ]; then
  echo "Error: La compilación falló. Abortando la creación del paquete."
  exit 1
fi

echo "-> Maven terminó exitosamente."
echo ""

# Detectar el sistema operativo
OS="$(uname -s)"
echo "-> Sistema operativo detectado: $OS"

if [ "$OS" = "Linux" ]; then
    echo "-> Creando el instalador .deb para Linux..."
    jpackage \
      --type deb \
      --name "$APP_NAME" \
      --linux-package-name "$PKG_NAME" \
      --app-version "$APP_VERSION" \
      --description "$APP_DESC" \
      --vendor "David" \
      --input simplereader-app/target \
      --main-jar "$FAT_JAR" \
      --main-class app.simplereader.Main \
      --icon simplereader-app/src/main/resources/icons/app_icon.png \
      --linux-shortcut \
      --linux-menu-group "Graphics;Education" \
      --dest dist

elif [[ "$OS" == *"MINGW"* ]] || [[ "$OS" == *"CYGWIN"* ]]; then
    # Para usuarios de Windows que ejecutan build.sh desde Git Bash
    echo "-> Creando el instalador .exe para Windows..."
    jpackage \
      --type exe \
      --name "$APP_NAME" \
      --app-version "$APP_VERSION" \
      --description "$APP_DESC" \
      --vendor "David" \
      --input simplereader-app/target \
      --main-jar "$FAT_JAR" \
      --main-class app.simplereader.Main \
      --icon simplereader-app/src/main/resources/icons/app_icon.ico \
      --win-shortcut \
      --win-menu \
      --win-dir-chooser \
      --dest dist

elif [ "$OS" = "Darwin" ]; then
    echo "-> Creando el instalador .dmg para macOS..."
    jpackage \
      --type dmg \
      --name "$APP_NAME" \
      --app-version "$APP_VERSION" \
      --description "$APP_DESC" \
      --vendor "David" \
      --input simplereader-app/target \
      --main-jar "$FAT_JAR" \
      --main-class app.simplereader.Main \
      --dest dist
else
    echo "OS no soportado automáticamente por este script."
    exit 1
fi

if [ $? -eq 0 ]; then
  echo "====================================="
  echo "¡Paquete creado exitosamente en 'dist'!"
  echo "====================================="
else
  echo "Error: Ocurrió un problema al usar jpackage."
  exit 1
fi
