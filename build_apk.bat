@echo off
REM Evita "Couldn't delete R.jar" (archivo en uso): cierra IDE, detiene Gradle, limpia y compila.
echo ============================================
echo  BUILD APK - Evitar error R.jar bloqueado
echo ============================================
echo.
echo 1. Cierra Android Studio / Cursor si tienen el proyecto abierto.
echo 2. Cierra el Explorador de archivos si esta carpeta esta abierta.
echo.
pause

echo Deteniendo todos los daemons de Gradle...
call gradlew.bat --stop
timeout /t 3 /nobreak >nul

echo Limpiando build anterior...
call gradlew.bat clean
if errorlevel 1 (
    echo ERROR en clean. Prueba borrar manualmente la carpeta app\build
    pause
    exit /b 1
)
timeout /t 2 /nobreak >nul

echo Compilando APK debug...
call gradlew.bat assembleDebug
if errorlevel 1 (
    echo BUILD FALLIDO
    pause
    exit /b 1
)

echo.
echo BUILD OK. APK en: app\build\outputs\apk\debug\
pause
