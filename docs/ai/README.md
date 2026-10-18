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
6. **Implementação** (fase seguinte): uma fatia vertical por sessão, TDD no domínio (skill `tdd`) e `./gradlew test lint` antes de cada commit.

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
