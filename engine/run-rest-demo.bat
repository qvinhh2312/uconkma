@echo off
setlocal
powershell -ExecutionPolicy Bypass -File "%~dp0run-rest-demo.ps1" %*
endlocal
