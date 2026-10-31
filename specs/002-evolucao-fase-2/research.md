# Decisões de planejamento

## Inventário da fase 1

Os controladores em `serviceorder/api/controller/` já expõem abertura administrativa, rastreio por token,
aprovação e recusa pelo cliente. `ServiceOrderLifecycleIntegrationTest` cobre o fluxo de abertura,
aprovação, execução e rastreio; testes específicos cobrem segurança e consulta. A consulta administrativa
existente filtra estado e pagina, mas não define a priorização operacional exigida. O caso de uso
`DecideQuotation` autentica por token de rastreio, portanto a integração externa requer outro limite
de autenticação e deduplicação. A tabela de histórico atual aceita apenas atores ADMINISTRATOR e CUSTOMER;
um novo ator de integração exige migração compatível ou um mapeamento de domínio explicitamente definido.
`SecurityConfiguration` nega rotas não reconhecidas; a nova integração exige configuração explícita.

## Arquitetura

**Decisão**: manter monólito modular e implantar múltiplas réplicas sem estado. **Motivo**: preserva limites de domínio e atende escalabilidade. **Alternativa**: microsserviços exigem complexidade não solicitada.

## Cluster e banco

**Decisão**: AWS EKS com nodes gerenciados e RDS PostgreSQL, provisionados via Terraform. O banco deve ficar privado e acessível apenas pelo grupo de segurança da aplicação; definir backup, retenção e disponibilidade conforme orçamento. O pipeline acessa AWS por identidade de curta duração e permissões restritas. **Alternativas**: Kubernetes local reduz custo, mas não atende o objetivo escolhido de operar na AWS; banco efêmero perde dados. EKS, computação, rede e RDS geram custos; região, dimensionamento e estratégia Multi-AZ dependem do orçamento. Múltiplas réplicas não garantem alta disponibilidade sem capacidade e configuração adequadas.

## Integração de aprovação

**Decisão**: endpoint próprio com credencial de integração configurada externamente, comparação segura, TLS no ambiente exposto, chave de evento e versão do orçamento; reutilizar regra de decisão. **Alternativa**: token de cliente não identifica a integração nem fornece deduplicação de eventos.

## Notificações

**Decisão**: outbox transacional em PostgreSQL, worker dentro da aplicação e SMTP configurável; Mailpit local para validação. Bloqueio concorrente e leases evitam processamento simultâneo; entrega é pelo menos uma vez, pois SMTP não garante exatamente uma vez após falha. **Alternativa**: envio síncrono pode perder mensagem ou bloquear transição.

## Pipeline e segredos

**Decisão**: GitHub Actions com build e testes antes da imagem e implantação no EKS; identidade federada de curta duração e sem deploy em PRs não confiáveis. Estado Terraform em backend remoto protegido, com bloqueio e controle de acesso. **Alternativa**: segredos em YAML ou estado versionado expõem credenciais.

## Escala e medições

**Decisão**: HPA por CPU com requests definidos, metrics-server e teste de carga documentado; limitar réplicas e conexões ao banco. O alvo de latência no plano é inicial, não exigência numérica do PDF. **Alternativa**: contar ordens sem gerar carga não demonstra autoscaling.
