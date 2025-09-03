# 🚀 Guia de Execução - Inventory API

## 📋 Índice
- [Perfis de Execução](#perfis-de-execução)
- [Inicialização](#inicialização)
- [Exemplos de Uso](#exemplos-de-uso)
- [Endpoints Disponíveis](#endpoints-disponíveis)
- [Headers Obrigatórios](#headers-obrigatórios)
- [Códigos de Status](#códigos-de-status)
- [Troubleshooting](#troubleshooting)

## 🔧 Perfis de Execução

### 1. Perfil Local (In-Memory) - Padrão
```bash
# Execução padrão com repositórios em memória
mvn spring-boot:run

# Ou explicitamente
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**Características:**
- ✅ Dados em memória (ConcurrentHashMap)
- ✅ Reinicia com dados limpos
- ✅ Ideal para desenvolvimento e testes
- ✅ Performance máxima

### 2. Perfil File (Persistência JSON)
```bash
# Execução com persistência em arquivos JSON
mvn spring-boot:run -Dspring-boot.run.profiles=file
```

**Características:**
- 💾 Dados persistidos em `data/` (JSON)
- 🔄 Dados mantidos entre reinicializações
- 📁 Estrutura: `data/items.json`, `data/inventory.json`, etc.
- ⚠️ Criação automática do diretório `data/`

## 🚀 Inicialização

### Pré-requisitos
- Java 21+
- Maven 3.8+
- Porta 8080 disponível

### Comandos de Inicialização

```bash
# 1. Clone e navegue para o diretório
cd InfoLabsProducts

# 2. Compile o projeto
mvn clean compile

# 3. Execute os testes
mvn test

# 4. Inicie a aplicação
mvn spring-boot:run
```

### Verificação de Saúde
```bash
# Health Check
curl http://localhost:8080/actuator/health

# Métricas
curl http://localhost:8080/actuator/metrics

# Swagger UI
open http://localhost:8080/swagger-ui.html
```

## 📚 Exemplos de Uso

### Headers Obrigatórios
```bash
# Para operações de mutação (POST, PUT)
-H "Idempotency-Key: unique-key-123"

# Para correlação (opcional, mas recomendado)
-H "X-Correlation-Id: corr-456"

# Content-Type
-H "Content-Type: application/json"
```

### 1. 🏷️ Gerenciamento de Itens

#### Criar Item
```bash
curl -X POST http://localhost:8080/items \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: create-item-001" \
  -H "X-Correlation-Id: corr-001" \
  -d '{
    "sku": "LAPTOP-001",
    "name": "Laptop Dell XPS 13",
    "attributes": {
      "brand": "Dell",
      "model": "XPS 13",
      "color": "Silver",
      "storage": "512GB SSD"
    }
  }'
```

#### Buscar Item
```bash
curl -X GET http://localhost:8080/items/LAPTOP-001 \
  -H "X-Correlation-Id: corr-002"
```

#### Buscar Itens (com filtros)
```bash
# Busca geral
curl -X GET "http://localhost:8080/items" \
  -H "X-Correlation-Id: corr-003"

# Busca por loja
curl -X GET "http://localhost:8080/items?storeId=STORE-01" \
  -H "X-Correlation-Id: corr-004"

# Busca com filtro de texto
curl -X GET "http://localhost:8080/items?q=laptop&storeId=STORE-01&page=0&pageSize=10" \
  -H "X-Correlation-Id: corr-005"
```

### 2. 📦 Ajuste de Estoque

#### Aumentar Estoque
```bash
curl -X PUT http://localhost:8080/items/LAPTOP-001/adjust \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: adjust-stock-001" \
  -H "X-Correlation-Id: corr-006" \
  -d '{
    "storeId": "STORE-01",
    "delta": 50,
    "expectedVersion": 1
  }'
```

#### Diminuir Estoque
```bash
curl -X PUT http://localhost:8080/items/LAPTOP-001/adjust \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: adjust-stock-002" \
  -H "X-Correlation-Id: corr-007" \
  -d '{
    "storeId": "STORE-01",
    "delta": -10,
    "expectedVersion": 2
  }'
```

#### Ajuste Global (sem storeId)
```bash
curl -X PUT http://localhost:8080/items/LAPTOP-001/adjust \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: adjust-global-001" \
  -H "X-Correlation-Id: corr-008" \
  -d '{
    "delta": 100
  }'
```

### 3. 🔒 Sistema de Reservas

#### Criar Reserva
```bash
curl -X POST http://localhost:8080/items/LAPTOP-001/reserve \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: reserve-001" \
  -H "X-Correlation-Id: corr-009" \
  -d '{
    "qty": 5,
    "storeId": "STORE-01",
    "ttlSeconds": 900,
    "expectedVersion": 3
  }'
```

#### Confirmar Reserva (Commit)
```bash
curl -X POST http://localhost:8080/items/LAPTOP-001/commit \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: commit-001" \
  -H "X-Correlation-Id: corr-010" \
  -d '{
    "reservationId": "RES-123",
    "expectedVersion": 4
  }'
```

#### Liberar Reserva (Release)
```bash
curl -X POST http://localhost:8080/items/LAPTOP-001/release \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: release-001" \
  -H "X-Correlation-Id: corr-011" \
  -d '{
    "reservationId": "RES-123",
    "expectedVersion": 5
  }'
