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

**Purchase**:
Uma tentativa de compra de um cliente, cobrada num único pagamento, que pode reunir vários Events.
_Avoid_: Order (reservado para a Order da Cielo), sale

**Purchase Item**:
Uma linha da Purchase: um Event e a quantidade de ingressos dele.
_Avoid_: Line, cart item

**Ticket**:
O direito de entrar num Event, emitido só a partir de uma Purchase aprovada.
_Avoid_: Ingresso (no código), voucher

**Cielo Order**:
O pedido que o app da Cielo cria, a partir do nosso Deep Link, para cobrar uma Purchase; conceito externo, não do nosso domínio.
_Avoid_: Order sozinho
