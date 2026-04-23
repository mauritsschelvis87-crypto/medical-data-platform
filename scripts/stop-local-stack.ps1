param(
    [int[]]$Ports = @(8081, 4200, 8080, 8001)
)

$root = "C:\Users\mauri\Projects\medical-data-platform"
$backendDir = Join-Path $root "apps\backend"
$frontendDir = Join-Path $root "apps\frontend"
$logDir = Join-Path $root "runtime-logs"
$pidFiles = @(
    (Join-Path $logDir "backend-full.wrapper.pid"),
    (Join-Path $logDir "frontend-full.wrapper.pid")
)

$processIds = New-Object System.Collections.Generic.List[int]

function Add-ProcessId([int]$ProcessId) {
    if ($ProcessId -and $ProcessId -gt 0 -and -not $processIds.Contains($ProcessId)) {
        [void]$processIds.Add($ProcessId)
    }
}

foreach ($pidFile in $pidFiles) {
    if (Test-Path $pidFile) {
        $pidValue = Get-Content -Path $pidFile -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($pidValue -match '^\d+$') {
            Add-ProcessId([int]$pidValue)
        }
        Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
    }
}

foreach ($port in $Ports) {
    $connections = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    foreach ($connection in $connections) {
        Add-ProcessId([int]$connection.OwningProcess)
    }
}

$candidateProcesses = Get-CimInstance Win32_Process -ErrorAction SilentlyContinue | Where-Object {
    $_.Name -in @("java.exe", "node.exe", "npm.cmd", "cmd.exe")
}

foreach ($process in $candidateProcesses) {
    $commandLine = [string]$process.CommandLine
    if ([string]::IsNullOrWhiteSpace($commandLine)) {
        continue
    }

    $isBackendProcess =
        $commandLine.Contains($backendDir) -and (
            $commandLine.Contains("spring-boot:run") -or
            $commandLine.Contains("com.mauri.backend.BackendApplication") -or
            $commandLine.Contains("target\\classes")
        )

    $isFrontendProcess =
        $commandLine.Contains($frontendDir) -and (
            $commandLine.Contains("ng.js") -or
            $commandLine.Contains("npm-cli.js") -or
            $commandLine.Contains("ng serve")
        )

    if ($isBackendProcess -or $isFrontendProcess) {
        Add-ProcessId([int]$process.ProcessId)
    }
}

foreach ($processId in $processIds) {
    try {
        Stop-Process -Id $processId -Force -ErrorAction Stop
        Write-Output "Stopped process $processId"
    } catch {
        Write-Output "Could not stop process $processId"
    }
}

for ($attempt = 0; $attempt -lt 8; $attempt++) {
    $inUse = $false
    foreach ($port in $Ports) {
        if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) {
            $inUse = $true
            break
        }
    }
    if (-not $inUse) {
        break
    }
    Start-Sleep -Seconds 1
}

foreach ($port in $Ports) {
    $inUse = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    if ($inUse) {
        $owners = ($inUse | Select-Object -ExpandProperty OwningProcess | Sort-Object -Unique) -join ","
        Write-Output "Port $port still in use by $owners"
    } else {
        Write-Output "Port $port free"
    }
}
