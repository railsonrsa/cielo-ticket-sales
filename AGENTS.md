# AGENTS.md

Instruções para qualquer agente de IA que trabalhe neste repositório. Humanos: comecem pelo [README](README.md).

## Contexto

- Desafio técnico: app Android (Kotlin) de venda de ingressos pago na Cielo Smart/LIO, testado só no emulador da Cielo, sem backend.
- Toda decisão nasceu no [mapa de decisões](https://github.com/railsonrsa/cielo-ticket-sales/issues/1) (issues com a label `wayfinder:map`). As issues guardam o histórico; os arquivos em `docs/` guardam o estado atual.
- **Ordem de leitura antes de mexer em código:**
  1. [`GLOSSARY.md`](GLOSSARY.md): a linguagem canônica
  2. [`docs/specs/`](docs/specs/): só o arquivo da área da tarefa (domínio, pagamento-cielo, arquitetura, persistência, testes, falhas)
  3. [`docs/adr/`](docs/adr/): as decisões difíceis de reverter
- Código e identificadores em inglês. README, docs, issues e mensagens ao Operator em português.

## Regras inegociáveis

- **Use os termos do glossário.** "Purchase", nunca "Order" (Order é a Cielo Order). "Ticket", "Capacity", "Cart", "Operator". Se um termo novo surgir, pare e pergunte antes de inventar um nome.
- **Gravar antes de disparar:** a Purchase `AwaitingPayment` vai para o disco antes do deep link. Só pode existir uma `AwaitingPayment` por vez, e essa trava fica no use case, não na UI ([ADR 0001](docs/adr/0001-anti-duplicidade-e-reconciliacao.md)).
- **A Reconciliation nunca roda no resume**, só na abertura a frio e no botão "Verificar pagamento".
- **A porta `PaymentTerminal` só dispara**; a resposta entra por use case ([ADR 0002](docs/adr/0002-porta-de-pagamento-so-de-disparo.md)). Nada de `suspend fun pay()`.
- **Aprovação = soma de `payments[].amount` com `statusCode` ∈ {"0","1"} igual ao total.** Nunca use `status` nem `paidAmount` da Cielo.
- **Módulos de feature só têm presentation** e só enxergam `:core:domain` e `:core:ui`. Uma feature não conhece outra.
- **Testes só com fakes escritos à mão**: sem MockK nem Mockito. Use Truth para asserções.
- **TDD em `:core:domain`**: teste vermelho primeiro.
- **É proibido usar `fallbackToDestructiveMigration`** e qualquer biblioteca que dependa de Google Play Services (o terminal não tem GMS).
- O domínio não carrega texto para o usuário: `FailureReason` é um tipo fechado, e o texto mora no `strings.xml` da feature.

## Como verificar

```bash
export JAVA_HOME=~/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home  # o JBR do Studio é Java 25 e quebra o build
./gradlew test lint
```

- Rode antes de cada commit. Vermelho = não commita.
- O fluxo real com a Cielo não é automatizado: siga [`docs/emulador/roteiro-de-verificacao.md`](docs/emulador/roteiro-de-verificacao.md) quando mexer em `:payment:cielo-lio`.

## Fluxo de trabalho

- **Uma fatia vertical por sessão e por PR** (do domínio à tela), sempre com os testes da fatia.
- Uma decisão nova ou que contradiga uma spec não se resolve no código: pare, aponte a contradição e atualize a spec ou o ADR junto com o humano.
- Ao fim de uma sessão relevante, registre um diário em `docs/ai/sessions/AAAA-MM-DD-<tema>.md` seguindo [`docs/ai/README.md`](docs/ai/README.md).
