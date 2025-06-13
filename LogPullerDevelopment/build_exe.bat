@echo off
powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-PS2EXE -inputFile .\PullLogs.ps1 -outputFile .\FTCLogPuller.exe"
powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-PS2EXE -inputFile .\PullAndDeleteLogs.ps1 -outputFile .\PullAndDeleteLogs.exe"
pause
