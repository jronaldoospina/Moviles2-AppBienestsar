package com.example.myapplication;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class CitaReminderReceiver extends BroadcastReceiver {

    public static final String EXTRA_CITA_ID = "cita_id";
    public static final String EXTRA_TITULO = "titulo";
    public static final String EXTRA_CUERPO = "cuerpo";
    public static final String CHANNEL_ID = "recordatorios_citas";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        String citaId = intent.getStringExtra(EXTRA_CITA_ID);
        String titulo = intent.getStringExtra(EXTRA_TITULO);
        String cuerpo = intent.getStringExtra(EXTRA_CUERPO);
        if (citaId == null || titulo == null || cuerpo == null) return;

        crearCanal(context);

        Intent contenido = new Intent(context, MainActivity.class);
        contenido.putExtra(MainActivity.EXTRA_NAV_DESTINO, "citas");
        contenido.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(context, 0, contenido,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(titulo)
                .setContentText(cuerpo)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(cuerpo))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi);

        try {
            NotificationManagerCompat.from(context).notify(requestCode(citaId), b.build());
        } catch (SecurityException ignored) {
        }
    }

    private void crearCanal(Context context) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            NotificationChannel canal = new NotificationChannel(CHANNEL_ID,
                    "Recordatorios de citas", NotificationManager.IMPORTANCE_HIGH);
            canal.setDescription("Aviso antes de una cita de bienestar");
            nm.createNotificationChannel(canal);
        }
    }

    private static int requestCode(String citaId) {
        int h = citaId.hashCode();
        return h == Integer.MIN_VALUE ? 0 : Math.abs(h);
    }
}