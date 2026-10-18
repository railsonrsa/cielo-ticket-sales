# Fixtures do callback `order://response`

O JSON decodificado do parâmetro `response`, capturado pela sonda [`spikes/lio-probe`](../../../../../../spikes/lio-probe) no AVD `Cielo_API29` com o emulador Cielo 1.61.8, em 17/10/2026 (cenários do [roteiro](../../../../../../docs/emulador/roteiro-de-verificacao.md)). Os testes recodificam em Base64.

| Arquivo | Cenário | Origem |
|---|---|---|
| `payment-approved.json` | C1: aprovado, `status: "PAID"` com `paidAmount: 0` | capturado |
| `payment-approved-two-events.json` | C6: dois Events, R$ 190 | capturado |
| `payment-value-below-items.json` | C6b: `value` R$ 100 com R$ 190 em itens, vem `PAID` | capturado |
| `payment-duplicate-first.json`, `payment-duplicate-second.json` | C5: o mesmo `reference` cobrado duas vezes | capturado |
| `failure-canceled.json` | C2: código 1 | capturado |
| `failure-error.json` | C3: código 2 | capturado |
| `failure-declined.json`, `failure-auth.json` | códigos 3 e 4 | **derivado**: o emulador não os gerou; mesmo formato `{code, reason}`, com o `reason` inventado |
