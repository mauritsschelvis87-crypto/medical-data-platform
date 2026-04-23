$ErrorActionPreference = "Stop"

$root = "C:\Users\mauri\Projects\medical-data-platform"
$backendDir = Join-Path $root "apps\backend"
$frontendDir = Join-Path $root "apps\frontend"
$backendCmd = Join-Path $backendDir "mvnw.cmd"
$npmCmd = "npm.cmd"
$logDir = Join-Path $root "runtime-logs"
$backendOut = Join-Path $logDir "backend-full.out.log"
$backendErr = Join-Path $logDir "backend-full.err.log"
$frontendOut = Join-Path $logDir "frontend-full.out.log"
$frontendErr = Join-Path $logDir "frontend-full.err.log"
$backendPidFile = Join-Path $logDir "backend-full.wrapper.pid"
$frontendPidFile = Join-Path $logDir "frontend-full.wrapper.pid"

New-Item -ItemType Directory -Force -Path $logDir | Out-Null

powershell -ExecutionPolicy Bypass -File (Join-Path $root "scripts\stop-local-stack.ps1") | Out-Host

Remove-Item $backendOut, $backendErr, $frontendOut, $frontendErr -ErrorAction SilentlyContinue
Remove-Item $backendPidFile, $frontendPidFile -ErrorAction SilentlyContinue

$backendProcess = Start-Process -FilePath $backendCmd `
    -ArgumentList "spring-boot:run", "-Dspring-boot.run.profiles=full", "-Dspring-boot.run.fork=false" `
    -WorkingDirectory $backendDir `
    -RedirectStandardOutput $backendOut `
    -RedirectStandardError $backendErr `
    -PassThru

Set-Content -Path $backendPidFile -Value $backendProcess.Id

$deadline = (Get-Date).AddMinutes(2)
do {
    Start-Sleep -Seconds 3
    $backendUp = Test-NetConnection localhost -Port 8081 -WarningAction SilentlyContinue | Select-Object -ExpandProperty TcpTestSucceeded
    if ($backendUp) {
        break
    }
} while ((Get-Date) -lt $deadline)

if (-not $backendUp) {
    Write-Output "Backend did not start on 8081 with full profile."
    Get-Content -Path $backendOut -Tail 80
    exit 1
}

$frontendProcess = Start-Process -FilePath $npmCmd `
    -ArgumentList "start", "--", "--host", "0.0.0.0" `
    -WorkingDirectory $frontendDir `
    -RedirectStandardOutput $frontendOut `
    -RedirectStandardError $frontendErr `
    -PassThru

Set-Content -Path $frontendPidFile -Value $frontendProcess.Id

$deadline = (Get-Date).AddMinutes(2)
do {
    Start-Sleep -Seconds 3
    $frontendUp = Test-NetConnection localhost -Port 4200 -WarningAction SilentlyContinue | Select-Object -ExpandProperty TcpTestSucceeded
    if ($frontendUp) {
        break
    }
} while ((Get-Date) -lt $deadline)

if (-not $frontendUp) {
    Write-Output "Frontend did not start on 4200."
    Get-Content -Path $frontendOut -Tail 80
    exit 1
}

Write-Output "Full stack started."
Write-Output "Backend:  http://localhost:8081"
Write-Output "Frontend: http://localhost:4200"
Write-Output "Profile:  full"
Write-Output "Logs:     $logDir"
