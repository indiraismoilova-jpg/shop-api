$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "Set-LocalEnvironment.ps1")
$root = Split-Path $PSScriptRoot -Parent
$composeArgs = @("compose", "--project-directory", $root, "-f", (Join-Path $root "compose.yaml"))
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)

& docker @composeArgs up -d
if ($LASTEXITCODE -ne 0) { throw "Docker Compose startup failed" }

$ready = $false
for ($attempt = 0; $attempt -lt 30; $attempt++) {
    & docker @composeArgs exec -T postgres pg_isready -U $env:DB_USERNAME -d shopdb | Out-Null
    if ($LASTEXITCODE -eq 0) { $ready = $true; break }
    Start-Sleep -Seconds 1
}
if (-not $ready) { throw "PostgreSQL did not become ready" }

function Invoke-DbScalar([string]$Sql) {
    $result = & docker @composeArgs exec -T postgres psql -U $env:DB_USERNAME -d shopdb -v ON_ERROR_STOP=1 -q -t -A -c $Sql
    if ($LASTEXITCODE -ne 0) { throw "Database query failed" }
    return ($result -join "`n").Trim()
}

$hasUsers = Invoke-DbScalar "SELECT to_regclass('public.users') IS NOT NULL;"
$hasLedger = Invoke-DbScalar "SELECT to_regclass('public.schema_migrations') IS NOT NULL;"
if ($hasUsers -eq "t" -and $hasLedger -eq "f") {
    throw "This database was initialized manually. Keep it as-is. For a clean verification, load the environment with -Port 5434 -ProjectName shopverify, then run this script again."
}

$null = Invoke-DbScalar "CREATE TABLE IF NOT EXISTS schema_migrations (filename VARCHAR(255) PRIMARY KEY, checksum VARCHAR(64) NOT NULL, applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP);"

$sqlFolder = Join-Path $root "src/main/resources/db"
$files = @(Get-ChildItem $sqlFolder -File | Where-Object { $_.Name -match '^[0-9]{3}_[A-Za-z0-9_]+\.sql$' } | Sort-Object Name)
if ($files.Count -ne 13) { throw "Expected 13 numbered SQL files, found $($files.Count)" }

foreach ($file in $files) {
    $hash = (Get-FileHash $file.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    $stored = Invoke-DbScalar "SELECT checksum FROM schema_migrations WHERE filename = '$($file.Name)';"
    if ($stored) {
        if ($stored -ne $hash) { throw "Applied migration has changed: $($file.Name)" }
        Write-Host "Already applied: $($file.Name)"
        continue
    }

    $body = Get-Content -Raw -Encoding UTF8 -ErrorAction Stop $file.FullName
    $body = [regex]::Replace($body, '(?is)^\s*BEGIN\s*;\s*', '')
    $body = [regex]::Replace($body, '(?is)\s*COMMIT\s*;\s*$', '')
    $payload = "BEGIN;`n$body`nINSERT INTO schema_migrations (filename, checksum) VALUES ('$($file.Name)', '$hash');`nCOMMIT;"

    Write-Host "Applying: $($file.Name)"
    $payload | & docker @composeArgs exec -T postgres psql -U $env:DB_USERNAME -d shopdb -v ON_ERROR_STOP=1 -q
    if ($LASTEXITCODE -ne 0) { throw "Migration failed and was rolled back: $($file.Name)" }
}

Write-Host "All 13 SQL files are applied."
& docker @composeArgs exec -T postgres psql -U $env:DB_USERNAME -d shopdb -c "SELECT 'categories' AS entity, COUNT(*) FROM categories UNION ALL SELECT 'brands', COUNT(*) FROM brands UNION ALL SELECT 'products', COUNT(*) FROM products UNION ALL SELECT 'users', COUNT(*) FROM users UNION ALL SELECT 'orders', COUNT(*) FROM orders UNION ALL SELECT 'reviews', COUNT(*) FROM reviews UNION ALL SELECT 'promo_codes', COUNT(*) FROM promo_codes;"
if ($LASTEXITCODE -ne 0) { throw "Seed verification query failed" }
