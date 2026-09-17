package com.example.myapplication.models;

public class PacienteResumen {

    private String uid;
    private String nombre;
    private String nivelRiesgo;
    private int animo;
    private int alertasActivas;

    public PacienteResumen() {}

    public PacienteResumen(String uid, String nombre, String nivelRiesgo, int alertasActivas) {
        this.uid = uid;
        this.nombre = nombre;
        this.nivelRiesgo = nivelRiesgo;
        this.alertasActivas = alertasActivas;
        this.animo = 0;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getNivelRiesgo() { return nivelRiesgo; }
    public void setNivelRiesgo(String nivelRiesgo) { this.nivelRiesgo = nivelRiesgo; }

    public int getAnimo() { return animo; }
    public void setAnimo(int animo) { this.animo = animo; }

    public int getAlertasActivas() { return alertasActivas; }
    public void setAlertasActivas(int alertasActivas) { this.alertasActivas = alertasActivas; }

    public String getIniciales() {
        if (nombre == null || nombre.trim().isEmpty()) return "?";
        String[] partes = nombre.trim().split("\\s+");
        if (partes.length == 1) {
            return partes[0].substring(0, 1).toUpperCase();
        }
        return (partes[0].substring(0, 1) + partes[1].substring(0, 1)).toUpperCase();
    }
}