# Fase 2 — evidências e pendências

## Evidências disponíveis

- [Repositório do projeto](https://github.com/IagoNovoaAntunes2001/garage-flow) e [lista de tarefas da fase 2](../../specs/002-evolucao-fase-2/tasks.md).
- [Arquitetura da aplicação, AWS e deploy](../architecture/phase-2-proposed.md) e [instruções de infraestrutura](../../infra/README.md).
- [Pipeline de build, testes, Karpenter e deploy executada com sucesso](https://github.com/IagoNovoaAntunes2001/garage-flow/actions/runs/41233829975).
- [Plano de infraestrutura sem alterações após provisionamento do Karpenter](https://github.com/IagoNovoaAntunes2001/garage-flow/actions/runs/41231926147).
- [Teste documentado de criação e consolidação de nó](../architecture/phase-2-karpenter.md). A instância temporária foi encerrada após o teste.
- Em 31/10/2026, autenticação e consulta administrativa pelo API Gateway responderam HTTP 200. A aplicação conectou ao RDS e a implantação ficou com duas réplicas disponíveis.
- A pipeline executa testes, cobertura e varredura de imagem; ver o resultado de cada execução para evidências da versão correspondente.

## Pendente para a submissão

- Configurar SMTP e validar a entrega real de notificações; o SES está em modo de testes caso seja escolhido.
- Registrar teste de escala de pods pelo HPA com carga, métricas e retorno após a carga. O teste de nós do Karpenter não substitui essa evidência.
- Executar e registrar os cenários completos de APIs da fase 2, incluindo decisões externas, ordenação e persistência.
- Gravar e publicar o [vídeo de até 15 minutos](phase-2-video-script.md), informar sua URL no README e preparar o PDF de submissão com os links exigidos.
- Confirmar o compartilhamento e acesso exigidos pela instituição. O repositório da demonstração é público; verificar os requisitos específicos de entrega.
- Confirmar por execução real o novo job de entrega do RDS; a criação de um banco novo e o bootstrap das credenciais da aplicação precisam de validação separada.
