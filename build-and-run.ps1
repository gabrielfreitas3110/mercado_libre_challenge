# 🚀 Script de Build e Execução - Inventory API

param(
    [string]$Profile = "local",
    [switch]$Build,
    [switch]$Test,
    [switch]$Docker,
    [switch]$Help
)

if ($Help) {
    Write-Host "🚀 Script de Build e Execução - Inventory API" -ForegroundColor Green
    Write-Host ""
    Write-Host "Uso:" -ForegroundColor Cyan
    Write-Host "  .\build-and-run.ps1 [opções]" -ForegroundColor White
    Write-Host ""
    Write-Host "Opções:" -ForegroundColor Cyan
    Write-Host "  -Profile <perfil>    Perfil a usar (local, file, dev, prod)" -ForegroundColor White
    Write-Host "  -Build              Executar build antes de rodar" -ForegroundColor White
    Write-Host "  -Test               Executar testes" -ForegroundColor White
    Write-Host "  -Docker             Usar Docker em vez de Maven" -ForegroundColor White
    Write-Host "  -Help               Mostrar esta ajuda" -ForegroundColor White
    Write-Host ""
    Write-Host "Exemplos:" -ForegroundColor Cyan
    Write-Host "  .\build-and-run.ps1                    # Executar com perfil local" -ForegroundColor White
    Write-Host "  .\build-and-run.ps1 -Profile file      # Executar com perfil file" -ForegroundColor White
    Write-Host "  .\build-and-run.ps1 -Build -Test       # Build, test e executar" -ForegroundColor White
    Write-Host "  .\build-and-run.ps1 -Docker            # Executar com Docker" -ForegroundColor White
    exit 0
}

Write-Host "🚀 Build and Run - Inventory API" -ForegroundColor Green
Write-Host "📋 Perfil: $Profile" -ForegroundColor Cyan
Write-Host ""

# Verificar se Maven está disponível
if (-not $Docker) {
    try {
        $mavenVersion = mvn -version 2>$null
        Write-Host "✅ Maven encontrado" -ForegroundColor Green
    }
    catch {
        Write-Host "❌ Maven não encontrado. Instale o Maven ou use -Docker" -ForegroundColor Red
        exit 1
    }
}

# Verificar se Docker está disponível (se necessário)
if ($Docker) {
    try {
        $dockerVersion = docker --version 2>$null
        Write-Host "✅ Docker encontrado" -ForegroundColor Green
    }
    catch {
        Write-Host "❌ Docker não encontrado" -ForegroundColor Red
        exit 1
    }
}

# Executar testes se solicitado
if ($Test) {
    Write-Host "🧪 Executando testes..." -ForegroundColor Yellow
    try {
        mvn test
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Testes passaram!" -ForegroundColor Green
        }
        else {
            Write-Host "❌ Testes falharam!" -ForegroundColor Red
            exit 1
        }
    }
    catch {
        Write-Host "❌ Erro ao executar testes: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
    Write-Host ""
}

# Executar build se solicitado
if ($Build) {
    Write-Host "🔨 Executando build..." -ForegroundColor Yellow
    try {
        mvn clean compile
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Build concluído!" -ForegroundColor Green
        }
        else {
            Write-Host "❌ Build falhou!" -ForegroundColor Red
            exit 1
        }
    }
    catch {
        Write-Host "❌ Erro no build: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
    Write-Host ""
}

# Executar aplicação
if ($Docker) {
    Write-Host "🐳 Executando com Docker..." -ForegroundColor Yellow
    
    # Build da imagem Docker
    Write-Host "🔨 Construindo imagem Docker..." -ForegroundColor Cyan
    try {
        docker build -t inventory-api .
        Write-Host "✅ Imagem Docker construída!" -ForegroundColor Green
    }
    catch {
        Write-Host "❌ Erro ao construir imagem Docker: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
    
    # Executar container
    Write-Host "🚀 Iniciando container..." -ForegroundColor Cyan
    $envVars = @(
        "SPRING_PROFILES_ACTIVE=$Profile"
    )
    
    $dockerArgs = @("run", "-p", "8080:8080")
    foreach ($envVar in $envVars) {
        $dockerArgs += @("-e", $envVar)
    }
    $dockerArgs += "inventory-api"
    
    try {
        & docker $dockerArgs
    }
    catch {
        Write-Host "❌ Erro ao executar container: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}
else {
    Write-Host "☕ Executando com Maven..." -ForegroundColor Yellow
    
    $mavenArgs = @("spring-boot:run")
    
    if ($Profile -ne "local") {
        $mavenArgs += @("-Dspring-boot.run.profiles=$Profile")
    }
    
    try {
        & mvn $mavenArgs
    }
    catch {
        Write-Host "❌ Erro ao executar aplicação: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
Write-Host "🎉 Aplicação executada com sucesso!" -ForegroundColor Green
Write-Host "📡 API disponível em: http://localhost:8080" -ForegroundColor Cyan
Write-Host "📚 Swagger UI: http://localhost:8080/swagger-ui.html" -ForegroundColor Cyan
Write-Host "🏥 Health Check: http://localhost:8080/actuator/health" -ForegroundColor Cyan
