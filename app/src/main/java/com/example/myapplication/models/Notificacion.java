package com.example.myapplication.models;

public class Notificacion {

    private String id;
    private String receptorId;
    private String creadaPor;
    private String origenNombre;
    private String tipo;
    private String titulo;
    private String cuerpo;
    private String conversacionId;
    private boolean leida;
    private long fecha;

    public Notificacion() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReceptorId() {
        return receptorId;
    }

    public void setReceptorId(String receptorId) {
        this.receptorId = receptorId;
    }

    public String getCreadaPor() {
        return creadaPor;
    }

    public void setCreadaPor(String creadaPor) {
        this.creadaPor = creadaPor;
    }

    public String getOrigenNombre() {
        return origenNombre;
    }

    public void setOrigenNombre(String origenNombre) {
        this.origenNombre = origenNombre;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getCuerpo() {
        return cuerpo;
    }

    public void setCuerpo(String cuerpo) {
        this.cuerpo = cuerpo;
    }

    public String getConversacionId() {
        return conversacionId;
    }

    public void setConversacionId(String conversacionId) {
        this.conversacionId = conversacionId;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
    }

    public long getFecha() {
        return fecha;
    }

    public void setFecha(long fecha) {
        this.fecha = fecha;
    }
}