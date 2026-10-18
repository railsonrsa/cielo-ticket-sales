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
6. **Callback idempotente**: só transiciona a partir de `AwaitingPayment`. Exceção única: um callback autorizado atrasado **promove `Failed` → `Approved`** (o dinheiro saiu). Ele emite os Tickets e decrementa a Capacity a partir dos Purchase Items, **não mexe no Cart**, e o próximo "Pagar" pede confirmação num dialog até a pessoa aceitar (aceite gravado em disco).

## Alternativas descartadas

- Confiar na ordenação de `lio://orders`: não é documentada e pode mudar no terminal real.
- Estado `Unverified`: o `AwaitingPayment` com "Verificar pagamento" já cobre esse caso.
- Reconciliar no resume: corre o risco de marcar `Failed` um pagamento que ainda está em andamento.

## Consequências

- Em casos raros (aprovação atrasada), a Capacity pode ficar negativa. Aceitamos isso e registramos.
- "Minhas compras" passa a mostrar a `AwaitingPayment` no topo.
