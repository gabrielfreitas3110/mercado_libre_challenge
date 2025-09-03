# Distributed Inventory API (Marketplace Brazil)

API para gestão de estoque distribuído com consistência forte, reservas com TTL, CQRS e mutações idempotentes. Adequada para um marketplace com múltiplas lojas/vendedores em todo o Brasil.

## 🎯 SLOs e KPIs

### Performance
- **Freshness do estoque**: p95 < 5s desde a última atualização canônica
- **Latência de leitura**: p95 < 50ms (cache-first)
- **Latência de escrita**: p95 < 150ms (otimistic locking)

### Precisão
- **Oversell**: < 0,1% (controle otimista + locks finos)

### Custo Operacional
- **Custo por requisição**: ~R$ 0,001/req (infraestrutura compartilhada)
- **Custo mensal estimado**: R$ 500-2000/mês (dependendo do volume)

### Como Medir

#### Métricas Disponíveis
```bash
# Latência de leitura (p95)
curl "http://localhost:8080/actuator/prometheus" | grep "http_server_requests_seconds"

# Latência de escrita
curl "http://localhost:8080/actuator/prometheus" | grep "inventory_write_latency"

# Conflitos de versão
curl "http://localhost:8080/actuator/prometheus" | grep "inventory_version_conflicts_total"

# Oversell (deve ser próximo de zero)
curl "http://localhost:8080/actuator/prometheus" | grep "inventory_oversell_conflicts_total"
```

#### Scripts de Validação
```bash
# Teste de concorrência (valida oversell < 0,1%)
./scripts/concurrency_test.sh

# Validação de critérios de aceite
./scripts/validate-criteria.sh
```

#### Logs Estruturados
```bash
# Buscar por correlation ID
grep "correlationId" logs/application.log | jq

# Verificar latência de operações
grep "inventory_read_latency\|inventory_write_latency" logs/application.log
```

## 🏗️ Arquitetura

### Camadas
- **config**: Configurações (OpenAPI, Security, CORS, Jackson, Schedulers)
- **web**: Controllers + mapeamento RFC 7807
- **domain**: Entidades, enums, value objects, eventos
- **service**: Regras de negócio e orquestração
- **repository**: Interfaces + implementações in-memory/arquivo
- **infra**: Cache Caffeine, idempotency store, correlation filter, event bus

### Tecnologias
- **Java 17**
- **Spring Boot 3.5.5**
- **Maven**
- **Caffeine Cache**
- **SpringDoc OpenAPI**
- **Spring Security + JWT**
- **Micrometer + Actuator**
- **Lombok**

## 🚀 Endpoints

### Items (Write)
- `POST /items` - Criar item (SKU)
- `PUT /items/{sku}/adjust` - Ajustar estoque (conditional write)

### Reservations (Write)
- `POST /items/{sku}/reserve` - Criar reserva (com TTL)
- `POST /items/{sku}/commit` - Confirmar reserva
- `POST /items/{sku}/release` - Liberar reserva

### Inventory (Read)
- `GET /items/{sku}` - Detalhes do item + disponibilidade
- `GET /availability` - Leitura em lote por SKUs
- `GET /items` - Buscar itens (opcionalmente escopo por loja)

## 🔐 Headers Transversais

### Obrigatórios para Mutações
- `Idempotency-Key`: Chave de idempotência (máx 128 chars)

### Opcionais
- `X-Correlation-Id`: ID de correlação para rastreamento
- `Authorization`: Bearer JWT token (formato: `Bearer {token}`)

## 📋 Exemplos de Uso

### Criar Item
```bash
curl -X POST http://localhost:8080/items \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Idempotency-Key: key-123" \
  -d '{
    "sku": "SKU-123",
    "name": "Headphone XYZ",
    "attributes": {
      "color": "black",
      "brand": "ACME"
    }
  }'
```

### Ajustar Estoque
```bash
curl -X PUT http://localhost:8080/items/SKU-123/adjust \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Idempotency-Key: key-456" \
  -d '{
    "storeId": "SP-01",
    "delta": 10,
    "expectedVersion": 3
  }'
```

