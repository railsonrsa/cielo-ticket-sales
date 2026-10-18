# Cielo Ticket Sales

App de venda de ingressos para eventos locais, pago numa maquininha Cielo Smart (LIO).

## Language

**Event**:
Um acontecimento local (show, peça, palestra) à venda, com preço por ingresso e uma capacidade.
_Avoid_: Show, session

**Capacity**:
Quantos ingressos de um Event ainda podem ser vendidos.
_Avoid_: Stock, inventory, lotação

**Cart**:
A seleção de Events e quantidades que o cliente monta antes de pagar; sobrevive a crash e ao fechamento do app, e ainda não é uma cobrança.
_Avoid_: Basket, carrinho (no código)

**Cart Item**:
Uma linha do Cart: um Event e a quantidade de ingressos dele, sem preço; o subtotal usa o preço atual do Event.
_Avoid_: Line, Purchase Item (esse é a linha da Purchase, com o preço congelado)

**Capacity Conflict**:
Quando um Cart Item não cabe mais na Capacity atual do Event: "Esgotado" (Capacity 0) ou "Só restam N" (quantidade acima da Capacity). Bloqueia o "Pagar" até o item ser ajustado ou removido.
_Avoid_: Erro de estoque, overbooking

**Purchase**:
Uma tentativa de compra de um cliente, cobrada num único pagamento, que pode reunir vários Events.
_Avoid_: Order (reservado para a Order da Cielo), sale

**Purchase Item**:
Uma linha da Purchase: um Event e a quantidade de ingressos dele.
_Avoid_: Line, cart item

**Reconciliation**:
Descobrir, consultando a Cielo pelo `reference`, o desfecho de uma Purchase cujo resultado do pagamento nunca chegou ao app.
_Avoid_: Sync, verificação (no código)

**Operator**:
Quem segura a maquininha e conduz a venda; é quem lê as mensagens do app.
_Avoid_: Usuário, vendedor, cliente (o cliente compra, o Operator opera)

**Failure Reason**:
O motivo pelo qual uma Purchase terminou `Failed`, escolhido de uma lista fechada.
_Avoid_: Erro, status

**Pending Refund**:
Marca numa Purchase cujo dinheiro saiu do cliente sem contrapartida (cobrança duplicada, valor divergente ou Event esgotado após o pagamento) e precisa ser estornado fora do app.
_Avoid_: Estorno (é o ato, não a marca), refund sozinho

**Ticket**:
O direito de entrar num Event, emitido só a partir de uma Purchase aprovada.
_Avoid_: Ingresso (no código), voucher

**Cielo Order**:
O pedido que o app da Cielo cria, a partir do nosso Deep Link, para cobrar uma Purchase; conceito externo, não do nosso domínio.
_Avoid_: Order sozinho
