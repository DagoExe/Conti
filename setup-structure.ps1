# Script per creare la struttura del progetto Conti
# Package: com.example.conti

$basePackagePath = "app\src\main\java\com\example\conti"

# Array con tutte le cartelle da creare
$folders = @(
    "data\database\entities",
    "data\database\dao",
    "data\repository",
    "data\excel",
    "ui\home",
    "ui\movimenti",
    "ui\abbonamenti",
    "ui\conti",
    "ui\adapters",
    "utils"
)

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "  Creazione struttura progetto Conti" -ForegroundColor Green
Write-Host "  Package: com.example.conti" -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""

# Crea ogni cartella
foreach ($folder in $folders) {
    $fullPath = Join-Path $basePackagePath $folder
    if (!(Test-Path $fullPath)) {
        New-Item -ItemType Directory -Path $fullPath -Force | Out-Null
        Write-Host "[CREATA]    $folder" -ForegroundColor Green
    } else {
        Write-Host "[ESISTENTE] $folder" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "  Struttura creata con successo!" -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Path base: $basePackagePath" -ForegroundColor Gray
Write-Host ""
Write-Host "Prossimi passi:" -ForegroundColor Yellow
Write-Host "1. Aggiorna il file build.gradle.kts" -ForegroundColor White
Write-Host "2. Crea i file delle entità (Conto, Movimento, Abbonamento)" -ForegroundColor White
Write-Host "3. Crea i DAO e il Database" -ForegroundColor White