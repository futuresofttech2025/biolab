#!/bin/bash
# ════════════════════════════════════════════════════════════════════════════
# generate-postgres-ssl.sh
#
# Generates a self-signed SSL certificate for the local PostgreSQL container.
# Run this ONCE before starting docker-compose.
#
# Output files (all go into postgres/ssl/):
#   server.key  — PostgreSQL private key  (chmod 600, owned by postgres uid 999)
#   server.crt  — PostgreSQL certificate  (self-signed, valid 3650 days / 10 years)
#   root.crt    — CA certificate          (same as server.crt for self-signed)
#
# Production: replace these with certificates from your CA (Let's Encrypt,
#             AWS ACM, or your internal PKI). Same file names, same permissions.
#
# Usage:
#   chmod +x generate-postgres-ssl.sh
#   ./generate-postgres-ssl.sh
#   docker-compose up -d
# ════════════════════════════════════════════════════════════════════════════

set -euo pipefail

SSL_DIR="$(dirname "$0")/postgres/ssl"
mkdir -p "$SSL_DIR"

echo "Generating PostgreSQL SSL certificate in $SSL_DIR ..."

# Generate private key (RSA 2048)
openssl genrsa -out "$SSL_DIR/server.key" 2048

# Generate self-signed certificate
openssl req -new -x509 \
  -key    "$SSL_DIR/server.key" \
  -out    "$SSL_DIR/server.crt" \
  -days   3650 \
  -subj   "/C=US/ST=State/L=City/O=BioLabs/OU=Dev/CN=postgres"

# Copy cert as CA root (for self-signed, server cert IS the CA cert)
cp "$SSL_DIR/server.crt" "$SSL_DIR/root.crt"

# PostgreSQL requires the key to be owned by the postgres user (uid 999 in alpine)
# and not readable by group or others.
chmod 600 "$SSL_DIR/server.key"
chmod 644 "$SSL_DIR/server.crt"
chmod 644 "$SSL_DIR/root.crt"

echo ""
echo "✓ SSL certificates generated:"
ls -la "$SSL_DIR"
echo ""
echo "Next steps:"
echo "  docker-compose up -d"
echo "  # Test SSL connection:"
echo "  psql \"postgresql://biolab:biolab@localhost:5432/biolab?sslmode=require\" -c 'SELECT ssl_is_used();'"