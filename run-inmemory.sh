#!/bin/bash

# Script para executar a aplicação com perfil inmemory (H2)
echo "Iniciando aplicação com perfil inmemory (H2 in-memory database)..."

# Verificar se o Maven está disponível
if command -v mvn &> /dev/null; then
    echo "Usando Maven para executar a aplicação..."
    mvn spring-boot:run -Dspring-boot.run.profiles=inmemory
else
    echo "Maven não encontrado. Tentando executar diretamente..."
    
    # Verificar se o JAR foi compilado
    if [ -d "target/classes" ]; then
        echo "Executando com java diretamente..."
        java -cp "target/classes:target/dependency/*" -Dspring.profiles.active=inmemory com.quickcoders.infolabsproducts.InfoLabsProductsApplication
    else
        echo "Erro: Classes não compiladas. Execute 'mvn compile' primeiro."
        exit 1
    fi
fi

echo "Aplicação finalizada."

