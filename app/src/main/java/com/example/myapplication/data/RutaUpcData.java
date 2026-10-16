package com.example.myapplication.data;

import com.example.myapplication.models.RutaParada;

import java.util.ArrayList;
import java.util.List;

/**
 * Ruta oficial bus UPC Seccional Aguachica.
 *
 * Horarios (grupo Ruta UPC Aguachica):
 *  - Recogida (Hora Inicio): 05:00 A.M. / 01:00 P.M.
 *  - Salida desde UPC (Hora Salida): 12:00 M. / 06:00 P.M.
 *
 * Orden de recogida: 1 -> 9 -> UPC.
 */
public class RutaUpcData {

    public static final String HORARIO_RECOGIDA = "Recogida: 05:00 A.M. / 01:00 P.M.";
    public static final String HORARIO_SALIDA = "Salida UPC: 12:00 M. / 06:00 P.M.";

    // Sede UPC Aguachica - Cra 40 Vía al mar (verificado en OSM/Nominatim)
    public static final double UPC_LAT = 8.3149249;
    public static final double UPC_LNG = -73.5963296;

    // Centro aproximado de Aguachica para centrar el mapa
    public static final double CENTER_LAT = 8.3095;
    public static final double CENTER_LNG = -73.6050;

    public static List<RutaParada> getParadas() {
        List<RutaParada> list = new ArrayList<>();
        // Coordenadas aproximadas siguiendo la trama urbana SO -> NE hacia la UPC.
        // El orden es el del grupo de WhatsApp.
        list.add(new RutaParada(1, "Parque Nuevo Amanecer", "Punto 1 - Inicio", 8.3005, -73.6150));
        list.add(new RutaParada(2, "Patinódromo", "Punto 2", 8.3035, -73.6112));
        list.add(new RutaParada(3, "Calle 16 Cra 14", "Semáforo Bavaria", 8.3060, -73.6085));
        list.add(new RutaParada(4, "Calle 11 con Cra 14", "Tienda D1", 8.3076, -73.6062));
        // La parada 5 estaba en 8.3091, -73.6042, que cae DENTRO del
        // poligono del Hospital Regional. OSRM tenia que subir por el camino
        // interno del hospital (274 m sin nombre) para llegar a ella.
        //
        // Segundo ajuste: Calle 5 es una via dividida y 8.30980 quedaba en
        // el carril equivocado (el que va hacia el oeste).alli el bus
        // llegaba a la parada y tenia que retroceder por la otra via:
        // 304 m de rodeo con forma de "diente". El carril por el que el bus
        // pasa de largo hacia la UPC es la franja lat 8.30962-8.30972; se
        // toma el centro para dejar margen a los dos bordes de la calzada.
        list.add(new RutaParada(5, "Cra 14 con Calle 7", "Semáforo", 8.30967, -73.60460));
        list.add(new RutaParada(6, "Calle 8 Cra 24", "Punto 6", 8.3106, -73.6021));
        list.add(new RutaParada(7, "Calle 10 Cra 27A", "Punto 7", 8.3121, -73.6002));
        list.add(new RutaParada(8, "Calle 10 Cra 32", "Punto 8", 8.3134, -73.5986));
        list.add(new RutaParada(9, "Cancha barrio San Martín", "Punto 9 - último antes de UPC", 8.3141, -73.5974));
        return list;
    }
}
