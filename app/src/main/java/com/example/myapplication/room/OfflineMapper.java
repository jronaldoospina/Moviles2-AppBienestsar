package com.example.myapplication.room;

import com.example.myapplication.models.Cita;

import java.util.ArrayList;
import java.util.List;

/**
 * Puente entre el modelo de la app (Firestore) y la entidad local (Room).
 * Permite cachear citas para verlas sin conexion en zonas rurales.
 */
public final class OfflineMapper {

    private OfflineMapper() {}

    public static CitaEntity toEntity(Cita c, String id) {
        CitaEntity e = new CitaEntity();
        e.id = id != null ? id : (c.getId() != null ? c.getId() : "");
        e.estudianteId = c.getEstudianteId();
        e.estudianteNombre = c.getEstudianteNombre();
        e.psicologoId = c.getPsicologoId();
        e.psicologoNombre = c.getPsicologoNombre();
        e.fecha = c.getFecha();
        e.hora = c.getHora();
        e.duracionMinutos = c.getDuracionMinutos();
        e.motivo = c.getMotivo();
        e.estado = c.getEstado();
        e.notasPsicologo = c.getNotasPsicologo();
        e.lugar = c.getLugar();
        e.fechaCreacion = c.getFechaCreacion();
        e.ultimaActualizacion = c.getUltimaActualizacion();
        return e;
    }

    public static Cita toModel(CitaEntity e) {
        Cita c = new Cita();
        c.setId(e.id);
        c.setEstudianteId(e.estudianteId);
        c.setEstudianteNombre(e.estudianteNombre);
        c.setPsicologoId(e.psicologoId);
        c.setPsicologoNombre(e.psicologoNombre);
        c.setFecha(e.fecha);
        c.setHora(e.hora);
        c.setDuracionMinutos(e.duracionMinutos);
        c.setMotivo(e.motivo);
        c.setEstado(e.estado);
        c.setNotasPsicologo(e.notasPsicologo);
        c.setLugar(e.lugar);
        c.setFechaCreacion(e.fechaCreacion);
        c.setUltimaActualizacion(e.ultimaActualizacion);
        return c;
    }

    public static List<Cita> toModels(List<CitaEntity> entities) {
        List<Cita> out = new ArrayList<>();
        if (entities == null) return out;
        for (CitaEntity e : entities) {
            if (e != null) out.add(toModel(e));
        }
        return out;
    }

    // ================= USUARIO =================

    public static UsuarioEntity toEntity(com.example.myapplication.models.Usuario u) {
        if (u == null) return null;
        UsuarioEntity e = new UsuarioEntity();
        e.uid = u.getUid();
        e.email = u.getEmail();
        e.usuario = u.getUsuario();
        e.nombre = u.getNombre();
        e.rol = u.getRol();
        e.documento = u.getDocumento();
        e.telefono = u.getTelefono();
        e.fotoUrl = u.getFotoUrl();
        e.fechaRegistro = u.getFechaRegistro();
        e.fechaCreacion = u.getFechaCreacion();
        e.ultimaActualizacion = u.getUltimaActualizacion();
        e.activo = u.isActivo();
        e.nivelRiesgo = u.getNivelRiesgo();
        e.promedio = u.getPromedio();
        e.codigo = u.getCodigo();
        e.programa = u.getPrograma();
        e.semestre = u.getSemestre();
        return e;
    }

    public static List<UsuarioEntity> toEntities(
            List<com.example.myapplication.models.Usuario> users) {
        List<UsuarioEntity> out = new ArrayList<>();
        if (users == null) return out;
        for (com.example.myapplication.models.Usuario u : users) {
            UsuarioEntity e = toEntity(u);
            if (e != null && e.uid != null) out.add(e);
        }
        return out;
    }

    // ============== REPORTE EMOCIONAL ==============

    public static ReporteEmocionalEntity toEntity(
            com.example.myapplication.models.ReporteEmocional r, String id) {
        if (r == null) return null;
        ReporteEmocionalEntity e = new ReporteEmocionalEntity();
        e.id = id != null ? id : (r.getId() != null ? r.getId() : "");
        e.estudianteId = r.getEstudianteId();
        e.animo = r.getAnimo();
        e.descripcion = r.getDescripcion();
        e.fechaDia = r.getFechaDia();
        e.fecha = r.getFecha();
        return e;
    }

    public static List<ReporteEmocionalEntity> toReporteEntities(
            List<com.example.myapplication.models.ReporteEmocional> reportes) {
        List<ReporteEmocionalEntity> out = new ArrayList<>();
        if (reportes == null) return out;
        for (com.example.myapplication.models.ReporteEmocional r : reportes) {
            ReporteEmocionalEntity e = toEntity(r, r.getId());
            if (e != null && !e.id.isEmpty()) out.add(e);
        }
        return out;
    }
}