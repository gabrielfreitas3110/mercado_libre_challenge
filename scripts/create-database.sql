-- Script para criar o banco de dados PostgreSQL
-- Execute este script como usuário postgres

-- Criar o banco de dados
CREATE DATABASE inventory_db;

-- Conceder privilégios ao usuário postgres
GRANT ALL PRIVILEGES ON DATABASE inventory_db TO postgres;

-- Conectar ao banco de dados
\c inventory_db;

-- Criar extensões necessárias (se não existirem)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
