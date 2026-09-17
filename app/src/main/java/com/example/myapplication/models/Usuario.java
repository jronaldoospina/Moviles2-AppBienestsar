package com.example.myapplication.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de Usuario para Firestore.
 * Se usa para todos los roles: Admin, Psicólogo, Docente, Estudiante.
 */
public class Usuario {

    // ============================================
    // DATOS BÁSICOS (TODOS LOS ROLES)
    // ============================================
    private String uid;
    private String email;
    private String usuario;          // Parte antes del @
    private String documento;        // Número de cédula (único)
    private String nombre;
    private String rol;              // ADMIN | PSICOLOGO | DOCENTE | ESTUDIANTE
    private boolean activo;
    private String telefono;
    private String fotoUrl;
    private long fechaRegistro;

    // ============================================
    // CONTROL DE CONTRASEÑA
    // ============================================
    private boolean requiereCambioContrasena;
    private long ultimoCambioContrasena;

    // ============================================
    // SOLO DOCENTE
    // ============================================
    private List<String> cursos;

    // ============================================
    // SOLO ESTUDIANTE
    // ============================================
    private String codigo;           // Código estudiantil (diferente de cédula)
    private String programa;
    private int semestre;
    private double promedio;
    private String nivelRiesgo;      // CRITICO | ALTO | MEDIO | BAJO

    // ============================================
    // SOLO PSICOLOGO
    // ============================================
    private String especialidad;
    private List<String> estudiantesAsignados;

    // ============================================
    // AUDITORÍA
    // ============================================
    private String creadoPor;        // UID del que lo creó
    private long fechaCreacion;
    private long ultimaActualizacion;

    /**
     * Constructor vacío (REQUERIDO por Firestore)
     */
    public Usuario() {
        this.cursos = new ArrayList<>();
        this.estudiantesAsignados = new ArrayList<>();
        this.activo = true;
        this.requiereCambioContrasena = true;
    }

    /**
     * Constructor básico
     */
    public Usuario(String uid, String email, String usuario, String documento,
                   String nombre, String rol) {
        this();
        this.uid = uid;
        this.email = email;
        this.usuario = usuario;
        this.documento = documento;
        this.nombre = nombre;
        this.rol = rol;
        this.fechaRegistro = System.currentTimeMillis();
        this.fechaCreacion = System.currentTimeMillis();
        this.ultimaActualizacion = System.currentTimeMillis();
    }

    // ============================================
    // GETTERS Y SETTERS
    // ============================================

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public long getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(long fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public boolean isRequiereCambioContrasena() { return requiereCambioContrasena; }
    public void setRequiereCambioContrasena(boolean requiereCambioContrasena) {
        this.requiereCambioContrasena = requiereCambioContrasena;
    }

    public long getUltimoCambioContrasena() { return ultimoCambioContrasena; }
    public void setUltimoCambioContrasena(long ultimoCambioContrasena) {
        this.ultimoCambioContrasena = ultimoCambioContrasena;
    }

    public List<String> getCursos() { return cursos; }
    public void setCursos(List<String> cursos) { this.cursos = cursos; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getPrograma() { return programa; }
    public void setPrograma(String programa) { this.programa = programa; }

    public int getSemestre() { return semestre; }
    public void setSemestre(int semestre) { this.semestre = semestre; }

    public double getPromedio() { return promedio; }
    public void setPromedio(double promedio) { this.promedio = promedio; }

    public String getNivelRiesgo() { return nivelRiesgo; }
    public void setNivelRiesgo(String nivelRiesgo) { this.nivelRiesgo = nivelRiesgo; }

    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }

    public List<String> getEstudiantesAsignados() { return estudiantesAsignados; }
    public void setEstudiantesAsignados(List<String> estudiantesAsignados) {
        this.estudiantesAsignados = estudiantesAsignados;
    }

    public String getCreadoPor() { return creadoPor; }
    public void setCreadoPor(String creadoPor) { this.creadoPor = creadoPor; }

    public long getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(long fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public long getUltimaActualizacion() { return ultimaActualizacion; }
    public void setUltimaActualizacion(long ultimaActualizacion) {
        this.ultimaActualizacion = ultimaActualizacion;
    }

    // ============================================
    // MÉTODOS ÚTILES
    // ============================================

    public boolean esAdmin() { return "ADMIN".equals(rol); }
    public boolean esPsicologo() { return "PSICOLOGO".equals(rol); }
    public boolean esDocente() { return "DOCENTE".equals(rol); }
    public boolean esEstudiante() { return "ESTUDIANTE".equals(rol); }

    /**
     * Verifica si el rol es válido
     */
    public static boolean esRolValido(String rol) {
        return "ADMIN".equals(rol) || "PSICOLOGO".equals(rol) ||
                "DOCENTE".equals(rol) || "ESTUDIANTE".equals(rol);
    }

    /**
     * Obtiene las iniciales del nombre (para avatares)
     */
    public String getIniciales() {
        if (nombre == null || nombre.isEmpty()) return "??";
        String[] partes = nombre.trim().split(" ");
        if (partes.length == 1) {
            return partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        }
        return (partes[0].charAt(0) + "" + partes[partes.length - 1].charAt(0)).toUpperCase();
    }
}