### Criar Reserva (com expectedVersion)
```bash
curl -X POST http://localhost:8080/items/SKU-123/reserve \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Idempotency-Key: key-789" \
  -d '{
    "qty": 2,
    "storeId": "GLOBAL",
    "ttlSeconds": 900,
    "expectedVersion": 5
  }'
```

### Confirmar Reserva
```bash
curl -X POST http://localhost:8080/items/SKU-123/commit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Idempotency-Key: key-101" \
  -d '{
    "reservationId": "res-123",
    "expectedVersion": 6
  }'
```

### Liberar Reserva
```bash
curl -X POST http://localhost:8080/items/SKU-123/release \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Idempotency-Key: key-102" \
  -d '{
    "reservationId": "res-123",
    "expectedVersion": 6
  }'
```

### Buscar Itens
```bash
curl -X GET "http://localhost:8080/items?q=headphone&page=0&pageSize=20" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

## ⚠️ Tratamento de Conflitos (409)

### Conflito de Versão
Quando ocorre conflito de versão (expectedVersion ≠ actualVersion), a API retorna 409 com Problem Details:

```json
{
  "type": "https://api.example.com/problems/conflict",
  "title": "Conflict",
  "status": 409,
  "detail": "Version conflict. Expected: 5, Actual: 6",
  "instance": "/api/items/SKU-123/reserve",
  "expectedVersion": 5,
  "actualVersion": 6,
  "reason": "version_conflict"
}
```

**Estratégia de Retry do Cliente:**
```bash
# 1. Fazer GET para obter versão atual
curl -X GET http://localhost:8080/items/SKU-123 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Resposta: {"item": {...}, "availability": [{"version": 6, ...}]}

# 2. Tentar novamente com versão correta
curl -X POST http://localhost:8080/items/SKU-123/reserve \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Idempotency-Key: key-789-retry" \
  -d '{
    "qty": 2,
    "storeId": "GLOBAL",
    "ttlSeconds": 900,
    "expectedVersion": 6
  }'
```

### Estoque Insuficiente
Quando não há estoque suficiente para reserva:

```json
{
  "type": "https://api.example.com/problems/conflict",
  "title": "Conflict",
  "status": 409,
  "detail": "Insufficient stock. Available: 100, Reserved: 95, Requested: 10",
  "instance": "/api/items/SKU-123/reserve",
  "reason": "insufficient_stock"
}
```

## 🔄 Idempotência

Todas as mutações são idempotentes através do header `Idempotency-Key`. A mesma chave sempre retorna o mesmo resultado.

**Exemplo de resposta idempotente:**
```bash
# Primeira requisição
curl -X POST http://localhost:8080/items/SKU-123/reserve \
  -H "Idempotency-Key: key-123" \
  -d '{"qty": 2, "expectedVersion": 1}'
# Retorna: 201 com reservationId: "res-abc"

# Segunda requisição com mesma chave
curl -X POST http://localhost:8080/items/SKU-123/reserve \
  -H "Idempotency-Key: key-123" \
  -d '{"qty": 2, "expectedVersion": 1}'
# Retorna: 201 com reservationId: "res-abc" (mesmo resultado)
```

## 🔄 Fluxos Completos

### Fluxo Reserve → Commit
```bash
# 1. Criar reserva
RESPONSE=$(curl -s -X POST http://localhost:8080/items/SKU-123/reserve \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Idempotency-Key: flow-reserve-$(date +%s)" \
  -d '{
    "qty": 3,
    "storeId": "SP-01",
    "ttlSeconds": 900,
    "expectedVersion": 1
  }')

# Extrair reservationId
RESERVATION_ID=$(echo $RESPONSE | jq -r '.reservationId')

# 2. Confirmar reserva
curl -X POST http://localhost:8080/items/SKU-123/commit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Idempotency-Key: flow-commit-$(date +%s)" \
  -d "{
    \"reservationId\": \"$RESERVATION_ID\",
    \"expectedVersion\": 2
  }"
