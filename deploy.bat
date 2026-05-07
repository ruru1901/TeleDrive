@echo off
set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
set ADB=%ANDROID_HOME%\platform-tools\adb.exe
call gradlew.bat :app:assembleDebug --no-daemon --console=plain --max-workers=2
if %ERRORLEVEL% neq 0 ( echo BUILD FAILED & exit /b 1 )
"%ADB%" install -r app\build\outputs\apk\debug\app-debug.apk
if %ERRORLEVEL% neq 0 ( echo INSTALL FAILED & exit /b 1 )
"%ADB%" shell am start -n com.teledrive.android/.MainActivity
echo DONE
