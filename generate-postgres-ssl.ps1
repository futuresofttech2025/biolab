# ════════════════════════════════════════════════════════════════════════════
# generate-postgres-ssl.ps1  —  Windows / PowerShell (uses Docker)
# ════════════════════════════════════════════════════════════════════════════

$ErrorActionPreference = "Stop"
$SSL_DIR = Join-Path $PWD "postgres\ssl"

Write-Host ""
Write-Host "BioLab — PostgreSQL SSL Certificate Generator" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host ""

# ── Docker check ─────────────────────────────────────────────────────────
Write-Host "Checking Docker..." -ForegroundColor Yellow
$dockerInfo = & docker info 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "  ERROR: Docker is not running. Start Docker Desktop first." -ForegroundColor Red
    exit 1
}
Write-Host "  Docker OK" -ForegroundColor Green

# ── Create ssl directory ──────────────────────────────────────────────────
New-Item -ItemType Directory -Path $SSL_DIR -Force | Out-Null
Write-Host "  Output: $SSL_DIR" -ForegroundColor Green
Write-Host ""

# ── Step 1: generate private key ─────────────────────────────────────────
Write-Host "[1/3] Generating private key..." -ForegroundColor Yellow
& docker run --rm -v "${SSL_DIR}:/ssl" alpine:3.19 `
    sh -c "apk add -q openssl && openssl genrsa -out /ssl/server.key 2048"
if ($LASTEXITCODE -ne 0) { Write-Host "FAILED" -ForegroundColor Red; exit 1 }
Write-Host "  Done" -ForegroundColor Green

# ── Step 2: generate self-signed certificate ──────────────────────────────
Write-Host "[2/3] Generating self-signed certificate..." -ForegroundColor Yellow
& docker run --rm -v "${SSL_DIR}:/ssl" alpine:3.19 `
    sh -c "apk add -q openssl && openssl req -new -x509 -key /ssl/server.key -out /ssl/server.crt -days 3650 -subj /CN=postgres"
if ($LASTEXITCODE -ne 0) { Write-Host "FAILED" -ForegroundColor Red; exit 1 }
Write-Host "  Done" -ForegroundColor Green

# ── Step 3: copy cert as CA root + fix permissions ────────────────────────
Write-Host "[3/3] Copying root CA and fixing permissions..." -ForegroundColor Yellow
& docker run --rm -v "${SSL_DIR}:/ssl" alpine:3.19 `
    sh -c "cp /ssl/server.crt /ssl/root.crt && chmod 600 /ssl/server.key && chmod 644 /ssl/server.crt /ssl/root.crt"
if ($LASTEXITCODE -ne 0) { Write-Host "FAILED" -ForegroundColor Red; exit 1 }
Write-Host "  Done" -ForegroundColor Green

# ── Verify ────────────────────────────────────────────────────────────────
Write-Host ""
$allOk = $true
foreach ($f in @("server.key", "server.crt", "root.crt")) {
    $path = Join-Path $SSL_DIR $f
    if (Test-Path $path) {
        $size = (Get-Item $path).Length
        Write-Host "  [OK] $f  ($size bytes)" -ForegroundColor Green
    } else {
        Write-Host "  [MISSING] $f" -ForegroundColor Red
        $allOk = $false
    }
}

if (-not $allOk) { Write-Host "`nERROR: Some files missing." -ForegroundColor Red; exit 1 }

Write-Host ""
Write-Host "Certificates ready. Run: docker-compose up -d" -ForegroundColor Green
Write-Host ""
