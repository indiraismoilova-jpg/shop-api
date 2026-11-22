param(
    [string]$Port = "",
    [string]$ProjectName = ""
)

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$envFile = Join-Path $root ".env"
$keys = @("DB_USERNAME", "DB_PASSWORD", "DB_PORT", "COMPOSE_PROJECT_NAME", "JWT_SECRET", "DB_URL")
$lines = @()
$values = @{}

if (Test-Path $envFile) {
    $lines = @(Get-Content -Encoding UTF8 $envFile)
    foreach ($line in $lines) {
        if ($line -match '^\s*([A-Z_]+)\s*=(.*)$' -and $keys -contains $Matches[1]) {
            $key = $Matches[1]
            $value = $Matches[2].Trim()
            if ($value.Length -ge 2 -and (
                ($value.StartsWith('"') -and $value.EndsWith('"')) -or
                ($value.StartsWith("'") -and $value.EndsWith("'")))) {
                $value = $value.Substring(1, $value.Length - 2)
            }
            $values[$key] = $value
        }
    }
}

foreach ($key in $keys) {
    $processValue = [Environment]::GetEnvironmentVariable($key, "Process")
    if (-not [string]::IsNullOrWhiteSpace($processValue)) {
        $values[$key] = $processValue
    }
}

if ($Port) { $values["DB_PORT"] = $Port }
if ($ProjectName) { $values["COMPOSE_PROJECT_NAME"] = $ProjectName }
if (-not $values["DB_PORT"]) { $values["DB_PORT"] = "5433" }
if (-not $values["DB_USERNAME"]) { $values["DB_USERNAME"] = "shop" }
if (-not $values["COMPOSE_PROJECT_NAME"]) { $values["COMPOSE_PROJECT_NAME"] = "shop" }

$parsedPort = 0
if (-not [int]::TryParse($values["DB_PORT"], [ref]$parsedPort) -or
    $parsedPort -lt 1024 -or $parsedPort -gt 65535) {
    throw "DB_PORT must be between 1024 and 65535"
}
if ($values["COMPOSE_PROJECT_NAME"] -notmatch '^[a-z0-9][a-z0-9_-]*$') {
    throw "Use lowercase letters, digits, hyphens or underscores in the Compose project name"
}

if (-not $values["DB_PASSWORD"]) {
    $containerIds = @(docker ps -aq --filter "label=com.docker.compose.project=$($values['COMPOSE_PROJECT_NAME'])" --filter "label=com.docker.compose.service=postgres")
    if ($LASTEXITCODE -ne 0) { throw "Start Docker Desktop first" }
    if ($containerIds.Count -eq 1 -and $containerIds[0]) {
        $containerEnv = docker inspect --format '{{json .Config.Env}}' $containerIds[0] | ConvertFrom-Json
        if ($LASTEXITCODE -ne 0) { throw "Could not inspect the local PostgreSQL container" }
        foreach ($entry in $containerEnv) {
            if ($entry.StartsWith("POSTGRES_PASSWORD=")) {
                $values["DB_PASSWORD"] = $entry.Substring("POSTGRES_PASSWORD=".Length)
            }
        }
    }
    if (-not $values["DB_PASSWORD"]) {
        $values["DB_PASSWORD"] = "LocalDb1!" + [guid]::NewGuid().ToString("N")
    }
}

if (-not $values["JWT_SECRET"]) {
    $bytes = New-Object byte[] 64
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    $values["JWT_SECRET"] = [Convert]::ToBase64String($bytes)
}

if ($Port -or -not $values["DB_URL"]) {
    $values["DB_URL"] = "jdbc:postgresql://127.0.0.1:$($values['DB_PORT'])/shopdb"
}

foreach ($key in $keys) {
    Set-Item -Path "Env:$key" -Value $values[$key]
    $matched = $false
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match "^\s*$key\s*=") {
            $lines[$i] = "$key=$($values[$key])"
            $matched = $true
        }
    }
    if (-not $matched) { $lines += "$key=$($values[$key])" }
}
[System.IO.File]::WriteAllLines($envFile, [string[]]$lines, [System.Text.UTF8Encoding]::new($false))
Write-Host "Local environment loaded. Local credentials are stored in the ignored .env file."
