# 📋 Critérios de Aceite - API de Inventário

## 🎯 Visão Geral

Este documento define os critérios de aceite para validar se a implementação da API de inventário está completa e funcionando corretamente, atendendo a todos os requisitos especificados.

## ✅ 1. Endpoints OpenAPI Implementados

### 1.1 Endpoints Obrigatórios
- [ ] **POST /items** - Criar item
- [ ] **PUT /items/{sku}/adjust** - Ajustar estoque
- [ ] **POST /items/{sku}/reserve** - Reservar estoque
- [ ] **POST /items/{sku}/commit** - Confirmar reserva
- [ ] **POST /items/{sku}/release** - Liberar reserva
- [ ] **GET /items/{sku}** - Obter item
- [ ] **GET /availability** - Consultar disponibilidade
- [ ] **GET /items** - Listar itens

### 1.2 Compatibilidade OpenAPI
- [ ] Todos os endpoints retornam status codes corretos conforme OpenAPI
- [ ] Schemas de request/response compatíveis com OpenAPI 3.1
- [ ] Headers obrigatórios implementados (`Idempotency-Key`, `X-Correlation-Id`)
- [ ] Content-Type correto (`application/json`)

## ✅ 2. Idempotência Funcional

### 2.1 Chave de Idempotência
- [ ] **Mesma chave → mesma resposta**: Requisições com mesmo `Idempotency-Key` retornam resposta idêntica
- [ ] **Cache de 24h**: Chaves de idempotência são mantidas por 24 horas
- [ ] **Validação obrigatória**: Endpoints de mutação requerem `Idempotency-Key`
- [ ] **Resposta 400**: Falta de `Idempotency-Key` retorna erro 400

### 2.2 Cenários de Teste
- [ ] **Criar item**: Mesmo SKU com mesma chave retorna 200 (não 409)
- [ ] **Ajustar estoque**: Mesmo ajuste com mesma chave retorna mesmo resultado
- [ ] **Reservar estoque**: Mesma reserva com mesma chave retorna mesma resposta
- [ ] **Commit/Release**: Operações idempotentes funcionam corretamente

## ✅ 3. Conflitos de Versão (409)

### 3.1 Optimistic Locking
- [ ] **Version conflict**: Tentativa de ajuste com `expectedVersion` incorreto retorna 409
- [ ] **Problem Details**: Resposta 409 inclui RFC 7807 Problem Details completo
- [ ] **Mensagem clara**: Erro indica versão esperada vs. atual
- [ ] **Correlation ID**: Erro inclui `X-Correlation-Id` para rastreamento

### 3.2 Cenários de Teste
- [ ] **Ajuste concorrente**: Dois ajustes simultâneos, um deve retornar 409
- [ ] **Reserva concorrente**: Reservas simultâneas com versão incorreta retornam 409
- [ ] **Commit concorrente**: Commits simultâneos com versão incorreta retornam 409

## ✅ 4. Scheduler de Expiração

### 4.1 Reservas com TTL
- [ ] **TTL configurável**: Reservas têm tempo de vida configurável (padrão: 30min)
- [ ] **Scheduler ativo**: Background job executa a cada 1 minuto
- [ ] **Expiração automática**: Reservas expiradas são automaticamente liberadas
- [ ] **Ajuste de estoque**: Quantidade `reserved` é reduzida quando reserva expira

### 4.2 Cenários de Teste
- [ ] **Expiração natural**: Reserva expira após TTL configurado
- [ ] **Liberação automática**: Estoque `reserved` é ajustado automaticamente
- [ ] **Evento de expiração**: Evento `ReservationExpired` é emitido
- [ ] **Métricas**: Contador de expirações é incrementado

## ✅ 5. Cache com TTL e Invalidação

### 5.1 Cache Caffeine
- [ ] **TTL configurável**: Cache tem TTL de 8 segundos
- [ ] **Operações de leitura**: `GetItem`, `GetAvailability`, `SearchItems` usam cache
- [ ] **Invalidação por eventos**: Cache é invalidado via eventos de domínio
- [ ] **Fallback**: Cache-first com fallback para repositório

### 5.2 Cenários de Teste
- [ ] **Cache hit**: Primeira consulta popula cache, segunda retorna do cache
- [ ] **Cache miss**: Após TTL, consulta vai para repositório
- [ ] **Invalidação**: Ajuste de estoque invalida cache do item
- [ ] **Performance**: Consultas com cache são mais rápidas

## ✅ 6. Testes Passando

### 6.1 Cobertura de Domínio ≥70%
- [ ] **Testes unitários**: Domínio, serviços, repositórios
- [ ] **Testes de concorrência**: Múltiplas threads, zero oversell
- [ ] **Testes web**: MockMvc, status codes, validações
- [ ] **Testes de integração**: OpenAPI, contratos

### 6.2 Cenários Críticos
- [ ] **Zero oversell**: Reservas simultâneas não excedem estoque disponível
- [ ] **Version locking**: Optimistic locking previne conflitos
- [ ] **Fine-grained locks**: Locks por SKU+StoreId funcionam
- [ ] **Idempotência**: Operações idempotentes são consistentes

