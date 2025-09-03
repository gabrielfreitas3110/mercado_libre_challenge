# Configuração PostgreSQL

Este documento descreve como configurar o PostgreSQL para o projeto Inventory API.

## Pré-requisitos

1. PostgreSQL instalado e rodando
2. Usuário `postgres` com senha `postgresql`
3. Porta padrão 5432 disponível

## Configuração do Banco de Dados

### Opção 1: Script Automático (Recomendado)

Execute o script PowerShell fornecido:

```powershell
.\scripts\setup-postgresql.ps1
```

### Opção 2: Manual

1. Conecte ao PostgreSQL como usuário postgres:
```bash
psql -U postgres -h localhost -p 5432
```

2. Execute o script SQL:
```sql
CREATE DATABASE inventory_db;
GRANT ALL PRIVILEGES ON DATABASE inventory_db TO postgres;
```

3. Conecte ao banco criado:
```sql
\c inventory_db;
```

## Configuração da Aplicação

A aplicação está configurada para usar:
- **Host**: localhost
- **Porta**: 5432
- **Banco**: inventory_db
- **Usuário**: postgres
- **Senha**: postgresql

## Migrações

O Flyway está configurado para executar automaticamente as migrações na inicialização da aplicação. As migrações estão localizadas em:
- `src/main/resources/db/migration/`

## Executando a Aplicação

Após configurar o banco de dados, execute:

```bash
.\mvnw.cmd spring-boot:run
```

A aplicação irá:
1. Conectar ao PostgreSQL
2. Executar as migrações automaticamente
3. Criar as tabelas necessárias
4. Iniciar o servidor na porta 8080

## Verificação

Para verificar se tudo está funcionando:

1. Acesse: http://localhost:8080/actuator/health
2. Verifique os logs da aplicação para confirmar a conexão com o banco
3. Acesse: http://localhost:8080/swagger-ui.html para ver a documentação da API

## Estrutura das Tabelas

- **items**: Armazena informações dos produtos
- **item_attributes**: Atributos dos produtos (chave-valor)
- **inventory_records**: Registros de estoque por loja
- **reservations**: Reservas de produtos

## Troubleshooting

### Erro de Conexão
- Verifique se o PostgreSQL está rodando
- Confirme as credenciais no `application.yaml`
- Verifique se a porta 5432 está disponível

### Erro de Migração
- Verifique se o banco `inventory_db` existe
- Confirme se o usuário `postgres` tem privilégios
- Verifique os logs do Flyway na inicialização
