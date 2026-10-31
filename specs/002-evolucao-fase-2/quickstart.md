# Guia de validação da fase 2

Roteiro a executar após implementação; arquivos e comandos específicos de Terraform, Kubernetes e pipeline ainda serão criados. Usar Docker, Java 17, Terraform, kubectl e ferramentas de cluster nas versões documentadas pelo projeto. Não usar dados pessoais ou credenciais reais nos exemplos.

1. Executar `./gradlew clean test koverVerify`; iniciar `docker compose up --build` e conferir `/actuator/health`.
2. Criar cliente, veículo e itens; abrir OS autenticada e verificar ID. Consultar rastreio com token específico, validar estados e verificar negação de acesso indevido.
3. Criar OS em cada estado e com datas distintas. Consultar listagem operacional em múltiplas páginas: prioridade correta, antigas primeiro, desempate estável e ausência de finalizadas/entregues. Confirmar consulta histórica.
4. Enviar aprovação e recusa com credencial de integração; repetir evento, alterar conteúdo com mesmo ID e testar versão obsoleta. Verificar respostas e ausência de duplicação ou execução não autorizada.
5. Usar SMTP local; transicionar OS e confirmar mensagem. Induzir falha de envio, conferir registro e recuperação após restabelecer serviço.
6. Revisar custos e plano Terraform antes de aplicar em AWS; verificar EKS e RDS privado, configurar segredos sem versioná-los, publicar imagem e aplicar `k8s/`. Confirmar rollout, readiness, migração e APIs.
7. Executar CI/CD em ambiente autorizado e verificar build, testes, imagem, banco e deploy. Gerar carga até ultrapassar o alvo do HPA; registrar métricas e mudança de réplicas e verificar retorno gradual após cessar carga.
8. Revisar README, desenho de componentes/infraestrutura/deploy e collection; gravar vídeo público ou não listado de até 15 minutos com deploy, pipeline, APIs e escala. Preparar PDF com repositório compartilhado com `soat-architecture`, arquitetura e link do vídeo.

Execução depende das credenciais e do ambiente escolhido. Não destruir volumes ou infraestrutura sem ação deliberada.
