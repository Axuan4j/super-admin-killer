@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "SCRIPT_DIR=%~dp0"
set "ENV_FILE=%SCRIPT_DIR%env\superkiller.env"

if not "%~1"=="" (
    if /I not "%~1"=="--" (
        set "ENV_FILE=%~1"
        shift
    )
)

if not exist "%ENV_FILE%" (
    echo Environment file not found: %ENV_FILE%
    echo Copy scripts\env\superkiller.env.example to scripts\env\superkiller.env first.
    exit /b 1
)

set /a LOADED_COUNT=0

for /f "usebackq tokens=1* delims==" %%A in ("%ENV_FILE%") do (
    set "KEY=%%A"
    set "VALUE=%%B"
    if not "!KEY!"=="" (
        if not "!KEY:~0,1!"=="#" (
            set "!KEY!=!VALUE!"
            set /a LOADED_COUNT+=1
        )
    )
)

echo Loaded !LOADED_COUNT! environment variables from %ENV_FILE%

if "%~1"=="" (
    echo If you want to keep variables in the current CMD session, run:
    echo call scripts\import-env.bat
    exit /b 0
)

if /I "%~1"=="--" shift
if not "%~1"=="" (
    call %*
    exit /b %ERRORLEVEL%
)

exit /b 0
