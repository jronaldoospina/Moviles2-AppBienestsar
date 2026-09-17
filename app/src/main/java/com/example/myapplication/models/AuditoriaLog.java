package com.example.myapplication.models;

public class AuditoriaLog {

    private String id;
    private String usuarioId;
    private String usuarioNombre;
    private String rol;
    private String tipo;      // "LOGIN", "USUARIO_CREADO", "ALERTA_CREADA", ...
    private String detalle;
    private long fecha;

    public AuditoriaLog() {}

    public AuditoriaLog(String usuarioId, String usuarioNombre, String rol,
                        String tipo, String detalle) {
        this.usuarioId = usuarioId;
        this.usuarioNombre = usuarioNombre;
        this.rol = rol;
        this.tipo = tipo;
        this.detalle = detalle;
        this.fecha = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }

    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String usuarioNombre) { this.usuarioNombre = usuarioNombre; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }

    public long getFecha() { return fecha; }
    public void setFecha(long fecha) { this.fecha = fecha; }
}