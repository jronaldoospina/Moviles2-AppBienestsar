package com.example.myapplication.models;

public class Tutoria {

    private String id;
    private String docenteId;
    private String docenteNombre;
    private String cursoId;
    private String cursoNombre;
    private String codigoCurso;
    private String materia;
    private String diaSemana;       // "LUNES", "MARTES", ...
    private String horaInicio;      // "10:00"
    private String horaFin;         // "11:00"
    private String aula;            // opcional
    private boolean activa;
    private long fechaCreacion;
    private long ultimaActualizacion;

    public Tutoria() {}

    public Tutoria(String docenteId, String docenteNombre, String cursoId,
                   String cursoNombre, String materia, String diaSemana,
                   String horaInicio, String horaFin, String aula) {
        this.docenteId = docenteId;
        this.docenteNombre = docenteNombre;
        this.cursoId = cursoId;
        this.cursoNombre = cursoNombre;
        this.materia = materia;
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.aula = aula;
        this.activa = true;
        this.fechaCreacion = System.currentTimeMillis();
        this.ultimaActualizacion = System.currentTimeMillis();
    }

    // Getters y setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDocenteId() { return docenteId; }
    public void setDocenteId(String docenteId) { this.docenteId = docenteId; }

    public String getDocenteNombre() { return docenteNombre; }
    public void setDocenteNombre(String docenteNombre) { this.docenteNombre = docenteNombre; }

    public String getCursoId() { return cursoId; }
    public void setCursoId(String cursoId) { this.cursoId = cursoId; }

    public String getCursoNombre() { return cursoNombre; }
    public void setCursoNombre(String cursoNombre) { this.cursoNombre = cursoNombre; }

    public String getCodigoCurso() { return codigoCurso; }
    public void setCodigoCurso(String codigoCurso) { this.codigoCurso = codigoCurso; }

    public String getMateria() { return materia; }
    public void setMateria(String materia) { this.materia = materia; }

    public String getDiaSemana() { return diaSemana; }
    public void setDiaSemana(String diaSemana) { this.diaSemana = diaSemana; }

    public String getHoraInicio() { return horaInicio; }
    public void setHoraInicio(String horaInicio) { this.horaInicio = horaInicio; }

    public String getHoraFin() { return horaFin; }
    public void setHoraFin(String horaFin) { this.horaFin = horaFin; }

    public String getAula() { return aula; }
    public void setAula(String aula) { this.aula = aula; }

    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }

    public long getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(long fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public long getUltimaActualizacion() { return ultimaActualizacion; }
    public void setUltimaActualizacion(long ultimaActualizacion) { this.ultimaActualizacion = ultimaActualizacion; }
}