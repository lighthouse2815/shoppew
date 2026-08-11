[CmdletBinding(SupportsShouldProcess, ConfirmImpact = 'High')]
param(
    [switch]$Force
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $repoRoot 'docker-compose.yml'

if (-not (Test-Path -LiteralPath $composeFile)) {
    throw "Compose file not found: $composeFile"
}

$confirmed = $Force -or $PSCmdlet.ShouldProcess(
    'shoppew Docker Compose database and local service volumes',
    'Delete local PostgreSQL, Redis, MinIO, and Mailpit data and recreate services'
)
if (-not $confirmed) {
    return
}

Push-Location $repoRoot
try {
    docker compose down --volumes
    docker compose up -d postgres redis minio minio-init mailpit
    docker compose ps
}
finally {
    Pop-Location
}
