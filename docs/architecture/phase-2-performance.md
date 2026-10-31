# Medição inicial da listagem operacional

Em 2026-10-30, no ambiente local macOS ARM64 com JVM 17.0.14 e PostgreSQL 16 em Testcontainers, o teste em `ServiceOrderLifecycleIntegrationTest` criou 10.000 ordens, incluindo aproximadamente 6.668 abertas, e mediu 50 consultas após 10 aquecimentos. Página de 20 itens, via MockMvc, sem rede externa: p95 de **15 ms**, abaixo do alvo preliminar de 500 ms.

No `EXPLAIN ANALYZE` da seleção de IDs, o PostgreSQL utilizou `ix_service_orders_status`, bitmap heap scan e ordenação top-N; tempo de execução registrado de 2,041 ms. Esse plano simplificado não inclui carregamento de relacionamentos e serialização; a medição MockMvc inclui o fluxo da página.

As ordens compartilham cliente e veículo e, exceto a original, possuem um item de serviço e não possuem histórico ou orçamento. Não há concorrência de usuários, tráfego de rede, dados de produção ou medições no EKS. Repetir com dados representativos e carga concorrente no ambiente de destino antes de concluir qualquer avaliação de capacidade ou disponibilidade.

Para reproduzir em uma máquina com Docker:

```sh
PHASE2_PERFORMANCE=true ./gradlew --no-daemon test --tests 'com.example.techchallenge.serviceorder.api.ServiceOrderLifecycleIntegrationTest.measure*' -Pkotlin.compiler.execution.strategy=in-process
```

O resultado e plano aparecem em `build/test-results/test/TEST-com.example.techchallenge.serviceorder.api.ServiceOrderLifecycleIntegrationTest.xml`; o banco de teste é temporário.
