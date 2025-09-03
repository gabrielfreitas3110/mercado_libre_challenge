#!/bin/bash

# Script de teste de concorrência para validar oversell < 0,1%
# Simula múltiplas reservas simultâneas e verifica se há oversell

set -e

BASE_URL="http://localhost:8080"
SKU="CONCURRENCY-TEST-001"
STORE_ID="TEST-STORE"
TOTAL_QUANTITY=100
CONCURRENT_REQUESTS=50
REQUEST_PER_THREAD=10

echo "🧪 Teste de Concorrência - Validando Oversell < 0,1%"
echo "=================================================="

# 1. Verificar se a aplicação está rodando
echo "1. Verificando se a aplicação está rodando..."
if ! curl -s "$BASE_URL/actuator/health" > /dev/null; then
    echo "❌ Aplicação não está rodando em $BASE_URL"
    echo "   Execute: mvn spring-boot:run"
    exit 1
fi
echo "✅ Aplicação está rodando"

# 2. Criar item de teste
echo "2. Criando item de teste..."
curl -s -X POST "$BASE_URL/items" \
    -H "Content-Type: application/json" \
    -H "Idempotency-Key: create-item-$(date +%s)" \
    -d "{\"sku\": \"$SKU\", \"name\": \"Concurrency Test Item\"}" > /dev/null

# 3. Ajustar estoque inicial
echo "3. Ajustando estoque inicial para $TOTAL_QUANTITY..."
curl -s -X PUT "$BASE_URL/items/$SKU/adjust" \
    -H "Content-Type: application/json" \
    -H "Idempotency-Key: adjust-stock-$(date +%s)" \
    -d "{\"storeId\": \"$STORE_ID\", \"delta\": $TOTAL_QUANTITY, \"expectedVersion\": 1}" > /dev/null

echo "✅ Estoque inicial configurado: $TOTAL_QUANTITY unidades"

# 4. Função para fazer reservas
make_reservation() {
    local thread_id=$1
    local request_id=$2
    local qty=$3
    
    local idempotency_key="reserve-$thread_id-$request_id-$(date +%s)"
    local correlation_id="corr-$thread_id-$request_id"
    
    response=$(curl -s -w "%{http_code}" -X POST "$BASE_URL/items/$SKU/reserve" \
        -H "Content-Type: application/json" \
        -H "Idempotency-Key: $idempotency_key" \
        -H "X-Correlation-Id: $correlation_id" \
        -d "{\"qty\": $qty, \"storeId\": \"$STORE_ID\", \"ttlSeconds\": 300, \"expectedVersion\": 2}")
    
    http_code="${response: -3}"
    body="${response%???}"
    
    if [ "$http_code" = "201" ]; then
        echo "✅ Thread $thread_id: Reserva $request_id criada (qty: $qty)"
        echo "SUCCESS"
    elif [ "$http_code" = "409" ]; then
        echo "❌ Thread $thread_id: Reserva $request_id falhou - conflito (qty: $qty)"
        echo "CONFLICT"
    else
        echo "❌ Thread $thread_id: Reserva $request_id falhou - código $http_code (qty: $qty)"
        echo "ERROR"
    fi
}

# 5. Executar testes de concorrência
echo "4. Executando testes de concorrência..."
echo "   - $CONCURRENT_REQUESTS threads"
echo "   - $REQUEST_PER_THREAD requisições por thread"
echo "   - Total: $((CONCURRENT_REQUESTS * REQUEST_PER_THREAD)) requisições"

# Criar arquivo temporário para resultados
results_file=$(mktemp)
success_count=0
conflict_count=0
error_count=0

# Função para executar thread
run_thread() {
    local thread_id=$1
    for i in $(seq 1 $REQUEST_PER_THREAD); do
        result=$(make_reservation $thread_id $i 2)
        echo "$result" >> "$results_file"
        sleep 0.1  # Pequena pausa entre requisições
    done
}

# Executar threads em background
for thread in $(seq 1 $CONCURRENT_REQUESTS); do
    run_thread $thread &
done

# Aguardar todas as threads terminarem
wait

# 6. Contar resultados
echo "5. Analisando resultados..."
while IFS= read -r result; do
    case $result in
        "SUCCESS")
            ((success_count++))
            ;;
        "CONFLICT")
            ((conflict_count++))
            ;;
        "ERROR")
            ((error_count++))
            ;;
    esac
done < "$results_file"

total_requests=$((CONCURRENT_REQUESTS * REQUEST_PER_THREAD))
success_rate=$(echo "scale=2; $success_count * 100 / $total_requests" | bc)
conflict_rate=$(echo "scale=2; $conflict_count * 100 / $total_requests" | bc)

echo ""
echo "📊 Resultados do Teste de Concorrência:"
echo "======================================"
echo "Total de requisições: $total_requests"
echo "Sucessos: $success_count ($success_rate%)"
echo "Conflitos (409): $conflict_count ($conflict_rate%)"
echo "Erros: $error_count"

# 7. Verificar oversell
echo ""
echo "6. Verificando oversell..."
if [ "$conflict_rate" = "0.00" ]; then
    echo "❌ ALERTA: Nenhum conflito detectado - possível oversell!"
    echo "   Verifique se o controle otimista está funcionando"
elif (( $(echo "$conflict_rate < 0.1" | bc -l) )); then
    echo "✅ Oversell < 0,1% - Teste PASSOU"
    echo "   Taxa de conflitos: $conflict_rate%"
else
    echo "❌ Oversell > 0,1% - Teste FALHOU"
    echo "   Taxa de conflitos: $conflict_rate%"
    echo "   Esperado: < 0,1%"
fi

# 8. Verificar métricas
echo ""
echo "7. Verificando métricas de conflitos..."
METRICS=$(curl -s "$BASE_URL/actuator/prometheus")

version_conflicts=$(echo "$METRICS" | grep "inventory_version_conflicts_total" | grep -v "#" | awk '{print $2}' | head -1)
oversell_conflicts=$(echo "$METRICS" | grep "inventory_oversell_conflicts_total" | grep -v "#" | awk '{print $2}' | head -1)

echo "   Conflitos de versão: ${version_conflicts:-0}"
echo "   Conflitos de oversell: ${oversell_conflicts:-0}"

# 9. Limpeza
rm -f "$results_file"

echo ""
echo "🎯 Próximos passos:"
echo "1. Monitore as métricas: curl $BASE_URL/actuator/prometheus"
echo "2. Verifique os logs: tail -f logs/application.log | grep correlationId"
echo "3. Execute novamente para validar consistência"

echo ""
if (( $(echo "$conflict_rate < 0.1" | bc -l) )); then
    echo "✅ Teste de concorrência PASSOU!"
    exit 0
else
    echo "❌ Teste de concorrência FALHOU!"
    exit 1
fi

