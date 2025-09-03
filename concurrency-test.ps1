# 🔄 Script de Teste de Concorrência - Inventory API
# Testa reservas concorrentes para verificar 0 oversell

param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$Threads = 10,
    [int]$AvailableStock = 5,
    [int]$ReserveQuantity = 2
)

$ErrorActionPreference = "Stop"

$CorrelationId = "concurrency-test-$(Get-Date -Format 'yyyyMMddHHmmss')"
$Sku = "CONCURRENT-TEST-001"

Write-Host "🔄 Iniciando teste de concorrência" -ForegroundColor Green
Write-Host "📡 Base URL: $BaseUrl" -ForegroundColor Cyan
Write-Host "🔗 Correlation ID: $CorrelationId" -ForegroundColor Cyan
Write-Host "🧵 Threads: $Threads" -ForegroundColor Cyan
Write-Host "📦 Estoque disponível: $AvailableStock" -ForegroundColor Cyan
Write-Host "🔒 Quantidade por reserva: $ReserveQuantity" -ForegroundColor Cyan
Write-Host ""

# Função para fazer requisições
function Invoke-ApiRequest {
    param(
        [string]$Method,
        [string]$Endpoint,
        [string]$Data = $null,
        [string]$IdempotencyKey = $null
    )
    
    $headers = @{
        "X-Correlation-Id" = $CorrelationId
    }
    
    if ($Data) {
        $headers["Content-Type"] = "application/json"
        if ($IdempotencyKey) {
            $headers["Idempotency-Key"] = $IdempotencyKey
        }
    }
    
    try {
        if ($Data) {
            return Invoke-RestMethod -Uri "$BaseUrl$Endpoint" -Method $Method -Headers $headers -Body $Data
        }
        else {
            return Invoke-RestMethod -Uri "$BaseUrl$Endpoint" -Method $Method -Headers $headers
        }
    }
    catch {
        return @{
            Error = $_.Exception.Message
            StatusCode = $_.Exception.Response.StatusCode.value__
        }
    }
}

# Verificar se a API está rodando
Write-Host "🔍 Verificando se a API está rodando..." -ForegroundColor Cyan
try {
    $healthResponse = Invoke-ApiRequest -Method "GET" -Endpoint "/actuator/health"
    Write-Host "✅ API está rodando!" -ForegroundColor Green
}
catch {
    Write-Host "❌ API não está rodando em $BaseUrl" -ForegroundColor Red
    Write-Host "💡 Execute: mvn spring-boot:run" -ForegroundColor Yellow
    exit 1
}

# Setup: Criar item e adicionar estoque
Write-Host "🏷️ Configurando item de teste..." -ForegroundColor Green

# Criar item
$itemData = @{
    sku = $Sku
    name = "Item Teste Concorrência"
    attributes = @{
        type = "concurrency-test"
    }
} | ConvertTo-Json -Depth 3

$createResponse = Invoke-ApiRequest -Method "POST" -Endpoint "/items" -Data $itemData -IdempotencyKey "concurrency-setup"
if ($createResponse.Error) {
    Write-Host "⚠️ Item já existe ou erro na criação: $($createResponse.Error)" -ForegroundColor Yellow
}

# Adicionar estoque
$stockData = @{
    storeId = "STORE-01"
    delta = $AvailableStock
} | ConvertTo-Json

