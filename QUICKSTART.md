# 🚀 Quick Start - Inventory API

## ⚡ Execução Rápida

### 1. Iniciar a API
```bash
# Perfil padrão (in-memory)
mvn spring-boot:run

# Ou com persistência
mvn spring-boot:run -Dspring-boot.run.profiles=file
```

### 2. Verificar se está rodando
```bash
curl http://localhost:8080/actuator/health
```

### 3. Testar com exemplos
```bash
# Windows PowerShell
.\examples.ps1

# Linux/Mac
./examples.sh
```

## 🎯 Testes Rápidos

### Criar Item
```bash
curl -X POST http://localhost:8080/items \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: test-001" \
  -d '{"sku": "TEST-001", "name": "Item Teste"}'
```

### Ajustar Estoque
```bash
curl -X PUT http://localhost:8080/items/TEST-001/adjust \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: test-002" \
  -d '{"storeId": "STORE-01", "delta": 100}'
```

### Verificar Disponibilidade
```bash
curl "http://localhost:8080/availability?skus=TEST-001&storeId=STORE-01"
```

## 🔄 Teste de Concorrência
```bash
# Windows PowerShell
.\concurrency-test.ps1

# Parâmetros customizados
.\concurrency-test.ps1 -Threads 20 -AvailableStock 10 -ReserveQuantity 2
```

## 📊 Monitoramento
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health
- **Métricas**: http://localhost:8080/actuator/metrics
- **Prometheus**: http://localhost:8080/actuator/prometheus

## 🛠️ Perfis Disponíveis

| Perfil | Comando | Descrição |
|--------|---------|-----------|
| `local` | `mvn spring-boot:run` | In-memory, JWT habilitado |
| `file` | `mvn spring-boot:run -Dspring-boot.run.profiles=file` | Persistência JSON, JWT habilitado |
| `dev` | `mvn spring-boot:run -Dspring-boot.run.profiles=dev` | In-memory, JWT desabilitado |
| `test` | `mvn test` | Perfil de teste |

## 📖 Documentação Completa
- [run.md](run.md) - Guia completo de execução e exemplos
- [README.md](README.md) - Documentação principal

## 🚨 Troubleshooting

### Porta 8080 em uso
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Linux/Mac
lsof -ti:8080 | xargs kill -9
```

### Erro de Idempotency-Key
- Adicione o header: `-H "Idempotency-Key: unique-key-123"`

### Erro de autenticação
- Use o perfil dev: `mvn spring-boot:run -Dspring-boot.run.profiles=dev`
