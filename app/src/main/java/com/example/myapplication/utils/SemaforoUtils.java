package com.example.myapplication.utils;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.example.myapplication.R;
import com.example.myapplication.models.Curso;
import com.example.myapplication.models.Nota;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Utilidades compartidas para el cálculo y presentación del semáforo académico.
 * Escala 0.0 - 5.0:
 *  - Verde  (BAJO)  : >= 4.0
 *  - Ámbar  (MEDIO) : 3.0 - 3.9
 *  - Rojo   (ALTO)  : < 3.0
 */
public class SemaforoUtils {

    public static final double APROBADO = 3.0;
    public static final int NUM_CORTES = 3;

    public static int colorFor(Context ctx, double valor) {
        if (valor >= 4.0) return ContextCompat.getColor(ctx, R.color.green_primary);
        if (valor >= 3.0) return ContextCompat.getColor(ctx, R.color.risk_medium_text);
        return ContextCompat.getColor(ctx, R.color.risk_high_text);
    }

    public static int drawableFor(double valor) {
        if (valor >= 4.0) return R.drawable.semaforo_circle_green;
        if (valor >= 3.0) return R.drawable.semaforo_circle_amber;
        return R.drawable.semaforo_circle_red;
    }

    public static String nivelFor(double valor) {
        if (valor >= 4.0) return "BAJO";
        if (valor >= 3.0) return "MEDIO";
        return "ALTO";
    }

    public static String format(double valor) {
        return String.format(Locale.US, "%.1f", valor);
    }

    public static String shortName(String nombre) {
        if (nombre == null || nombre.length() <= 12) return nombre;
        return nombre.substring(0, 12) + "...";
    }

    /**
     * Promedio de una materia (último valor registrado por corte).
     */
    public static class PromedioMateria {
        public String id;
        public String nombre;
        public String codigo;
        public double promedio;
        public Double[] cortes = new Double[NUM_CORTES];
    }

    public static PromedioMateria computePromedioMateria(String cursoId, List<Nota> notas, Curso curso) {
        PromedioMateria cp = new PromedioMateria();
        cp.id = cursoId;
        cp.nombre = (curso != null && curso.getNombre() != null) ? curso.getNombre() : "Materia";
        cp.codigo = (curso != null && curso.getCodigo() != null) ? curso.getCodigo() : "";

        Map<String, Nota> latest = new HashMap<>();
        for (Nota n : notas) {
            if (n.getCorteId() == null) continue;
            Nota current = latest.get(n.getCorteId());
            if (current == null || n.getFechaRegistro() > current.getFechaRegistro()) {
                latest.put(n.getCorteId(), n);
            }
        }

        double suma = 0;
        int count = 0;
        for (int i = 0; i < NUM_CORTES; i++) {
            Nota n = latest.get("corte" + (i + 1));
            if (n != null) {
                cp.cortes[i] = n.getValor();
                suma += n.getValor();
                count++;
            }
        }
        cp.promedio = count > 0 ? suma / count : 0;
        return cp;
    }

    public static int enRiesgo(List<PromedioMateria> items) {
        int count = 0;
        for (PromedioMateria cp : items) {
            if (cp.promedio > 0 && cp.promedio < APROBADO) count++;
        }
        return count;
    }

    public static int aprobadas(List<PromedioMateria> items) {
        int count = 0;
        for (PromedioMateria cp : items) {
            if (cp.promedio >= APROBADO) count++;
        }
        return count;
    }

    public static double promedioGeneral(List<PromedioMateria> items) {
        if (items.isEmpty()) return 0;
        double suma = 0;
        for (PromedioMateria cp : items) suma += cp.promedio;
        return suma / items.size();
    }
}