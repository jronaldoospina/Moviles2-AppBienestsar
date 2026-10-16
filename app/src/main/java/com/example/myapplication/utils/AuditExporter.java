package com.example.myapplication.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.example.myapplication.models.AuditoriaLog;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Exporta el registro de auditoria a PDF y lo abre para compartir.
 * Permite a la direccion obtener evidencia de la trazabilidad del sistema.
 */
public final class AuditExporter {

    private AuditExporter() {}

    public static void exportar(Context context, List<AuditoriaLog> logs) {
        if (context == null || logs == null || logs.isEmpty()) return;
        try {
            File dir = new File(context.getCacheDir(), "documentos");
            if (!dir.exists() && !dir.mkdirs()) return;

            String stamp = new SimpleDateFormat("yyyyMMdd_HHmm", new Locale("es", "CO"))
                    .format(new Date());
            File pdf = new File(dir, "auditoria_" + stamp + ".pdf");

            List<String> lineas = new java.util.ArrayList<>();
            lineas.add("Total de registros: " + logs.size());
            lineas.add("");
            lineas.add("FECHA HORA | ROL | USUARIO | ACCION | DETALLE");
            lineas.add("------------------------------------------------------------");

            SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("es", "CO"));
            for (AuditoriaLog l : logs) {
                String fecha = l.getFecha() > 0 ? f.format(new Date(l.getFecha())) : "-";
                String rol = l.getRol() != null ? l.getRol() : "-";
                String nombre = l.getUsuarioNombre() != null ? l.getUsuarioNombre() : "-";
                String tipo = l.getTipo() != null ? l.getTipo() : "-";
                String detalle = l.getDetalle() != null ? l.getDetalle() : "-";
                lineas.add(fecha + " | " + rol + " | " + nombre
                        + " | " + tipo + " | " + detalle);
            }

            String autor = "Generado: " + f.format(new Date())
                    + "  |  App: Bienestar Universitario UPC Aguachica";
            PdfExporter.escribirExpediente(new FileOutputStream(pdf),
                    "Reporte de Auditoria - Trazabilidad", autor, lineas);

            compartir(context, pdf);

        } catch (Exception e) {
            android.widget.Toast.makeText(context,
                    "No se pudo exportar: " + e.getMessage(),
                    android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private static void compartir(Context context, File pdf) {
        Uri uri = FileProvider.getUriForFile(context,
                context.getPackageName() + ".fileprovider", pdf);
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("application/pdf");
        i.putExtra(Intent.EXTRA_STREAM, uri);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(i, "Compartir reporte de auditoria"));
    }
}