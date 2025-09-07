# Script para executar a aplicação com perfil inmemory (H2)
Write-Host "Iniciando aplicação com perfil inmemory (H2 in-memory database)..." -ForegroundColor Green

# Verificar se o Maven está disponível
if (Get-Command mvn -ErrorAction SilentlyContinue) {
    Write-Host "Usando Maven para executar a aplicação..." -ForegroundColor Yellow
    mvn spring-boot:run -Dspring-boot.run.profiles=inmemory
} else {
    Write-Host "Maven não encontrado. Tentando executar diretamente..." -ForegroundColor Yellow
    
    # Verificar se o JAR foi compilado
    if (Test-Path "target\classes") {
        Write-Host "Executando com java diretamente..." -ForegroundColor Yellow
        java -cp "target\classes;target\dependency\*" -Dspring.profiles.active=inmemory com.quickcoders.infolabsproducts.InfoLabsProductsApplication
    } else {
        Write-Host "Erro: Classes não compiladas. Execute 'mvn compile' primeiro." -ForegroundColor Red
        exit 1
    }
}

Write-Host "Aplicação finalizada." -ForegroundColor Green

