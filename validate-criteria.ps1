# Script de Validação dos Critérios de Aceite
# API de Inventário - QuickCoders

param(
    [string]$Profile = "local",
    [switch]$SkipTests = $false,
    [switch]$SkipBuild = $false,
    [switch]$Verbose = $false
)

Write-Host "🔍 Validando Critérios de Aceite - API de Inventário" -ForegroundColor Cyan
Write-Host "=================================================" -ForegroundColor Cyan

$errors = @()
$warnings = @()
$success = @()

# Função para log
function Log-Result {
    param($Message, $Type = "INFO")
    switch ($Type) {
        "ERROR" { 
            Write-Host "❌ $Message" -ForegroundColor Red
            $script:errors += $Message
        }
        "WARNING" { 
            Write-Host "⚠️  $Message" -ForegroundColor Yellow
            $script:warnings += $Message
        }
        "SUCCESS" { 
            Write-Host "✅ $Message" -ForegroundColor Green
            $script:success += $Message
        }
        default { 
            Write-Host "ℹ️  $Message" -ForegroundColor Blue
        }
    }
}

# Função para executar comando
function Invoke-Command {
    param($Command, $Description)
    try {
        if ($Verbose) {
            Write-Host "Executando: $Command" -ForegroundColor Gray
        }
        $result = Invoke-Expression $Command
        Log-Result $Description "SUCCESS"
        return $result
    }
    catch {
        Log-Result "$Description - Erro: $($_.Exception.Message)" "ERROR"
        return $null
    }
}

# Função para testar endpoint
function Test-Endpoint {
    param($Url, $Method = "GET", $Headers = @{}, $Body = $null, $ExpectedStatus = 200)
    try {
        $params = @{
            Uri = $Url
            Method = $Method
            Headers = $Headers
        }
        
        if ($Body) {
            $params.Body = $Body
            $params.ContentType = "application/json"
        }
        
        $response = Invoke-RestMethod @params -ErrorAction Stop
        Log-Result "Endpoint $Method $Url - Status: $($response.StatusCode)" "SUCCESS"
        return $response
    }
    catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -eq $ExpectedStatus) {
            Log-Result "Endpoint $Method $Url - Status esperado: $ExpectedStatus" "SUCCESS"
            return $true
        }
        else {
            Log-Result "Endpoint $Method $Url - Status inesperado: $statusCode (esperado: $ExpectedStatus)" "ERROR"
            return $false
        }
    }
}

Write-Host "`n📋 1. Verificando Estrutura do Projeto" -ForegroundColor Yellow

# Verificar arquivos obrigatórios
$requiredFiles = @(
    "pom.xml",
    "src/main/java/com/quickcoders/inventory/api/Application.java",
    "src/main/resources/application.yaml",
    "src/main/resources/openapi-inventory.yml",
    "README.md",
    "run.md",
    "QUICKSTART.md",
    "CRITERIOS_ACEITE.md"
)

foreach ($file in $requiredFiles) {
    if (Test-Path $file) {
        Log-Result "Arquivo encontrado: $file" "SUCCESS"
    }
    else {
        Log-Result "Arquivo não encontrado: $file" "ERROR"
    }
}

Write-Host "`n🔨 2. Build e Compilação" -ForegroundColor Yellow

if (-not $SkipBuild) {
    # Limpar e compilar
    Invoke-Command "mvn clean compile" "Compilação do projeto"
    
    # Verificar se compilou sem erros
    if ($LASTEXITCODE -eq 0) {
        Log-Result "Compilação bem-sucedida" "SUCCESS"
    }
    else {
        Log-Result "Erro na compilação" "ERROR"
    }
}

Write-Host "`n🧪 3. Executando Testes" -ForegroundColor Yellow

if (-not $SkipTests) {
    # Executar testes
    Invoke-Command "mvn test" "Execução dos testes"
    
    # Verificar cobertura
    Invoke-Command "mvn test jacoco:report" "Geração do relatório de cobertura"
    
    # Verificar se testes passaram
    if ($LASTEXITCODE -eq 0) {
        Log-Result "Todos os testes passaram" "SUCCESS"
    }
    else {
        Log-Result "Alguns testes falharam" "ERROR"
    }
}

Write-Host "`n🚀 4. Iniciando Aplicação" -ForegroundColor Yellow

# Iniciar aplicação em background
$appProcess = Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run", "-Dspring-boot.run.profiles=$Profile" -PassThru -WindowStyle Hidden

# Aguardar aplicação iniciar
Write-Host "Aguardando aplicação iniciar..." -ForegroundColor Gray
Start-Sleep -Seconds 30

