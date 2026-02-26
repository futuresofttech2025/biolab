#!/bin/bash
# ════════════════════════════════════════════════════════════════
# BioLabs — Kibana Automatic Setup
# Run after ELK stack is healthy:
#   docker exec biolab-kibana bash /opt/setup-kibana.sh
# ════════════════════════════════════════════════════════════════

KIBANA_URL="http://localhost:5601"
ES_URL="http://elasticsearch:9200"

echo "⏳ Waiting for Kibana to be ready..."
until curl -s "$KIBANA_URL/api/status" | grep -q '"overall":{"level":"available"'; do
  sleep 5
done
echo "✅ Kibana is ready!"

# ── Create Data Views (Index Patterns) ────────────────────────
echo "📊 Creating data views..."

# All BioLab logs
curl -s -X POST "$KIBANA_URL/api/data_views/data_view" \
  -H "kbn-xsrf: true" -H "Content-Type: application/json" \
  -d '{
    "data_view": {
      "title": "biolab-*",
      "name": "BioLab — All Services",
      "timeFieldName": "@timestamp"
    }
  }'

# Per-service views
for svc in auth-service user-service catalog-service project-service invoice-service messaging-service document-service notification-service audit-service api-gateway; do
  curl -s -X POST "$KIBANA_URL/api/data_views/data_view" \
    -H "kbn-xsrf: true" -H "Content-Type: application/json" \
    -d "{
      \"data_view\": {
        \"title\": \"biolab-biolab-${svc}-*\",
        \"name\": \"BioLab — ${svc}\",
        \"timeFieldName\": \"@timestamp\"
      }
    }"
  echo ""
done

echo ""
echo "✅ Kibana setup complete!"
echo "   📊 Access Kibana at: http://localhost:5601"
echo "   📋 Data Views: Stack Management → Data Views"
echo "   🔍 Explore: Discover → select 'BioLab — All Services'"
