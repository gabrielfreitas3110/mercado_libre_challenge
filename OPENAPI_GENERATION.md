# OpenAPI Code Generation

Este projeto usa o OpenAPI Generator para gerar automaticamente código Java a partir da especificação OpenAPI.

## Arquivo OpenAPI

O arquivo de especificação está em: `src/main/resources/openapi/openapi.yaml`

## Como Gerar Código

### 1. Gerar apenas os arquivos OpenAPI (sem compilar)
```bash
mvn clean generate-sources
```

### 2. Gerar e compilar o projeto completo
```bash
mvn clean compile
```

### 3. Gerar durante o build
```bash
mvn clean install
```

## Arquivos Gerados

Após a geração, os seguintes arquivos serão criados em `target/generated-sources/openapi/`:

- **Models**: Classes Java para todos os schemas definidos no OpenAPI
- **APIs**: Interfaces Java para todos os endpoints definidos
- **Configuration**: Classes de configuração do Spring

## Estrutura dos Arquivos Gerados

```
target/generated-sources/openapi/
├── com/quickcoders/infolabsproducts/
│   ├── api/           # Interfaces dos controllers
│   ├── model/         # Classes dos DTOs
│   └── ApiClient.java # Cliente HTTP (se necessário)
```

## Configuração do Plugin

O plugin está configurado no `pom.xml` com as seguintes opções:

- **Generator**: `spring` (gera código Spring Boot)
- **Interface Only**: `true` (gera apenas interfaces)
- **Spring Boot 3**: `true` (usa Spring Boot 3)
- **Jackson**: Serialização com Jackson
- **Java 8**: Usa Java 8 date/time

## Implementação dos Controllers

Após gerar o código, você precisará implementar as interfaces geradas:

1. Criar classes que implementem as interfaces geradas
2. Adicionar anotações `@RestController` e `@RequestMapping`
3. Implementar a lógica de negócio

## Exemplo de Implementação

```java
@RestController
@RequestMapping("/api/v1")
public class ItemsController implements ItemsApi {
    
    @Override
    public ResponseEntity<Item> createItem(CreateItemRequest createItemRequest) {
        // Implementar lógica de criação
        return ResponseEntity.ok().build();
    }
    
    // Implementar outros métodos...
}
```

## Validação

O projeto inclui `spring-boot-starter-validation` para validação automática dos DTOs gerados.

## Documentação

A documentação da API estará disponível em:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

## Troubleshooting

### Erro de Compilação
Se houver erros de compilação após a geração:
1. Verifique se o arquivo `openapi.yaml` está válido
2. Execute `mvn clean` antes de gerar novamente
3. Verifique se todas as dependências estão no `pom.xml`

### Problemas de Validação
Se houver problemas com validação:
1. Verifique se `spring-boot-starter-validation` está incluído
2. Adicione anotações de validação nos schemas do OpenAPI se necessário
