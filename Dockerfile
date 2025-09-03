# 🐳 Dockerfile - Inventory API

FROM openjdk:21-jdk-slim

# Metadados
LABEL maintainer="quickcoders@example.com"
LABEL description="Distributed Inventory API for Marketplace Brazil"
LABEL version="1.0.0"

# Configurar diretório de trabalho
WORKDIR /app

# Copiar arquivos de build
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn
COPY pom.xml .

# Copiar código fonte
COPY src src

# Instalar Maven (se necessário)
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Build da aplicação
RUN mvn clean package -DskipTests

# Expor porta
EXPOSE 8080

# Configurar variáveis de ambiente
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

# Comando de inicialização
CMD ["java", "-jar", "target/inventory-api-1.0.0.jar"]