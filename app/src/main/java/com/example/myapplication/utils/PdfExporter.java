package com.example.myapplication.utils;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Genera un PDF simple de texto (A4) para exportar expedientes.
 */
public class PdfExporter {

    private static final int W = 595;   // A4 en puntos
    private static final int H = 842;

    public static void escribirExpediente(OutputStream out,
                                          String titulo,
                                          String autor,
                                          List<String> lineas) throws IOException {
        Paint tituloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tituloPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        tituloPaint.setTextSize(18f);
        tituloPaint.setColor(0xFF2E7D32);

        Paint autorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        autorPaint.setTextSize(11f);
        autorPaint.setColor(0xFF757575);

        Paint lineaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lineaPaint.setColor(0xFFE0E0E0);
        lineaPaint.setStrokeWidth(1.5f);

        Paint textoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textoPaint.setTextSize(11f);
        textoPaint.setColor(0xFF212121);

        PdfDocument doc = new PdfDocument();
        int pagina = 1;
        PdfDocument.Page page = doc.startPage(
                new PdfDocument.PageInfo.Builder(W, H, pagina).create());
        Canvas canvas = page.getCanvas();

        canvas.drawText(titulo, 40, 50, tituloPaint);
        if (autor != null && !autor.isEmpty()) {
            canvas.drawText(autor, 40, 66, autorPaint);
        }
        canvas.drawLine(40, 76, W - 40, 76, lineaPaint);

        int y = 96;
        for (String linea : lineas) {
            if (y > H - 50) {
                doc.finishPage(page);
                pagina++;
                page = doc.startPage(
                        new PdfDocument.PageInfo.Builder(W, H, pagina).create());
                canvas = page.getCanvas();
                y = 50;
            }
            for (String parte : envolver(canvas, linea, textoPaint, W - 80)) {
                canvas.drawText(parte, 40, y, textoPaint);
                y += 16;
            }
        }

        doc.finishPage(page);
        doc.writeTo(out);
        doc.close();
    }

    private static List<String> envolver(Canvas canvas, String linea,
                                         Paint paint, float maxW) {
        List<String> partes = new ArrayList<>();
        if (linea == null || linea.isEmpty()) {
            partes.add(" ");
            return partes;
        }
        if (canvas.getWidth() <= 0 || paint.measureText(linea) <= maxW) {
            partes.add(linea);
            return partes;
        }
        StringBuilder actual = new StringBuilder();
        for (String palabra : linea.split(" ")) {
            String prueba = actual.length() == 0
                    ? palabra
                    : actual + " " + palabra;
            if (paint.measureText(prueba) > maxW && actual.length() > 0) {
                partes.add(actual.toString());
                actual = new StringBuilder(palabra);
            } else {
                actual = new StringBuilder(prueba);
            }
        }
        if (actual.length() > 0) {
            partes.add(actual.toString());
        }
        return partes;
    }
}
