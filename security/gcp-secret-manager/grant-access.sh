#!/usr/bin/env bash
set -euo pipefail

: "${PROJECT_ID:?Set PROJECT_ID}"
: "${MEMBER:?Set MEMBER, for example serviceAccount:cloud-run-sa@PROJECT_ID.iam.gserviceaccount.com}"

SECRETS=(
  DB_PASSWORD
  REDIS_PASSWORD
  GOOGLE_CLIENT_ID
  GOOGLE_CLIENT_SECRET
  MAIL_PASSWORD
  APP_REMEMBER_ME_KEY
)

for secret_id in "${SECRETS[@]}"; do
  gcloud secrets add-iam-policy-binding "$secret_id" \
    --project="$PROJECT_ID" \
    --member="$MEMBER" \
    --role="roles/secretmanager.secretAccessor"
  echo "granted roles/secretmanager.secretAccessor on $secret_id to $MEMBER"
done