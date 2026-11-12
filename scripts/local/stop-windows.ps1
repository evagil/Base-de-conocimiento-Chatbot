$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$localRoot = Join-Path $projectRoot '.local'
foreach ($component in @('frontend', 'backend')) {
    $pidPath = Join-Path $localRoot "$component.pid"
    if (Test-Path $pidPath) {
        $savedProcessId = [int](Get-Content $pidPath)
        $process = Get-CimInstance Win32_Process -Filter "ProcessId=$savedProcessId" -ErrorAction SilentlyContinue
        if ($process -and $process.CommandLine.Contains($projectRoot)) { Stop-Process -Id $savedProcessId }
        Remove-Item -LiteralPath $pidPath
    }
}
$config = Get-Content "$localRoot\windows-config.json" -Raw | ConvertFrom-Json
& "$($config.pgBin)\pg_ctl.exe" -D "$localRoot\postgres" -m fast -w stop
