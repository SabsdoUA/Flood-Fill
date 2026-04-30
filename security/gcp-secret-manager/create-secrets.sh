#!/usr/bin/env bash
set -euo pipefail

: "${PROJECT_ID:?Set PROJECT_ID}"

SECRETS=(
  DB_PASSWORD
  REDIS_PASSWORD
  GOOGLE_CLIENT_ID
  GOOGLE_CLIENT_SECRET
  MAIL_PASSWORD
  APP_REMEMBER_ME_KEY
)

gcloud services enable secretmanager.googleapis.com --project="$PROJECT_ID"

for secret_id in "${SECRETS[@]}"; do
  if gcloud secrets describe "$secret_id" --project="$PROJECT_ID" >/dev/null 2>&1; then
    echo "exists: $secret_id"
  else
    gcloud secrets create "$secret_id" \
      --project="$PROJECT_ID" \
      --replication-policy="automatic"
    echo "created: $secret_id"
  fi
done