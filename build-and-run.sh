#!/bin/bash

# 🚀 Script de Build e Execução - Inventory API

set -e

PROFILE="local"
BUILD=false
TEST=false
DOCKER=false

# Função de ajuda
show_help() {
    echo "🚀 Script de Build e Execução - Inventory API"
    echo ""
    echo "Uso:"
    echo "  ./build-and-run.sh [opções]"
    echo ""
    echo "Opções:"
    echo "  -p, --profile <perfil>    Perfil a usar (local, file, dev, prod)"
    echo "  -b, --build              Executar build antes de rodar"
    echo "  -t, --test               Executar testes"
    echo "  -d, --docker             Usar Docker em vez de Maven"
    echo "  -h, --help               Mostrar esta ajuda"
    echo ""
    echo "Exemplos:"
    echo "  ./build-and-run.sh                    # Executar com perfil local"
    echo "  ./build-and-run.sh -p file            # Executar com perfil file"
    echo "  ./build-and-run.sh -b -t              # Build, test e executar"
    echo "  ./build-and-run.sh -d                 # Executar com Docker"
}

# Parse argumentos
while [[ $# -gt 0 ]]; do
    case $1 in
        -p|--profile)
            PROFILE="$2"
            shift 2
            ;;
        -b|--build)
            BUILD=true
            shift
            ;;
        -t|--test)
            TEST=true
            shift
            ;;
        -d|--docker)
            DOCKER=true
            shift
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            echo "❌ Opção desconhecida: $1"
            show_help
            exit 1
            ;;
    esac
done

echo "🚀 Build and Run - Inventory API"
echo "📋 Perfil: $PROFILE"
echo ""

# Verificar se Maven está disponível
if [ "$DOCKER" = false ]; then
    if ! command -v mvn &> /dev/null; then
        echo "❌ Maven não encontrado. Instale o Maven ou use -d"
        exit 1
    fi
    echo "✅ Maven encontrado"
fi

# Verificar se Docker está disponível (se necessário)
if [ "$DOCKER" = true ]; then
    if ! command -v docker &> /dev/null; then
        echo "❌ Docker não encontrado"
        exit 1
    fi
    echo "✅ Docker encontrado"
fi

# Executar testes se solicitado
if [ "$TEST" = true ]; then
    echo "🧪 Executando testes..."
    if mvn test; then
        echo "✅ Testes passaram!"
    else
        echo "❌ Testes falharam!"
        exit 1
    fi
    echo ""
fi

# Executar build se solicitado
if [ "$BUILD" = true ]; then
    echo "🔨 Executando build..."
    if mvn clean compile; then
        echo "✅ Build concluído!"
    else
        echo "❌ Build falhou!"
        exit 1
    fi
    echo ""
fi

# Executar aplicação
if [ "$DOCKER" = true ]; then
    echo "🐳 Executando com Docker..."
    
    # Build da imagem Docker
    echo "🔨 Construindo imagem Docker..."
    if docker build -t inventory-api .; then
        echo "✅ Imagem Docker construída!"
    else
        echo "❌ Erro ao construir imagem Docker"
        exit 1
    fi
    
    # Executar container
    echo "🚀 Iniciando container..."
    docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE="$PROFILE" inventory-api
else
    echo "☕ Executando com Maven..."
    
    if [ "$PROFILE" = "local" ]; then
        mvn spring-boot:run
    else
        mvn spring-boot:run -Dspring-boot.run.profiles="$PROFILE"
    fi
fi

echo ""
echo "🎉 Aplicação executada com sucesso!"
echo "📡 API disponível em: http://localhost:8080"
echo "📚 Swagger UI: http://localhost:8080/swagger-ui.html"
echo "🏥 Health Check: http://localhost:8080/actuator/health"