```

### Fluxo Reserve → Release
```bash
# 1. Criar reserva
RESPONSE=$(curl -s -X POST http://localhost:8080/items/SKU-123/reserve \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Idempotency-Key: flow-release-reserve-$(date +%s)" \
  -d '{
    "qty": 2,
    "storeId": "SP-01",
    "ttlSeconds": 900,
    "expectedVersion": 1
  }')

# Extrair reservationId
RESERVATION_ID=$(echo $RESPONSE | jq -r '.reservationId')

# 2. Liberar reserva (antes do TTL expirar)
curl -X POST http://localhost:8080/items/SKU-123/release \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Idempotency-Key: flow-release-$(date +%s)" \
  -d "{
    \"reservationId\": \"$RESERVATION_ID\",
    \"expectedVersion\": 2
  }"
```

## 📊 Observabilidade

### Métricas Disponíveis
- `inventory.reservations.created` - Reservas criadas
- `inventory.reservations.committed` - Reservas confirmadas
- `inventory.reservations.released` - Reservas liberadas
- `inventory.reservations.expired` - Reservas expiradas
- `inventory.conflicts.version` - Conflitos de versão
- `inventory.conflicts.optimistic_locking` - Conflitos de lock otimista

### Endpoints de Monitoramento
- `GET /actuator/health` - Health check
- `GET /actuator/metrics` - Métricas disponíveis
- `GET /actuator/prometheus` - Métricas no formato Prometheus

## 🔒 Segurança por Ambiente

### Development (dev)
```bash
# Swagger UI habilitado
# JWT desabilitado
# CORS permissivo
./mvnw spring-boot:run -Dspring.profiles.active=dev
```

### Production (prod)
```bash
# Swagger UI desabilitado
# JWT obrigatório
# CORS restrito
./mvnw spring-boot:run -Dspring.profiles.active=prod
```

### Variáveis de Ambiente (Production)
```bash
export JWT_SECRET="your-secret-key"
export JWT_EXPIRATION="3600"
export CORS_ALLOWED_ORIGINS="https://api.example.com"
export CORS_ALLOWED_METHODS="GET,POST,PUT,DELETE"
export CORS_ALLOWED_HEADERS="Authorization,Content-Type,X-Correlation-Id,Idempotency-Key"
export CORS_ALLOW_CREDENTIALS="false"
```

## 🚀 Como Executar

### Pré-requisitos
- Java 17+
- Maven 3.6+

### Execução Local
```bash
# Compilar
mvn clean compile

# Executar (dev profile)
mvn spring-boot:run -Dspring.profiles.active=dev

# Executar (prod profile)
mvn spring-boot:run -Dspring.profiles.active=prod

# Ou usar o wrapper
./mvnw spring-boot:run
```

### Acessar Documentação
- **Swagger UI**: http://localhost:8080/swagger-ui.html (dev apenas)
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs (dev apenas)
- **Health Check**: http://localhost:8080/actuator/health

## 🧪 Testes

```bash
# Executar todos os testes
mvn test

# Executar testes de domínio
mvn test -Dtest=*Test

# Executar testes de integração
mvn test -Dtest=*ControllerTest

# Verificar cobertura
mvn jacoco:report
```

## 🔄 CI/CD

### GitHub Actions
O projeto inclui CI automatizado com:
- Build e testes em JDK 17
- Execução de scripts de validação
- Upload de relatórios de cobertura
- Análise de segurança

### Workflow
```yaml
# .github/workflows/ci.yml
- Build com Maven
- Testes unitários e integração
- Execução de scripts de validação
- Upload de artifacts
```

## 📋 Checklist de Deploy

### ✅ Pré-deploy
- [ ] Todos os testes passando
- [ ] Cobertura de código ≥ 70%
- [ ] Scripts de validação executados
- [ ] Métricas configuradas
- [ ] Logs estruturados ativos

### ✅ Deploy
- [ ] Profile correto (prod)
- [ ] Variáveis de ambiente configuradas
- [ ] JWT habilitado
- [ ] Swagger desabilitado
- [ ] CORS restrito

### ✅ Pós-deploy
- [ ] Health check respondendo
- [ ] Métricas expostas
- [ ] Logs estruturados funcionando
- [ ] Performance dentro dos SLOs
