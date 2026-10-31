# Estimativa parcial de custos AWS

Referência: preços públicos consultados em 2026-10-30 para `us-east-2` (Ohio), região aprovada para o projeto. Valores em dólares, sob demanda e sem considerar créditos, impostos ou franquias. Não é um orçamento completo.

| Recurso | Quantidade | Valor por hora aproximado |
| --- | ---: | ---: |
| EKS em suporte padrão | 1 | US$ 0,1000 |
| EC2 Linux t3.medium | 2 | US$ 0,0832 |
| RDS PostgreSQL db.t4g.micro Single-AZ | 1 | US$ 0,0160 |
| NAT Gateway | 1 | US$ 0,0450 |
| Subtotal | | **US$ 0,2442/h** |

Esse subtotal equivale a cerca de US$ 5,86 por 24 horas ou US$ 178,27 em 730 horas. Ainda faltam armazenamento EBS e RDS, IPv4 público, tráfego e processamento do NAT, backups, logs, ECR, Secrets Manager, CodeBuild e os recursos opcionais API Gateway, VPC Link, NLB e Lambda. O total real pode ser maior, inclusive após exclusão do cluster se houver recursos retidos. Confirmar se os créditos da conta se aplicam a cada item e consultar custos efetivos antes e durante o uso.

O preço do EKS está na [tabela oficial](https://aws.amazon.com/eks/pricing/); o do NAT está no [exemplo oficial para Ohio](https://aws.amazon.com/vpc/pricing/). Valores de EC2 e RDS foram consultados pela AWS Price List API para Ohio. O custo depende também da duração exata do provisionamento e da remoção. O uso de créditos não significa custo zero de recursos, e esgotá-los pode encerrar o plano gratuito conforme suas regras.
