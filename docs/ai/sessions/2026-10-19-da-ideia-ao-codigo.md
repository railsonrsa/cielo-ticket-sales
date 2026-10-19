# README: da ideia ao código — 2026-10-19

**Prompt do usuário** (literal):
> Olhando o que senti falta no readme, algo para exemplificar o uso da skill e transformando a ideia em planejamento e specs e como isso virou tickets no github. Algo como também onde ficaram o mapa das decisões, o mapa das implementações e afins, poderia mostrar um prototipo de sugestões de como ficaria?

**Objetivo:** mostrar no README o caminho da ideia até os PRs e onde fica cada artefato, e publicar os prompts do humano.

## O que foi feito

- Protótipo da seção revisado pelo humano antes de entrar no README: diagrama do fluxo, tabela "Onde está cada coisa" e o exemplo da cobrança duplicada ([#4](https://github.com/railsonrsa/cielo-ticket-sales/issues/4) → ADR 0001 → [#15](https://github.com/railsonrsa/cielo-ticket-sales/issues/15) e [#18](https://github.com/railsonrsa/cielo-ticket-sales/issues/18)).
- A seção "Da ideia ao código" ocupa o lugar do antigo "Uso de IA" e subiu para logo depois de "O que o app faz".
- [`docs/ai/prompts.md`](../prompts.md): os prompts do humano de 16 a 19/10, selecionados e revisados por ele numa página de revisão antes da exportação (69 no total).

## Onde o humano corrigiu a IA

| Recomendação da IA | Decisão humana | Por quê |
|---|---|---|
| A página de revisão marcava cada prompt editado | A exportação sai sem essa marca | As edições foram pontuais: português e contexto desnecessário |
| Exportar o report como estava (66 prompts) | Conferir antes: faltava o prompt do trunk-based e de "nunca fazer merge na `main`" | O humano notou a ausência. O extrator lia só 5 sessões da pasta-mãe e perdeu 34 prompts, da noite de 17/10 (grillings do #4, #5, #6, #7, #10 e #8) à criação do mapa de implementação. Eles foram recuperados e revisados antes de exportar |

## Pendências

- Nenhuma.
