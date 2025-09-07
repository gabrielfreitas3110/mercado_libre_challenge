# Script para executar a aplicação localmente
Write-Host "Iniciando aplicação Inventory API no perfil local..." -ForegroundColor Green

# Verificar se o Maven wrapper existe
if (Test-Path ".\mvnw.cmd") {
    Write-Host "Executando com Maven wrapper..." -ForegroundColor Yellow
    .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
} else {
    Write-Host "Maven wrapper não encontrado. Tentando com Maven..." -ForegroundColor Yellow
    mvn spring-boot:run -Dspring-boot.run.profiles=local
}

Write-Host "Aplicação finalizada." -ForegroundColor Cyan

