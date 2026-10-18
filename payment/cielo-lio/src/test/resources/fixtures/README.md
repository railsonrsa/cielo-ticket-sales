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
| `orders-page-1.json`, `orders-page-2.json` | C12b: `lio://orders` com 10 Orders; a página 1 traz a semente e repete a última na página 2 | capturado em 18/10/2026, pelo `order://orders` do app |
| `orders-page-beyond-last.json` | `lio://orders` na página 3 de 2: `results` vazio | capturado em 18/10/2026 |
| `failure-declined.json`, `failure-auth.json` | códigos 3 e 4 | **derivado**: o emulador não os gerou; mesmo formato `{code, reason}`, com o `reason` inventado |
