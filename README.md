# Cielo Ticket Sales

App Android (Kotlin) de venda de ingressos para eventos locais, pago numa maquininha **Cielo Smart / LIO**. Desafio técnico.

> **Status:** spec concluída; implementação em andamento.

## Como rodar

Requisitos: Android SDK e **JDK 17** (o JBR do Android Studio é Java 25 e quebra o build). Para pagar de verdade, também um AVD Android 10 ARM64 sem Google Play com o emulador da Cielo; o passo a passo está no [roteiro do emulador](docs/emulador/roteiro-de-verificacao.md).

```bash
export JAVA_HOME=~/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home
./gradlew installCieloDebug   # app pagando no emulador da Cielo
./gradlew installDemoDebug    # app com a maquininha simulada, sem emulador
./gradlew test lint           # testes e lint (o mesmo que a CI roda)
```

O build tem dois flavors: o `cielo` paga de verdade por Deep Link (`:payment:cielo-lio`), e o `demo` usa a `FakePaymentTerminal` (`:payment:fake`), que tem um botão para cada desfecho. Os dois instalam lado a lado (o `demo` ganha o sufixo `.demo`).

## Decisões

A spec está em [`docs/specs/`](docs/specs/), uma página por área: [domínio](docs/specs/dominio.md) · [pagamento Cielo](docs/specs/pagamento-cielo.md) · [arquitetura](docs/specs/arquitetura.md) · [persistência](docs/specs/persistencia.md) · [testes](docs/specs/testes.md) · [falhas](docs/specs/falhas.md). As decisões difíceis de reverter estão nos [ADRs](docs/adr/), e o vocabulário no [`GLOSSARY.md`](GLOSSARY.md). O histórico de cada decisão está no [mapa](https://github.com/railsonrsa/cielo-ticket-sales/issues/1). A pesquisa da documentação da Cielo está em [`docs/research/`](docs/research/cielo-lio-sdk.md). Os projetinhos descartáveis que responderam perguntas antes da spec estão em [`spikes/`](spikes/).

## Arquitetura e libs

Nove módulos Gradle: `core` (domain em Kotlin puro, data, ui), `payment` (cielo-lio, fake) e três features, cada uma só com presentation. Compose + MVVM, Koin, Room, Navigation Compose tipada, convention plugins. Detalhes em [arquitetura](docs/specs/arquitetura.md).

## Integração Cielo

Por **Deep Link `lio://`** (o SDK foi descontinuado e a Cielo exige deep link), disparado de um foreground service. O callback chega numa Activity sem UI do módulo Cielo. A Cielo não tem idempotência, e no emulador **cobrou duas vezes o mesmo `reference`**, então a anti-duplicidade é toda nossa: gravar antes de disparar e reconciliar via `lio://orders`. Ver [pagamento Cielo](docs/specs/pagamento-cielo.md) e o [ADR 0001](docs/adr/0001-anti-duplicidade-e-reconciliacao.md).

## Trade-offs

- `targetSdk 29`, que é a recomendação da Cielo e evita o tipo de FGS e a permissão de notificação, em vez do target mais recente.
- Camada por módulo, e não por feature: a aprovação atômica atravessa as três features.
- Aprovação atrasada sem lugar vira estorno em vez de emitir Ticket além da lotação: o cliente pode ser cobrado e precisar de estorno, mas a Capacity nunca mente.
- Só fakes nos testes, sem MockK: os cenários críticos dependem de estado.

## O que faria com mais tempo

Estorno (`lio://payment-reversal`) · TTL do Cart · QR assinado e validação na portaria (exige backend) · Capacity entre várias maquininhas (exige backend) · catálogo de Events vindo de um backend/BFF, com o Room como cache, em vez do seed local no device · migração do catálogo de Events · design visual próprio · analytics e monitoring sem GMS · criptografia em repouso (SQLCipher/Keystore) · filtro de tentativas falhas · cobertura com Kover.

## Uso de IA

Planejado e implementado com o Claude Code: a IA propõe e escreve, o humano decide. Onde olhar: o [mapa de decisões](https://github.com/railsonrsa/cielo-ticket-sales/issues/1), as instruções do agente em [`AGENTS.md`](AGENTS.md) e o método e os diários de sessão em [`docs/ai/`](docs/ai/).
