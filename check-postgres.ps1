# Script para verificar se o PostgreSQL está rodando
Write-Host "Verificando status do PostgreSQL..." -ForegroundColor Green

try {
    # Tentar conectar ao PostgreSQL
    $result = psql -U postgres -h localhost -p 5432 -c "SELECT version();" 2>$null
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ PostgreSQL está rodando!" -ForegroundColor Green
        Write-Host "Versão: $($result[1])" -ForegroundColor Cyan
        
        # Verificar se o banco inventory_db existe
        $dbCheck = psql -U postgres -h localhost -p 5432 -l 2>$null | Select-String "inventory_db"
        
        if ($dbCheck) {
            Write-Host "✅ Banco 'inventory_db' encontrado!" -ForegroundColor Green
        } else {
            Write-Host "⚠️  Banco 'inventory_db' não encontrado. Criando..." -ForegroundColor Yellow
            psql -U postgres -h localhost -p 5432 -c "CREATE DATABASE inventory_db;" 2>$null
            if ($LASTEXITCODE -eq 0) {
                Write-Host "✅ Banco 'inventory_db' criado com sucesso!" -ForegroundColor Green
            } else {
                Write-Host "❌ Erro ao criar banco 'inventory_db'" -ForegroundColor Red
            }
        }
    } else {
        Write-Host "❌ PostgreSQL não está rodando ou não está acessível" -ForegroundColor Red
        Write-Host "Verifique se o PostgreSQL está instalado e rodando na porta 5432" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ Erro ao verificar PostgreSQL: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Certifique-se de que o PostgreSQL está instalado e o comando 'psql' está no PATH" -ForegroundColor Yellow
}

Write-Host "`nPara executar a aplicação:" -ForegroundColor Cyan
Write-Host "  .\run-local.ps1    # Perfil local" -ForegroundColor White
Write-Host "  .\run-dev.ps1      # Perfil dev" -ForegroundColor White

