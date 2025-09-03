#!/bin/bash

# Script de Validação dos Critérios de Aceite
# API de Inventário - QuickCoders

set -e

PROFILE="local"
SKIP_TESTS=false
SKIP_BUILD=false
VERBOSE=false

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
GRAY='\033[0;37m'
NC='\033[0m' # No Color

# Contadores
SUCCESS_COUNT=0
WARNING_COUNT=0
ERROR_COUNT=0

# Função para log
log_result() {
    local message="$1"
    local type="${2:-INFO}"
    
    case $type in
        "ERROR")
            echo -e "${RED}❌ $message${NC}"
            ((ERROR_COUNT++))
            ;;
        "WARNING")
            echo -e "${YELLOW}⚠️  $message${NC}"
            ((WARNING_COUNT++))
            ;;
        "SUCCESS")
            echo -e "${GREEN}✅ $message${NC}"
            ((SUCCESS_COUNT++))
            ;;
        *)
            echo -e "${BLUE}ℹ️  $message${NC}"
            ;;
    esac
}

# Função para executar comando
invoke_command() {
    local command="$1"
    local description="$2"
    
    if [ "$VERBOSE" = true ]; then
        echo -e "${GRAY}Executando: $command${NC}"
    fi
    
    if eval "$command"; then
        log_result "$description" "SUCCESS"
        return 0
    else
        log_result "$description - Erro: $?" "ERROR"
        return 1
    fi
}

# Função para testar endpoint
test_endpoint() {
    local url="$1"
    local method="${2:-GET}"
    local headers="${3:-}"
    local body="${4:-}"
    local expected_status="${5:-200}"
    
    local curl_cmd="curl -s -w '%{http_code}' -X $method"
    
    if [ -n "$headers" ]; then
        curl_cmd="$curl_cmd $headers"
    fi
    
    if [ -n "$body" ]; then
        curl_cmd="$curl_cmd -H 'Content-Type: application/json' -d '$body'"
    fi
    
    curl_cmd="$curl_cmd '$url'"
    
    local response=$(eval "$curl_cmd")
    local status_code="${response: -3}"
    
    if [ "$status_code" = "$expected_status" ]; then
        log_result "Endpoint $method $url - Status: $status_code" "SUCCESS"
        return 0
    else
        log_result "Endpoint $method $url - Status inesperado: $status_code (esperado: $expected_status)" "ERROR"
        return 1
    fi
}

# Processar argumentos
while [[ $# -gt 0 ]]; do
    case $1 in
        --profile)
            PROFILE="$2"
            shift 2
            ;;
        --skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        --skip-build)
            SKIP_BUILD=true
            shift
            ;;
        --verbose)
            VERBOSE=true
            shift
            ;;
        -h|--help)
            echo "Uso: $0 [opções]"
            echo "Opções:"
            echo "  --profile PROFILE    Perfil Spring Boot (padrão: local)"
            echo "  --skip-tests        Pular execução dos testes"
            echo "  --skip-build        Pular build e compilação"
            echo "  --verbose           Output verboso"
            echo "  -h, --help          Mostrar esta ajuda"
            exit 0
            ;;
        *)
            echo "Opção desconhecida: $1"
            exit 1
            ;;
    esac
done

echo -e "${CYAN}🔍 Validando Critérios de Aceite - API de Inventário${NC}"
echo -e "${CYAN}=================================================${NC}"

echo -e "\n${YELLOW}📋 1. Verificando Estrutura do Projeto${NC}"

# Verificar arquivos obrigatórios
required_files=(
    "pom.xml"
    "src/main/java/com/quickcoders/inventory/api/Application.java"
    "src/main/resources/application.yaml"
    "src/main/resources/openapi-inventory.yml"
    "README.md"
    "run.md"
    "QUICKSTART.md"
    "CRITERIOS_ACEITE.md"
)

for file in "${required_files[@]}"; do
    if [ -f "$file" ]; then
        log_result "Arquivo encontrado: $file" "SUCCESS"
    else
        log_result "Arquivo não encontrado: $file" "ERROR"
    fi
done

echo -e "\n${YELLOW}🔨 2. Build e Compilação${NC}"

if [ "$SKIP_BUILD" = false ]; then
    # Limpar e compilar
    invoke_command "mvn clean compile" "Compilação do projeto"
    
    # Verificar se compilou sem erros
    if [ $? -eq 0 ]; then
        log_result "Compilação bem-sucedida" "SUCCESS"
    else
        log_result "Erro na compilação" "ERROR"
    fi
fi

echo -e "\n${YELLOW}🧪 3. Executando Testes${NC}"

