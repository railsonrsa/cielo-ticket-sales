# Fechamento: teste de UI, README e diários — 2026-10-18

**Prompt do usuário** (literal):
> vamos atuar no ticket de fechamento #20

**Objetivo:** fatia [#20](https://github.com/railsonrsa/cielo-ticket-sales/issues/20): provar o caminho feliz de ponta a ponta e deixar o repositório pronto para o avaliador.

## O que foi feito

- **Teste de UI Compose no `:app`** ([`HappyPathTest`](../../../app/src/testDemo/kotlin/dev/railson/ticketsales/HappyPathTest.kt)): catálogo → Cart → Pagar → "Aprovar" na maquininha simulada → "Pagamento aprovado" → catálogo com a Capacity baixada → "Minhas compras" com 1 ingresso. Roda no Robolectric, com o Room de verdade e a `FakePaymentTerminal`, em `src/testDemo`, então entra no `./gradlew test` da CI sem emulador. A "tela da Cielo" é outra Activity: o teste confere que o "Pagar" a disparou e a abre com o mesmo intent, como faria o sistema.
- **Dois tropeços do Robolectric**, ambos resolvidos no teste ou no build, sem mudar código de produção:
  - O `waitUntil` do Compose não roda o main looper, então a coroutine que o Room retomaria nunca andava. O teste roda o looper à mão enquanto espera o disparo.
  - Com dois testes Robolectric no `:app`, o segundo `Application` chamava o `startKoin` com o Koin do primeiro ainda vivo. O `stopKoin` no `@After` não bastou: a integração do Koin com o Compose seguia no escopo fechado. O `:app` passou a rodar uma JVM por classe de teste (`forkEvery = 1`).
- **README** revisto para o avaliador: status final, o que o app faz, como avaliar pelo `demo`, a pirâmide de testes, os trade-offs revistos depois da implementação e o que ficou fora (as pendências reais dos diários, separadas do "com mais tempo").
- **`docs/ai/README.md`**: o passo 6 do método virou passado, com um balanço de onde o humano corrigiu a IA ao longo do projeto.

## Onde o humano corrigiu a IA

| Recomendação da IA | Decisão humana | Por quê |
|---|---|---|
| Teste de UI no Robolectric (`src/testDemo`), não instrumentado | a revisar no PR | A CI roda `./gradlew test` sem emulador, e a spec de testes pede JUnit4 num runner só |
| Uma JVM por classe de teste no `:app` em vez de mexer no `startKoin` do `Application` | a revisar no PR | Isola o Koin global sem código de produção pensado para teste; são só 3 classes |
| Pendências reais no README, separadas do "com mais tempo" | a revisar no PR | O avaliador vê o que não foi conferido, não só o que não foi feito |

## Pendências

- **README revisado pelo humano** (critério de aceite do #20).
- **Report de prompts**: o humano decide se gera a partir dos transcripts locais, que apagam em 30 dias.
- **APK de debug numa release**: o humano decide se vale para o avaliador; se sim, o `demoDebug` é o que roda sem o emulador da Cielo.
- Ler o QR do AVD com a câmera de outro celular (critério de aceite do #16), que continua com o humano.
