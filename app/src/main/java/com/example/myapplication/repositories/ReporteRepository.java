package com.example.myapplication.repositories;

import com.example.myapplication.models.Reporte;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ReporteRepository {

    private static final String COLLECTION = "reportes";
    private final FirebaseFirestore db;

    public ReporteRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    private com.google.firebase.firestore.CollectionReference getCollection() {
        return db.collection(COLLECTION);
    }

    /** Crear un nuevo reporte (estudiante reporting problemática) */
    public Task<DocumentReference> crearReporte(Reporte reporte) {
        // Establecer valores por defecto
        if (reporte.getFecha() == 0) {
            reporte.setFecha(System.currentTimeMillis());
        }
        if (reporte.getUltimaActualizacion() == 0) {
            reporte.setUltimaActualizacion(System.currentTimeMillis());
        }
        if (reporte.getEstado() == null || reporte.getEstado().isEmpty()) {
            reporte.setEstado("PENDIENTE");
        }
        if (!reporte.isPlanCumplido()) {
            reporte.setPlanCumplido(false);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("estudianteId", reporte.getEstudianteId());
        data.put("estudianteNombre", reporte.getEstudianteNombre());
        data.put("psicologoAsignado", reporte.getPsicologoAsignado());
        data.put("psicologoNombre", reporte.getPsicologoNombre());
        data.put("titulo", reporte.getTitulo());
        data.put("tipo", reporte.getTipo());
        data.put("descripcion", reporte.getDescripcion());
        data.put("nivelRiesgo", reporte.getNivelRiesgo());
        data.put("estado", reporte.getEstado());
        data.put("fecha", reporte.getFecha());
        data.put("ultimaActualizacion", reporte.getUltimaActualizacion());
        data.put("planCumplido", reporte.isPlanCumplido());
        if (reporte.getAvances() != null) {
            data.put("avances", reporte.getAvances());
        }

        return getCollection().add(data);
    }

    /** Obtener reports por estudiante */
    public Task< com.google.firebase.firestore.QuerySnapshot> getReportesByEstudiante(String estudianteId) {
        return getCollection()
                .whereEqualTo("estudianteId", estudianteId)
                .orderBy("fecha", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get();
    }

    /** Obtener reports pendientes (sin plan cumplido) */
    public Task< com.google.firebase.firestore.QuerySnapshot> getReportesPendientes(String estudianteId) {
        return getCollection()
                .whereEqualTo("estudianteId", estudianteId)
                .whereEqualTo("planCumplido", false)
                .orderBy("fecha", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get();
    }

    /** Actualizar estado de un reporte */
    public Task<Void> actualizarReporte(String reporteId, Map<String, Object> updates) {
        return getCollection().document(reporteId).update(updates);
    }

    /** Actualizar plan cumplido */
    public Task<Void> marcarPlanCumplido(String reporteId) {
        return getCollection().document(reporteId).update(
                "planCumplido", true,
                "ultimaActualizacion", System.currentTimeMillis()
        );
    }
}