if [ "$SKIP_TESTS" = false ]; then
    # Executar testes
    invoke_command "mvn test" "Execução dos testes"
    
    # Verificar cobertura
    invoke_command "mvn test jacoco:report" "Geração do relatório de cobertura"
    
    # Verificar se testes passaram
    if [ $? -eq 0 ]; then
        log_result "Todos os testes passaram" "SUCCESS"
    else
        log_result "Alguns testes falharam" "ERROR"
    fi
fi

echo -e "\n${YELLOW}🚀 4. Iniciando Aplicação${NC}"

# Iniciar aplicação em background
mvn spring-boot:run -Dspring-boot.run.profiles=$PROFILE > app.log 2>&1 &
APP_PID=$!

# Aguardar aplicação iniciar
echo -e "${GRAY}Aguardando aplicação iniciar...${NC}"
sleep 30

# Verificar se aplicação está rodando
if curl -s http://localhost:8080/actuator/health > /dev/null; then
    log_result "Aplicação iniciada com sucesso" "SUCCESS"
else
    log_result "Falha ao iniciar aplicação" "ERROR"
    kill $APP_PID 2>/dev/null || true
    exit 1
fi

echo -e "\n${YELLOW}🌐 5. Testando Endpoints${NC}"

# Headers padrão
timestamp=$(date +%Y%m%d%H%M%S)
random_id=$(shuf -i 1000-9999 -n 1)
headers="-H 'Content-Type: application/json' -H 'Idempotency-Key: test-$timestamp' -H 'X-Correlation-Id: test-$random_id'"

# Testar endpoints obrigatórios
test_endpoint "http://localhost:8080/items" "POST" "$headers" '{"sku": "TEST-001", "name": "Item Teste"}'
test_endpoint "http://localhost:8080/items/TEST-001/adjust" "PUT" "$headers" '{"storeId": "STORE-01", "delta": 100}'
test_endpoint "http://localhost:8080/items/TEST-001" "GET" "$headers"
test_endpoint "http://localhost:8080/availability?skus=TEST-001&storeId=STORE-01" "GET" "$headers"
test_endpoint "http://localhost:8080/items" "GET" "$headers"

echo -e "\n${YELLOW}📊 6. Verificando Observabilidade${NC}"

# Testar endpoints de observabilidade
observability_endpoints=(
    "/actuator/health"
    "/actuator/metrics"
    "/actuator/prometheus"
    "/swagger-ui.html"
)

for endpoint in "${observability_endpoints[@]}"; do
    test_endpoint "http://localhost:8080$endpoint" "GET"
done

echo -e "\n${YELLOW}🔒 7. Testando Idempotência${NC}"

# Testar idempotência
idempotency_timestamp=$(date +%Y%m%d%H%M%S)
idempotency_random=$(shuf -i 1000-9999 -n 1)
idempotency_headers="-H 'Content-Type: application/json' -H 'Idempotency-Key: idempotency-test-$idempotency_timestamp' -H 'X-Correlation-Id: idempotency-test-$idempotency_random'"

# Primeira requisição
test_endpoint "http://localhost:8080/items" "POST" "$idempotency_headers" '{"sku": "IDEMPOTENCY-TEST", "name": "Teste Idempotência"}'

# Segunda requisição com mesma chave
test_endpoint "http://localhost:8080/items" "POST" "$idempotency_headers" '{"sku": "IDEMPOTENCY-TEST", "name": "Teste Idempotência"}'

echo -e "\n${YELLOW}⚡ 8. Testando Concorrência${NC}"

# Executar script de teste de concorrência se existir
if [ -f "concurrency-test.sh" ]; then
    invoke_command "./concurrency-test.sh" "Teste de concorrência"
elif [ -f "concurrency-test.ps1" ]; then
    log_result "Script de teste de concorrência PowerShell encontrado (use validate-criteria.ps1)" "WARNING"
else
    log_result "Script de teste de concorrência não encontrado" "WARNING"
fi

echo -e "\n${YELLOW}📋 9. Resumo da Validação${NC}"

echo -e "\n${GREEN}✅ Sucessos: $SUCCESS_COUNT${NC}"
echo -e "\n${YELLOW}⚠️  Avisos: $WARNING_COUNT${NC}"
echo -e "\n${RED}❌ Erros: $ERROR_COUNT${NC}"

# Parar aplicação
echo -e "\n${GRAY}🛑 Parando aplicação...${NC}"
kill $APP_PID 2>/dev/null || true

# Resultado final
if [ $ERROR_COUNT -eq 0 ]; then
    echo -e "\n${GREEN}🎉 VALIDAÇÃO CONCLUÍDA COM SUCESSO!${NC}"
    echo -e "${GREEN}Todos os critérios de aceite foram atendidos.${NC}"
    exit 0
else
    echo -e "\n${RED}❌ VALIDAÇÃO FALHOU!${NC}"
    echo -e "${RED}Existem $ERROR_COUNT erros que precisam ser corrigidos.${NC}"
    exit 1
fi
