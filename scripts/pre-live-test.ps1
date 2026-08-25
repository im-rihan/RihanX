# RihanX pre-live test gate — run before deploying to production.
# Usage:  .\scripts\pre-live-test.ps1
# Exit 0 = safe to deploy. Exit 1 = fix failures first.

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$Maven = Join-Path $Root ".tools\apache-maven-3.9.16\bin\mvn.cmd"
$JavaHome = "C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"

if (-not (Test-Path $Maven)) {
    Write-Host "Maven not found at $Maven" -ForegroundColor Red
    Write-Host "Run from repo root or install Maven to .tools/"
    exit 1
}

if (Test-Path $JavaHome) {
    $env:JAVA_HOME = $JavaHome
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  RihanX PRE-LIVE TEST GATE" -ForegroundColor Cyan
Write-Host "  Validates all 24 farm blueprints" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Push-Location $Root
try {
    & $Maven test
    $code = $LASTEXITCODE
} finally {
    Pop-Location
}

Write-Host ""
if ($code -eq 0) {
    Write-Host "PRE-LIVE GATE: PASSED" -ForegroundColor Green
    Write-Host "All farms validated (structure, hoppers, XP distance, cookers, slime seal, bamboo)."
    Write-Host "Jar: $Root\target\RihanX-1.0.0.jar"
    Write-Host ""
    Write-Host "Next: restart server, re-paste farms (/farm undo then /farm <id>)."
} else {
    Write-Host "PRE-LIVE GATE: FAILED" -ForegroundColor Red
    Write-Host "Fix test failures before going live. See target\surefire-reports\"
}
Write-Host ""

exit $code
