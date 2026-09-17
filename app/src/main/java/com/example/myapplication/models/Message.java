package com.example.myapplication.models;

public class Message {

    private String id;
    private String remitenteId;      // UID quien envió
    private String receptorId;       // UID quien recibió
    private String mensaje;          // Texto del mensaje
    private String tipo;             // "CHAT", "NOTA", "ALERTA"
    private long fecha;              // Timestamp en milisegundos
    private boolean leido;           // Si fue leído por el receptor
    private String nombreRemitente;  // Nombre del remitente (para mostrar)
    private String nombreReceptor;   // Nombre del receptor (para mostrar)

    // Constructor vacío para Firestore
    public Message() {}

    // Constructor completo
    public Message(String remitenteId, String receptorId, String mensaje,
                   String tipo, long fecha, boolean leido,
                   String nombreRemitente, String nombreReceptor) {
        this.remitenteId = remitenteId;
        this.receptorId = receptorId;
        this.mensaje = mensaje;
        this.tipo = tipo;
        this.fecha = fecha;
        this.leido = leido;
        this.nombreRemitente = nombreRemitente;
        this.nombreReceptor = nombreReceptor;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRemitenteId() { return remitenteId; }
    public void setRemitenteId(String remitenteId) { this.remitenteId = remitenteId; }

    public String getReceptorId() { return receptorId; }
    public void setReceptorId(String receptorId) { this.receptorId = receptorId; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public long getFecha() { return fecha; }
    public void setFecha(long fecha) { this.fecha = fecha; }

    public boolean isLeido() { return leido; }
    public void setLeido(boolean leido) { this.leido = leido; }

    public String getNombreRemitente() { return nombreRemitente; }
    public void setNombreRemitente(String nombreRemitente) { this.nombreRemitente = nombreRemitente; }

    public String getNombreReceptor() { return nombreReceptor; }
    public void setNombreReceptor(String nombreReceptor) { this.nombreReceptor = nombreReceptor; }
}