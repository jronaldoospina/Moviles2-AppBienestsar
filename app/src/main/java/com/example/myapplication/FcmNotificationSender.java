package com.example.myapplication;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import com.google.firebase.messaging.FirebaseMessaging;

public class FcmNotificationSender {

    private static final String CHANNEL_ID = "bienestar_upc_alerts";
    private static final String CHANNEL_NAME = "Alertas Bienestar UPC";

    public static void subscribeToAlertsTopic(Context context) {
        FirebaseMessaging.getInstance().subscribeToTopic(CHANNEL_ID)
                .addOnSuccessListener(aVoid -> {
                })
                .addOnFailureListener(e -> {
                });
    }

    public static String getToken(Context context) {
        return null;
    }

    public static void createNotificationChannel(NotificationManager manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notificaciones de alertas de bienestar Universitario");
            channel.enableLights(true);
            channel.setLightColor(0xFFFFC107);
            channel.enableVibration(true);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static String formatMotivo(String motivo) {
        if (motivo == null) return "Sin especificar";
        switch (motivo) {
            case "BAJO_RENDIMIENTO": return "Bajo rendimiento";
            case "INASISTENCIA": return "Inasistencia";
            case "COMPORTAMIENTO": return "Comportamiento";
            case "OTRO": return "Otro";
            default: return "Sin especificar";
        }
    }

    public static void enviarAlertaNotificacion(String token, String estudianteNombre, String motivo, String cursoNombre, String descripcion) {
        // Stub for FCM notification sending
    }
}