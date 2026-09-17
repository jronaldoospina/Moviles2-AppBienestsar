package com.example.myapplication.models;

import java.util.ArrayList;
import java.util.List;

public class Curso {

    private String id;
    private String nombre;
    private String codigo;            // Código del curso (ej: IS-901)
    private String programa;          // Programa al que pertenece
    private int semestre;
    private int creditos;
    private String periodo;           // Ej: "2025-1"

    // Docente asignado
    private String docenteId;         // uid del docente
    private String docenteNombre;     // nombre (para mostrar sin consultar)

    // Estudiantes inscritos (lista de uids)
    private List<String> estudiantesIds;

    // Cortes (ej: "Corte 1", "Corte 2", "Corte 3")
    private List<Corte> cortes;

    // Fechas
    private long fechaCreacion;
    private long ultimaActualizacion;

    public Curso() {
        this.estudiantesIds = new ArrayList<>();
        this.cortes = new ArrayList<>();
    }

    public Curso(String nombre, String codigo, String docenteId, String docenteNombre) {
        this.nombre = nombre;
        this.codigo = codigo;
        this.docenteId = docenteId;
        this.docenteNombre = docenteNombre;
        this.estudiantesIds = new ArrayList<>();
        this.cortes = new ArrayList<>();
        this.fechaCreacion = System.currentTimeMillis();
        this.ultimaActualizacion = System.currentTimeMillis();
    }

    // Getters y setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getPrograma() { return programa; }
    public void setPrograma(String programa) { this.programa = programa; }

    public int getSemestre() { return semestre; }
    public void setSemestre(int semestre) { this.semestre = semestre; }

    public int getCreditos() { return creditos; }
    public void setCreditos(int creditos) { this.creditos = creditos; }

    public String getPeriodo() { return periodo; }
    public void setPeriodo(String periodo) { this.periodo = periodo; }

    public String getDocenteId() { return docenteId; }
    public void setDocenteId(String docenteId) { this.docenteId = docenteId; }

    public String getDocenteNombre() { return docenteNombre; }
    public void setDocenteNombre(String docenteNombre) { this.docenteNombre = docenteNombre; }

    public List<String> getEstudiantesIds() { return estudiantesIds; }
    public void setEstudiantesIds(List<String> estudiantesIds) { this.estudiantesIds = estudiantesIds; }

    public List<Corte> getCortes() { return cortes; }
    public void setCortes(List<Corte> cortes) { this.cortes = cortes; }

    public long getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(long fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public long getUltimaActualizacion() { return ultimaActualizacion; }
    public void setUltimaActualizacion(long ultimaActualizacion) { this.ultimaActualizacion = ultimaActualizacion; }

    // Métodos útiles
    public int getTotalEstudiantes() {
        return estudiantesIds != null ? estudiantesIds.size() : 0;
    }

    public int getTotalCortes() {
        return cortes != null ? cortes.size() : 0;
    }

    // Clase interna Corte
    public static class Corte {
        private String id;           // "corte1", "corte2", "corte3"
        private String nombre;       // "Corte 1", "Corte 2", "Corte 3"
        private double porcentaje;   // 0.33, 0.33, 0.34
        private boolean activo;      // Si ya se puede registrar notas
        private boolean cerrado;     // Si ya se cerró
        private long fechaApertura;
        private long fechaCierre;

        public Corte() {}

        public Corte(String id, String nombre, double porcentaje) {
            this.id = id;
            this.nombre = nombre;
            this.porcentaje = porcentaje;
            this.activo = false;
            this.cerrado = false;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }

        public double getPorcentaje() { return porcentaje; }
        public void setPorcentaje(double porcentaje) { this.porcentaje = porcentaje; }

        public boolean isActivo() { return activo; }
        public void setActivo(boolean activo) { this.activo = activo; }

        public boolean isCerrado() { return cerrado; }
        public void setCerrado(boolean cerrado) { this.cerrado = cerrado; }

        public long getFechaApertura() { return fechaApertura; }
        public void setFechaApertura(long fechaApertura) { this.fechaApertura = fechaApertura; }

        public long getFechaCierre() { return fechaCierre; }
        public void setFechaCierre(long fechaCierre) { this.fechaCierre = fechaCierre; }
    }
}