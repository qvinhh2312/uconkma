@echo off
setlocal
powershell -ExecutionPolicy Bypass -File "%~dp0run-test.ps1" %*
endlocal
