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
$iaConfigPath = Join-Path $localRoot 'ia-config.json'
if (Test-Path $iaConfigPath) {
    $iaConfig = Get-Content -LiteralPath $iaConfigPath -Raw | ConvertFrom-Json
    $iaVariables = @('IA_ENABLED', 'OLLAMA_BASE_URL', 'OLLAMA_CHAT_MODEL', 'OLLAMA_AGENT_MODEL',
        'OLLAMA_VISION_MODEL', 'OLLAMA_EMBEDDING_MODEL', 'OLLAMA_TIMEOUT', 'OLLAMA_CONNECT_TIMEOUT',
        'OLLAMA_CONTEXT_TOKENS', 'IA_MAX_PROMPT_CHARS', 'IA_MAX_OUTPUT_TOKENS',
        'IA_MAX_EMBEDDING_BATCH', 'IA_MAX_RESPONSE_BYTES')
    foreach ($property in $iaConfig.PSObject.Properties) {
        if ($property.Name -notin $iaVariables) { throw "Variable de IA no admitida: $($property.Name)" }
        [Environment]::SetEnvironmentVariable($property.Name, [string]$property.Value, 'Process')
    }
}
$documentConfigPath = Join-Path $localRoot 'documentos-config.json'
if (Test-Path $documentConfigPath) {
    $documentConfig = Get-Content -LiteralPath $documentConfigPath -Raw | ConvertFrom-Json
    $documentVariables = @('DOCUMENTS_ENABLED', 'DOCUMENT_TEMP_PATH', 'DOCUMENT_TIMEOUT',
        'DOCUMENT_MAX_FILE_SIZE', 'DOCUMENT_MAX_PAGES', 'DOCUMENT_MAX_OCR_PAGES', 'DOCUMENT_MAX_CHARS',
        'DOCUMENT_MAX_PIXELS', 'DOCUMENT_MAX_TEMP_BYTES', 'DOCUMENT_RENDER_DPI',
        'TESSERACT_COMMAND', 'TESSERACT_DATA_PATH', 'TESSERACT_LANGUAGES')
    foreach ($property in $documentConfig.PSObject.Properties) {
        if ($property.Name -notin $documentVariables) { throw "Variable documental no admitida: $($property.Name)" }
        [Environment]::SetEnvironmentVariable($property.Name, [string]$property.Value, 'Process')
    }
}
$indexConfigPath = Join-Path $localRoot 'indice-config.json'
if (Test-Path $indexConfigPath) {
    $indexConfig = Get-Content -LiteralPath $indexConfigPath -Raw | ConvertFrom-Json
    $indexVariables = @('RAG_INDEX_ENABLED', 'RAG_EMBEDDINGS_ENABLED', 'RAG_CHUNK_SIZE', 'RAG_CHUNK_OVERLAP',
        'RAG_MAX_CHUNKS', 'RAG_MAX_SOURCES', 'RAG_MAX_ATTACHMENTS', 'RAG_INDEX_TIMEOUT',
        'RAG_INDEX_INTERVAL', 'RAG_INDEX_INITIAL_DELAY')
    foreach ($property in $indexConfig.PSObject.Properties) {
        if ($property.Name -notin $indexVariables) { throw "Variable de indexación no admitida: $($property.Name)" }
        [Environment]::SetEnvironmentVariable($property.Name, [string]$property.Value, 'Process')
    }
}
if (!(Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue)) {
    $sourceJar = Join-Path $projectRoot 'backend\target\base_conocimiento-0.0.1-SNAPSHOT.jar'
    $jar = Join-Path $localRoot 'backend.jar'
    if (Test-Path $sourceJar) { Copy-Item -LiteralPath $sourceJar -Destination $jar -Force }
    if (!(Test-Path $jar)) { throw 'Falta el JAR del backend. Compilar antes de iniciar.' }
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
