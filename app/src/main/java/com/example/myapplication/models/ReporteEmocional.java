package com.example.myapplication.models;

public class ReporteEmocional {

    private String id;
    private String estudianteId;
    private int animo;
    private String descripcion;
    private String fechaDia;
    private long fecha;

    public ReporteEmocional() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEstudianteId() {
        return estudianteId;
    }

    public void setEstudianteId(String estudianteId) {
        this.estudianteId = estudianteId;
    }

    public int getAnimo() {
        return animo;
    }

    public void setAnimo(int animo) {
        this.animo = animo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getFechaDia() {
        return fechaDia;
    }

    public void setFechaDia(String fechaDia) {
        this.fechaDia = fechaDia;
    }

    public long getFecha() {
        return fecha;
    }

    public void setFecha(long fecha) {
        this.fecha = fecha;
    }
}