# Error: Couldn't delete R.jar (archivo en uso)

## Causa

En Windows, el archivo `app\intermediates\...\R.jar` a veces queda **bloqueado** por otro proceso (Gradle daemon, Android Studio, antivirus o Explorador de archivos). Gradle no puede borrarlo y falla la compilación.

## Solución (en este orden)

### 1. Cerrar lo que pueda estar usando el proyecto

- **Cierra Android Studio o Cursor** si tienen este proyecto abierto.
- Cierra el **Explorador de archivos** si tienes abierta la carpeta del proyecto (por ejemplo `BLO` o `bloqueo_build`).

### 2. Usar el script que para Gradle, limpia y compila

En la carpeta del proyecto (donde está `gradlew.bat`):

```batch
build_apk.bat
```

Ese script hace: `gradlew --stop` → `gradlew clean` → `gradlew assembleDebug`.

### 3. Si compilas desde otra carpeta (ej. `bloqueo_build`)

Abre **CMD** o **PowerShell** en esa carpeta y ejecuta a mano:

```batch
gradlew.bat --stop
```

Espera 3 segundos. Luego:

```batch
gradlew.bat clean
gradlew.bat assembleDebug
```

### 4. Si sigue fallando: borrar carpetas de build a mano

1. Cierra todo (IDE, Explorador en esa ruta).
2. Ejecuta `gradlew.bat --stop`.
3. Borra estas carpetas si existen:
   - `app\build`
   - `app\intermediates`
4. Vuelve a compilar: `gradlew.bat assembleDebug`.

### 5. Antivirus

Si tienes antivirus que escanea en tiempo real, añade una **exclusión** para la carpeta del proyecto para evitar que bloquee archivos durante el build.
