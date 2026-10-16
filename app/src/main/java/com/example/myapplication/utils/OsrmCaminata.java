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
 * Ruta real caminando (perfil "foot" de OSRM / OpenStreetMap).
 * Devuelve la geometria de las calles y carreras que debe recorrer el
 * estudiante para llegar al paradero mas cercano, no una linea recta.
 *
 * Sin API key. Si falla devuelve lista vacia y distancia -1.
 */
public final class OsrmCaminata {

    private OsrmCaminata() {}

    /** Resultado: metros a caminar + los puntos de la calle que los recorre */
    public static class Resultado {
        public double metros = -1;
        public List<GeoPoint> puntos = new ArrayList<>();
    }

    public interface Callback {
        void onRuta(Resultado r);
    }

    /**
     * Calcula como caminar de origen a destino siguiendo la calle.
     * @param origen  punto de partida (tu ubicacion)
     * @param destino punto de llegada (el paradero)
     */
    public static void rutaCaminando(GeoPoint origen, GeoPoint destino, Callback cb) {
        new Thread(() -> {
            Resultado res = new Resultado();
            try {
                consultar(origen, destino, res);
            } catch (Exception ignored) {
                res.metros = -1;
                res.puntos = new ArrayList<>();
            }
            final Resultado resultado = res;
            new Handler(Looper.getMainLooper()).post(() -> cb.onRuta(resultado));
        }).start();
    }

    private static void consultar(GeoPoint origen, GeoPoint destino, Resultado res)
            throws Exception {
        // OSRM espera longitud primero. Perfil "foot" = solo catalyzed.
        String url = "https://router.project-osrm.org/route/v1/foot/"
                + origen.getLongitude() + "," + origen.getLatitude() + ";"
                + destino.getLongitude() + "," + destino.getLatitude()
                + "?overview=full&geometries=geojson&alternatives=false&steps=false";

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(7000);
        conn.setReadTimeout(7000);
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
        if (!"Ok".equals(root.optString("code"))) return;

        JSONArray routes = root.optJSONArray("routes");
        if (routes == null || routes.length() == 0) return;

        JSONObject route = routes.getJSONObject(0);
        double d = route.optDouble("distance", -1);
        res.metros = d > 0 ? d : -1;

        JSONObject geometry = route.optJSONObject("geometry");
        if (geometry == null) return;
        JSONArray coords = geometry.optJSONArray("coordinates");
        if (coords == null) return;

        List<GeoPoint> pts = new ArrayList<>();
        for (int i = 0; i < coords.length(); i++) {
            JSONArray c = coords.getJSONArray(i);
            pts.add(new GeoPoint(c.getDouble(1), c.getDouble(0)));
        }
        res.puntos = pts;
    }
}