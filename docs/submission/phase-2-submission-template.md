# SOAT — Tech Challenge — Fase 2

**Equipe e participantes:** preencher conforme a entrega.

**Repositório:** https://github.com/IagoNovoaAntunes2001/garage-flow

**Vídeo demonstrativo:** inserir o link do YouTube ou Vimeo após a gravação. O vídeo deve ser público ou não listado e ter até 15 minutos.

## Solução

API para gestão de ordens de serviço de oficina, implementada como monólito modular em Kotlin e Spring Boot, com PostgreSQL. A fase 2 contempla a abertura e consulta de ordens, a priorização da listagem, a integração de decisões de orçamento e o registro de notificações, além de implantação em Kubernetes com infraestrutura como código e CI/CD.

## Arquitetura e execução

O ambiente de demonstração usa API Gateway, Lambda authorizer, VPC Link, NLB interno, EKS e RDS PostgreSQL privado na região `us-east-2`. O Terraform descreve os recursos; o GitHub Actions executa build, testes, análise de imagem e deploy via runners CodeBuild. O HPA ajusta réplicas, e o Karpenter provisiona e consolida nós adicionais. O [desenho completo e fluxo de deploy](../architecture/phase-2-proposed.md), [instruções de infraestrutura](../../infra/README.md) e [evidências disponíveis](phase-2-validation.md) estão no repositório.

A [especificação OpenAPI completa](../api/openapi.json) pode ser importada no Postman; a versão gerada pela aplicação também fica disponível em `/v3/api-docs` no perfil local, conforme o [README](../../README.md). O [roteiro da gravação](phase-2-video-script.md) descreve os itens a demonstrar.

## Antes de exportar e entregar

- Preencher os dados da equipe e inserir a URL real do vídeo.
- Confirmar que o vídeo e o repositório estão acessíveis a quem fará a avaliação.
- Inserir o desenho da arquitetura neste documento ou garantir que o link no PDF seja acessível.
- Revisar as [pendências e evidências](phase-2-validation.md) e exportar a versão final em PDF.
