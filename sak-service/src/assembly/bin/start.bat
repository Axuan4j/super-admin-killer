@echo off
setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "APP_HOME=%%~fI"
set "APP_DIR=%APP_HOME%\app"
set "LIB_DIR=%APP_HOME%\lib"
set "CONFIG_DIR=%APP_HOME%\config"
set "APP_JAR="

if not exist "%APP_DIR%\*.jar" (
    echo No application jar found under "%APP_DIR%"
    exit /b 1
)

for %%I in ("%APP_DIR%\*.jar") do (
    set "APP_JAR=%%~fI"
    goto :jarFound
)

:jarFound
if defined JAVA_HOME (
    set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
) else (
    set "JAVA_CMD=java"
)

set "JAVA_OPTS=%JAVA_OPTS%"
set "SPRING_OPTS=%SPRING_OPTS%"

"%JAVA_CMD%" %JAVA_OPTS% -cp "%APP_JAR%;%LIB_DIR%\*" com.superkiller.backend.BackendApplication --spring.config.additional-location=optional:file:%CONFIG_DIR%/ %SPRING_OPTS% %*
exit /b %ERRORLEVEL%
