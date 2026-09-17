package com.example.myapplication;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private static int notificationIdCounter = 0;

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "Token renovado obtenido");
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (remoteMessage.getData().size() > 0) {
            String title = remoteMessage.getData().get("titulo");
            String body = remoteMessage.getData().get("cuerpo");
            String estudianteNombre = remoteMessage.getData().get("estudiante_nombre");
            String motivo = remoteMessage.getData().get("motivo");
            String cursoNombre = remoteMessage.getData().get("curso_nombre");

            if (title != null || body != null) {
                enviarNotificacion(title, body, estudianteNombre, motivo, cursoNombre);
            }
        }

        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Notification payload received: " + remoteMessage.getNotification().getBody());
        }
    }

    private void enviarNotificacion(String title, String body, String estudianteNombre, String motivo, String cursoNombre) {
        notificationIdCounter++;
        int notificationId = notificationIdCounter;

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = generateChannelId(title, motivo);
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Alertas Bienestar UPC",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notificaciones de alertas de bienestar Universitario");
            channel.setImportance(NotificationManager.IMPORTANCE_HIGH);
            channel.enableLights(true);
            channel.setLightColor(0xFFFFC107);
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, generateChannelId(title, motivo))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title != null ? title : "Nueva alerta de bienestar")
                .setContentText(body != null ? body : "Se ha reportado una alerta de bienestar")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        if (estudianteNombre != null) {
            builder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(body != null ? body : ""));
        }

        notificationManager.notify(notificationId, builder.build());
    }

    private String generateChannelId(String title, String motivo) {
        if (title != null && !title.isEmpty()) {
            return "bienestar_upc_" + title.hashCode();
        }
        if (motivo != null && !motivo.isEmpty()) {
            return "bienestar_upc_" + motivo.hashCode();
        }
        return "bienestar_upc_default";
    }
}