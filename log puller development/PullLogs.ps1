Add-Type -AssemblyName System.Windows.Forms

# WinAPI to set a window to foreground
Add-Type @"
using System;
using System.Runtime.InteropServices;
public class WinAPI {
    [DllImport("user32.dll")]
    public static extern bool SetForegroundWindow(IntPtr hWnd);
}
"@

function Show-TopmostMessageBox {
    param([string]$text, [string]$caption)
    $form = New-Object System.Windows.Forms.Form
    $form.TopMost = $true
    $form.WindowState = 'Minimized'
    $form.ShowInTaskbar = $false
    $form.Show()
    [System.Windows.Forms.MessageBox]::Show($form, $text, $caption)
    $form.Close(); $form.Dispose()
}

function Open-Folder {
    param([string]$path)
    $shell = New-Object -ComObject "Shell.Application"
    $win = $shell.Windows() | Where-Object {
        try { $_.Document.Folder.Self.Path -eq $path } catch { $false }
    }
    if ($win) {
        [WinAPI]::SetForegroundWindow([IntPtr]$win.HWND) | Out-Null
    } else {
        Start-Process explorer.exe $path
    }
}

function Download-And-Extract-Adb {
    param (
        [string]$downloadUrl,
        [string]$extractTargetPath
    )

    $tempZip = Join-Path $env:TEMP "platform-tools.zip"
    Write-Host "Downloading Platform Tools from $downloadUrl ..."
    try {
        Invoke-WebRequest -Uri $downloadUrl -OutFile $tempZip -ErrorAction Stop
    } catch {
        Show-TopmostMessageBox "Failed to download ADB. Please check your internet connection and try again." "Download Error"
        exit
    }

    Write-Host "Extracting ADB and dependencies..."
    Expand-Archive -Path $tempZip -DestinationPath $env:TEMP -Force

    $adbSource = Join-Path $env:TEMP "platform-tools"
    $adbDest   = Split-Path $extractTargetPath

    if (-not (Test-Path $adbDest)) {
        New-Item -ItemType Directory -Path $adbDest | Out-Null
    }

    Copy-Item "$adbSource\*" $adbDest -Recurse -Force

    Remove-Item $tempZip
}

function Test-InternetConnection {
    try {
        $req = [System.Net.WebRequest]::Create("https://www.google.com")
        $req.Timeout = 3000
        $res = $req.GetResponse()
        $res.Close()
        return $true
    } catch {
        return $false
    }
}

# Ask where to save logs
$dialog = New-Object System.Windows.Forms.FolderBrowserDialog
$dialog.Description = "Choose where to save the wpilog files"
if ($dialog.ShowDialog() -ne "OK") { exit }
$DestPath = $dialog.SelectedPath

# Paths
$BasePath = Split-Path -Parent ([System.Reflection.Assembly]::GetEntryAssembly().Location)
$AdbPath  = Join-Path $BasePath "adb\adb.exe"

# Check if adb.exe exists
if (-not (Test-Path $AdbPath)) {
    if (-not (Test-InternetConnection)) {
        Show-TopmostMessageBox "ADB is missing and cannot be downloaded because you are not connected to the internet.`n`nYou only need an internet connection the first time you run this script — after that, it will work offline." "ADB Not Found"
        exit
    }

    $platformToolsUrl = "https://dl.google.com/android/repository/platform-tools-latest-windows.zip"
    Download-And-Extract-Adb -downloadUrl $platformToolsUrl -extractTargetPath $AdbPath
}

# Find remote .wpilog files
$AllFiles = & $AdbPath shell "find /sdcard/Android/data -type f -name '*.wpilog' 2>/dev/null" |
            ForEach-Object { $_.Trim() }
if (-not $AllFiles) {
    Show-TopmostMessageBox "No .wpilog files found in Android/data" "FTC Log Puller"
    exit
}

# Pull, skipping ones already here
foreach ($remote in $AllFiles) {
    $fileName  = [IO.Path]::GetFileName($remote)
    $localFile = Join-Path $DestPath $fileName
    if (Test-Path $localFile) {
        Write-Host "Skipping $fileName (already exists)"
        continue
    }
    Write-Host "Pulling $fileName"
    & $AdbPath pull $remote $DestPath
}

# Open (or focus) folder & notify
Open-Folder $DestPath
Show-TopmostMessageBox "Done! Logs saved to:`n$DestPath" "FTC Log Puller"
