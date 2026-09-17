package com.example.myapplication.models;

public class Cita {

    private String id;
    private String estudianteId;
    private String estudianteNombre;
    private String psicologoId;
    private String psicologoNombre;
    private String fecha;           // "2026-03-15"
    private String hora;            // "10:00"
    private int duracionMinutos;    // 30, 45, 60
    private String motivo;
    private String estado;          // "PENDIENTE", "CONFIRMADA", "ATENDIDA", "CANCELADA"
    private String notasPsicologo;
    private long fechaCreacion;
    private long ultimaActualizacion;

    public Cita() {}

    public Cita(String estudianteId, String estudianteNombre, String psicologoId,
                String psicologoNombre, String fecha, String hora,
                int duracionMinutos, String motivo) {
        this.estudianteId = estudianteId;
        this.estudianteNombre = estudianteNombre;
        this.psicologoId = psicologoId;
        this.psicologoNombre = psicologoNombre;
        this.fecha = fecha;
        this.hora = hora;
        this.duracionMinutos = duracionMinutos;
        this.motivo = motivo;
        this.estado = "PENDIENTE";
        this.fechaCreacion = System.currentTimeMillis();
        this.ultimaActualizacion = System.currentTimeMillis();
    }

    // Getters y setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEstudianteId() { return estudianteId; }
    public void setEstudianteId(String estudianteId) { this.estudianteId = estudianteId; }

    public String getEstudianteNombre() { return estudianteNombre; }
    public void setEstudianteNombre(String estudianteNombre) { this.estudianteNombre = estudianteNombre; }

    public String getPsicologoId() { return psicologoId; }
    public void setPsicologoId(String psicologoId) { this.psicologoId = psicologoId; }

    public String getPsicologoNombre() { return psicologoNombre; }
    public void setPsicologoNombre(String psicologoNombre) { this.psicologoNombre = psicologoNombre; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }

    public int getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(int duracionMinutos) { this.duracionMinutos = duracionMinutos; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getNotasPsicologo() { return notasPsicologo; }
    public void setNotasPsicologo(String notasPsicologo) { this.notasPsicologo = notasPsicologo; }

    public long getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(long fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public long getUltimaActualizacion() { return ultimaActualizacion; }
    public void setUltimaActualizacion(long ultimaActualizacion) { this.ultimaActualizacion = ultimaActualizacion; }
}