# Minhas compras, comprovante e Ticket com QR — 2026-10-18

**Prompt do usuário** (literal):
> vamos atuar no ticket #16

**Objetivo:** fatia [#16](https://github.com/railsonrsa/cielo-ticket-sales/issues/16): o Operator consulta as compras, abre o comprovante e mostra o QR de cada Ticket.

## O que foi feito

- **Domínio em TDD:** `ObservePurchasesUseCase` em 3 ciclos vermelho → verde (aprovadas da mais recente para a mais antiga; as `Failed` sem dinheiro somem e as com Pending Refund ficam; a ordem `AwaitingPayment` → Pending Refund → `Approved`). Uma `Approved` com cobrança duplicada cai no grupo da Pending Refund. A porta ganhou `PurchaseRepository.observeAll()`, que não filtra nem ordena: a regra fica no use case. Também entrou o `ObserveTicketsUseCase`, só de passagem.
- **`:core:data`:** a query `observeAll` no `PurchaseDao`, sem mudar o esquema (o banco continua na v3), e um teste Robolectric de que ela acompanha a aprovação.
- **`:feature:purchases`:** "Minhas compras" (a órfã mostra "Pagamento sem resposta" e não abre nada; só a `Approved` abre o comprovante) e o comprovante, com um `TicketCard` por Ticket: QR, posição ("Ingresso 1 de 2") e o UUID em texto. O nome do Event vem do Purchase Item congelado. São 4 testes de ViewModel.
- **QR:** ZXing `core` gera a `BitMatrix` e um `Canvas` a desenha num `Path` só, preto no branco, com margem de 4 módulos.
- **Navegação:** ícone "Minhas compras" no catálogo e "Ver comprovante" na tela de resultado (o Voltar do comprovante leva ao catálogo).
- **Conferido no AVD API 29** ([evidências](../../evidencias/16-minhas-compras/README.md)): o QR do print decodifica para o UUID do Ticket, e os três grupos aparecem na ordem certa, com as `Abandoned` fora.

## Onde o humano corrigiu a IA

| Recomendação da IA | Decisão humana | Por quê |
|---|---|---|
| ZXing `core` (sugestão da spec) desenhado no Compose | a revisar no PR | Era a decisão em aberto do #16 no mapa de implementação |
| QR dentro do comprovante, um card por Ticket, em vez de uma tela por Ticket | a revisar no PR | A spec põe "os Tickets com QR" no comprovante |

## Pendências

- **Critério de aceite:** ler o QR do AVD com outro celular (o print já decodifica no OpenCV).
- A linha da Pending Refund ainda não tem selo nem os dados do estorno (#19); por ora mostra só "Nenhum ingresso emitido". A da órfã ainda não tem o "Verificar pagamento" (#18).
- O teste de UI do caminho feliz até "Minhas compras" fica no #20.
