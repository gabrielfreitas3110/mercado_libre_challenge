# Script para configurar o banco PostgreSQL
# Execute este script como administrador

Write-Host "Configurando banco PostgreSQL para o projeto Inventory API..." -ForegroundColor Green

# Verificar se o PostgreSQL está instalado
try {
    $psqlVersion = psql --version
    Write-Host "PostgreSQL encontrado: $psqlVersion" -ForegroundColor Green
} catch {
    Write-Host "PostgreSQL não encontrado. Por favor, instale o PostgreSQL primeiro." -ForegroundColor Red
    exit 1
}

# Criar o banco de dados
Write-Host "Criando banco de dados 'inventory_db'..." -ForegroundColor Yellow
try {
    psql -U postgres -c "CREATE DATABASE inventory_db;" 2>$null
    Write-Host "Banco de dados criado com sucesso!" -ForegroundColor Green
} catch {
    Write-Host "Erro ao criar banco de dados. Verifique se o PostgreSQL está rodando e as credenciais estão corretas." -ForegroundColor Red
    Write-Host "Tentando conectar com: psql -U postgres -h localhost -p 5432" -ForegroundColor Yellow
}

# Verificar se o banco foi criado
Write-Host "Verificando se o banco foi criado..." -ForegroundColor Yellow
try {
    $result = psql -U postgres -l | Select-String "inventory_db"
    if ($result) {
        Write-Host "Banco 'inventory_db' encontrado!" -ForegroundColor Green
    } else {
        Write-Host "Banco 'inventory_db' não foi encontrado." -ForegroundColor Red
    }
} catch {
    Write-Host "Erro ao verificar banco de dados." -ForegroundColor Red
}

Write-Host "Configuração concluída!" -ForegroundColor Green
Write-Host "Agora você pode executar a aplicação Spring Boot." -ForegroundColor Cyan
