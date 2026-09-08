@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0.mvn\wrapper\Run-Maven.ps1" %*
exit /b %ERRORLEVEL%
