# 🚀 Script de Exemplos - Inventory API (PowerShell)
# Execute este script para testar todos os endpoints da API

param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"

$CorrelationId = "example-$(Get-Date -Format 'yyyyMMddHHmmss')"

Write-Host "🚀 Iniciando exemplos da Inventory API" -ForegroundColor Green
Write-Host "📡 Base URL: $BaseUrl" -ForegroundColor Cyan
Write-Host "🔗 Correlation ID: $CorrelationId" -ForegroundColor Cyan
Write-Host ""

# Função para fazer requisições com headers padrão
function Make-Request {
    param(
        [string]$Method,
        [string]$Endpoint,
        [string]$Data = $null,
        [string]$IdempotencyKey = $null
    )
    
    Write-Host "📤 $Method $Endpoint" -ForegroundColor Yellow
    
    $headers = @{
        "X-Correlation-Id" = $CorrelationId
    }
    
    if ($Data) {
        $headers["Content-Type"] = "application/json"
        if ($IdempotencyKey) {
            $headers["Idempotency-Key"] = $IdempotencyKey
        }
        
        try {
            $response = Invoke-RestMethod -Uri "$BaseUrl$Endpoint" -Method $Method -Headers $headers -Body $Data
            $response | ConvertTo-Json -Depth 10
        }
        catch {
            Write-Host "❌ Erro na requisição: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    else {
        try {
            $response = Invoke-RestMethod -Uri "$BaseUrl$Endpoint" -Method $Method -Headers $headers
            $response | ConvertTo-Json -Depth 10
        }
        catch {
            Write-Host "❌ Erro na requisição: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    
    Write-Host ""
}

# Verificar se a API está rodando
Write-Host "🔍 Verificando se a API está rodando..." -ForegroundColor Cyan
try {
    $healthResponse = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -Method GET
    Write-Host "✅ API está rodando!" -ForegroundColor Green
}
catch {
    Write-Host "❌ API não está rodando em $BaseUrl" -ForegroundColor Red
    Write-Host "💡 Execute: mvn spring-boot:run" -ForegroundColor Yellow
    exit 1
}
Write-Host ""

# 1. Criar item
Write-Host "🏷️ 1. Criando item..." -ForegroundColor Green
$itemData = @{
    sku = "LAPTOP-001"
    name = "Laptop Dell XPS 13"
    attributes = @{
        brand = "Dell"
        model = "XPS 13"
        color = "Silver"
        storage = "512GB SSD"
    }
} | ConvertTo-Json -Depth 3

Make-Request -Method "POST" -Endpoint "/items" -Data $itemData -IdempotencyKey "create-item-001"

# 2. Buscar item criado
Write-Host "🔍 2. Buscando item criado..." -ForegroundColor Green
Make-Request -Method "GET" -Endpoint "/items/LAPTOP-001"

# 3. Ajustar estoque (aumentar)
Write-Host "📦 3. Ajustando estoque (+50 unidades)..." -ForegroundColor Green
$adjustData = @{
    storeId = "STORE-01"
    delta = 50
    expectedVersion = 1
} | ConvertTo-Json

Make-Request -Method "PUT" -Endpoint "/items/LAPTOP-001/adjust" -Data $adjustData -IdempotencyKey "adjust-stock-001"

# 4. Verificar disponibilidade
Write-Host "📊 4. Verificando disponibilidade..." -ForegroundColor Green
Make-Request -Method "GET" -Endpoint "/availability?skus=LAPTOP-001&storeId=STORE-01"

# 5. Criar reserva
Write-Host "🔒 5. Criando reserva (5 unidades)..." -ForegroundColor Green
$reserveData = @{
    qty = 5
    storeId = "STORE-01"
    ttlSeconds = 900
    expectedVersion = 2
} | ConvertTo-Json

try {
    $reserveResponse = Invoke-RestMethod -Uri "$BaseUrl/items/LAPTOP-001/reserve" -Method POST -Headers @{
        "Content-Type" = "application/json"
        "Idempotency-Key" = "reserve-001"
        "X-Correlation-Id" = $CorrelationId
    } -Body $reserveData
    
    Write-Host "📤 POST /items/LAPTOP-001/reserve" -ForegroundColor Yellow
    $reserveResponse | ConvertTo-Json -Depth 10
    Write-Host ""
    
    $reservationId = $reserveResponse.reservationId
    
    if ($reservationId) {
        Write-Host "✅ Reserva criada com ID: $reservationId" -ForegroundColor Green
        
        # 6. Verificar disponibilidade após reserva
        Write-Host "📊 6. Verificando disponibilidade após reserva..." -ForegroundColor Green
        Make-Request -Method "GET" -Endpoint "/availability?skus=LAPTOP-001&storeId=STORE-01"
        
        # 7. Confirmar reserva (commit)
        Write-Host "✅ 7. Confirmando reserva (commit)..." -ForegroundColor Green
        $commitData = @{
            reservationId = $reservationId
            expectedVersion = 3
        } | ConvertTo-Json
        
        Make-Request -Method "POST" -Endpoint "/items/LAPTOP-001/commit" -Data $commitData -IdempotencyKey "commit-001"
        
        # 8. Verificar disponibilidade após commit
        Write-Host "📊 8. Verificando disponibilidade após commit..." -ForegroundColor Green
        Make-Request -Method "GET" -Endpoint "/availability?skus=LAPTOP-001&storeId=STORE-01"
    }
    else {
        Write-Host "⚠️ Não foi possível extrair o ID da reserva, pulando operações de commit" -ForegroundColor Yellow
    }
}
catch {
    Write-Host "❌ Erro ao criar reserva: $($_.Exception.Message)" -ForegroundColor Red
}

# 9. Criar segundo item
Write-Host "🏷️ 9. Criando segundo item..." -ForegroundColor Green
$item2Data = @{
    sku = "MOUSE-001"
    name = "Mouse Logitech MX Master 3"
    attributes = @{
        brand = "Logitech"
        model = "MX Master 3"
        color = "Graphite"
        connectivity = "Bluetooth"
    }
} | ConvertTo-Json -Depth 3

Make-Request -Method "POST" -Endpoint "/items" -Data $item2Data -IdempotencyKey "create-item-002"

# 10. Buscar todos os itens
Write-Host "📋 10. Buscando todos os itens..." -ForegroundColor Green
Make-Request -Method "GET" -Endpoint "/items"

# 11. Buscar itens com filtro
Write-Host "🔍 11. Buscando itens com filtro 'laptop'..." -ForegroundColor Green
Make-Request -Method "GET" -Endpoint "/items?q=laptop&page=0&pageSize=10"

# 12. Verificar disponibilidade múltipla
Write-Host "📊 12. Verificando disponibilidade múltipla..." -ForegroundColor Green
Make-Request -Method "GET" -Endpoint "/availability?skus=LAPTOP-001,MOUSE-001&storeId=STORE-01"

# 13. Ajustar estoque do segundo item
Write-Host "📦 13. Ajustando estoque do mouse (+100 unidades)..." -ForegroundColor Green
$adjust2Data = @{
    storeId = "STORE-01"
    delta = 100
} | ConvertTo-Json

Make-Request -Method "PUT" -Endpoint "/items/MOUSE-001/adjust" -Data $adjust2Data -IdempotencyKey "adjust-stock-002"

# 14. Verificar métricas
Write-Host "📈 14. Verificando métricas da aplicação..." -ForegroundColor Green
Write-Host "📤 GET /actuator/metrics" -ForegroundColor Yellow
try {
    $metricsResponse = Invoke-RestMethod -Uri "$BaseUrl/actuator/metrics" -Method GET
    $metricsResponse.names | Select-Object -First 10 | ForEach-Object { Write-Host "  - $_" -ForegroundColor Cyan }
}
catch {
    Write-Host "❌ Erro ao buscar métricas: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# 15. Health check
Write-Host "🏥 15. Health check..." -ForegroundColor Green
Make-Request -Method "GET" -Endpoint "/actuator/health"

Write-Host "🎉 Exemplos concluídos!" -ForegroundColor Green
Write-Host ""
Write-Host "📖 Para mais exemplos e cenários avançados, consulte:" -ForegroundColor Cyan
Write-Host "   - run.md - Documentação completa" -ForegroundColor White
Write-Host "   - http://localhost:8080/swagger-ui.html - Swagger UI" -ForegroundColor White
Write-Host ""
Write-Host "🔗 Correlation ID usado: $CorrelationId" -ForegroundColor Cyan
Write-Host "💡 Use este ID para rastrear as requisições nos logs da aplicação" -ForegroundColor Yellow
