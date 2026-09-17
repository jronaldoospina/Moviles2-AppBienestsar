package com.example.myapplication.models;

import java.util.List;
import java.util.Map;

public class Conversacion {

    private String id;
    private String par;
    private List<String> participantes;
    private String estudianteId;
    private String estudianteNombre;
    private String psicologoId;
    private String psicologoNombre;
    private String ultimoMensaje;
    private long ultimaActualizacion;
    private Map<String, Long> pendientes;

    public Conversacion() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPar() {
        return par;
    }

    public void setPar(String par) {
        this.par = par;
    }

    public List<String> getParticipantes() {
        return participantes;
    }

    public void setParticipantes(List<String> participantes) {
        this.participantes = participantes;
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

    public String getPsicologoId() {
        return psicologoId;
    }

    public void setPsicologoId(String psicologoId) {
        this.psicologoId = psicologoId;
    }

    public String getPsicologoNombre() {
        return psicologoNombre;
    }

    public void setPsicologoNombre(String psicologoNombre) {
        this.psicologoNombre = psicologoNombre;
    }

    public String getUltimoMensaje() {
        return ultimoMensaje;
    }

    public void setUltimoMensaje(String ultimoMensaje) {
        this.ultimoMensaje = ultimoMensaje;
    }

    public long getUltimaActualizacion() {
        return ultimaActualizacion;
    }

    public void setUltimaActualizacion(long ultimaActualizacion) {
        this.ultimaActualizacion = ultimaActualizacion;
    }

    public Map<String, Long> getPendientes() {
        return pendientes;
    }

    public void setPendientes(Map<String, Long> pendientes) {
        this.pendientes = pendientes;
    }
}