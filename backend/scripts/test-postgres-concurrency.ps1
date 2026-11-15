param(
    [string]$PgBin = 'C:\Program Files\PostgreSQL\17\bin',
    [string]$JavaHome = 'C:\Program Files\Java\jdk-21',
    [int]$Port = 55439,
    [ValidatePattern('^[A-Za-z0-9_,]+$')][string]$TestPattern
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$testRoot = Join-Path $projectRoot '.local'
$testData = Join-Path $testRoot 'postgres-tests'
$buildDirectory = [IO.Path]::GetFullPath((Join-Path $testRoot 'backend-build'))
if (!$buildDirectory.StartsWith(([IO.Path]::GetFullPath($projectRoot) + [IO.Path]::DirectorySeparatorChar), [StringComparison]::OrdinalIgnoreCase)) {
    throw 'La salida de compilación debe quedar dentro del proyecto.'
}
$startedHere = $false
$names = @('JAVA_HOME', 'PATH', 'MAVEN_OPTS', 'BC_TEST_DB_URL', 'BC_TEST_DB_USER', 'BC_TEST_DB_PASSWORD')
$previous = @{}
foreach ($name in $names) { $previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
try {
    New-Item -ItemType Directory -Force $testRoot | Out-Null
    if (!(Test-Path "$testData\PG_VERSION")) {
        & "$PgBin\initdb.exe" -D $testData -U bc_test_admin -A trust --encoding=UTF8 --locale=C
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear el clúster de pruebas.' }
    }
    & "$PgBin\pg_ctl.exe" -D $testData status
    if ($LASTEXITCODE -ne 0) {
        if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) {
            throw "El puerto $Port está ocupado. No se modificó el proceso existente."
        }
        $startedHere = $true
        $control = Start-Process -FilePath "$PgBin\pg_ctl.exe" -ArgumentList @(
            '-D', ('"' + $testData + '"'), '-l', ('"' + "$testRoot\postgres-tests.log" + '"'),
            '-o', ('"-h 127.0.0.1 -p ' + $Port + '"'), '-w', 'start'
        ) -WindowStyle Hidden -PassThru -RedirectStandardOutput "$testRoot\postgres-tests-control.log" -RedirectStandardError "$testRoot\postgres-tests-control-error.log"
        if (!$control.WaitForExit(30000)) { throw 'PostgreSQL de pruebas no inició dentro de 30 segundos.' }
        $control.Refresh()
        if ($control.ExitCode -ne 0) { throw 'No se pudo iniciar PostgreSQL de pruebas. Revisar los logs locales.' }
    }
    $exists = & "$PgBin\psql.exe" -w -h 127.0.0.1 -p $Port -U bc_test_admin -d postgres -Atc "SELECT count(*) FROM pg_database WHERE datname='bc_concurrency_test'"
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo acceder a la instancia de pruebas.' }
    if ($exists -eq '0') {
        & "$PgBin\createdb.exe" -w -h 127.0.0.1 -p $Port -U bc_test_admin bc_concurrency_test
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear la base descartable.' }
    }
    $env:JAVA_HOME = $JavaHome
    $env:PATH = "$JavaHome\bin;$env:PATH"
    $env:MAVEN_OPTS = '-Xmx512m'
    $env:BC_TEST_DB_URL = "jdbc:postgresql://127.0.0.1:$Port/bc_concurrency_test"
    $env:BC_TEST_DB_USER = 'bc_test_admin'
    $env:BC_TEST_DB_PASSWORD = ''
    Push-Location "$projectRoot\backend"
    try {
        if ($TestPattern) { & .\mvnw.cmd -B "-Dbc.build.directory=$buildDirectory" "-Dtest=$TestPattern" test }
        else { & .\mvnw.cmd -B "-Dbc.build.directory=$buildDirectory" clean verify }
        if ($LASTEXITCODE -ne 0) { throw 'Falló la verificación del backend. Revisar los reportes de Maven.' }
    } finally { Pop-Location }
} finally {
    if ($startedHere) { & "$PgBin\pg_ctl.exe" -D $testData -m fast -w stop }
    foreach ($name in $names) { [Environment]::SetEnvironmentVariable($name, $previous[$name], 'Process') }
}
