#!/bin/bash

# Script de Teste de Concorrência
# API de Inventário - QuickCoders

set -e

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configurações
BASE_URL="http://localhost:8080"
THREADS=10
REQUESTS_PER_THREAD=5
SKU="CONCURRENCY-TEST"
STORE_ID="STORE-01"

echo -e "${CYAN}⚡ Teste de Concorrência - API de Inventário${NC}"
echo -e "${CYAN}===========================================${NC}"

# Função para log
log() {
    echo -e "${BLUE}[$(date +'%H:%M:%S')] $1${NC}"
}

# Função para testar endpoint
test_endpoint() {
    local url="$1"
    local method="$2"
    local headers="$3"
    local body="$4"
    
    local response=$(curl -s -w "%{http_code}" -X "$method" $headers -d "$body" "$url")
    local status_code="${response: -3}"
    echo "$status_code"
}

# Função para thread de teste
test_thread() {
    local thread_id="$1"
    local results=()
    
    for i in $(seq 1 $REQUESTS_PER_THREAD); do
        local timestamp=$(date +%s%N)
        local headers="-H 'Content-Type: application/json' -H 'Idempotency-Key: thread-$thread_id-req-$i-$timestamp' -H 'X-Correlation-Id: thread-$thread_id-req-$i-$timestamp'"
        
        # Testar reserva
        local reserve_body="{\"storeId\": \"$STORE_ID\", \"quantity\": 1, \"ttlMinutes\": 30}"
        local reserve_status=$(test_endpoint "$BASE_URL/items/$SKU/reserve" "POST" "$headers" "$reserve_body")
        
        if [ "$reserve_status" = "200" ] || [ "$reserve_status" = "201" ]; then
            # Testar commit
            local commit_body="{\"storeId\": \"$STORE_ID\", \"quantity\": 1}"
            local commit_status=$(test_endpoint "$BASE_URL/items/$SKU/commit" "POST" "$headers" "$commit_body")
            
            if [ "$commit_status" = "200" ] || [ "$commit_status" = "201" ]; then
                results+=("SUCCESS")
            else
                results+=("COMMIT_FAILED:$commit_status")
            fi
        else
            results+=("RESERVE_FAILED:$reserve_status")
        fi
        
        # Pequena pausa entre requisições
        sleep 0.1
    done
    
    # Retornar resultados
    printf '%s\n' "${results[@]}"
}

# Verificar se aplicação está rodando
log "Verificando se aplicação está rodando..."
if ! curl -s "$BASE_URL/actuator/health" > /dev/null; then
    echo -e "${RED}❌ Aplicação não está rodando em $BASE_URL${NC}"
    echo -e "${YELLOW}Execute: mvn spring-boot:run${NC}"
    exit 1
fi

log "Aplicação está rodando ✅"

# Criar item de teste
log "Criando item de teste..."
timestamp=$(date +%s%N)
headers="-H 'Content-Type: application/json' -H 'Idempotency-Key: setup-$timestamp' -H 'X-Correlation-Id: setup-$timestamp'"
create_body="{\"sku\": \"$SKU\", \"name\": \"Item Teste Concorrência\"}"

create_status=$(test_endpoint "$BASE_URL/items" "POST" "$headers" "$create_body")
if [ "$create_status" != "200" ] && [ "$create_status" != "201" ]; then
    echo -e "${RED}❌ Falha ao criar item: $create_status${NC}"
    exit 1
fi

log "Item criado ✅"

# Ajustar estoque inicial
log "Ajustando estoque inicial..."
adjust_body="{\"storeId\": \"$STORE_ID\", \"delta\": 100}"
adjust_status=$(test_endpoint "$BASE_URL/items/$SKU/adjust" "PUT" "$headers" "$adjust_body")
if [ "$adjust_status" != "200" ] && [ "$adjust_status" != "201" ]; then
    echo -e "${RED}❌ Falha ao ajustar estoque: $adjust_status${NC}"
    exit 1
fi

log "Estoque ajustado ✅"

# Executar teste de concorrência
log "Iniciando teste de concorrência..."
log "Threads: $THREADS"
log "Requisições por thread: $REQUESTS_PER_THREAD"
log "Total de requisições: $((THREADS * REQUESTS_PER_THREAD))"

# Executar threads em paralelo
pids=()
for i in $(seq 1 $THREADS); do
    test_thread $i > "thread_$i.log" &
    pids+=($!)
done

# Aguardar todas as threads terminarem
log "Aguardando threads terminarem..."
for pid in "${pids[@]}"; do
    wait $pid
done

log "Todas as threads terminaram ✅"

