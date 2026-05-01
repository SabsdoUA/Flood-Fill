param(
    [ValidateSet("validate", "repair", "migrate")]
    [string]$Action = "validate",
    [string]$ProjectId = "project-d8c92b26-085d-46d0-a85",
    [string]$InstanceConnectionName = "project-d8c92b26-085d-46d0-a85:europe-west1:flood-fill-db",
    [string]$DbName = "postgres",
    [string]$DbUser = "postgres"
)

$ErrorActionPreference = "Stop"

$env:INSTANCE_CONNECTION_NAME = $InstanceConnectionName
$env:DB_NAME = $DbName
$env:DB_USER = $DbUser
$env:DB_PASSWORD = gcloud secrets versions access latest --secret=DB_PASSWORD --project=$ProjectId

mvn -q -DskipTests compile exec:java "-Dexec.mainClass=sk.tuke.gamestudio.infrastructure.db.FlywayMaintenance" "-Dexec.args=$Action"
