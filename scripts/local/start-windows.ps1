$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$localRoot = Join-Path $projectRoot '.local'
$config = Get-Content (Join-Path $localRoot 'windows-config.json') -Raw | ConvertFrom-Json
$pgBin = $config.pgBin
& "$pgBin\pg_isready.exe" -h 127.0.0.1 -p 55432 -q
if ($LASTEXITCODE -ne 0) {
    & "$pgBin\pg_ctl.exe" -D "$localRoot\postgres" -l "$localRoot\postgres.log" -w start
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo iniciar PostgreSQL.' }
}
$env:DB_URL = 'jdbc:postgresql://127.0.0.1:55432/base_conocimiento'
$env:DB_USERNAME = 'base_conocimiento'
$env:DB_PASSWORD = $config.dbPassword
$env:JWT_SECRET = $config.jwtSecret
$env:STORAGE_PATH = Join-Path $localRoot 'storage'
$env:SERVER_ADDRESS = '127.0.0.1'
$env:CORS_ALLOWED_ORIGINS = 'http://localhost:5173,http://127.0.0.1:5173'
if (!(Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue)) {
    $jar = Join-Path $projectRoot 'backend\target\base_conocimiento-0.0.1-SNAPSHOT.jar'
    $backendProcess = Start-Process -FilePath "$($config.javaHome)\bin\java.exe" -ArgumentList @('-jar', ('"' + $jar + '"')) -WorkingDirectory "$projectRoot\backend" -WindowStyle Hidden -RedirectStandardOutput "$localRoot\backend.log" -RedirectStandardError "$localRoot\backend-error.log" -PassThru
    $backendProcess.Id | Set-Content "$localRoot\backend.pid"
}
if (!(Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue)) {
    $vite = Join-Path $projectRoot 'frontend\node_modules\vite\bin\vite.js'
    $frontendProcess = Start-Process -FilePath (Get-Command node.exe).Source -ArgumentList @(('"' + $vite + '"'), '--host', '127.0.0.1', '--port', '5173', '--strictPort') -WorkingDirectory "$projectRoot\frontend" -WindowStyle Hidden -RedirectStandardOutput "$localRoot\frontend.log" -RedirectStandardError "$localRoot\frontend-error.log" -PassThru
    $frontendProcess.Id | Set-Content "$localRoot\frontend.pid"
}
Write-Output 'Frontend: http://localhost:5173 | Backend: http://127.0.0.1:8080'
Write-Output "Logs: $localRoot"