```

### 4. 📊 Consulta de Disponibilidade

#### Disponibilidade por SKU
```bash
curl -X GET "http://localhost:8080/availability?skus=LAPTOP-001&storeId=STORE-01" \
  -H "X-Correlation-Id: corr-012"
```

#### Disponibilidade Múltipla
```bash
curl -X GET "http://localhost:8080/availability?skus=LAPTOP-001,MOUSE-001,KEYBOARD-001&storeId=STORE-01" \
  -H "X-Correlation-Id: corr-013"
```

#### Disponibilidade Global
```bash
curl -X GET "http://localhost:8080/availability?skus=LAPTOP-001" \
  -H "X-Correlation-Id: corr-014"
```

## 🎯 Cenários de Teste Completos

### Cenário 1: Fluxo Completo de Venda
```bash
# 1. Criar item
curl -X POST http://localhost:8080/items \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: scenario-001" \
  -H "X-Correlation-Id: scenario-001" \
  -d '{
    "sku": "PHONE-001",
    "name": "iPhone 15 Pro",
    "attributes": {"brand": "Apple", "storage": "256GB"}
  }'

# 2. Adicionar estoque
curl -X PUT http://localhost:8080/items/PHONE-001/adjust \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: scenario-002" \
  -H "X-Correlation-Id: scenario-001" \
  -d '{
    "storeId": "STORE-01",
    "delta": 100
  }'

# 3. Verificar disponibilidade
curl -X GET "http://localhost:8080/availability?skus=PHONE-001&storeId=STORE-01" \
  -H "X-Correlation-Id: scenario-001"

# 4. Reservar para cliente
curl -X POST http://localhost:8080/items/PHONE-001/reserve \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: scenario-003" \
  -H "X-Correlation-Id: scenario-001" \
  -d '{
    "qty": 2,
    "storeId": "STORE-01",
    "ttlSeconds": 1800
  }'

# 5. Confirmar venda
curl -X POST http://localhost:8080/items/PHONE-001/commit \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: scenario-004" \
  -H "X-Correlation-Id: scenario-001" \
  -d '{
    "reservationId": "RES-XXX",
    "expectedVersion": 2
  }'
```

### Cenário 2: Teste de Concorrência
```bash
# Script para testar reservas concorrentes
#!/bin/bash

# Criar item com estoque limitado
curl -X POST http://localhost:8080/items \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: concurrency-setup" \
  -d '{
    "sku": "LIMITED-001",
    "name": "Item Limitado",
    "attributes": {"type": "test"}
  }'

curl -X PUT http://localhost:8080/items/LIMITED-001/adjust \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: concurrency-stock" \
  -d '{
    "storeId": "STORE-01",
    "delta": 5
  }'

# Executar reservas concorrentes (5 threads, 2 unidades cada)
for i in {1..5}; do
  curl -X POST http://localhost:8080/items/LIMITED-001/reserve \
    -H "Content-Type: application/json" \
    -H "Idempotency-Key: concurrent-$i" \
    -d '{
      "qty": 2,
      "storeId": "STORE-01",
      "ttlSeconds": 900
    }' &
done