# Coletar resultados
log "Coletando resultados..."
total_requests=0
successful_requests=0
reserve_failures=0
commit_failures=0
version_conflicts=0

for i in $(seq 1 $THREADS); do
    while IFS= read -r result; do
        ((total_requests++))
        
        case $result in
            "SUCCESS")
                ((successful_requests++))
                ;;
            "RESERVE_FAILED:"*)
                ((reserve_failures++))
                local status="${result#RESERVE_FAILED:}"
                if [ "$status" = "409" ]; then
                    ((version_conflicts++))
                fi
                ;;
            "COMMIT_FAILED:"*)
                ((commit_failures++))
                local status="${result#COMMIT_FAILED:}"
                if [ "$status" = "409" ]; then
                    ((version_conflicts++))
                fi
                ;;
        esac
    done < "thread_$i.log"
done

# Verificar estoque final
log "Verificando estoque final..."
availability_response=$(curl -s "$BASE_URL/availability?skus=$SKU&storeId=$STORE_ID")
available_quantity=$(echo "$availability_response" | grep -o '"available":[0-9]*' | cut -d':' -f2)

# Calcular estoque esperado
expected_available=$((100 - successful_requests))

# Limpar arquivos temporários
rm -f thread_*.log

# Exibir resultados
echo -e "\n${CYAN}📊 Resultados do Teste de Concorrência${NC}"
echo -e "${CYAN}=====================================${NC}"

echo -e "\n${BLUE}📈 Estatísticas:${NC}"
echo -e "  • Total de requisições: $total_requests"
echo -e "  • Requisições bem-sucedidas: $successful_requests"
echo -e "  • Falhas de reserva: $reserve_failures"
echo -e "  • Falhas de commit: $commit_failures"
echo -e "  • Conflitos de versão (409): $version_conflicts"

echo -e "\n${BLUE}📦 Estoque:${NC}"
echo -e "  • Estoque inicial: 100"
echo -e "  • Estoque final disponível: $available_quantity"
echo -e "  • Estoque esperado: $expected_available"

echo -e "\n${BLUE}📊 Taxa de Sucesso:${NC}"
if [ $total_requests -gt 0 ]; then
    success_rate=$((successful_requests * 100 / total_requests))
    echo -e "  • Taxa de sucesso: $success_rate%"
else
    echo -e "  • Taxa de sucesso: 0%"
fi

# Validar resultados
echo -e "\n${CYAN}🔍 Validação:${NC}"

# Verificar se não houve oversell
if [ "$available_quantity" -ge "$expected_available" ]; then
    echo -e "${GREEN}✅ Zero oversell: Estoque final >= esperado${NC}"
else
    echo -e "${RED}❌ Oversell detectado: Estoque final < esperado${NC}"
    echo -e "${RED}   Diferença: $((expected_available - available_quantity))${NC}"
fi

# Verificar se houve conflitos de versão
if [ $version_conflicts -gt 0 ]; then
    echo -e "${GREEN}✅ Conflitos de versão detectados: $version_conflicts${NC}"
    echo -e "${GREEN}   Optimistic locking funcionando corretamente${NC}"
else
    echo -e "${YELLOW}⚠️  Nenhum conflito de versão detectado${NC}"
    echo -e "${YELLOW}   Pode indicar que não houve concorrência real${NC}"
fi

# Verificar taxa de sucesso
if [ $success_rate -ge 80 ]; then
    echo -e "${GREEN}✅ Taxa de sucesso aceitável: $success_rate%${NC}"
elif [ $success_rate -ge 50 ]; then
    echo -e "${YELLOW}⚠️  Taxa de sucesso baixa: $success_rate%${NC}"
else
    echo -e "${RED}❌ Taxa de sucesso muito baixa: $success_rate%${NC}"
fi

# Resultado final
if [ "$available_quantity" -ge "$expected_available" ] && [ $success_rate -ge 50 ]; then
    echo -e "\n${GREEN}🎉 TESTE DE CONCORRÊNCIA PASSOU!${NC}"
    echo -e "${GREEN}✅ Zero oversell confirmado${NC}"
    echo -e "${GREEN}✅ Sistema suporta concorrência${NC}"
    exit 0
else
    echo -e "\n${RED}❌ TESTE DE CONCORRÊNCIA FALHOU!${NC}"
    if [ "$available_quantity" -lt "$expected_available" ]; then
        echo -e "${RED}❌ Oversell detectado${NC}"
    fi
    if [ $success_rate -lt 50 ]; then
        echo -e "${RED}❌ Taxa de sucesso muito baixa${NC}"
    fi
    exit 1
fi
