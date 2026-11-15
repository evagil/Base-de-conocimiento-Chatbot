param(
    [ValidatePattern('^http://(127\.0\.0\.1|localhost):[0-9]+/?$')]
    [string]$OllamaUrl = 'http://127.0.0.1:11434',
    [switch]$SoloIntegral
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$localRoot = Join-Path $projectRoot '.local'
$tesseract = Join-Path $localRoot 'tools\tesseract\tesseract.exe'
$tessdata = Join-Path $localRoot 'tools\tesseract\tessdata'
if (!(Test-Path -LiteralPath $tesseract)) { throw 'Falta Tesseract local. Revisar docs/USO_WINDOWS.md antes de validar.' }
$models = (Invoke-RestMethod -Uri ($OllamaUrl.TrimEnd('/') + '/api/tags') -TimeoutSec 10).models.name
foreach ($model in @('granite4.2:3b', 'embeddinggemma:300m')) {
    if ($model -notin $models) { throw "Falta el modelo local $model. La validación no descarga modelos." }
}
$names = @('BC_TEST_OLLAMA_URL', 'BC_TEST_INTEGRAL', 'BC_TEST_TESSERACT', 'BC_TEST_TESSDATA', 'NODE_OPTIONS')
$previous = @{}
foreach ($name in $names) { $previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
New-Item -ItemType Directory -Force -Path $localRoot | Out-Null
try {
    $env:BC_TEST_OLLAMA_URL = $OllamaUrl
    $env:BC_TEST_INTEGRAL = 'true'
    $env:BC_TEST_TESSERACT = $tesseract
    $env:BC_TEST_TESSDATA = $tessdata
    $env:NODE_OPTIONS = '--max-old-space-size=1536'
    Push-Location $projectRoot
    try {
        $started = [DateTime]::UtcNow
        $backendArgs = @{}
        if ($SoloIntegral) { $backendArgs.TestPattern = 'ValidacionIntegralIaTest' }
        & "$projectRoot\backend\scripts\test-postgres-concurrency.ps1" @backendArgs *> "$localRoot\validacion8-backend.log"
        if ($LASTEXITCODE -ne 0) { throw 'Falló el backend. Revisar .local/validacion8-backend.log.' }
        $reportPath = Join-Path $localRoot 'evaluacion-integral.json'
        if (!(Test-Path -LiteralPath $reportPath) -or (Get-Item -LiteralPath $reportPath).LastWriteTimeUtc -lt $started) { throw 'No se generó un informe integral nuevo. No se acepta un resultado de una corrida anterior.' }
        $report = Get-Content -LiteralPath $reportPath -Raw | ConvertFrom-Json
        if ($report.estado -ne 'APROBADO' -or $report.corpusVersion -ne 1 -or $report.consultas.Count -ne 8) { throw 'La evaluación integral no terminó aprobada. Revisar el informe local.' }
        if (!$SoloIntegral) {
            Push-Location "$projectRoot\frontend"
            try {
                & npm.cmd run lint *> "$localRoot\validacion8-frontend-lint.log"
                if ($LASTEXITCODE -ne 0) { throw 'Falló lint. Revisar el log local.' }
                & npm.cmd run test:coverage -- --maxWorkers=1 *> "$localRoot\validacion8-frontend-coverage.log"
                if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas frontend. Revisar el log local.' }
                & npm.cmd run build *> "$localRoot\validacion8-frontend-build.log"
                if ($LASTEXITCODE -ne 0) { throw 'Falló el build frontend. Revisar el log local.' }
            } finally { Pop-Location }
        }
        Write-Output 'Validación aprobada. Informe: .local/evaluacion-integral.json'
        if ($SoloIntegral) { Write-Output 'Modo seleccionado: solo evaluación integral; no reemplaza la suite completa ni su cobertura.' }
    } finally { Pop-Location }
} finally {
    foreach ($name in $names) { [Environment]::SetEnvironmentVariable($name, $previous[$name], 'Process') }
}
