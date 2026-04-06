[app]
title = Stay Focused
package.name = stayfocused
package.domain = com.stayfocused
source.dir = .
source.include_exts = py,png,jpg,kv,atlas,json
source.exclude_dirs = app,gradle,build,.gradle,.idea,bin,venv,__pycache__
version = 1.0.0
requirements = python3,kivy
orientation = portrait
fullscreen = 0

# Android - Permisos para app tipo Stay Focused
android.permissions = INTERNET,RECEIVE_BOOT_COMPLETED,FOREGROUND_SERVICE,REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
android.api = 33
android.minapi = 21
android.archs = arm64-v8a,armeabi-v7a
android.allow_backup = True
android.wakelock = True

# Icono - usar el existente del proyecto Android si existe
# icon.filename = %(source.dir)s/app/src/main/res/mipmap-hdpi/ic_launcher.png

[buildozer]
log_level = 2
warn_on_root = 1
