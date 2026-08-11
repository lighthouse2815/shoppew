[CmdletBinding()]
param(
    [switch]$InfrastructureOnly
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot

Push-Location $repoRoot
try {
    docker compose up -d postgres redis minio minio-init mailpit
    if ($InfrastructureOnly) {
        docker compose ps
        return
    }

    Write-Host 'Infrastructure is running. Starting backend in the current terminal.'
    & (Join-Path $repoRoot 'backend\mvnw.cmd') spring-boot:run
}
finally {
    Pop-Location
}
