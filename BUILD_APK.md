# Generar APK de Stay Focused

Esta guía explica cómo compilar la aplicación Stay Focused (Kivy) en un APK para Android.

## Requisitos previos

**Buildozer solo funciona en Linux.** En Windows tienes estas opciones:

### Opción 1: WSL2 (recomendado en Windows)

1. Instala WSL2 con Ubuntu:
   ```powershell
   wsl --install -d Ubuntu
   ```

2. Abre Ubuntu (WSL) e instala dependencias:
   ```bash
   sudo apt update
   sudo apt install -y python3-pip python3-venv build-essential ccache \
       git zip unzip openjdk-17-jdk autoconf libtool pkg-config \
       zlib1g-dev libncurses5-dev libncursesw5-dev libtinfo5 cmake libffi-dev \
       libssl-dev
   ```

3. Instala Buildozer:
   ```bash
   pip3 install --user buildozer
   pip3 install --user cython
   ```

4. Entra en la carpeta del proyecto (ruta de Windows en WSL):
   ```bash
   cd /mnt/c/Users/josep/OneDrive/Escritorio/Bloqueo
   ```

5. Compila el APK:
   ```bash
   buildozer android debug
   ```

   El APK se generará en `bin/stayfocused-1.0.0-arm64-v8a_armeabi-v7a-debug.apk`

### Opción 2: Pydroid 3 (Android)

Puedes ejecutar la app directamente en Pydroid 3:

1. Instala Pydroid 3 en tu móvil
2. Instala Kivy desde el gestor de paquetes de Pydroid
3. Copia `main.py` y ejecútalo

### Opción 3: Docker (alternativa multiplataforma)

```bash
docker run --rm -v $(pwd):/home/user/hostcwd kivy/buildozer android debug
```

## Comandos útiles de Buildozer

| Comando | Descripción |
|---------|-------------|
| `buildozer android debug` | Compila APK de depuración |
| `buildozer android release` | Compila APK de release (requiere firma) |
| `buildozer android clean` | Limpia la compilación anterior |
| `buildozer android logcat` | Ver logs del dispositivo conectado |

## Permisos de la app

La app solicita permisos típicos de Stay Focused:

- **RECEIVE_BOOT_COMPLETED**: Ejecutarse al iniciar el teléfono
- **FOREGROUND_SERVICE**: Servicio en primer plano
- **PACKAGE_USAGE_STATS**: Estadísticas de uso de apps
- **QUERY_ALL_PACKAGES**: Listar aplicaciones instaladas
- **INTERNET**: Para futuras funciones de red

## Nota sobre bloqueo real de apps

El bloqueo real de aplicaciones en Android requiere **Accessibility Service** o **Device Admin**, que implican código Java/Kotlin nativo. Esta versión Kivy gestiona la **configuración y estadísticas**; para bloqueo en tiempo real se necesitaría integrar un servicio nativo Android.
