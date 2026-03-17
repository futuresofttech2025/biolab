#!/bin/bash
# ════════════════════════════════════════════════════════════════════════════
# setup-es-users.sh — Sprint 1 GAP-10
#
# Creates minimal-privilege Elasticsearch users for Logstash and Kibana.
# Run ONCE after the Elasticsearch container first starts with security enabled.
#
# Prerequisites:
#   - Elasticsearch is running and healthy
#   - ELASTIC_PASSWORD, LOGSTASH_PASSWORD, KIBANA_PASSWORD are set
#
# Usage:
#   chmod +x setup-es-users.sh
#   ./setup-es-users.sh
# ════════════════════════════════════════════════════════════════════════════

set -euo pipefail

ES_URL="http://localhost:9200"
ELASTIC_USER="elastic"

# Wait for Elasticsearch to be ready
echo "Waiting for Elasticsearch..."
until curl -sf -u "${ELASTIC_USER}:${ELASTIC_PASSWORD}" "${ES_URL}/_cluster/health" > /dev/null; do
  sleep 2
done
echo "Elasticsearch is ready."

# ── Create logstash_writer role (write-only to biolab-* indices) ────────────
echo "Creating logstash_writer role..."
curl -sf -X PUT \
  -u "${ELASTIC_USER}:${ELASTIC_PASSWORD}" \
  -H "Content-Type: application/json" \
  "${ES_URL}/_security/role/logstash_writer" \
  -d '{
    "cluster": ["monitor", "manage_index_templates"],
    "indices": [
      {
        "names": ["biolab-*"],
        "privileges": ["write", "create", "create_index", "manage", "manage_ilm"]
      }
    ]
  }'
echo ""

# ── Create logstash_writer user ──────────────────────────────────────────────
echo "Creating logstash_writer user..."
curl -sf -X PUT \
  -u "${ELASTIC_USER}:${ELASTIC_PASSWORD}" \
  -H "Content-Type: application/json" \
  "${ES_URL}/_security/user/logstash_writer" \
  -d "{
    \"password\": \"${LOGSTASH_PASSWORD}\",
    \"roles\": [\"logstash_writer\"],
    \"full_name\": \"Logstash Writer\",
    \"email\": \"logstash@biolab.internal\"
  }"
echo ""

# ── Set kibana_system password ───────────────────────────────────────────────
# kibana_system is a built-in user — we just change its password
echo "Setting kibana_system password..."
curl -sf -X PUT \
  -u "${ELASTIC_USER}:${ELASTIC_PASSWORD}" \
  -H "Content-Type: application/json" \
  "${ES_URL}/_security/user/kibana_system/_password" \
  -d "{\"password\": \"${KIBANA_PASSWORD}\"}"
echo ""

echo "✓ Elasticsearch users configured successfully."
echo "  - logstash_writer: write access to biolab-* indices"
echo "  - kibana_system:   Kibana built-in user (password rotated)"
