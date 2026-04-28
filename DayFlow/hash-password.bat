@echo off
REM Batch script to hash a password using the compiled Java class
REM Usage: hash-password.bat "YourPassword"

setlocal

set PASSWORD=%~1
if "%PASSWORD%"=="" set PASSWORD=Admin123!

echo === Password Hasher ===
echo.
echo Password: %PASSWORD%
echo.
echo Compiling Java classes...

REM Compile the project first
call mvn -q compile

if errorlevel 1 (
    echo Compilation failed!
    exit /b 1
)

echo Running HashPassword utility...
echo.

REM Run the HashPassword class
java -cp "target/classes;%USERPROFILE%\.m2\repository\org\mindrot\jbcrypt\0.4\jbcrypt-0.4.jar" HashPassword "%PASSWORD%"

echo.
echo === Done ===

endlocal
