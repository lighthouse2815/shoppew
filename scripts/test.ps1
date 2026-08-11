[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot

Push-Location $repoRoot
try {
    & (Join-Path $repoRoot 'backend\mvnw.cmd') test
    pnpm --recursive --if-present lint
    pnpm --recursive --if-present typecheck
    pnpm --recursive --if-present test
}
finally {
    Pop-Location
}
