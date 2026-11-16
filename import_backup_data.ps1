
$ErrorActionPreference = 'Stop'

function Resolve-MavenCommand {
    $mvnCommand = Get-Command mvn -ErrorAction SilentlyContinue
    if ($mvnCommand) {
        return $mvnCommand.Source
    }
    $fallback = "C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.11\bin\mvn.cmd"
    if (Test-Path $fallback) {
        return $fallback
    }
    throw "Maven executable not found. Install Maven or add it to PATH."
}

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $repoRoot

$zipPath = Join-Path $repoRoot 'Audino-main.zip'
if (!(Test-Path $zipPath)) {
    throw "Backup zip not found: $zipPath"
}

$extractRoot = Join-Path $env:TEMP 'audino-main-backup-extract'
$dataRoot = Join-Path $extractRoot 'Audino-main\src\main\resources\data'

if (!(Test-Path $dataRoot)) {
    if (Test-Path $extractRoot) {
        Remove-Item -Recurse -Force $extractRoot
    }
    New-Item -ItemType Directory -Path $extractRoot | Out-Null
    Expand-Archive -Path $zipPath -DestinationPath $extractRoot -Force
}

Write-Host "Copying backup data to project resources..." -ForegroundColor Cyan
Copy-Item -Path (Join-Path $dataRoot '*') -Destination (Join-Path $repoRoot 'src\main\resources\data') -Recurse -Force

Write-Host "Running AppSeeder to inject backup data into PostgreSQL at: data/pg-data" -ForegroundColor Cyan
$mvnCmd = Resolve-MavenCommand
& $mvnCmd compile exec:java "-Dexec.mainClass=com.audino.util.AppSeeder" "-Dexec.classpathScope=compile"

if ($LASTEXITCODE -ne 0) {
    throw "Import failed during AppSeeder execution."
}

Write-Host "Backup data successfully imported into the PostgreSQL database." -ForegroundColor Green