## ✅ 7. Observabilidade

### 7.1 Logging Estruturado
- [ ] **JSON format**: Logs em formato JSON estruturado
- [ ] **Correlation ID**: `X-Correlation-Id` presente em todos os logs
- [ ] **User ID**: `userId` presente quando autenticado
- [ ] **Request tracking**: Início e fim de requisições logados

### 7.2 Métricas Micrometer
- [ ] **Contadores**: Reservas criadas, commits, releases, expirações, conflitos
- [ ] **Timers**: Tempo de operações críticas (adjust, reserve, commit, release)
- [ ] **Endpoints**: `/actuator/metrics` e `/actuator/prometheus` funcionando
- [ ] **Grafana**: Métricas visíveis no dashboard

## ✅ 8. Segurança

### 8.1 Autenticação JWT
- [ ] **Bearer token**: Autenticação via `Authorization: Bearer <token>`
- [ ] **Perfil dev**: JWT desabilitado em desenvolvimento
- [ ] **Perfil prod**: JWT obrigatório em produção
- [ ] **User context**: `userId` disponível nos logs e métricas

### 8.2 Endpoints Públicos
- [ ] **Health check**: `/actuator/health` público
- [ ] **Métricas**: `/actuator/metrics` e `/actuator/prometheus` públicos
- [ ] **Swagger**: `/swagger-ui.html` público
- [ ] **API**: Endpoints de negócio requerem autenticação

## ✅ 9. Perfis de Execução

### 9.1 Perfil Local (In-Memory)
- [ ] **Repositórios em memória**: Dados não persistem entre execuções
- [ ] **JWT desabilitado**: Aceita qualquer token
- [ ] **Logging DEBUG**: Logs detalhados para desenvolvimento
- [ ] **Porta 8080**: Aplicação roda na porta padrão

### 9.2 Perfil File (Persistência)
- [ ] **Persistência JSON**: Dados salvos em arquivos JSON
- [ ] **Diretório data/**: Arquivos organizados por tipo
- [ ] **Lifecycle**: `@PostConstruct` e `@PreDestroy` funcionando
- [ ] **Recuperação**: Dados carregados na inicialização

## ✅ 10. Documentação e Exemplos

### 10.1 Documentação Completa
- [ ] **README.md**: Instruções de execução e configuração
- [ ] **run.md**: Exemplos detalhados de uso
- [ ] **QUICKSTART.md**: Guia de início rápido
- [ ] **CRITERIOS_ACEITE.md**: Este documento

### 10.2 Scripts de Execução
- [ ] **examples.sh/ps1**: Scripts com exemplos curl
- [ ] **concurrency-test.ps1**: Teste de concorrência
- [ ] **build-and-run.sh/ps1**: Scripts de build e execução
- [ ] **Docker**: Dockerfile e docker-compose funcionando

## 🧪 Como Validar

### 1. Executar Testes
```bash
# Executar todos os testes
mvn test

# Verificar cobertura
mvn test jacoco:report
```

### 2. Testar Endpoints
```bash
# Usar scripts de exemplo
./examples.sh
# ou
./examples.ps1
```

### 3. Testar Concorrência
```bash
# Executar teste de concorrência
./concurrency-test.ps1
```

### 4. Verificar Observabilidade
```bash
# Health check
curl http://localhost:8080/actuator/health

# Métricas
curl http://localhost:8080/actuator/metrics

# Prometheus
curl http://localhost:8080/actuator/prometheus
```

### 5. Verificar Swagger
- Acessar: http://localhost:8080/swagger-ui.html
- Validar todos os endpoints estão documentados
- Testar endpoints via interface

## 🎯 Critérios de Sucesso

### ✅ Implementação Completa
- [ ] Todos os 8 endpoints obrigatórios implementados
- [ ] OpenAPI 3.1 compatível
- [ ] Arquitetura em camadas funcionando
- [ ] Testes passando com cobertura ≥70%

### ✅ Funcionalidades Críticas
- [ ] Idempotência funcionando (mesma chave → mesma resposta)
- [ ] Optimistic locking (409 em conflitos de versão)
- [ ] Scheduler expirando reservas automaticamente
- [ ] Cache com TTL e invalidação por eventos

### ✅ Qualidade e Observabilidade
- [ ] Logging estruturado em JSON
- [ ] Métricas Micrometer funcionando
- [ ] Segurança JWT configurada
- [ ] Documentação completa e exemplos funcionando

### ✅ Execução e Deploy
- [ ] Perfis local e file funcionando
- [ ] Docker e docker-compose funcionando
- [ ] Scripts de execução funcionando
- [ ] Porta 8080 configurada corretamente

## 🚀 Próximos Passos

Após validar todos os critérios de aceite:

1. **Deploy em ambiente de teste**
2. **Testes de carga e performance**
3. **Monitoramento em produção**
4. **Documentação de operação**
5. **Treinamento da equipe**

---

> 📝 **Nota**: Este documento deve ser usado como checklist para validar se a implementação está pronta para produção. Todos os itens devem estar marcados como ✅ antes de considerar a implementação completa.
