package com.example.myapplication.models;

public class Nota {

    private String id;
    private String estudianteId;
    private String cursoId;
    private String corteId;        // "corte1", "corte2", "corte3"
    private double valor;          // 0.0 - 5.0
    private String observacion;
    private String registradoPor;  // uid del docente
    private long fechaRegistro;
    private long ultimaActualizacion;
    private String nivelRiesgo;

    public Nota() {}

    public Nota(String estudianteId, String cursoId, String corteId,
                double valor, String registradoPor) {
        this.estudianteId = estudianteId;
        this.cursoId = cursoId;
        this.corteId = corteId;
        this.valor = valor;
        this.registradoPor = registradoPor;
        this.fechaRegistro = System.currentTimeMillis();
        this.ultimaActualizacion = System.currentTimeMillis();
    }

    // Getters y setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEstudianteId() { return estudianteId; }
    public void setEstudianteId(String estudianteId) { this.estudianteId = estudianteId; }

    public String getCursoId() { return cursoId; }
    public void setCursoId(String cursoId) { this.cursoId = cursoId; }

    public String getCorteId() { return corteId; }
    public void setCorteId(String corteId) { this.corteId = corteId; }

    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public String getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(String registradoPor) { this.registradoPor = registradoPor; }

    public long getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(long fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public long getUltimaActualizacion() { return ultimaActualizacion; }
    public void setUltimaActualizacion(long ultimaActualizacion) { this.ultimaActualizacion = ultimaActualizacion; }

    public String getNivelRiesgo() { return nivelRiesgo; }
    public void setNivelRiesgo(String nivelRiesgo) { this.nivelRiesgo = nivelRiesgo; }
}