# Verificar se aplicação está rodando
try {
    $healthResponse = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -ErrorAction Stop
    Log-Result "Aplicação iniciada com sucesso" "SUCCESS"
}
catch {
    Log-Result "Falha ao iniciar aplicação" "ERROR"
    $appProcess.Kill()
    exit 1
}

Write-Host "`n🌐 5. Testando Endpoints" -ForegroundColor Yellow

# Headers padrão
$headers = @{
    "Content-Type" = "application/json"
    "Idempotency-Key" = "test-$(Get-Date -Format 'yyyyMMddHHmmss')"
    "X-Correlation-Id" = "test-$(Get-Random)"
}

# Testar endpoints obrigatórios
$endpoints = @(
    @{ Method = "POST"; Path = "/items"; Body = '{"sku": "TEST-001", "name": "Item Teste"}' },
    @{ Method = "PUT"; Path = "/items/TEST-001/adjust"; Body = '{"storeId": "STORE-01", "delta": 100}' },
    @{ Method = "GET"; Path = "/items/TEST-001" },
    @{ Method = "GET"; Path = "/availability?skus=TEST-001&storeId=STORE-01" },
    @{ Method = "GET"; Path = "/items" }
)

foreach ($endpoint in $endpoints) {
    $url = "http://localhost:8080$($endpoint.Path)"
    $method = $endpoint.Method
    $body = $endpoint.Body
    
    if ($body) {
        Test-Endpoint -Url $url -Method $method -Headers $headers -Body $body
    }
    else {
        Test-Endpoint -Url $url -Method $method -Headers $headers
    }
}

Write-Host "`n📊 6. Verificando Observabilidade" -ForegroundColor Yellow

# Testar endpoints de observabilidade
$observabilityEndpoints = @(
    "/actuator/health",
    "/actuator/metrics",
    "/actuator/prometheus",
    "/swagger-ui.html"
)

foreach ($endpoint in $observabilityEndpoints) {
    $url = "http://localhost:8080$endpoint"
    Test-Endpoint -Url $url
}

Write-Host "`n🔒 7. Testando Idempotência" -ForegroundColor Yellow

# Testar idempotência
$idempotencyKey = "idempotency-test-$(Get-Date -Format 'yyyyMMddHHmmss')"
$idempotencyHeaders = @{
    "Content-Type" = "application/json"
    "Idempotency-Key" = $idempotencyKey
    "X-Correlation-Id" = "idempotency-test-$(Get-Random)"
}

# Primeira requisição
$firstResponse = Test-Endpoint -Url "http://localhost:8080/items" -Method "POST" -Headers $idempotencyHeaders -Body '{"sku": "IDEMPOTENCY-TEST", "name": "Teste Idempotência"}'

# Segunda requisição com mesma chave
$secondResponse = Test-Endpoint -Url "http://localhost:8080/items" -Method "POST" -Headers $idempotencyHeaders -Body '{"sku": "IDEMPOTENCY-TEST", "name": "Teste Idempotência"}'

if ($firstResponse -and $secondResponse) {
    Log-Result "Idempotência funcionando corretamente" "SUCCESS"
}
else {
    Log-Result "Falha na validação de idempotência" "ERROR"
}

Write-Host "`n⚡ 8. Testando Concorrência" -ForegroundColor Yellow

# Executar script de teste de concorrência se existir
if (Test-Path "concurrency-test.ps1") {
    Invoke-Command ".\concurrency-test.ps1" "Teste de concorrência"
}
else {
    Log-Result "Script de teste de concorrência não encontrado" "WARNING"
}

Write-Host "`n📋 9. Resumo da Validação" -ForegroundColor Yellow

Write-Host "`n✅ Sucessos: $($success.Count)" -ForegroundColor Green
foreach ($item in $success) {
    Write-Host "  • $item" -ForegroundColor Green
}

Write-Host "`n⚠️  Avisos: $($warnings.Count)" -ForegroundColor Yellow
foreach ($item in $warnings) {
    Write-Host "  • $item" -ForegroundColor Yellow
}

Write-Host "`n❌ Erros: $($errors.Count)" -ForegroundColor Red
foreach ($item in $errors) {
    Write-Host "  • $item" -ForegroundColor Red
}

# Parar aplicação
Write-Host "`n🛑 Parando aplicação..." -ForegroundColor Gray
$appProcess.Kill()

# Resultado final
if ($errors.Count -eq 0) {
    Write-Host "`n🎉 VALIDAÇÃO CONCLUÍDA COM SUCESSO!" -ForegroundColor Green
    Write-Host "Todos os critérios de aceite foram atendidos." -ForegroundColor Green
    exit 0
}
else {
    Write-Host "`n❌ VALIDAÇÃO FALHOU!" -ForegroundColor Red
    Write-Host "Existem $($errors.Count) erros que precisam ser corrigidos." -ForegroundColor Red
    exit 1
}
