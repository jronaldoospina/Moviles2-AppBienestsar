package com.example.myapplication.models;

/**
 * Paradero de la Ruta UPC Aguachica.
 * Fuente: grupo "Ruta UPC Aguachica" (WhatsApp).
 * Coordenadas aproximadas sobre Aguachica, terminando en la sede
 * Cra 40 Vía al mar (8.3149249, -73.5963296 - OSM).
 * Si Bienestar ajusta un punto, solo se cambia lat/lng aquí o en Firestore.
 */
public class RutaParada {
    private int orden;
    private String nombre;
    private String referencia;
    private double lat;
    private double lng;

    public RutaParada() {}

    public RutaParada(int orden, String nombre, String referencia, double lat, double lng) {
        this.orden = orden;
        this.nombre = nombre;
        this.referencia = referencia;
        this.lat = lat;
        this.lng = lng;
    }

    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }
}
