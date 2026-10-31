# Análise da imagem — fase 2

Em 2026-10-30, a imagem local `tech-challenge-backend` foi analisada com Trivy 0.69.3, usando base de vulnerabilidades atualizada e filtros HIGH e CRITICAL. A primeira análise apontou 6 achados críticos e 5 altos no Java. Nenhum achado HIGH/CRITICAL foi reportado para pacotes Ubuntu.

Tomcat foi atualizado de 10.1.55 para 10.1.61 e Jackson de 2.21.4 para 2.21.8; a suíte e a cobertura passaram após as alterações. A nova análise da imagem aponta **2 críticos e nenhum alto**, ambos em `org.springframework:spring-webmvc:6.2.19`:

- `CVE-2026-47884`: o scanner classifica como crítico; o [aviso do Spring](https://spring.io/security/cve-2026-47884/) classifica como médio e descreve condições envolvendo `XsltView` e resolução de views. A versão corrigida 6.2.20 é comercial; 7.0.9 é OSS.
- `CVE-2026-47890`: o scanner classifica como crítico; o [aviso do Spring](https://spring.io/security/cve-2026-47890/) classifica como baixo e descreve condições envolvendo fragmentos de view em Server-Sent Events. A versão corrigida 6.2.20 é comercial; 7.0.9 é OSS.

Em 2026-10-31, a aplicação foi migrada para Spring Boot 4.0.9 e Spring Framework 7, com Springdoc compatível, Flyway e testes ajustados. A suíte `./gradlew --no-daemon check koverVerify` passou; o teste opcional de desempenho ficou ignorado nessa execução. A imagem `garage-flow:security-check` foi construída e analisada com Trivy 0.69.3 e bases atualizadas: **nenhum achado HIGH ou CRITICAL** em Ubuntu ou Java, sem supressões. O gate de críticos da pipeline permanece ativo. A varredura é uma fotografia da imagem e das bases naquela data, não uma garantia permanente.

Os relatórios brutos temporários estão em `/private/tmp/garage-flow-trivy/` nesta máquina, incluindo `boot4-report.json`; reproduzir com a imagem construída e um scanner atualizado, pois resultados mudam com a base. A análise cobre dependências da imagem; não comprova segurança de configuração AWS, credenciais ou recursos em execução.