wait
echo "Reservas concorrentes concluídas"
```

### Cenário 3: Teste de Idempotência
```bash
# Executar a mesma operação múltiplas vezes
for i in {1..3}; do
  echo "Tentativa $i:"
  curl -X POST http://localhost:8080/items \
    -H "Content-Type: application/json" \
    -H "Idempotency-Key: idempotent-test" \
    -d '{
      "sku": "IDEM-001",
      "name": "Teste Idempotência"
    }'
  echo ""
done
```

## 📊 Endpoints Disponíveis

| Método | Endpoint | Descrição | Idempotency-Key |
|--------|----------|-----------|-----------------|
| POST | `/items` | Criar item | ✅ Obrigatório |
| GET | `/items/{sku}` | Buscar item | ❌ |
| GET | `/items` | Listar itens | ❌ |
| PUT | `/items/{sku}/adjust` | Ajustar estoque | ✅ Obrigatório |
| POST | `/items/{sku}/reserve` | Reservar estoque | ✅ Obrigatório |
| POST | `/items/{sku}/commit` | Confirmar reserva | ✅ Obrigatório |
| POST | `/items/{sku}/release` | Liberar reserva | ✅ Obrigatório |
| GET | `/availability` | Consultar disponibilidade | ❌ |

## 🔍 Códigos de Status

| Código | Significado | Quando Ocorre |
|--------|-------------|---------------|
| 200 | OK | Operação bem-sucedida |
| 201 | Created | Item criado com sucesso |
| 400 | Bad Request | Dados inválidos ou Idempotency-Key ausente |
| 404 | Not Found | Item não encontrado |
| 409 | Conflict | Conflito de versão (optimistic locking) |
| 500 | Internal Server Error | Erro interno do servidor |

## 🛠️ Troubleshooting

### Problemas Comuns

#### 1. Porta 8080 em uso
```bash
# Verificar processos na porta 8080
netstat -ano | findstr :8080

# Matar processo (Windows)
taskkill /PID <PID> /F

# Matar processo (Linux/Mac)
kill -9 <PID>
```

#### 2. Erro de Idempotency-Key
```bash
# Erro: Missing Idempotency Key
# Solução: Adicionar header obrigatório
-H "Idempotency-Key: unique-key-$(date +%s)"
```

#### 3. Conflito de Versão (409)
```bash
# Erro: Version conflict
# Solução: Buscar versão atual e tentar novamente
curl -X GET http://localhost:8080/items/SKU-001
# Usar a versão retornada no campo "version"
```

#### 4. Estoque Insuficiente
```bash
# Erro: Insufficient inventory
# Solução: Verificar disponibilidade antes de reservar
curl -X GET "http://localhost:8080/availability?skus=SKU-001&storeId=STORE-01"
```

### Logs e Debugging

#### Ativar Logs Detalhados
```bash
# Executar com logs DEBUG
mvn spring-boot:run -Dspring-boot.run.arguments="--logging.level.com.quickcoders.inventory.api=DEBUG"
```

#### Verificar Métricas
```bash
# Métricas de reservas
curl http://localhost:8080/actuator/metrics/inventory.reservations.created

# Métricas de conflitos
curl http://localhost:8080/actuator/metrics/inventory.conflicts.version
```

#### Health Check Detalhado
```bash
# Status completo da aplicação
curl http://localhost:8080/actuator/health | jq
```

## 🔧 Configurações Avançadas

### Variáveis de Ambiente
```bash
# Porta customizada
export SERVER_PORT=9090

# Perfil customizado
export SPRING_PROFILES_ACTIVE=file

# Log level
export LOGGING_LEVEL_COM_QUICKCODERS_INVENTORY_API=DEBUG
```

### Configuração de JVM
```bash
# Executar com configurações de memória
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx2g -Xms1g"
```

## 📈 Monitoramento

### Métricas Disponíveis
- `inventory.reservations.created` - Reservas criadas
- `inventory.reservations.committed` - Reservas confirmadas
- `inventory.reservations.released` - Reservas liberadas
- `inventory.reservations.expired` - Reservas expiradas
- `inventory.conflicts.version` - Conflitos de versão
- `inventory.operations.*` - Tempos de operação

### Prometheus
```bash
# Endpoint Prometheus
curl http://localhost:8080/actuator/prometheus
```

---

## 🎉 Pronto para Usar!

A API está configurada e pronta para uso. Comece com os exemplos básicos e evolua para cenários mais complexos conforme necessário.

**Dica:** Use sempre o `X-Correlation-Id` para rastrear requisições através dos logs estruturados em JSON.
