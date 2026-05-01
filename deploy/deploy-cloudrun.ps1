param(
    [string]$Service = "flood-fill",
    [string]$ProjectId = "project-d8c92b26-085d-46d0-a85",
    [string]$Region = "europe-west1",
    [string]$EnvFile = "deploy/cloudrun.env.yaml",
    [string]$CloudSqlInstance = "project-d8c92b26-085d-46d0-a85:europe-west1:flood-fill-db",
    [string]$Network = "default",
    [string]$Subnet = "default"
)

$ErrorActionPreference = "Stop"

$requiredSecrets = @(
    "DB_PASSWORD=DB_PASSWORD:latest",
    "GOOGLE_CLIENT_ID=GOOGLE_CLIENT_ID:latest",
    "GOOGLE_CLIENT_SECRET=GOOGLE_CLIENT_SECRET:latest",
    "MAIL_PASSWORD=MAIL_PASSWORD:latest",
    "APP_REMEMBER_ME_KEY=APP_REMEMBER_ME_KEY:latest"
)

$missingSecrets = @()
foreach ($secretMapping in $requiredSecrets) {
    $secretName = ($secretMapping -split "=")[1].Split(":")[0]
    gcloud secrets describe $secretName --project $ProjectId *> $null
    if ($LASTEXITCODE -ne 0) {
        $missingSecrets += $secretName
    }
}

if ($missingSecrets.Count -gt 0) {
    throw "Missing Secret Manager secrets in project '$ProjectId': $($missingSecrets -join ', ')"
}

$secretArg = $requiredSecrets -join ","

gcloud run deploy $Service `
    --project $ProjectId `
    --source . `
    --region $Region `
    --allow-unauthenticated `
    --env-vars-file $EnvFile `
    --set-secrets $secretArg `
    --add-cloudsql-instances $CloudSqlInstance `
    --network $Network `
    --subnet $Subnet `
    --vpc-egress private-ranges-only
