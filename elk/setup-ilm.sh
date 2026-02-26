#!/bin/bash
# ════════════════════════════════════════════════════════════════
# BioLabs — Elasticsearch ILM Policy
# Manages log retention: hot (7 days) → delete (30 days)
# ════════════════════════════════════════════════════════════════

ES_URL="http://localhost:9200"

echo "⏳ Waiting for Elasticsearch..."
until curl -s "$ES_URL/_cluster/health" | grep -q '"status":"green\|yellow"'; do
  sleep 5
done
echo "✅ Elasticsearch is ready!"

# Create ILM policy
curl -s -X PUT "$ES_URL/_ilm/policy/biolab-ilm-policy" \
  -H "Content-Type: application/json" \
  -d '{
    "policy": {
      "phases": {
        "hot": {
          "min_age": "0ms",
          "actions": {
            "rollover": {
              "max_size": "5gb",
              "max_age": "7d"
            },
            "set_priority": { "priority": 100 }
          }
        },
        "warm": {
          "min_age": "7d",
          "actions": {
            "shrink": { "number_of_shards": 1 },
            "forcemerge": { "max_num_segments": 1 },
            "set_priority": { "priority": 50 }
          }
        },
        "delete": {
          "min_age": "30d",
          "actions": {
            "delete": {}
          }
        }
      }
    }
  }'

echo ""
echo "✅ ILM policy created: hot(7d) → warm → delete(30d)"
