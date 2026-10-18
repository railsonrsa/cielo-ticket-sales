# Spikes

Projetos pequenos e descartáveis, feitos para responder a uma pergunta antes de decidir a spec. **Não fazem parte do app**: cada um tem o próprio build Gradle e não entra no build principal nem na CI.

| Spike | Pergunta que respondeu | Resultado |
|---|---|---|
| [`lio-probe/`](lio-probe/) | Como o emulador da Cielo responde de verdade a `lio://payment` e `lio://orders` (aprovado, cancelado, erro, mesmo `reference` duas vezes, app morto, Voltar)? | [Roteiro e resultados](../docs/emulador/roteiro-de-verificacao.md) · ticket [Rodar o emulador Cielo e um pagamento de ponta a ponta](https://github.com/railsonrsa/cielo-ticket-sales/issues/9) |

A sonda foi escrita pela IA e rodada pelo humano no AVD, numa sessão assistida (ver [`docs/ai/`](../docs/ai/)).
