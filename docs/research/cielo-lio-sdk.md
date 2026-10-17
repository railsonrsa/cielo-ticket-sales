# Cielo Smart / LIO: integração local (SDK Order Manager vs Deep Link)

Pesquisa do ticket [#2](https://github.com/railsonrsa/cielo-ticket-sales/issues/2). Data da coleta: 16/10/2026.

**Legenda:** ✅ = verificado numa fonte primária (URL ao lado) · 🔶 = inferência nossa, não está escrito na fonte.

As fontes são a documentação oficial (`docs.cielo.com.br/cielo-smart`, lida pela versão `.md` de cada página) e o app de exemplo oficial [DeveloperCielo/LIO-SDK-Sample-Integracao-Local](https://github.com/DeveloperCielo/LIO-SDK-Sample-Integracao-Local), lido no commit `1f626fd` (29/10/2025). O manual antigo (`developercielo.github.io/manual/cielo-lio`), que o sample ainda cita, hoje dá 404.

---

## TL;DR

1. **Use Deep Link (`lio://…`), não o SDK.** O SDK Order Manager foi **descontinuado** em dez/2024 e só recebe patches. Desde 30/09/2026 a Cielo tem uma **política de "camada de abstração obrigatória"** que nomeia o Deep Link `lio://` como a interface obrigatória para pagamento, pedidos e impressão. ✅ [comunicados](https://docs.cielo.com.br/cielo-smart/docs/comunicados) · [política](https://docs.cielo.com.br/cielo-smart/docs/politica-de-integracao-camada-de-abstracao-obrigatoria)
2. No Deep Link **não existe bind/createDraft/addItem/place no nosso app**. Mandamos **uma** intent `lio://payment?request=<base64(json)>&urlCallback=<nosso esquema>://<host>` com os itens dentro, e o app da Cielo cria a Order e cobra. A resposta volta como intent `ACTION_VIEW` na nossa Activity, com `response=<base64(json)>`. ✅
3. **Idempotência:** o campo `reference` (livre, opcional) volta na Order. Dá para consultar uma Order pelo **`orderId`** (`lio://order`) ou listar Orders paginadas (`lio://orders`, **no máximo 5 por página**, sem filtro por `reference`). Não existe "buscar por reference" e a Cielo não deduplica por `reference`. ✅ / 🔶
4. **Plataforma:** `minSdk 24` e `targetSdk 29` são o recomendado (target < 29 é rejeitado). Os terminais rodam Android 7.1, 10, 11 e 12. O emulador só instala em **Android 7.1 ou 10**: AVD ou aparelho, **nunca** num terminal. Os terminais **não têm Google Play Services nem WebView**. ✅
5. **Erros:** o JSON de erro é `{code, reason}`, com `1` cancelado pelo usuário, `2` erro genérico, `3` erro no pagamento e `4` erro de autenticação. O `responsecode` da URI **não é confiável** para distinguir sucesso de erro (a própria doc mostra erro com `responsecode=0`). ✅ / 🔶

---

## 1. SDK Order Manager vs Deep Link: qual usar

| | SDK Order Manager (`cielo.sdk.order.OrderManager`) | Deep Link (`lio://`) |
|---|---|---|
| Status | **Descontinuado** (dez/2024): "serão disponibilizados apenas patches… garantir versão 2.1.0 ou superior" ✅ [comunicados](https://docs.cielo.com.br/cielo-smart/docs/comunicados) | **Recomendado e obrigatório pela política** ✅ [política](https://docs.cielo.com.br/cielo-smart/docs/politica-de-integracao-camada-de-abstracao-obrigatoria) |
| Dependência | `.aar` + `event-tracker` + Datadog transitivo (o sample vendoriza `libs/order-manager-2.7.2.aar`) ✅ [app/build.gradle](https://github.com/DeveloperCielo/LIO-SDK-Sample-Integracao-Local/blob/1f626fd986bce866205e03fca9331f1e0c85260f/app/build.gradle) | Nenhuma: só `Intent` + Base64 + JSON |
| Emulador | Suporta ("Integração Local") ✅ | Suporta, e a doc diz "Recomendamos utilizar a integração via Deep Link" ✅ [baixando-o-emulador](https://docs.cielo.com.br/cielo-smart/docs/baixando-o-emulador-cielo) |
| Publicação | — | Exige `<meta-data android:name="cs_integration_type" android:value="uri"/>`; sem ele, a loja dá o erro "metadata de integração não encontrado" ✅ [FAQ deep link](https://docs.cielo.com.br/cielo-smart/docs/integracao-via-deep-link) |

O README do sample também avisa que, a partir de 15/10/2025, os apps precisam estar adaptados à Cielo Smart e que a forma de migrar é "Utilize a integração via Deeplink". ✅ [README](https://github.com/DeveloperCielo/LIO-SDK-Sample-Integracao-Local)

**Coordenadas Maven do SDK:** o sample traz comentada a linha `implementation 'com.cielo.lio:order-manager:2.7.2'` e a nota de que as `.aar` estão em `libs/` "para facilitar". ✅ A versão mais nova que vimos é a **2.7.2**. O artefato **não está no Maven Central** (o `maven-metadata.xml` dá 404), e o manual que dizia de qual repositório baixar saiu do ar. 🔶 Na prática, quem usa o SDK hoje depende do `.aar` vendorizado.

> **Recomendação:** Deep Link. É o único caminho suportado pela política vigente. Também elimina conflito de dependências (Kotlin, coroutines, Datadog) com Compose e Hilt, e reduz a integração Cielo a uma fronteira fina e fácil de falsificar em teste: "monta URI → recebe URI".

---

## 2. Fluxo completo

### 2.1 Deep Link (recomendado)

O conceito de Order da Cielo continua existindo (`draft → adicionar itens → place → checkout`) ✅ [FAQ deep link](https://docs.cielo.com.br/cielo-smart/docs/integracao-via-deep-link). **No Deep Link, porém, esses passos acontecem dentro do app da Cielo** (`com.ads.lio.uriappclient`, o "UriApp"). Nosso app só faz:

1. **Manifest** ✅ [configurando-o-android-manifest](https://docs.cielo.com.br/cielo-smart/docs/configurando-o-android-manifest) · [comunicados](https://docs.cielo.com.br/cielo-smart/docs/comunicados)
   ```xml
   <queries><package android:name="com.ads.lio.uriappclient"/></queries>   <!-- obrigatório API 30+ -->
   <application>
     <meta-data android:name="cs_integration_type" android:value="uri"/>
     <activity android:name=".CieloResponseActivity" android:exported="true">
       <intent-filter>
         <action android:name="android.intent.action.VIEW"/>
         <category android:name="android.intent.category.DEFAULT"/>
         <data android:scheme="order" android:host="response"/>  <!-- nomes livres; batem com urlCallback -->
       </intent-filter>
     </activity>
     <service android:name=".PaymentForegroundService" android:exported="false"
              android:foregroundServiceType="dataSync"/>
   </application>
   ```
2. **Montar o JSON de pagamento → Base64 → URI** ✅ [pagamento](https://docs.cielo.com.br/cielo-smart/docs/pagamento) · [DeepLinkIntegrationActivity.kt](https://github.com/DeveloperCielo/LIO-SDK-Sample-Integracao-Local/blob/1f626fd986bce866205e03fca9331f1e0c85260f/app/src/main/java/com/cielo/ordermanager/sdk/sample/activities/DeepLinkIntegrationActivity.kt)
   ```json
   { "accessToken": "...", "clientID": "...",
     "reference": "<nossa chave>",          // opcional
     "merchantCode": "...",                 // opcional (multi-EC)
     "email": "...", "installments": 0,
     "items": [{ "name": "Show X", "quantity": 2, "sku": "evt-42", "unitOfMeasure": "unidade", "unitPrice": 5000 }],
     "paymentCode": "CREDITO_AVISTA",       // opcional; se omitido, o operador escolhe na tela
     "value": "10000" }                     // centavos
   ```
   - Campos obrigatórios: `accessToken`, `clientID`, `items`, `value`. ✅
   - A doc tipa `value` como **string** em centavos, mas o sample manda `Long`. 🔶 Os dois parecem aceitos; isso se confirma no #9.
   - Valores aceitos em `paymentCode`: `CREDITO_AVISTA`, `CREDITO_PARCELADO_LOJA`, `CREDITO_PARCELADO_ADM`, `DEBITO_AVISTA`, `PIX`, `VOUCHER_*` etc. ✅ [valores-aceitos-no-campo-paymentcode](https://docs.cielo.com.br/cielo-smart/docs/valores-aceitos-no-campo-paymentcode)
   - URI: `lio://payment?request=$base64&urlCallback=order://response` ✅ (sample)
3. **Iniciar um foreground service e então `startActivity(Intent(ACTION_VIEW, uri))`.** A doc diz que é **essencial**: sem isso, o Android pode matar nosso app enquanto o app da Cielo está na frente, e "transações… não retornam ao meu app". ✅ [FAQ deep link](https://docs.cielo.com.br/cielo-smart/docs/integracao-via-deep-link) · [servico-em-primeiro-plano](https://docs.cielo.com.br/cielo-smart/docs/servico-em-primeiro-plano). O sample para o serviço ao receber a resposta (`callToStopService`). ✅
4. **Receber a resposta**, em `onCreate`/`onNewIntent` da Activity do callback ✅ [recuperando-dados](https://docs.cielo.com.br/cielo-smart/docs/recuperando-dados)
   ```kotlin
   val response = intent.data?.getQueryParameter("response")
   val json = String(Base64.decode(response, Base64.DEFAULT))
   ```

Outras actions do UriApp: `payment-reversal`, `order`, `orders`, `print`, `products`, `enabledproducts`, `establishments`, `terminalinfo`. ✅ [política](https://docs.cielo.com.br/cielo-smart/docs/politica-de-integracao-camada-de-abstracao-obrigatoria)

### 2.2 SDK Order Manager (legado: só para referência)

Nomes de API tirados do sample ✅ ([OrderManagerConnector.kt](https://github.com/DeveloperCielo/LIO-SDK-Sample-Integracao-Local/blob/1f626fd986bce866205e03fca9331f1e0c85260f/app/src/main/java/com/cielo/ordermanager/sdk/orders/OrderManagerConnector.kt), [OrderManagerController.kt](https://github.com/DeveloperCielo/LIO-SDK-Sample-Integracao-Local/blob/1f626fd986bce866205e03fca9331f1e0c85260f/app/src/main/java/com/cielo/ordermanager/sdk/orders/OrderManagerController.kt), [PaymentActivity.kt](https://github.com/DeveloperCielo/LIO-SDK-Sample-Integracao-Local/blob/1f626fd986bce866205e03fca9331f1e0c85260f/app/src/main/java/com/cielo/ordermanager/sdk/sample/activities/PaymentActivity.kt), [CancelPaymentActivity.kt](https://github.com/DeveloperCielo/LIO-SDK-Sample-Integracao-Local/blob/1f626fd986bce866205e03fca9331f1e0c85260f/app/src/main/java/com/cielo/ordermanager/sdk/sample/activities/CancelPaymentActivity.kt)):

```kotlin
val om = OrderManager(Credentials(clientId, accessToken), context)        // cielo.sdk.order / cielo.orders.domain
om.bind(context, object : ServiceBindListener {                           // onServiceBound / onServiceUnbound / onServiceBoundError(Throwable)
  override fun onServiceBound() {
    val order = om.createDraftOrder("reference")                          // Order? em DRAFT
    order.addItem(sku, name, unitPrice, quantity, "EACH")
    om.placeOrder(order)                                                  // DRAFT -> ENTERED
    val req = CheckoutRequest.Builder().orderId(order.id).amount(order.pendingAmount())
        .installments(n).paymentCode(code).ec(ec).email(email).build()
    om.checkoutOrder(req, object : PaymentListener {
      override fun onStart() {}
      override fun onPayment(order: Order) { om.updateOrder(order) }
      override fun onCancel() {}
      override fun onError(error: PaymentError) { error.description }
    })
  }
  …
})
// Consultas: retrieveOrders(pageSize, page), findOrderById(id), refreshOrdersFromServer(), retrieveEnabledProducts()
// Cancelamento: CancellationRequest.Builder().orderId().authCode().cieloCode().value().ec().build()
//               om.cancelOrder(req, CancellationListener { onSuccess(Order) / onCancel() / onError(PaymentError) })
```

Estados da Order (`Status`): `DRAFT, ENTERED, CANCELED, PAID, APPROVED, REJECTED, RE_ENTERED, CLOSED`. Itens só são editáveis em `DRAFT`, e `place` só é permitido a partir de `DRAFT`. ✅ [Status.kt](https://github.com/DeveloperCielo/LIO-SDK-Sample-Integracao-Local/blob/1f626fd986bce866205e03fca9331f1e0c85260f/app/src/main/java/com/cielo/ordermanager/sdk/utils/Status.kt)

---

## 3. Callbacks e dados devolvidos

**Sucesso**, ou seja, a Order completa em JSON ✅ [recuperando-dados](https://docs.cielo.com.br/cielo-smart/docs/recuperando-dados). Campos-chave:

- Order: `id` (UUID da Order Cielo), `reference` (o nosso), `status` (ex.: `ENTERED`/`PAID`), `price`, `paidAmount`, `pendingAmount`, `items[]` (com `id` gerado pela Cielo), `payments[]`, `createdAt`, `updatedAt`, `type: "PAYMENT"`.
- `payments[]`: `id`, `externalId`, `amount`, `authCode`, `cieloCode` (NSU), `brand`, `mask`, `installments`, `merchantCode`, `terminal`, `primaryCode`/`secondaryCode`, `requestDate` e `paymentFields{…}`. Este último traz `statusCode` (**0 = Pix, 1 = autorizada, 2 = cancelada**), `productName`, `paymentTransactionId`, `cardCaptureType`, `merchantName`, `hasPrintedClientReceipt` etc.
- Para cancelar depois, é preciso guardar **`order.id`, `payments[i].cieloCode`, `payments[i].authCode`, `payments[i].amount` e `merchantCode`**. ✅ [cancelamento](https://docs.cielo.com.br/cielo-smart/docs/cancelamento)

**Cancelado pelo usuário / erro:** `response` decodifica para `{"code": 1, "reason": "CANCELADO PELO USUÁRIO"}`. O exemplo da doc vem com `responsecode=0`. ✅ [codigos-de-erro](https://docs.cielo.com.br/cielo-smart/docs/codigos-de-erro)
🔶 Logo, **o discriminador é a forma do JSON** (tem `code`+`reason` → falha; tem `id`+`payments` → Order), e não o `responsecode`. Isso precisa ser confirmado no emulador (#9).

**No emulador:** ao tocar "Pagar", o operador escolhe **Sucesso / Cancelado / Erro**. O resultado é enviado ao integrador e o emulador vai para segundo plano. ✅ [realizando-pagamentos](https://docs.cielo.com.br/cielo-smart/docs/realizando-pagamentos)

🔶 Ponto aberto: a doc não diz qual `status` a Order traz num pagamento aprovado. O exemplo mostra `"status":"ENTERED"` com `paidAmount == price` e `pendingAmount: 0`, e a listagem mostra `"PAID"`. **Não decida "aprovado" pelo `status` da Order.** Decida por `paidAmount >= valor` **e** por um `payment` com `paymentFields.statusCode ∈ {0,1}`.

---

## 4. Credenciais

- Para obter `clientID` e `accessToken`: cadastre um app no [Portal de Desenvolvedores](https://desenvolvedores.cielo.com.br/api-portal/pt-br/myapps/new) e escolha a API **"Cielo Smart - Order Manager"**. As credenciais aparecem em **Perfil → Client-IDs Cadastrados**. ✅ [autenticacao-e-credenciais](https://docs.cielo.com.br/cielo-smart/docs/autenticacao-e-credenciais)
- A FAQ diz que o cadastro é necessário para testar. ✅ [FAQ deep link](https://docs.cielo.com.br/cielo-smart/docs/integracao-via-deep-link)
- 🔶 Não achamos na doc se o **emulador valida** as credenciais. O sample roda com placeholders `"xxxxxxxxxx"`, o que sugere que o emulador aceita qualquer valor. Confirmar no #9. Se aceitar, o case pode rodar sem conta no portal. Em qualquer caso, as credenciais devem vir de `local.properties`/`BuildConfig`, nunca commitadas ("Nunca exponha tokens") ✅ [boas-praticas](https://docs.cielo.com.br/cielo-smart/docs/boas-praticas-para-desenvolvedores).

---

## 5. Emulador

- APK **1.61.8**: `https://s3-sa-east-1.amazonaws.com/cielo-lio-store/apps/lio-emulator/1.61.8/lio-emulator.apk` ✅ [baixando-o-emulador](https://docs.cielo.com.br/cielo-smart/docs/baixando-o-emulador-cielo)
- Só instala em **Android 7(.1) ou 10**, em celular, tablet ou **AVD**. Versões acima de 10 "podem apresentar incompatibilidades". **Proibido** instalar em terminal debug. ✅ [FAQ deep link](https://docs.cielo.com.br/cielo-smart/docs/integracao-via-deep-link) · [emulador-cielo](https://docs.cielo.com.br/cielo-smart/docs/emulador-cielo)
- Passos: `adb install lio-emulator.apk` → abrir o emulador (botões **Configurações**, para respostas automáticas de impressão, e **Limpar dados**, que apaga as Orders criadas) → instalar nosso app no **mesmo** AVD → disparar o deep link → escolher Sucesso/Cancelado/Erro. ✅
- No emulador, o cancelamento é simplificado: basta **Confirmar Cancelamento** e escolher a resposta. ✅ [cancelando-pagamentos](https://docs.cielo.com.br/cielo-smart/docs/cancelando-pagamentos)
- 🔶 Para o #9: criar um AVD **API 29 (Android 10)**, que é a mesma versão do `targetSdk` recomendado e cobre o DX8000.

---

## 6. Restrições de plataforma

| Item | Valor | Fonte |
|---|---|---|
| Linguagem | Java ou Kotlin | ✅ [pre-requisitos](https://docs.cielo.com.br/cielo-smart/docs/pre-requisitos) |
| `minSdk` / `targetSdk` | recomendado **24 / 29**; target < 29 **não é distribuído**; target ≥ 31 exige assinatura **APK Signature Scheme v2** | ✅ [pre-requisitos](https://docs.cielo.com.br/cielo-smart/docs/pre-requisitos) |
| O sample usa | `compileSdk 34`, `minSdk 25`, `targetSdk 34`, Kotlin 1.9.0, AGP 8.1.3, Java 17, View Binding | ✅ [app/build.gradle](https://github.com/DeveloperCielo/LIO-SDK-Sample-Integracao-Local/blob/1f626fd986bce866205e03fca9331f1e0c85260f/app/build.gradle) |
| Android dos terminais | DX8000 = 10 · L300 v3 = **7.1** · L400 = 11 · L300 v4 = 11 · N950U = 12; 1–2 GB de RAM; telas de 480x854 a 720x1600 | ✅ [terminais](https://docs.cielo.com.br/cielo-smart/docs/terminais-e-caracteristicas-da-cielo-smart) |
| Removidos do SO | **Google Play Services** (nada de FCM/Maps/Play Integrity) e **WebView**; usar WebView reprova a certificação | ✅ [recursos-indisponiveis](https://docs.cielo.com.br/cielo-smart/docs/recursos-indisponiveis-e-sugestoes-de-substitutos) |
| Kiosk mode / Device Owner | indisponível | ✅ [funcionalidades-1](https://docs.cielo.com.br/cielo-smart/docs/funcionalidades-1) |
| Proibido | acessar o SDK/libs do **fabricante** (Ingenico/Positivo/Newland) | ✅ [política](https://docs.cielo.com.br/cielo-smart/docs/politica-de-integracao-camada-de-abstracao-obrigatoria) |
| UI | a Cielo recomenda Material Design | ✅ [boas-praticas](https://docs.cielo.com.br/cielo-smart/docs/boas-praticas-para-desenvolvedores) |

**Compose / Hilt / versão de Kotlin** 🔶 (a doc não fala nada sobre isso):
- Com Deep Link não entra nenhuma lib da Cielo no classpath, então **não há conflito de versão** de Kotlin, coroutines ou Datadog. Compose, Hilt, Room e Navigation ficam livres. Com o SDK, haveria o risco de conflito que a própria Cielo cita ("Compatibilidade de bibliotecas terceiras").
- `minSdk 24` atende o Compose atual e o Hilt. O Compose pesa em terminal de **1 GB de RAM** (L300 v3/DX8000), o que é aceitável para um app pequeno e merece uma menção no README.
- Hilt não depende de Play Services: ok. **Crashlytics e Firebase dependem de GMS**, apesar de a Cielo sugerir Crashlytics. Fica fora.
- Se escolhermos `targetSdk ≥ 34` (como o sample), um FGS com `foregroundServiceType="dataSync"` exige a permissão `FOREGROUND_SERVICE_DATA_SYNC`, e a partir do Android 13 há o pedido de `POST_NOTIFICATIONS`. Com `targetSdk 29`, nenhuma das duas se aplica. O recomendado pela Cielo (29) simplifica, mas o Android Studio atual vai reclamar do target baixo. **Decisão para o #5.**
- O callback chega numa **Activity** via intent-filter. Com Compose single-activity, dá para usar a própria `MainActivity` com `launchMode="singleTop"` + `onNewIntent` (o sample faz isso em `DeepLinkIntegrationActivity`) ✅, ou uma `ResponseActivity` "trampolim" que só repassa o resultado ao repositório e fecha. 🔶

---

## 7. Idempotência do lado Cielo

| Pergunta | Resposta |
|---|---|
| Existe chave de idempotência? | **Não documentada.** Existe `reference`, string livre e opcional, "utilizada para controle interno". O sample gera `"uriapp #<epochSeconds>"`. ✅ [pagamento](https://docs.cielo.com.br/cielo-smart/docs/pagamento). 🔶 Nada indica que a Cielo rejeite um segundo `lio://payment` com o mesmo `reference`: **assuma que ela cobra de novo**. |
| Quem gera o `orderId`? | A Cielo, dentro do UriApp. **Só ficamos sabendo do `id` pelo callback.** 🔶 Se o callback se perder (app morto, terminal desligado), não temos o `id`. |
| Consultar uma Order | `lio://order` com `{orderId, clientID, accessToken}`, ou `{amount, authCode, cieloCode, orderId, …}`. Order inexistente → `{"code":2,"reason":"Order not found"}`. ✅ [consulta-de-pedido-avulso](https://docs.cielo.com.br/cielo-smart/docs/consulta-de-pedido-avulso) |
| Listar Orders | `lio://orders` com `{pageSize ≤ 5, page, clientID, accessToken}`. Retorna `{currentPage, results[], totalItems, totalPages}`. **Sem filtro por reference.** ✅ [listagem-de-pedidos](https://docs.cielo.com.br/cielo-smart/docs/listagem-de-pedidos). 🔶 A ordenação não é documentada. |
| Cancelar / estornar | `lio://payment-reversal` com `{id, clientID, accessToken, cieloCode, authCode, value}` (o sample também manda `merchantCode`). O sucesso devolve a Order com um novo `payment` em `paymentFields.statusCode = "2"`. Prazos: **crédito 30 dias, débito no mesmo dia, Pix 90 dias**; parcial ou total. ✅ [cancelamento](https://docs.cielo.com.br/cielo-smart/docs/cancelamento) · [cancelando-pagamentos](https://docs.cielo.com.br/cielo-smart/docs/cancelando-pagamentos) |
| Aviso de cancelamento feito fora do app | Só no SDK: broadcast `cielo.action.NOTIFY_TRANSACTION_CANCEL` com `ParcelableOrder` e `ParcelableTransaction`, filtrado por `accessKey`/`secretAccessKey`. ✅ [manifest do sample](https://github.com/DeveloperCielo/LIO-SDK-Sample-Integracao-Local/blob/1f626fd986bce866205e03fca9331f1e0c85260f/app/src/main/AndroidManifest.xml). 🔶 Não está documentado para Deep Link. |
| Integração remota (REST Order Manager, webhooks) | Existe (criar, consultar e listar Orders, notificações de status), mas é o modelo **remoto**, com servidor. Está fora do escopo (sem backend). ✅ [llms.txt](https://docs.cielo.com.br/cielo-smart/llms.txt) |

🔶 **O que isso implica para o #4:** a idempotência tem que ser **nossa**.
1. Gerar o `reference = purchaseId` (UUID) e persistir a Purchase como "aguardando pagamento" **antes** de disparar o deep link.
2. Chamar o deep link dentro do foreground service.
3. Ao receber o callback, casar pelo `reference`.
4. Na reconciliação de Purchase pendente (app reaberto sem callback), paginar `lio://orders` (5 por vez) procurando o `reference`. Se achar a Order paga, aprovar. Se não achar, oferecer "tentar de novo" **com o mesmo `reference`**.

Cada consulta `lio://order`/`orders` também é uma ida e volta por intent (o app da Cielo vem para a frente). Por isso a reconciliação é cara e deve ser rara.

---

## 8. Catálogo de erros

| Origem | Formato | Códigos |
|---|---|---|
| `payment` / `payment-reversal` / `orders` | `response` = Base64 de `{code, reason}` | `1` Cancelado pelo usuário · `2` Erro genérico · `3` Erro no pagamento (ex.: saldo insuficiente) · `4` Erro de autenticação ✅ [codigos-de-erro](https://docs.cielo.com.br/cielo-smart/docs/codigos-de-erro) · [recuperando-dados](https://docs.cielo.com.br/cielo-smart/docs/recuperando-dados) |
| `order` (consulta) | idem | `2` "Order not found" (com `responsecode=2`) ✅ [consulta-de-pedido-avulso](https://docs.cielo.com.br/cielo-smart/docs/consulta-de-pedido-avulso) |
| `print` | Base64 com o motivo | ver [impressão](https://docs.cielo.com.br/cielo-smart/docs/impressao) ✅ |
| Lado do nosso app 🔶 | — | `ActivityNotFoundException` (UriApp/emulador não instalado ou `<queries>` faltando) · callback que nunca chega (processo morto, ver §2.1 passo 3) · `response` ausente ou Base64/JSON inválido |
| Erros exibidos no terminal | — | catálogo do terminal: [atendimento/erros-comuns](https://docs.cielo.com.br/atendimento/docs/erros-comuns) ✅ (citado em [julho-de-2026](https://docs.cielo.com.br/cielo-smart/docs/julho-de-2026); não lido em detalhe) |

---

## 9. Para confirmar no #9 (emulador)

- [ ] O emulador aceita `clientID`/`accessToken` falsos?
- [ ] Qual é o `responsecode` real em sucesso, cancelamento e erro? O JSON de sucesso traz o `reference` que mandamos?
- [ ] Qual é o `status` da Order no sucesso (`ENTERED` ou `PAID`)?
- [ ] `value` como string ou número?
- [ ] O que acontece com um segundo `lio://payment` com o mesmo `reference`?
- [ ] Ordem e conteúdo do `lio://orders` depois de "Limpar dados".
- [ ] Tempo de ida e volta e comportamento com o app em background ou morto (com e sem foreground service).
