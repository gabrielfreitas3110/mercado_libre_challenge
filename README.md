# Distributed Inventory API (Marketplace Brazil)

API para gestão de estoque distribuído com consistência forte, reservas com TTL, CQRS e mutações idempotentes. Adequada para um marketplace com múltiplas lojas/vendedores em todo o Brasil.

## 🏗️ Arquitetura

### Camadas
- **config**: Configurações (OpenAPI, Security, CORS, Jackson, Schedulers)
- **web**: Controllers + mapeamento RFC 7807
- **domain**: Entidades, enums, value objects, eventos
- **service**: Regras de negócio e orquestração
- **repository**: Interfaces + implementações in-memory/arquivo
- **infra**: Cache Caffeine, idempotency store, correlation filter, event bus

### Tecnologias
- **Java 21**
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

### Criar Reserva
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

### Buscar Itens
```bash
curl -X GET "http://localhost:8080/items?q=headphone&page=0&pageSize=20" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### Verificar Disponibilidade
```bash
curl -X GET "http://localhost:8080/availability?skus=SKU-123,SKU-456&storeId=GLOBAL" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

## 🚀 Como Executar

### Pré-requisitos
- Java 21+
- Maven 3.8+
- Porta 8080 disponível

### Perfis de Execução

#### 1. Perfil Local (In-Memory) - Padrão
```bash
# Execução padrão com repositórios em memória
mvn spring-boot:run

# Dados em memória, reinicia limpo a cada execução
# Ideal para desenvolvimento e testes
```

#### 2. Perfil File (Persistência JSON)
```bash
# Execução com persistência em arquivos JSON
mvn spring-boot:run -Dspring-boot.run.profiles=file

# Dados persistidos em data/ (JSON)
# Mantém dados entre reinicializações
```

#### 3. Perfil Dev (Desenvolvimento)
```bash
# Execução sem autenticação JWT
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# JWT desabilitado, aceita qualquer token
# Logging DEBUG level
```

### Comandos Básicos
```bash
# Compilar
mvn clean compile

# Executar testes
mvn test

# Iniciar aplicação (perfil local)
mvn spring-boot:run

# Iniciar aplicação (perfil file)
mvn spring-boot:run -Dspring-boot.run.profiles=file

# Iniciar aplicação (perfil dev)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Acesso
- **API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health
- **Métricas**: http://localhost:8080/actuator/metrics
- **Prometheus**: http://localhost:8080/actuator/prometheus

### Exemplo Rápido
```bash
# 1. Criar item
curl -X POST http://localhost:8080/items \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: test-001" \
  -d '{"sku": "TEST-001", "name": "Item Teste"}'

# 2. Ajustar estoque
curl -X PUT http://localhost:8080/items/TEST-001/adjust \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: test-002" \
  -d '{"storeId": "STORE-01", "delta": 100}'

# 3. Verificar disponibilidade
curl -X GET "http://localhost:8080/availability?skus=TEST-001&storeId=STORE-01"
```

> 📖 **Documentação Completa**:
> - [QUICKSTART.md](QUICKSTART.md) - Guia de execução rápida
> - [run.md](run.md) - Exemplos detalhados e cenários de teste
> - [CRITERIOS_ACEITE.md](CRITERIOS_ACEITE.md) - Critérios de aceite e validação

### Build e Testes
```bash
# Build completo
mvn clean install

# Apenas testes
mvn test

# Testes com cobertura
mvn test jacoco:report
```

### Validação dos Critérios de Aceite
```bash
# Windows (PowerShell)
.\validate-criteria.ps1

# Linux/Mac (Bash)
./validate-criteria.sh

