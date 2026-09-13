$ErrorActionPreference = 'Stop'
Write-Host 'Checking required production environment variables...'
$required = @('DB_HOST','DB_PORT','DB_NAME','DB_USERNAME','DB_PASSWORD','JWT_SECRET')
foreach ($name in $required) {
  if ([string]::IsNullOrWhiteSpace((Get-Item "Env:$name" -ErrorAction SilentlyContinue).Value)) {
    throw "Missing environment variable: $name"
  }
}
if ($env:JWT_SECRET.Length -lt 32) { throw 'JWT_SECRET must be at least 32 characters.' }
Write-Host 'Environment checks passed.'
Write-Host 'Run: mvn clean verify'
