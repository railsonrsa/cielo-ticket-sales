# Roteiro de verificação no emulador Cielo

Ticket: [Rodar o emulador Cielo e um pagamento de ponta a ponta](https://github.com/railsonrsa/cielo-ticket-sales/issues/9).
Objetivo: confirmar no emulador o que a [pesquisa do SDK](../research/cielo-lio-sdk.md) deixou como 🔶 (inferência), antes de decidir a [Estratégia contra cobrança duplicada](https://github.com/railsonrsa/cielo-ticket-sales/issues/4).

---

## 1. Requisitos obrigatórios

| Item | Valor | Por quê |
|---|---|---|
| Android do AVD | **10 (API 29)** | O emulador Cielo só roda em 7.1 ou 10 e o APK exige `minSdk 25`; 10 é o `targetSdk` recomendado e o Android do DX8000 |
| Imagem do sistema | `system-images;android-29;default;arm64-v8a` | Mac Apple Silicon → ARM64. **`default` = sem Google Play Services**, igual aos terminais Cielo |
| Emulador Cielo | `lio-emulator.apk` **1.61.8** (pacote `br.com.cielosmart.orderservice`) | Último publicado; sem libs nativas, roda em ARM64 |
| JDK | **17** (`~/Library/Java/JavaVirtualMachines/jbr-17.0.14`) | o JBR do Android Studio é Java 25 e o Kotlin 2.0.21 da sonda não o reconhece |
| App sonda | app mínimo que dispara `lio://` e mostra o callback | sem um app com o intent-filter do `urlCallback`, a resposta da Cielo se perde |
| Credenciais | começar com valores falsos (`xxxxxxxx`) | o C4 verifica se o emulador aceita; só criar conta no portal se falhar |

> **Fato já confirmado inspecionando o APK:** no emulador, o `lio://` é atendido pelo pacote `br.com.cielosmart.orderservice`, não pelo `com.ads.lio.uriappclient` que a doc manda colocar em `<queries>`. Com `targetSdk 29` isso não importa (visibilidade de pacotes começa no 30). Com `targetSdk ≥ 30`, o `<queries>` precisa dos dois pacotes, ou de um `<intent>` com `scheme="lio"`. Hosts aceitos: `payment`, `payment-reversal`, `order`, `orders`, `print`, `terminalinfo`, `products`, `enabledproducts`, `establishments`.

---

## 2. Passo a passo

Cole os comandos no terminal, na ordem. Eles assumem que o SDK está em `~/Library/Android/sdk`.

```bash
# 0. Atalhos desta sessão de terminal
export ANDROID_HOME=~/Library/Android/sdk
export JAVA_HOME=~/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

# 1. Baixar a imagem do Android 10 (ARM64, sem Google)  ~1 GB
sdkmanager "system-images;android-29;default;arm64-v8a"

# 2. Criar o AVD
avdmanager create avd -n Cielo_API29 -k "system-images;android-29;default;arm64-v8a" -d pixel_3

# 3. Ligar o AVD (deixe este terminal aberto)
emulator -avd Cielo_API29
```

Em **outro** terminal (repita o bloco 0 nele):

```bash
# 4. Esperar o boot e instalar o emulador Cielo
adb wait-for-device
curl -L -o ~/Downloads/lio-emulator.apk \
  https://s3-sa-east-1.amazonaws.com/cielo-lio-store/apps/lio-emulator/1.61.8/lio-emulator.apk
adb install ~/Downloads/lio-emulator.apk

# 5. Compilar e instalar o app sonda (spikes/lio-probe)
(cd spikes/lio-probe && ./gradlew assembleDebug)
adb install spikes/lio-probe/app/build/outputs/apk/debug/app-debug.apk

# 6. Deixar o log do callback aberto durante todos os testes
adb logcat -s LIO_PROBE
```

No AVD:

7. Abra o app **Cielo Emulador**. Toque **Limpar dados**. Em **Configurações**, deixe as respostas de impressão como estão.
8. Abra o **app sonda** e rode os cenários abaixo **na ordem**. O JSON do callback aparece na tela e no `logcat`.
9. Anote cada resultado na tabela da seção 4. Se algo surpreender, tire um print.

---

## 3. Cenários

**Obrigatórios** (sem eles a Estratégia contra cobrança duplicada não fecha):

| # | Cenário | Como | O que observar |
|---|---|---|---|
| C1 | **Pagamento aprovado** | 1 item, R$ 50,00, `reference = UUID`, crédito à vista → no emulador, **Sucesso** | JSON completo; `responsecode`; o `reference` volta igual? `status` = `ENTERED` ou `PAID`? `paidAmount`, `payments[0].paymentFields.statusCode`, `cieloCode`, `authCode` |
| C2 | **Cancelado pelo usuário** | igual ao C1 → **Cancelado** | `{code, reason}` = `1`? `responsecode` = ? |
| C3 | **Erro** | igual ao C1 → **Erro** | qual `code` (2, 3 ou 4)? `responsecode` = ? |
| C4 | **Credenciais falsas** | o próprio C1, com `clientID`/`accessToken` = `xxxxxxxx` | aprovou normalmente? (se sim, o case roda sem conta no portal) |
| C5 | **Mesmo `reference` duas vezes** | repetir o C1 com o **mesmo** UUID → Sucesso | a Cielo recusa ou cobra de novo? Dois `orderId` diferentes? |
| C6 | **Multi-Event** | 2 itens (2 × R$ 50 + 3 × R$ 30), `value` = 19000 | `items[]` volta igual? Testar também `value` ≠ soma dos itens: recusa ou cobra o `value`? |
| C7 | **Voltar sem escolher** | disparar o pagamento → no emulador, botão **Voltar**/Home, sem escolher nada | chega callback? Qual? Ou nada? |
| C8 | **App morto durante o pagamento (com foreground service)** | disparar, e com a tela da Cielo aberta rodar `adb shell am kill <pacote da sonda>` → Sucesso | o callback chega e reabre a sonda? |
| C9 | **App morto sem foreground service** | igual ao C8, com o FGS desligado na sonda | o callback se perde? (confirma por que o FGS é "essencial") |

**Desejáveis** (se sobrar tempo):

| # | Cenário | O que observar |
|---|---|---|
| C10 | `value` como string × número | os dois são aceitos? |
| C11 | `lio://order` pelo `orderId` do C1 | o JSON bate com o do callback? |
| C12 | `lio://orders` (página 1, 5 por página) depois de C1–C6 | ordem (mais nova primeiro?); dá para achar o `reference`? |
| C12b | `lio://orders` na **página 2** e na **última** (com mais de 10 Orders no emulador) | a ordem se mantém entre páginas? Cada Order traz `createdAt`? Em que formato? (confirma o atalho de parada da Reconciliation, ver [ADR 0001](../adr/0001-anti-duplicidade-e-reconciliacao.md)) |
| C13 | Force-stop da sonda (`adb shell am force-stop`) durante o pagamento | o que a Cielo faz com o callback que ninguém recebe |
| C14 | Tempo de ida e volta | do toque em "Pagar" até o callback, em segundos |

---

## 4. Resultados

Rodados em 17/10/2026 no AVD `Cielo_API29` (Android 10, ARM64, sem GMS) com o emulador Cielo 1.61.8 e a sonda em [`spikes/lio-probe`](../../spikes/lio-probe/).

| # | Resultado | `responsecode` | Surpresas |
|---|---|---|---|
| C1 | ✅ `status: "PAID"`, `reference` volta igual, `payments[0].amount` = 5000, `paymentFields.statusCode` = `"1"` | `0` | ⚠️ **`paidAmount: 0`** mesmo com `PAID`; `statusCode` é **string**; `merchantCode` vem como a string `"null"` |
| C2 | ✅ `{"code":1,"reason":"CANCELADO PELO USUÁRIO"}` | `0` | resposta de erro **não traz `reference` nem `orderId`** |
| C3 | ✅ `{"code":2,"reason":"Falha no processo de pagamento"}` | **ausente** | o parâmetro `responsecode` nem vem na URI |
| C4 | ✅ credenciais `xxxxxxxx` aceitas | — | dá para rodar o case sem conta no portal |
| C5 | ⚠️ **cobra duas vezes**: 2 Orders `PAID`, `orderId`/`authCode`/`cieloCode` diferentes, sem aviso | `0` | a Cielo não deduplica por `reference` |
| C6 | ✅ `items[]` volta com `sku`, `name`, `quantity`, `unitPrice`; `amount` = 19000 | `0` | — |
| C6b | ⚠️ cobra o **`value`** (R$ 100) e ignora a soma dos itens (R$ 190); vem **`PAID`** com `pendingAmount: 0` | `0` | a Cielo não valida `value` × itens |
| C7 | ⚠️ Voltar ◀ na tela da Cielo → **nenhum callback, nunca** | — | Purchase órfã sem nenhum crash |
| C8 | ✅ com FGS, `am kill` **não mata** o processo; callback chega normal | `0` | — |
| C9 | ✅ sem FGS, `am kill` mata; a Cielo dispara o callback e o Android **recria o processo** para entregá-lo | `0` | callback não se perdeu no emulador (a doc diz que o FGS é essencial; terminal real pode diferir) |
| C11 | ✅ `lio://order` silencioso (35 ms), devolve a Order completa | `0` | — |
| C12 | ✅ `lio://orders` silencioso (41 ms); **mais antiga primeiro**; `totalItems`/`totalPages` | `0` | cancelados/erros/abandonados **também viram Order**, com `status: "ENTERED"` e o `reference`; há uma Order-semente `id: "1"` |
| C12b | ✅ (18/10, no #18) ordem mantida entre as páginas, mais antiga primeiro; `createdAt` = `Oct 18, 2026 4:52:48 PM`, hora local | `0` | ⚠️ a página 1 traz 6 Orders (a semente a mais) e **a última da página 1 se repete na página 2**; uma página além da última vem com `results: []` |
| C10, C13 | não rodados | | `value` como número funciona (todos os cenários); C13 pouco informativo |
| C14 | ida e volta do sistema: 35 ms (consultas) a ~2,5 s (pagamento, sem contar os toques) | | |
