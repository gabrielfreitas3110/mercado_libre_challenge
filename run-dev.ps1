# Script para executar a aplicação em modo desenvolvimento
Write-Host "Iniciando aplicação Inventory API no perfil dev..." -ForegroundColor Green

# Verificar se o Maven wrapper existe
if (Test-Path ".\mvnw.cmd") {
    Write-Host "Executando com Maven wrapper..." -ForegroundColor Yellow
    .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
} else {
    Write-Host "Maven wrapper não encontrado. Tentando com Maven..." -ForegroundColor Yellow
    mvn spring-boot:run -Dspring-boot.run.profiles=dev
}

Write-Host "Aplicação finalizada." -ForegroundColor Cyan

