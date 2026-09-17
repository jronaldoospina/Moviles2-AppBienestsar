package com.example.myapplication.models;

import java.util.List;
import java.util.Map;

public class Reporte {

    private String id;
    private String estudianteId;
    private String estudianteNombre;
    private String psicologoAsignado;
    private String psicologoNombre;
    private String titulo;
    private String tipo;
    private String descripcion;
    private String nivelRiesgo;
    private String estado;
    private long fecha;
    private long ultimaActualizacion;
    private List<Map<String, Object>> avances;
    private boolean planCumplido;

    public Reporte() {
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

    public String getEstudianteNombre() {
        return estudianteNombre;
    }

    public void setEstudianteNombre(String estudianteNombre) {
        this.estudianteNombre = estudianteNombre;
    }

    public String getPsicologoAsignado() {
        return psicologoAsignado;
    }

    public void setPsicologoAsignado(String psicologoAsignado) {
        this.psicologoAsignado = psicologoAsignado;
    }

    public String getPsicologoNombre() {
        return psicologoNombre;
    }

    public void setPsicologoNombre(String psicologoNombre) {
        this.psicologoNombre = psicologoNombre;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getNivelRiesgo() {
        return nivelRiesgo;
    }

    public void setNivelRiesgo(String nivelRiesgo) {
        this.nivelRiesgo = nivelRiesgo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public long getFecha() {
        return fecha;
    }

    public void setFecha(long fecha) {
        this.fecha = fecha;
    }

    public long getUltimaActualizacion() {
        return ultimaActualizacion;
    }

    public void setUltimaActualizacion(long ultimaActualizacion) {
        this.ultimaActualizacion = ultimaActualizacion;
    }

    public List<Map<String, Object>> getAvances() {
        return avances;
    }

    public void setAvances(List<Map<String, Object>> avances) {
        this.avances = avances;
    }

    public boolean isPlanCumplido() {
        return planCumplido;
    }

    public void setPlanCumplido(boolean planCumplido) {
        this.planCumplido = planCumplido;
    }
}