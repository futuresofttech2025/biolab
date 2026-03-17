@echo off
REM ════════════════════════════════════════════════════════════════════
REM generate-postgres-ssl.bat
REM Windows equivalent of generate-postgres-ssl.sh
REM
REM PREREQUISITES:
REM   Git for Windows (includes openssl) OR
REM   Download openssl from https://slproweb.com/products/Win32OpenSSL.html
REM
REM Run this ONCE before docker-compose up
REM ════════════════════════════════════════════════════════════════════

set SSL_DIR=postgres\ssl

if not exist %SSL_DIR% mkdir %SSL_DIR%

echo Generating PostgreSQL SSL certificate...

openssl genrsa -out %SSL_DIR%\server.key 2048
if errorlevel 1 (
    echo ERROR: openssl not found. Install Git for Windows or OpenSSL for Windows.
    exit /b 1
)

openssl req -new -x509 ^
  -key    %SSL_DIR%\server.key ^
  -out    %SSL_DIR%\server.crt ^
  -days   3650 ^
  -subj   "/C=US/ST=State/L=City/O=BioLabs/OU=Dev/CN=postgres"

copy %SSL_DIR%\server.crt %SSL_DIR%\root.crt

echo.
echo SSL certificates generated in %SSL_DIR%\
dir %SSL_DIR%
echo.
echo Next step: docker-compose up -d
