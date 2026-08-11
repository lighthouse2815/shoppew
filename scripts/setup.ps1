[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot

if (-not (Test-Path -LiteralPath (Join-Path $repoRoot '.env'))) {
    Copy-Item -LiteralPath (Join-Path $repoRoot '.env.example') -Destination (Join-Path $repoRoot '.env')
    Write-Host 'Created .env from .env.example. These credentials are development-only.'
}

Push-Location $repoRoot
try {
    docker compose config --quiet
    pnpm install
    Write-Host 'shoppew dependencies are ready. Run scripts/dev.ps1 to start local services.'
}
finally {
    Pop-Location
}
