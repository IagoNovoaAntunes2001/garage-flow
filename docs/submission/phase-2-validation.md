# Fase 2 — evidências e pendências

## Evidências disponíveis

- [Repositório do projeto](https://github.com/IagoNovoaAntunes2001/garage-flow) e [lista de tarefas da fase 2](../../specs/002-evolucao-fase-2/tasks.md).
- [Especificação completa das APIs gerada pela aplicação](../api/openapi.json).
- [Arquitetura da aplicação, AWS e deploy](../architecture/phase-2-proposed.md) e [instruções de infraestrutura](../../infra/README.md).
- [Pipeline de build, testes, Karpenter e deploy executada com sucesso](https://github.com/IagoNovoaAntunes2001/garage-flow/actions/runs/41233829975).
- [Plano de infraestrutura sem alterações após provisionamento do Karpenter](https://github.com/IagoNovoaAntunes2001/garage-flow/actions/runs/41231926147).
- [Teste documentado de criação e consolidação de nó](../architecture/phase-2-karpenter.md). A instância temporária foi encerrada após o teste.
- [Job de planejamento e aplicação controlada do RDS no GitHub Actions](https://github.com/IagoNovoaAntunes2001/garage-flow/actions/runs/41262408554): validado com o banco existente.
- Teste de HPA: carga temporária no endpoint de saúde elevou a CPU de aproximadamente 2% para 341% do pedido, acima do alvo de 70%; o HPA aumentou de 2 para 4 réplicas, que ficaram disponíveis. A carga foi removida e os dois nós gerenciados suportaram o teste; após a janela de estabilização, o HPA reduziu para 2 réplicas e o Deployment ficou 2/2 disponível.
- Teste real no API Gateway: cadastro fictício de cliente, veículo, serviço e peça; abertura e rastreamento da OS; diagnóstico; solicitação de orçamento; aprovação externa idempotente; execução, finalização e entrega. A OS entregue não apareceu na listagem ativa. Os registros fictícios foram preservados para demonstração.
- Em 31/10/2026, autenticação e consulta administrativa pelo API Gateway responderam HTTP 200. A aplicação conectou ao RDS e a implantação ficou com duas réplicas disponíveis.
- A pipeline executa testes, cobertura e varredura de imagem; ver o resultado de cada execução para evidências da versão correspondente.

## Pendente para a submissão

- Configurar SMTP e validar a entrega real de notificações; o SES está em modo de testes caso seja escolhido.
- Repetir a demonstração de escala do HPA durante a gravação do vídeo.
- Complementar a validação das APIs com recusa, ordenação entre múltiplos estados e teste de persistência após reinício.
- Gravar e publicar o [vídeo de até 15 minutos](phase-2-video-script.md), informar sua URL no README e preparar o PDF de submissão com os links exigidos.
- Confirmar o compartilhamento e acesso exigidos pela instituição. O repositório da demonstração é público; verificar os requisitos específicos de entrega.
- Validar separadamente a criação de um banco novo e o bootstrap das credenciais da aplicação; o job do RDS já foi executado com o banco existente.
