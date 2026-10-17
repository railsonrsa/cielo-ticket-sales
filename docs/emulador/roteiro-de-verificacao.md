# Roteiro de verificação no emulador Cielo

Ticket: [Rodar o emulador Cielo e um pagamento de ponta a ponta](https://github.com/railsonrsa/cielo-ticket-sales/issues/9).
Objetivo: confirmar no emulador o que a [pesquisa do SDK](https://github.com/railsonrsa/cielo-ticket-sales/blob/research/cielo-lio-sdk/docs/research/cielo-lio-sdk.md) deixou como 🔶 (inferência), antes de decidir a [Estratégia contra cobrança duplicada](https://github.com/railsonrsa/cielo-ticket-sales/issues/4).

---

## 1. Requisitos obrigatórios

| Item | Valor | Por quê |
|---|---|---|
| Android do AVD | **10 (API 29)** | O emulador Cielo só roda em 7.1 ou 10 e o APK exige `minSdk 25`; 10 é o `targetSdk` recomendado e o Android do DX8000 |
| Imagem do sistema | `system-images;android-29;default;arm64-v8a` | Mac Apple Silicon → ARM64. **`default` = sem Google Play Services**, igual aos terminais Cielo |
| Emulador Cielo | `lio-emulator.apk` **1.61.8** (pacote `br.com.cielosmart.orderservice`) | Último publicado; sem libs nativas, roda em ARM64 |
| Android Studio | já instalado; usar o JDK dele (`jbr`) | o `java` do sistema (24) não serve para o `sdkmanager` |
| App sonda | app mínimo que dispara `lio://` e mostra o callback | sem um app com o intent-filter do `urlCallback`, a resposta da Cielo se perde |
| Credenciais | começar com valores falsos (`xxxxxxxx`) | o C4 verifica se o emulador aceita; só criar conta no portal se falhar |

> **Fato já confirmado inspecionando o APK:** no emulador, o `lio://` é atendido pelo pacote `br.com.cielosmart.orderservice`, não pelo `com.ads.lio.uriappclient` que a doc manda colocar em `<queries>`. Com `targetSdk 29` isso não importa (visibilidade de pacotes começa no 30). Com `targetSdk ≥ 30`, o `<queries>` precisa dos dois pacotes, ou de um `<intent>` com `scheme="lio"`. Hosts aceitos: `payment`, `payment-reversal`, `order`, `orders`, `print`, `terminalinfo`, `products`, `enabledproducts`, `establishments`.

---

## 2. Passo a passo

Cole os comandos no terminal, na ordem. Eles assumem que o SDK está em `~/Library/Android/sdk`.

```bash
# 0. Atalhos desta sessão de terminal
export ANDROID_HOME=~/Library/Android/sdk
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
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

# 5. Instalar o app sonda (caminho definido quando ele existir)
adb install <caminho>/lio-probe.apk

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
| C13 | Force-stop da sonda (`adb shell am force-stop`) durante o pagamento | o que a Cielo faz com o callback que ninguém recebe |
| C14 | Tempo de ida e volta | do toque em "Pagar" até o callback, em segundos |

---

## 4. Resultados

Preencha à medida que for testando. Cole os JSONs longos em [Rodar o emulador Cielo e um pagamento de ponta a ponta](https://github.com/railsonrsa/cielo-ticket-sales/issues/9), como comentário.

| # | Resultado | `responsecode` | Surpresas |
|---|---|---|---|
| C1 | | | |
| C2 | | | |
| C3 | | | |
| C4 | | | |
| C5 | | | |
| C6 | | | |
| C7 | | | |
| C8 | | | |
| C9 | | | |
