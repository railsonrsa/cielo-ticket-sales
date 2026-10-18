# 0001 — Anti-duplicidade e Reconciliation sem confiar na Cielo

**Status:** aceito (17/10/2026) · Ticket: [Estratégia contra cobrança duplicada](https://github.com/railsonrsa/cielo-ticket-sales/issues/4)

## Contexto

No emulador, a Cielo **cobrou duas vezes o mesmo `reference`**. Ela não tem chave de idempotência, o Voltar na tela dela não devolve callback e o callback de falha não traz `reference`. A ordenação de `lio://orders` **não é documentada**: só vimos a página 1, da mais antiga para a mais nova.

## Decisão

1. **Gravar antes de disparar.** A `AwaitingPayment` vai para o disco antes do deep link. A invariante "uma `AwaitingPayment` por vez" é a única trava, aplicada no use case. Duplo toque, rotação e retry caem nela.
2. **Reconciliation da órfã** na abertura a frio do app e pelo botão "Verificar pagamento". **Nunca no resume**: a pessoa pode estar no meio do pagamento.
3. **Resultado**: soma autorizada == total → `Approved`; só `ENTERED` → `Failed("pagamento não concluído")`; nenhuma Order → `Failed("pagamento não iniciado")`; consulta falhou ou foi inconclusiva → continua `AwaitingPayment`. Sem estado `Unverified`.
4. **Busca independente da ordem**: lê as páginas das duas pontas para o meio (1, N, 2, N-1…). Para quando acha o `reference` (e lê a página vizinha para pegar cobranças duplicadas), quando cada ponta já passou do `createdAt` da Purchase (com folga), quando as pontas se encontram ou quando chega a 20 páginas (inconclusivo). Só marca `Failed` com certeza.
5. **Anomalias guardadas, não escondidas**: um `orderId` pago a mais (cobrança duplicada) ou uma soma autorizada ≠ total (`Failed("valor divergente")`) ficam gravados com o alerta "cobrança a estornar". O estorno em si fica fora do escopo.
6. **Callback idempotente**: só transiciona a partir de `AwaitingPayment`. Exceção única: um callback autorizado atrasado sobre uma Purchase `Failed` (o dinheiro saiu):
   - **há Capacity para todos os itens** → promove `Failed` → `Approved`, emite os Tickets e decrementa a Capacity a partir dos Purchase Items. **Não mexe no Cart**, e o próximo "Pagar" pede confirmação num dialog até a pessoa aceitar (o aceite fica gravado em disco);
   - **falta Capacity em algum item** → continua `Failed("esgotado após pagamento")` com a marca "cobrança a estornar". Não emite Tickets, não mexe na Capacity e não faz aprovação parcial (tudo ou nada). Vale "quem confirma primeiro leva": quem comprou nesse meio-tempo não é afetado.
   _Revisto em 17/10/2026 no ticket [Estratégia de testes dos cenários críticos](https://github.com/railsonrsa/cielo-ticket-sales/issues/7). A versão anterior promovia sempre e deixava a Capacity negativa._ Simulador do cenário: [`docs/simuladores/aprovacao-atrasada.html`](../simuladores/aprovacao-atrasada.html) (abrir no navegador).

7. **Saída de emergência da órfã**: depois de pelo menos uma Reconciliation inconclusiva, o Operator pode "Desistir desta compra" (com confirmação), e a Purchase vai para `Failed(Abandoned)`. Sem isso, a Cielo fora do ar travaria a maquininha para sempre. Uma aprovação que chegue depois cai na regra do item 6.
8. **Callback ilegível** (`response` ausente ou com Base64/JSON inválido) é tratado como callback que não chegou: a Purchase continua `AwaitingPayment`.

_Emenda de 17/10/2026, ticket [Mensagens e ações do usuário para cada falha](https://github.com/railsonrsa/cielo-ticket-sales/issues/10): os motivos entre aspas acima viraram o tipo fechado `FailureReason` (`NotCompleted`, `NotStarted`, `AmountMismatch`, `SoldOutAfterPayment`…), e "cobrança a estornar" virou a marca **Pending Refund** (ver `GLOSSARY.md`). Os itens 7 e 8 entraram nessa emenda._

## Alternativas descartadas

- Confiar na ordenação de `lio://orders`: não é documentada e pode mudar no terminal real.
- Estado `Unverified`: o `AwaitingPayment` com "Verificar pagamento" já cobre esse caso.
- Reconciliar no resume: corre o risco de marcar `Failed` um pagamento que ainda está em andamento.

## Consequências

- A Capacity nunca fica abaixo de 0, e nunca há mais Tickets do que lugares. O preço disso é que, em casos raros (aprovação atrasada com o Event esgotado), o cliente é cobrado e precisa de estorno.
- "Minhas compras" passa a mostrar a `AwaitingPayment` no topo.
