# Especificação OpenAPI

[openapi.json](openapi.json) é a especificação completa gerada pela aplicação da fase 2 e pode ser importada em ferramentas compatíveis, como Postman. Os caminhos incluem `/api/v1`; o servidor usa a origem que servir o documento. A interface Swagger e o documento gerado ao vivo estão disponíveis no perfil `local`, respectivamente em `/swagger-ui.html` e `/v3/api-docs`.

O arquivo foi exportado a partir de uma imagem construída com o código atual. A verificação `OpenApiContractIntegrationTest` compara rotas, esquemas e configurações de segurança do arquivo com os gerados pela aplicação, excluindo apenas controladores e esquemas auxiliares existentes no perfil de teste. Ao alterar a API, execute a aplicação atual no perfil local, obtenha `/v3/api-docs`, substitua a URL do servidor pela origem `/`, atualize o arquivo e execute os testes antes de publicar. Não inclua tokens ou credenciais no arquivo.
