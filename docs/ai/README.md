# Como a IA foi usada

O projeto foi planejado e implementado com o **Claude Code** num papel definido: **a IA propõe, pesquisa e escreve; o humano decide.** Esta pasta é a evidência disso.

## Onde olhar

| O quê | Onde |
|---|---|
| Instruções e restrições dadas ao agente | [`AGENTS.md`](../../AGENTS.md) (o [`CLAUDE.md`](../../CLAUDE.md) só importa esse arquivo) |
| O mapa de decisões, ticket por ticket | [issue #1 — Mapa](https://github.com/railsonrsa/cielo-ticket-sales/issues/1) e suas sub-issues |
| A spec resultante | [`docs/specs/`](../specs/), [`docs/adr/`](../adr/), [`GLOSSARY.md`](../../GLOSSARY.md) |
| Diários de sessão | [`sessions/`](sessions/) |
| Permissões do agente no repo | [`.claude/settings.json`](../../.claude/settings.json) |

## Método

1. **Mapa antes de código.** A ideia foi charted como um mapa de decisões nas GitHub Issues (skill `wayfinder`). Cada ticket é uma pergunta, resolvida numa sessão, com a resposta registrada como comentário e o ticket fechado. O que não cabia no prazo foi tirado de escopo explicitamente.
2. **Grilling, não ditado.** Nos tickets de decisão (skills `grilling` + `domain-modeling`), a IA faz rodadas de perguntas numeradas, cada uma com a resposta que ela recomenda, e **o humano responde**. A IA nunca responde às próprias perguntas.
3. **Linguagem única.** Todo termo resolvido entra no `GLOSSARY.md` na hora ("Purchase", nunca "Order"). Decisões difíceis de reverter viram ADR.
4. **Fatos antes de opinião.** A pesquisa da documentação da Cielo rodou num subagente (skill `research`), com fonte em cada afirmação e inferências marcadas. Ela foi depois confrontada com o **emulador real**, numa sessão assistida: a IA escreveu o roteiro e a sonda, o humano rodou os cenários. Dois achados mudaram o projeto: a Cielo cobra duas vezes o mesmo `reference`, e o `paidAmount` vem 0 em pagamento aprovado.
5. **Protótipos para decidir.** Quando o texto não bastava, a IA gerou um simulador descartável ([`docs/simuladores/`](../simuladores/)) para o humano reagir.
6. **Implementação:** uma fatia vertical por sessão e por PR, do domínio à tela, seguindo o [mapa de implementação](https://github.com/railsonrsa/cielo-ticket-sales/issues/11). TDD no domínio (skill `tdd`), `./gradlew test lint` antes de cada commit e o fluxo da Cielo conferido à mão no emulador ([evidências](../evidencias/)). Todo PR foi revisado e mesclado pelo humano.

## Onde o humano corrigiu a IA, no balanço

As correções de rumo se concentraram no planejamento ([diário](sessions/2026-10-17-planejamento.md)). Foi lá que o humano mudou a regra da aprovação atrasada ("quem confirma primeiro leva", sem Ticket além da lotação) e a busca do `lio://orders` pelas duas pontas. Na implementação, a maioria das recomendações foi aceita porque a spec já respondia às perguntas. As correções foram de escopo e de custo: a CI só nos PRs e na `main` ([fundação](sessions/2026-10-18-fundacao.md)) e o dialog de esgotado só no Cart e em "Minhas compras", em vez de global ([aprovação atrasada](sessions/2026-10-18-aprovacao-atrasada.md)). Quando uma pendência pedia regra nova, como o texto do "Desistir" no [#18](sessions/2026-10-18-reconciliation.md), a IA parou e o humano decidiu.

## Skills usadas

`wayfinder`, `grilling`, `domain-modeling`, `research`, `prototype`, `tdd` (do pacote [mattpocock-skills](https://github.com/mattpocock/skills)).

## Formato do diário de sessão

`sessions/AAAA-MM-DD-<tema>.md`, escrito pela IA e **revisado pelo humano**. Sem transcrição bruta.

```markdown
# <tema> — AAAA-MM-DD

**Prompt do usuário** (literal): > ...
**Objetivo:** ...
**O que foi feito:** 3–6 bullets, com links para tickets, commits e PRs
**Onde o humano corrigiu a IA:**
| Recomendação da IA | Decisão humana | Por quê |
**Pendências:** ...
```
