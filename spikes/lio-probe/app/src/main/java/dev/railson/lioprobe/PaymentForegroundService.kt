package dev.railson.lioprobe

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder

/** Mesmo desenho do DeepLinkService do sample oficial: sobe em primeiro plano no onCreate. */
class PaymentForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        startForeground(1, notification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(true)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun notification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Pagamento", NotificationManager.IMPORTANCE_LOW)
            )
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("LIO Probe")
            .setContentText("Aguardando resposta da Cielo")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "payment"
        private const val ACTION_STOP = "STOP"

        fun start(context: Context) {
            context.startService(Intent(context, PaymentForegroundService::class.java))
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, PaymentForegroundService::class.java).setAction(ACTION_STOP)
            )
        }
    }
}
