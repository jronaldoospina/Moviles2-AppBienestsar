package com.example.myapplication.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Utilidades para formatear fechas (formato "yyyy-MM-dd").
 */
public class FechaUtils {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final Locale ES = new Locale("es", "CO");

    /** Convierte un timestamp en milisegundos a "15 de septiembre de 2026" */
    public static String fechaBonitaLong(long ms) {
        LocalDate d = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate();
        return d.getDayOfMonth() + " de "
                + d.getMonth().getDisplayName(TextStyle.FULL, ES)
                + " de " + d.getYear();
    }

    /** Convierte un timestamp en milisegundos a "15 sep" (fecha corta). */
    public static String fechaCorta(long ms) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("d MMM", ES);
        return sdf.format(new java.util.Date(ms));
    }

    /** Convierte un timestamp en milisegundos a "HH:mm". */
    public static String horaCorta(long ms) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", ES);
        return sdf.format(new java.util.Date(ms));
    }

    /** Convierte "2026-09-15" a "15 de septiembre" */
    public static String fechaBonita(String fecha) {
        try {
            LocalDate d = LocalDate.parse(fecha, ISO);
            return d.getDayOfMonth() + " de "
                    + d.getMonth().getDisplayName(TextStyle.FULL, ES);
        } catch (Exception e) {
            return fecha;
        }
    }

    /** Convierte "2026-09-15" a "15 de septiembre de 2026" */
    public static String fechaBonitaConAnio(String fecha) {
        try {
            LocalDate d = LocalDate.parse(fecha, ISO);
            return d.getDayOfMonth() + " de "
                    + d.getMonth().getDisplayName(TextStyle.FULL, ES)
                    + " de " + d.getYear();
        } catch (Exception e) {
            return fecha;
        }
    }

    /** Fecha de hoy en formato "yyyy-MM-dd" (para comparaciones) */
    public static String hoyIso() {
        return LocalDate.now().toString();
    }
}