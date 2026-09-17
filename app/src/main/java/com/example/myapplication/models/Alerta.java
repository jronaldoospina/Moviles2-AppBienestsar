package com.example.myapplication.models;

import android.text.format.DateFormat;

public class Alerta {

    private String id;
    private String docenteId;
    private String docenteNombre;
    private String estudianteId;
    private String estudianteNombre;
    private String cursoId;
    private String cursoNombre;
    private String motivo;         // "BAJO_RENDIMIENTO", "INASISTENCIA", "COMPORTAMIENTO", "OTRO"
    private String descripcion;
    private String estado;         // "PENDIENTE", "REVISADA", "ATENDIDA"
    private String atendidaPor;    // uid del psicólogo
    private long fecha;
    private long fechaAtencion;

    public Alerta() {}

    public Alerta(String docenteId, String docenteNombre, String estudianteId,
                  String estudianteNombre, String cursoId, String cursoNombre,
                  String motivo, String descripcion) {
        this.docenteId = docenteId;
        this.docenteNombre = docenteNombre;
        this.estudianteId = estudianteId;
        this.estudianteNombre = estudianteNombre;
        this.cursoId = cursoId;
        this.cursoNombre = cursoNombre;
        this.motivo = motivo;
        this.descripcion = descripcion;
        this.estado = "PENDIENTE";
        this.fecha = System.currentTimeMillis();
    }

    // Getters y setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDocenteId() { return docenteId; }
    public void setDocenteId(String docenteId) { this.docenteId = docenteId; }

    public String getDocenteNombre() { return docenteNombre; }
    public void setDocenteNombre(String docenteNombre) { this.docenteNombre = docenteNombre; }

    public String getEstudianteId() { return estudianteId; }
    public void setEstudianteId(String estudianteId) { this.estudianteId = estudianteId; }

    public String getEstudianteNombre() { return estudianteNombre; }
    public void setEstudianteNombre(String estudianteNombre) { this.estudianteNombre = estudianteNombre; }

    public String getCursoId() { return cursoId; }
    public void setCursoId(String cursoId) { this.cursoId = cursoId; }

    public String getCursoNombre() { return cursoNombre; }
    public void setCursoNombre(String cursoNombre) { this.cursoNombre = cursoNombre; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getMotivoLabel() {
        if (motivo == null) return "";
        switch (motivo) {
            case "INASISTENCIA":   return "Inasistencia";
            case "COMPORTAMIENTO": return "Comportamiento";
            case "OTRO":           return "Otro";
            default:               return "Bajo rendimiento";
        }
    }

    public String getTitulo() {
        return "Alerta: " + (cursoNombre != null ? cursoNombre : "General");
    }

    public String getFechaStr() {
        return DateFormat.format("dd/MM/yyyy HH:mm", fecha).toString();
    }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getAtendidaPor() { return atendidaPor; }
    public void setAtendidaPor(String atendidaPor) { this.atendidaPor = atendidaPor; }

    public long getFecha() { return fecha; }
    public void setFecha(long fecha) { this.fecha = fecha; }

    public long getFechaAtencion() { return fechaAtencion; }
    public void setFechaAtencion(long fechaAtencion) { this.fechaAtencion = fechaAtencion; }
}