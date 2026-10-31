# Roteiro do vídeo — fase 2

Publicar no YouTube ou Vimeo como público ou não listado, com duração máxima de 15 minutos. Os tempos abaixo são sugestões; substituir informações de exemplo pelo ambiente efetivamente implantado e não mostrar credenciais, dados pessoais nem configuração sensível.

1. **Objetivo e arquitetura (1 minuto)**: apresentar a oficina, o monólito, EKS, RDS, ECR, Terraform e a pipeline usando [o desenho proposto](../architecture/phase-2-proposed.md), atualizado para corresponder ao ambiente real.
2. **Infraestrutura e deploy (3 minutos)**: mostrar o plano e recursos já aprovados, resultado da execução da pipeline, implantação de banco, migrações, status do Deployment e disponibilidade da aplicação. Registrar a revisão dos custos antes de provisionar.
3. **CI/CD (2 minutos)**: mostrar uma execução real de build, testes, imagem, preparação do banco e aplicação de manifestos no cluster; não apresentar etapas simuladas como concluídas.
4. **APIs (4 minutos)**: demonstrar abertura da OS, retorno do identificador, consulta de status, decisão externa de aprovação ou recusa e listagem operacional priorizada. Mostrar comportamento para requisição inválida e notificação de status, sem expor tokens.
5. **Escalabilidade (3 minutos)**: mostrar métricas e número inicial de réplicas, gerar carga controlada, observar aumento das réplicas no HPA e apresentar recuperação após o fim da carga. Registrar também se havia capacidade de nodes suficiente.
6. **Encerramento (1 minuto)**: mostrar a documentação, collection, limitações do ambiente e link do repositório compartilhado com `soat-architecture`.

Após gravar, incluir a URL real no README e no PDF de submissão. Confirmar que o vídeo, o desenho e os comandos descrevem os recursos realmente implantados.
