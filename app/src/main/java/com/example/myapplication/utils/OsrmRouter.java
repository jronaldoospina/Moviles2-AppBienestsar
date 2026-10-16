package com.example.myapplication.utils;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcula la geometria real de la ruta (sobre las calles) usando el motor
 * de ruteo de OpenStreetMap: OSRM.
 *
 * Sin API key. Si no hay internet o el servicio falla, devuelve null y el
 * llamador dibuja la linea recta entre paradas (fallback degradado).
 *
 * OSRM espera las coordenadas en orden LONGITUD,LATITUD.
 */
public final class OsrmRouter {

    private static final String BASE =
            "https://router.project-osrm.org/route/v1/driving/";

    /** Callback con la geometria de la ruta (lista de puntos sobre la calle). */
    public interface Callback {
        void onRuta(List<GeoPoint> puntos);
    }

    private OsrmRouter() {}

    public static void calcularRuta(List<GeoPoint> paradas, Callback cb) {
        new Thread(() -> {
            List<GeoPoint> resultado = null;
            try {
                resultado = consultar(paradas);
            } catch (Exception ignored) {
                resultado = null;
            }
            final List<GeoPoint> puntos = resultado;
            new Handler(Looper.getMainLooper()).post(() -> cb.onRuta(puntos));
        }).start();
    }

    private static List<GeoPoint> consultar(List<GeoPoint> paradas)
            throws Exception {
        if (paradas == null || paradas.size() < 2) return null;

        StringBuilder coords = new StringBuilder();
        for (int i = 0; i < paradas.size(); i++) {
            GeoPoint p = paradas.get(i);
            if (i > 0) coords.append(";");
            // OSRM: longitud primero
            coords.append(p.getLongitude()).append(",").append(p.getLatitude());
        }

        String url = BASE + coords
                + "?overview=full&geometries=geojson";

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestMethod("GET");
        // User-Agent requerido por la politica de uso de OSRM
        conn.setRequestProperty("User-Agent", "BienestarUPC-Aguachica/1.0");

        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
        } finally {
            conn.disconnect();
        }

        JSONObject root = new JSONObject(sb.toString());
        if (!"Ok".equals(root.optString("code"))) return null;

        JSONArray routes = root.optJSONArray("routes");
        if (routes == null || routes.length() == 0) return null;

        JSONObject route = routes.getJSONObject(0);
        JSONObject geometry = route.optJSONObject("geometry");
        if (geometry == null) return null;

        JSONArray coordsArray = geometry.optJSONArray("coordinates");
        if (coordsArray == null || coordsArray.length() == 0) return null;

        List<GeoPoint> puntos = new ArrayList<>();
        for (int i = 0; i < coordsArray.length(); i++) {
            JSONArray c = coordsArray.getJSONArray(i);
            double lng = c.getDouble(0);
            double lat = c.getDouble(1);
            puntos.add(new GeoPoint(lat, lng));
        }
        return puntos;
    }
}