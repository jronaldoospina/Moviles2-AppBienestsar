package com.example.myapplication.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.myapplication.CitaReminderReceiver;
import com.example.myapplication.models.Cita;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public class CitaReminderScheduler {

    private static final long HORA_MS = 60L * 60L * 1000L;
    private static final long MARGEN_TARDE_MS = 15L * 60L * 1000L;

    private CitaReminderScheduler() {}

    /**
     * Programa (o cancela) el recordatorio local de una cita:
     * - Estado cancelada/atendida o cita pasada -> cancela cualquier alarma.
     * - Si la cita es futura, agenda el aviso 1 h antes (o 15 min si ya pasa).
     */
    public static void sincronizar(Context context, Cita cita) {
        if (cita == null || cita.getId() == null || cita.getId().isEmpty()) return;

        String estado = cita.getEstado() != null ? cita.getEstado() : "PENDIENTE";
        boolean activa = !("CANCELADA".equals(estado) || "ATENDIDA".equals(estado));
        long inicioMs = aEpochMillis(cita.getFecha(), cita.getHora());
        if (!activa || inicioMs <= 0 || inicioMs <= System.currentTimeMillis()) {
            cancelar(context, cita.getId());
            return;
        }

        long ahora = System.currentTimeMillis();
        long momento = inicioMs - HORA_MS;
        if (momento - ahora < MARGEN_TARDE_MS) {
            momento = inicioMs - MARGEN_TARDE_MS;
        }
        if (momento <= ahora) return;

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, CitaReminderReceiver.class);
        intent.putExtra(CitaReminderReceiver.EXTRA_CITA_ID, cita.getId());
        intent.putExtra(CitaReminderReceiver.EXTRA_TITULO, "Recordatorio de cita");
        String cuerpo = "Tienes una cita";
        if (cita.getEstudianteNombre() != null && !cita.getEstudianteNombre().isEmpty()) {
            cuerpo += " con " + cita.getEstudianteNombre();
        }
        cuerpo += " el " + (cita.getFecha() != null ? FechaUtils.fechaBonitaConAnio(cita.getFecha()) : "-")
                + " a las " + (cita.getHora() != null ? cita.getHora() : "-") + ".";
        intent.putExtra(CitaReminderReceiver.EXTRA_CUERPO, cuerpo);

        PendingIntent pi = pendingIntent(context, cita.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, momento, pi);
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, momento, pi);
        }
    }

    /** Cancela el recordatorio de una cita (mismo id, mismo requestCode). */
    public static void cancelar(Context context, String citaId) {
        if (citaId == null || citaId.isEmpty()) return;
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent intent = new Intent(context, CitaReminderReceiver.class);
        PendingIntent pi = pendingIntent(context, citaId, intent, PendingIntent.FLAG_NO_CREATE);
        if (pi != null) {
            am.cancel(pi);
            pi.cancel();
        }
    }

    private static PendingIntent pendingIntent(Context context, String citaId, Intent intent, int flags) {
        return PendingIntent.getBroadcast(context, requestCode(citaId), intent,
                flags | PendingIntent.FLAG_IMMUTABLE);
    }

    static int requestCode(String citaId) {
        int h = citaId.hashCode();
        return h == Integer.MIN_VALUE ? 0 : Math.abs(h);
    }

    private static long aEpochMillis(String fecha, String hora) {
        if (fecha == null || fecha.isEmpty()) return 0;
        try {
            String[] p = fecha.split("-");
            if (p.length != 3) return 0;
            LocalDate d = LocalDate.of(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]));
            LocalTime t = (hora != null && hora.matches("\\d{1,2}:\\d{2}"))
                    ? LocalTime.parse(hora)
                    : LocalTime.of(0, 0);
            return LocalDateTime.of(d, t).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            return 0;
        }
    }
}