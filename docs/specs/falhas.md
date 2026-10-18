# Spec — Falhas: o que o Operator vê e pode fazer

Origem: [Mensagens e ações do usuário para cada falha](https://github.com/railsonrsa/cielo-ticket-sales/issues/10). Modelo em [domínio](dominio.md); regras do pagamento em [pagamento-cielo](pagamento-cielo.md).

**O leitor é o Operator.** As mensagens são curtas, em pt-BR, e sempre dizem o que aconteceu e o que fazer. O texto mora no `strings.xml` da feature; o domínio só carrega o `FailureReason`.

| Falha | `FailureReason` | O que o Operator vê | Ações |
|---|---|---|---|
| Cancelado na maquininha (código 1) | `CanceledByOperator` | snackbar "Pagamento cancelado"; Cart intacto | — |
| Erro genérico (2) / negado (3) | `PaymentError` / `Declined` | dialog | **Tentar de novo** (Purchase nova, `reference` novo, mesmo Cart) · Voltar ao carrinho |
| Autenticação (4) | `TerminalAuthError` | dialog "Maquininha sem autenticação na Cielo. Contate o suporte" | OK (sem retry) |
| Capacity insuficiente / esgotado no Cart | — (nenhuma Purchase criada) | selo por item "Só restam N" / "Esgotado"; Pagar desabilitado; o use case revalida e devolve os Events em conflito | **Ajustar para N** · Remover |
| App da Cielo ausente | checagem prévia: nada é criado; na corrida com `ActivityNotFoundException`: `TerminalUnavailable` | "App de pagamento da Cielo não encontrado" | OK |
| `AwaitingPayment` órfã | — | banner no Cart e no topo de "Minhas compras": "Pagamento sem resposta"; bloqueia vendas novas | **Verificar pagamento**; se inconclusivo: "Não foi possível confirmar com a Cielo. Tente de novo em instantes" |
| Órfã sem saída | `Abandoned` | aparece só depois de ≥1 verificação inconclusiva | **Desistir desta compra** + confirmação ("se a cobrança tiver passado e a Cielo avisar depois, os ingressos são emitidos se ainda houver lugar; se não houver, a cobrança fica marcada para estorno") |
| Callback ilegível | — (continua `AwaitingPayment`) | log + banner da órfã | Verificar pagamento |
| Só `ENTERED` / nenhuma Order (Reconciliation) | `NotCompleted` / `NotStarted` | some da lista (sem dinheiro); Cart intacto | — |
| Esgotado após pagamento | `SoldOutAfterPayment` + Pending Refund | dialog bloqueante no Cart e em "Minhas compras", até o "Entendi" (o aceite fica gravado): "Pagamento aprovado, mas o evento esgotou. Nenhum ingresso foi emitido. Esta cobrança precisa ser estornada." + valor, `authCode`, `cieloCode` | Entendi; selo vermelho em "Minhas compras" |
| Cobrança duplicada / valor divergente | `Approved` + Pending Refund / `AmountMismatch` + Pending Refund | selo vermelho com os dados do estorno (valor, `authCode`, `cieloCode` e `orderId` de cada cobrança a estornar) | — |
| Aprovação atrasada com lugar | — (`Approved`) | aviso no Cart; no próximo "Pagar", dialog "Revisar carrinho" / "Continuar e pagar" até o Operator aceitar | "Revisar carrinho" fecha sem gravar; "Continuar e pagar" grava o aceite e paga |

**Com mais tempo:** filtro "mostrar tentativas falhas" em "Minhas compras".
