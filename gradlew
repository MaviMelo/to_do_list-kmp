#!/bin/sh

# Wrapper mínimo: baixa Gradle 8.7 se necessário e executa.
# Gerado manualmente porque o wrapper oficial requer gradle instalado.
set -e

APP_HOME=$(cd "$(dirname "$0")" && pwd)
GRADLE_VERSION=8.7
GRADLE_ZIP="$HOME/.gradle/wrapper/dists/gradle-$GRADLE_VERSION-bin.zip"
GRADLE_DIR="$HOME/.gradle/wrapper/dists/gradle-$GRADLE_VERSION"

mkdir -p "$HOME/.gradle/wrapper/dists"

if [ ! -x "$HOME/.gradle/wrapper/dists/gradle-$GRADLE_VERSION/bin/gradle" ]; then
    echo "Baixando Gradle $GRADLE_VERSION..."
    wget -qO "$GRADLE_ZIP" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
    unzip -qo "$GRADLE_ZIP" -d "$HOME/.gradle/wrapper/dists/"
    rm -f "$GRADLE_ZIP"
fi

exec "$HOME/.gradle/wrapper/dists/gradle-$GRADLE_VERSION/bin/gradle" -p "$APP_HOME" "$@"
