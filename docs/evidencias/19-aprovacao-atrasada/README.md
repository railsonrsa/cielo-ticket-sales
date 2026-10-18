# Evidências — Aprovação atrasada e Pending Refund (#19)

Capturadas em 18/10/2026 no AVD Android 10 (API 29), com o APK `demo` da branch `impl/19-aprovacao-atrasada`, instalado **por cima** do da fatia anterior: o banco migrou de v4 para v5 sem perder as compras.

| Print | Cenário | Conferido |
|---|---|---|
| 01 | Maquininha simulada com "Aprovar com atraso (30 s)" | fecha sem resposta; a Purchase fica `AwaitingPayment` |
| 02 | "Verificar pagamento" → `Failed(NotCompleted)`; 30 s depois a aprovação chega, com lugar | o app fica no Cart (não navega para "Pagamento aprovado"), o Cart não é esvaziado e aparece o aviso "Compra aprovada depois" |
| 03 | "Pagar" com a aprovação atrasada pendente | dialog "Revisar carrinho" / "Continuar e pagar"; "Revisar carrinho" fecha sem gravar e o próximo "Pagar" pergunta de novo |
| 04 | Voz e Violão (2 lugares): A × 2 aprovada com atraso, `Failed(NotCompleted)` na verificação, B × 2 aprovada na hora; a aprovação de A chega com o Event esgotado | dialog bloqueante em "Minhas compras" com valor, `authCode` e `cieloCode`; o Voltar não fecha, só o "Entendi" |
| 05 | Depois do "Entendi" | selo vermelho "Estorno pendente" com motivo, valor, códigos e `orderId`; o catálogo mostra Voz e Violão "Esgotado" (Capacity 0, nunca negativa) |

Achado durante a verificação, corrigido na mesma fatia (com teste de ViewModel): o Cart continuava acompanhando a Purchase depois do `Failed(NotCompleted)` e, na aprovação atrasada, navegava para "Pagamento aprovado" como se fosse uma venda normal.