$stockResponse = Invoke-ApiRequest -Method "PUT" -Endpoint "/items/$Sku/adjust" -Data $stockData -IdempotencyKey "concurrency-stock"
if ($stockResponse.Error) {
    Write-Host "❌ Erro ao adicionar estoque: $($stockResponse.Error)" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Item configurado com $AvailableStock unidades de estoque" -ForegroundColor Green

# Verificar estoque inicial
Write-Host "📊 Verificando estoque inicial..." -ForegroundColor Green
$availabilityResponse = Invoke-ApiRequest -Method "GET" -Endpoint "/availability?skus=$Sku&storeId=STORE-01"
if ($availabilityResponse.results -and $availabilityResponse.results.Count -gt 0) {
    $initialStock = $availabilityResponse.results[0].quantityAvailable
    Write-Host "📦 Estoque inicial: $initialStock unidades" -ForegroundColor Cyan
}
Write-Host ""

# Função para executar reserva
function Start-Reservation {
    param(
        [int]$ThreadNumber
    )
    
    $idempotencyKey = "concurrent-$ThreadNumber-$(Get-Date -Format 'HHmmss')"
    
    $reserveData = @{
        qty = $ReserveQuantity
        storeId = "STORE-01"
        ttlSeconds = 900
    } | ConvertTo-Json
    
    Write-Host "🧵 Thread $ThreadNumber iniciando reserva..." -ForegroundColor Yellow
    
    $response = Invoke-ApiRequest -Method "POST" -Endpoint "/items/$Sku/reserve" -Data $reserveData -IdempotencyKey $idempotencyKey
    
    if ($response.Error) {
        Write-Host "❌ Thread $ThreadNumber falhou: $($response.Error)" -ForegroundColor Red
        return @{
            ThreadNumber = $ThreadNumber
            Success = $false
            Error = $response.Error
        }
    }
    else {
        Write-Host "✅ Thread $ThreadNumber sucesso: Reserva $($response.reservationId)" -ForegroundColor Green
        return @{
            ThreadNumber = $ThreadNumber
            Success = $true
            ReservationId = $response.reservationId
            Qty = $response.qty
        }
    }
}

# Executar reservas concorrentes
Write-Host "🚀 Iniciando $Threads reservas concorrentes..." -ForegroundColor Green
Write-Host ""

$jobs = @()
$results = @()

# Criar jobs para execução paralela
for ($i = 1; $i -le $Threads; $i++) {
    $job = Start-Job -ScriptBlock {
        param($BaseUrl, $Sku, $ReserveQuantity, $CorrelationId, $ThreadNumber)
        
        function Invoke-ApiRequest {
            param(
                [string]$Method,
                [string]$Endpoint,
                [string]$Data = $null,
                [string]$IdempotencyKey = $null
            )
            
            $headers = @{
                "X-Correlation-Id" = $CorrelationId
            }
            
            if ($Data) {
                $headers["Content-Type"] = "application/json"
                if ($IdempotencyKey) {
                    $headers["Idempotency-Key"] = $IdempotencyKey
                }
            }
            
            try {
                if ($Data) {
                    return Invoke-RestMethod -Uri "$BaseUrl$Endpoint" -Method $Method -Headers $headers -Body $Data
                }
                else {
                    return Invoke-RestMethod -Uri "$BaseUrl$Endpoint" -Method $Method -Headers $headers
                }
            }
            catch {
                return @{
                    Error = $_.Exception.Message
                    StatusCode = $_.Exception.Response.StatusCode.value__
                }
            }
        }
        
        $idempotencyKey = "concurrent-$ThreadNumber-$(Get-Date -Format 'HHmmss')"
        
        $reserveData = @{
            qty = $ReserveQuantity
            storeId = "STORE-01"
            ttlSeconds = 900
        } | ConvertTo-Json
        
        $response = Invoke-ApiRequest -Method "POST" -Endpoint "/items/$Sku/reserve" -Data $reserveData -IdempotencyKey $idempotencyKey
        
        if ($response.Error) {
            return @{
                ThreadNumber = $ThreadNumber
                Success = $false
                Error = $response.Error
            }
        }
        else {
            return @{
                ThreadNumber = $ThreadNumber
                Success = $true
                ReservationId = $response.reservationId
                Qty = $response.qty
            }
        }
    } -ArgumentList $BaseUrl, $Sku, $ReserveQuantity, $CorrelationId, $i
    
    $jobs += $job
}

# Aguardar todos os jobs terminarem
Write-Host "⏳ Aguardando conclusão das reservas..." -ForegroundColor Cyan
$jobs | Wait-Job | Out-Null

# Coletar resultados
foreach ($job in $jobs) {
    $result = Receive-Job -Job $job
    $results += $result
    Remove-Job -Job $job
}

# Analisar resultados
Write-Host ""
Write-Host "📊 Análise dos Resultados:" -ForegroundColor Green
Write-Host "=========================" -ForegroundColor Green

$successfulReservations = $results | Where-Object { $_.Success -eq $true }
$failedReservations = $results | Where-Object { $_.Success -eq $false }

Write-Host "✅ Reservas bem-sucedidas: $($successfulReservations.Count)" -ForegroundColor Green
Write-Host "❌ Reservas falharam: $($failedReservations.Count)" -ForegroundColor Red
Write-Host ""

if ($successfulReservations.Count -gt 0) {
    $totalReserved = ($successfulReservations | Measure-Object -Property Qty -Sum).Sum
    Write-Host "📦 Total reservado: $totalReserved unidades" -ForegroundColor Cyan
    Write-Host "📦 Estoque inicial: $AvailableStock unidades" -ForegroundColor Cyan
    Write-Host "📦 Máximo esperado: $AvailableStock unidades" -ForegroundColor Cyan
    Write-Host ""
    
    if ($totalReserved -le $AvailableStock) {
        Write-Host "🎉 SUCESSO: Nenhum oversell detectado!" -ForegroundColor Green
        Write-Host "✅ Sistema manteve consistência de estoque" -ForegroundColor Green
    }
    else {
        Write-Host "🚨 FALHA: Oversell detectado!" -ForegroundColor Red
        Write-Host "❌ Sistema permitiu reservar mais do que disponível" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "📋 Detalhes das Reservas:" -ForegroundColor Cyan
foreach ($result in $results) {
    if ($result.Success) {
        Write-Host "  ✅ Thread $($result.ThreadNumber): Reserva $($result.ReservationId) - $($result.Qty) unidades" -ForegroundColor Green
    }
    else {
        Write-Host "  ❌ Thread $($result.ThreadNumber): $($result.Error)" -ForegroundColor Red
    }
}

# Verificar estoque final
Write-Host ""
Write-Host "📊 Verificando estoque final..." -ForegroundColor Green
$finalAvailabilityResponse = Invoke-ApiRequest -Method "GET" -Endpoint "/availability?skus=$Sku&storeId=STORE-01"
if ($finalAvailabilityResponse.results -and $finalAvailabilityResponse.results.Count -gt 0) {
    $finalStock = $finalAvailabilityResponse.results[0].quantityAvailable
    $finalReserved = $finalAvailabilityResponse.results[0].reserved
    Write-Host "📦 Estoque final disponível: $finalStock unidades" -ForegroundColor Cyan
    Write-Host "🔒 Estoque final reservado: $finalReserved unidades" -ForegroundColor Cyan
}

Write-Host ""
Write-Host "🎯 Teste de concorrência concluído!" -ForegroundColor Green
Write-Host "🔗 Correlation ID: $CorrelationId" -ForegroundColor Cyan
