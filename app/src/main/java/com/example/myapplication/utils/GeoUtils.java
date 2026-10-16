package com.example.myapplication.utils;

import org.osmdroid.util.GeoPoint;

import java.util.List;

/** Utilidades geometricas para ajustar puntos a una polilinea. */
public final class GeoUtils {

    private GeoUtils() {}

    /**
     * Devuelve el punto de la polilinea mas cercano a {@code p}.
     *
     * Sirve para el caso sin conexion: la ruta se dibuja en linea recta y los
     * marcadores deben caer sobre esa misma linea, no sobre la coordenada
     * original de la parada.
     *
     * @return el punto mas cercano, o {@code p} si la polilinea esta vacia.
     */
    public static GeoPoint masCercaEnPolilinea(GeoPoint p, List<GeoPoint> linea) {
        if (p == null || linea == null || linea.isEmpty()) return p;

        GeoPoint mejor = linea.get(0);
        double mejorDist = Float.MAX_VALUE;
        for (GeoPoint q : linea) {
            float[] out = new float[1];
            android.location.Location.distanceBetween(
                    p.getLatitude(), p.getLongitude(),
                    q.getLatitude(), q.getLongitude(), out);
            if (out[0] < mejorDist) {
                mejorDist = out[0];
                mejor = q;
            }
        }
        return mejor;
    }

    /**
     * Ajusta cada parada al punto mas cercano de la geometria, respetando el
     * orden. Si las cantidades no coinciden devuelve la lista original.
     */
    public static java.util.List<GeoPoint> ajustarParadas(
            List<GeoPoint> paradas, List<GeoPoint> linea) {
        if (paradas == null || linea == null
                || paradas.isEmpty() || linea.isEmpty()) {
            return paradas;
        }
        java.util.List<GeoPoint> out = new java.util.ArrayList<>(paradas.size());
        for (GeoPoint p : paradas) {
            out.add(masCercaEnPolilinea(p, linea));
        }
        return out;
    }
}
