package dev.railson.ticketsales.payment.cielolio.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import dev.railson.ticketsales.core.domain.model.FailureReason
import dev.railson.ticketsales.core.domain.terminal.PaymentOutcome
import dev.railson.ticketsales.core.domain.usecase.payment.HandlePaymentOutcomeUseCase
import dev.railson.ticketsales.payment.cielolio.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Dispara o `lio://payment` em primeiro plano e fica ligado até o callback, como no sample oficial da Cielo:
 * com o processo em primeiro plano, o Android não o mata enquanto a tela da Cielo está aberta (cenário C8).
 * Quem desliga é a `LioCallbackActivity`.
 */
internal class PaymentDispatchService : Service() {

    private val handleOutcome: HandlePaymentOutcomeUseCase by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, notification())
        val deepLink = intent?.getStringExtra(EXTRA_DEEP_LINK) ?: return stop()
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            // O app da Cielo sumiu depois da checagem prévia: a Purchase já está em disco e precisa de um desfecho.
            scope.launch {
                handleOutcome(PaymentOutcome.Failed(FailureReason.TerminalUnavailable))
                stop()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun stop(): Int {
        stopSelf()
        return START_NOT_STICKY
    }

    private fun notification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, getString(R.string.cielo_payment_channel), NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle(getString(R.string.cielo_payment_notification_title))
            .setContentText(getString(R.string.cielo_payment_notification_text))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "payment"
        private const val NOTIFICATION_ID = 1
        private const val EXTRA_DEEP_LINK = "deep_link"

        fun dispatch(context: Context, deepLink: String) {
            val intent = Intent(context, PaymentDispatchService::class.java).putExtra(EXTRA_DEEP_LINK, deepLink)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PaymentDispatchService::class.java))
        }
    }
}
