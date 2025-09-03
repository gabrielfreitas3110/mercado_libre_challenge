#!/bin/bash

# 🚀 Script de Exemplos - Inventory API
# Execute este script para testar todos os endpoints da API

set -e

BASE_URL="http://localhost:8080"
CORRELATION_ID="example-$(date +%s)"

echo "🚀 Iniciando exemplos da Inventory API"
echo "📡 Base URL: $BASE_URL"
echo "🔗 Correlation ID: $CORRELATION_ID"
echo ""

# Função para fazer requisições com headers padrão
make_request() {
    local method=$1
    local endpoint=$2
    local data=$3
    local idempotency_key=$4
    
    echo "📤 $method $endpoint"
    if [ -n "$data" ]; then
        curl -s -X $method "$BASE_URL$endpoint" \
            -H "Content-Type: application/json" \
            -H "Idempotency-Key: $idempotency_key" \
            -H "X-Correlation-Id: $CORRELATION_ID" \
            -d "$data" | jq '.' 2>/dev/null || echo "Resposta recebida (não é JSON válido)"
    else
        curl -s -X $method "$BASE_URL$endpoint" \
            -H "X-Correlation-Id: $CORRELATION_ID" | jq '.' 2>/dev/null || echo "Resposta recebida (não é JSON válido)"
    fi
    echo ""
}

# Verificar se a API está rodando
echo "🔍 Verificando se a API está rodando..."
if ! curl -s "$BASE_URL/actuator/health" > /dev/null; then
    echo "❌ API não está rodando em $BASE_URL"
    echo "💡 Execute: mvn spring-boot:run"
    exit 1
fi
echo "✅ API está rodando!"
echo ""

# 1. Criar item
echo "🏷️ 1. Criando item..."
make_request "POST" "/items" '{
    "sku": "LAPTOP-001",
    "name": "Laptop Dell XPS 13",
    "attributes": {
        "brand": "Dell",
        "model": "XPS 13",
        "color": "Silver",
        "storage": "512GB SSD"
    }
}' "create-item-001"

# 2. Buscar item criado
echo "🔍 2. Buscando item criado..."
make_request "GET" "/items/LAPTOP-001"

# 3. Ajustar estoque (aumentar)
echo "📦 3. Ajustando estoque (+50 unidades)..."
make_request "PUT" "/items/LAPTOP-001/adjust" '{
    "storeId": "STORE-01",
    "delta": 50,
    "expectedVersion": 1
}' "adjust-stock-001"

# 4. Verificar disponibilidade
echo "📊 4. Verificando disponibilidade..."
make_request "GET" "/availability?skus=LAPTOP-001&storeId=STORE-01"

# 5. Criar reserva
echo "🔒 5. Criando reserva (5 unidades)..."
RESERVE_RESPONSE=$(curl -s -X POST "$BASE_URL/items/LAPTOP-001/reserve" \
    -H "Content-Type: application/json" \
    -H "Idempotency-Key: reserve-001" \
    -H "X-Correlation-Id: $CORRELATION_ID" \
    -d '{
        "qty": 5,
        "storeId": "STORE-01",
        "ttlSeconds": 900,
        "expectedVersion": 2
    }')

echo "📤 POST /items/LAPTOP-001/reserve"
echo "$RESERVE_RESPONSE" | jq '.' 2>/dev/null || echo "$RESERVE_RESPONSE"
echo ""

# Extrair reservation ID da resposta
RESERVATION_ID=$(echo "$RESERVE_RESPONSE" | jq -r '.reservationId' 2>/dev/null || echo "")

if [ -n "$RESERVATION_ID" ] && [ "$RESERVATION_ID" != "null" ]; then
    echo "✅ Reserva criada com ID: $RESERVATION_ID"
    
    # 6. Verificar disponibilidade após reserva
    echo "📊 6. Verificando disponibilidade após reserva..."
    make_request "GET" "/availability?skus=LAPTOP-001&storeId=STORE-01"
    
    # 7. Confirmar reserva (commit)
    echo "✅ 7. Confirmando reserva (commit)..."
    make_request "POST" "/items/LAPTOP-001/commit" "{
        \"reservationId\": \"$RESERVATION_ID\",
        \"expectedVersion\": 3
    }" "commit-001"
    
    # 8. Verificar disponibilidade após commit
    echo "📊 8. Verificando disponibilidade após commit..."
    make_request "GET" "/availability?skus=LAPTOP-001&storeId=STORE-01"
    
else
    echo "⚠️  Não foi possível extrair o ID da reserva, pulando operações de commit"
fi

# 9. Criar segundo item
echo "🏷️ 9. Criando segundo item..."
make_request "POST" "/items" '{
    "sku": "MOUSE-001",
    "name": "Mouse Logitech MX Master 3",
    "attributes": {
        "brand": "Logitech",
        "model": "MX Master 3",
        "color": "Graphite",
        "connectivity": "Bluetooth"
    }
}' "create-item-002"

# 10. Buscar todos os itens
echo "📋 10. Buscando todos os itens..."
make_request "GET" "/items"

# 11. Buscar itens com filtro
echo "🔍 11. Buscando itens com filtro 'laptop'..."
make_request "GET" "/items?q=laptop&page=0&pageSize=10"

# 12. Verificar disponibilidade múltipla
echo "📊 12. Verificando disponibilidade múltipla..."
make_request "GET" "/availability?skus=LAPTOP-001,MOUSE-001&storeId=STORE-01"

# 13. Ajustar estoque do segundo item
echo "📦 13. Ajustando estoque do mouse (+100 unidades)..."
make_request "PUT" "/items/MOUSE-001/adjust" '{
    "storeId": "STORE-01",
    "delta": 100
}' "adjust-stock-002"

# 14. Verificar métricas
echo "📈 14. Verificando métricas da aplicação..."
echo "📤 GET /actuator/metrics"
curl -s "$BASE_URL/actuator/metrics" | jq '.names[]' 2>/dev/null | head -10 || echo "Métricas disponíveis (formato não-JSON)"
echo ""

# 15. Health check
echo "🏥 15. Health check..."
make_request "GET" "/actuator/health"

echo "🎉 Exemplos concluídos!"
echo ""
echo "📖 Para mais exemplos e cenários avançados, consulte:"
echo "   - run.md - Documentação completa"
echo "   - http://localhost:8080/swagger-ui.html - Swagger UI"
echo ""
echo "🔗 Correlation ID usado: $CORRELATION_ID"
echo "💡 Use este ID para rastrear as requisições nos logs da aplicação"
