# Evidências — Minhas compras, comprovante e Ticket com QR (#16)

Capturadas em 18/10/2026 no AVD Android 10 (API 29), com o APK `demo` da branch `impl/16-minhas-compras`, instalado do zero.

| Print | Cenário | Conferido |
|---|---|---|
| 01 | "Minhas compras" antes de qualquer venda | "Nenhuma compra ainda." |
| 02 | Aprovar 2 × Choro → resultado com "Ver comprovante" | — |
| 03 | Comprovante: itens, total, data/hora, `authCode`/`cieloCode` e um QR por Ticket | o QR do print, decodificado com o OpenCV no Mac, dá `a8d9d122-f936-4ed4-87e4-103c2a8c0ab7`, o UUID do Ticket |
| 04 | Valor divergente (Pending Refund) acima da aprovada; as `Abandoned` não aparecem | `sqlite3`: `FAILED / AmountMismatch / pending_refund = 1`, duas `FAILED / Abandoned` |
| 05 | Os três grupos: a `AwaitingPayment` no topo ("Pagamento sem resposta"), a Pending Refund, a aprovada | tocar na órfã não abre nada; tocar na aprovada abre o comprovante e o Voltar retorna à lista |

As órfãs criadas para o teste foram marcadas `FAILED / Abandoned` direto no banco (com o app parado), porque o "Verificar pagamento" só entra no #18.

**Falta (humano):** ler o QR da tela do AVD com a câmera de outro celular, como pede o critério de aceite.
