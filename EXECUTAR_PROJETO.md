# Como Executar o Projeto Inventory API

## 🚀 Execução Rápida

### Opção 1: Scripts PowerShell (Recomendado)

```powershell
# 1. Verificar PostgreSQL
.\check-postgres.ps1

# 2. Executar aplicação (escolha um perfil)
.\run-local.ps1    # Perfil local (sem autenticação)
.\run-dev.ps1      # Perfil dev (sem autenticação)
```

### Opção 2: Comandos Maven Diretos

```bash
# Perfil local
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local

# Perfil dev  
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

## 📋 Pré-requisitos

1. **Java 17+** instalado
2. **PostgreSQL** instalado e rodando
3. **Maven** (ou usar o wrapper incluído)

## 🗄️ Configuração do Banco de Dados

### Credenciais PostgreSQL:
- **Host**: localhost
- **Porta**: 5432
- **Usuário**: postgres
- **Senha**: postgresql
- **Banco**: inventory_db

### Criar o banco (se necessário):
```sql
CREATE DATABASE inventory_db;
```

## 🌐 Acessos Após Execução

| Recurso | URL | Descrição |
|---------|-----|-----------|
| **API** | http://localhost:8080 | Endpoints da API |
| **Swagger UI** | http://localhost:8080/swagger-ui.html | Documentação interativa |
| **Health Check** | http://localhost:8080/actuator/health | Status da aplicação |
| **Métricas** | http://localhost:8080/actuator/metrics | Métricas da aplicação |

## 🔐 Configurações de Segurança

### Perfil Local/Dev:
- ✅ **Sem autenticação** (permitAll)
- ✅ **CSRF desabilitado**
- ✅ **JWT desabilitado**
- ✅ **Endpoints públicos**: GET /items, GET /availability

### Perfil Prod:
- 🔒 **Autenticação JWT obrigatória**
- 🔒 **Endpoints protegidos**
- 🔒 **Apenas health/prometheus públicos**

## 🧪 Testando a API

### 1. Verificar se está funcionando:
```bash
curl http://localhost:8080/actuator/health
```

### 2. Buscar produtos:
```bash
curl http://localhost:8080/items
```

### 3. Verificar disponibilidade:
```bash
curl "http://localhost:8080/availability?skus=SKU-123&storeId=STORE-01"
```

## 🐛 Troubleshooting

### Erro de Conexão com PostgreSQL:
1. Verifique se o PostgreSQL está rodando
2. Confirme as credenciais no `application.yaml`
3. Execute: `.\check-postgres.ps1`

### Erro de Porta em Uso:
```bash
# Verificar processos na porta 8080
netstat -ano | findstr :8080

# Matar processo (substitua PID)
taskkill /PID <PID> /F
```

### Erro de Compilação:
```bash
# Limpar e recompilar
.\mvnw.cmd clean compile
```

## 📁 Estrutura de Arquivos

```
src/main/resources/
├── application.yaml          # Configuração base
├── application-local.yaml    # Perfil local
├── application-dev.yaml      # Perfil dev
├── application-prod.yaml     # Perfil prod
└── db/migration/             # Scripts de migração
```

## 🎯 Próximos Passos

1. **Executar aplicação**: `.\run-local.ps1`
2. **Acessar Swagger**: http://localhost:8080/swagger-ui.html
3. **Testar endpoints** via interface ou curl
4. **Verificar logs** no console
5. **Configurar DynamoDB** para homologação (próxima fase)