# Teste de concorrência
.\concurrency-test.ps1  # Windows
./concurrency-test.sh   # Linux/Mac
```

## 📚 Documentação

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs
- **OpenAPI YAML**: http://localhost:8080/openapi.yaml

### Acesso à Documentação
A documentação Swagger UI está disponível publicamente (não requer autenticação) para facilitar o desenvolvimento e testes.

## 📊 Observabilidade

### Métricas
- **Health Check**: http://localhost:8080/actuator/health
- **Métricas**: http://localhost:8080/actuator/metrics
- **Prometheus**: http://localhost:8080/actuator/prometheus

### Logging Estruturado
- **Formato**: JSON com correlation ID
- **Correlation ID**: Header `X-Correlation-Id` (gerado automaticamente se ausente)
- **User ID**: Incluído nos logs quando autenticado

### Métricas Disponíveis
- **Reservas**: `inventory.reservations.created`, `inventory.reservations.committed`, `inventory.reservations.released`, `inventory.reservations.expired`
- **Conflitos**: `inventory.conflicts.version`, `inventory.conflicts.optimistic_locking`
- **Operações**: `inventory.operations.reservation`, `inventory.operations.commit`, `inventory.operations.release`, `inventory.operations.adjust`

## 🔧 Configurações

### Cache
- **Caffeine**: Cache in-memory com expiração de 8 segundos
- **Idempotency**: Cache com expiração de 24 horas
- **Invalidação**: Automática por eventos de domínio

### Schedulers
- **Limpeza de Reservas**: Executa a cada minuto para remover reservas expiradas
- **Estatísticas de Cache**: Executa a cada 5 minutos para logar métricas

### Segurança
- **JWT Bearer**: Autenticação via token Bearer
- **CORS**: Configurado para permitir todas as origens
- **Stateless**: Sessões stateless
- **Perfil Dev**: JWT desabilitado para desenvolvimento

### JWT Configuration
```yaml
app:
  security:
    jwt:
      enabled: true  # false no perfil dev
      secret: local-secret-key-change-in-production
      expiration: 3600  # 1 hora em segundos
```

### Perfil File - Configurações de Arquivo
```yaml
app:
  data:
    file:
      items: data/items.json
      inventory: data/inventory.json
      reservations: data/reservations.json
      idempotency: data/idempotency.json
```

## 🏛️ Padrões Implementados

### RFC 7807 Problem Details
Todos os erros retornam detalhes estruturados seguindo o padrão RFC 7807.

### Idempotência
Todas as operações de escrita são idempotentes usando o header `Idempotency-Key`.

### Optimistic Locking
Ajustes de estoque usam versionamento para evitar conflitos.

### Event Sourcing
Eventos de domínio são publicados para auditoria e integração.

### CQRS
Separação entre comandos (writes) e queries (reads) com projeções otimizadas.

### Cache Strategy
- **Cache-First**: Operações de leitura verificam cache primeiro
- **Write-Through**: Respostas são armazenadas no cache após construção
- **Event-Driven Invalidation**: Cache invalidadado automaticamente por eventos
- **TTL**: Expiração automática de 8 segundos para evitar dados stale

### Security
- **JWT Bearer**: Autenticação via token Bearer
- **Stateless**: Sem sessões, cada requisição é autenticada independentemente
- **CORS**: Configurado para desenvolvimento
- **Problem Details**: Erros de autenticação seguem RFC 7807

## 🧪 Testes

### Estrutura de Testes
- **Unit Tests**: Testes unitários para services e repositories
- **Integration Tests**: Testes de integração para controllers
- **Contract Tests**: Testes de contrato OpenAPI

## 📦 Deploy

### Docker
```bash
docker build -t inventory-api .
docker run -p 8080:8080 inventory-api
```

### Kubernetes
```bash
kubectl apply -f k8s/
```

## 🤝 Contribuição

1. Fork o projeto
2. Crie uma branch para sua feature (`git checkout -b feature/AmazingFeature`)
3. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. Push para a branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

## 📄 Licença

Este projeto está licenciado sob a Licença MIT - veja o arquivo [LICENSE](LICENSE) para detalhes.
