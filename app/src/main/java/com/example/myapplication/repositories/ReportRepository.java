package com.example.myapplication.repositories;

import com.example.myapplication.models.Reporte;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Map;

public class ReportRepository {

    private static final String COLLECTION = "reportes";
    private final FirebaseFirestore db;

    public ReportRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    private CollectionReference getCollection() {
        return db.collection(COLLECTION);
    }

    public Task<DocumentReference> createReporte(Reporte reporte) {
        return getCollection().add(reporte);
    }

    public Task<DocumentReference> crearReporte(Reporte reporte) {
        return createReporte(reporte);
    }

    /** Reportes de un estudiante, sin orden server-side (se ordena en cliente, sin índice compuesto). */
    public Task<QuerySnapshot> getReportesByEstudianteSinOrden(String estudianteId) {
        return getCollection()
                .whereEqualTo("estudianteId", estudianteId)
                .get();
    }

    /** Todos los reportes (para Admin) */
    public Task<QuerySnapshot> getAllReportes() {
        return getCollection().get();
    }

    public Task<Void> updateReporte(String reporteId, Map<String, Object> updates) {
        updates.put("ultimaActualizacion", System.currentTimeMillis());
        return getCollection().document(reporteId).update(updates);
    }

    public Task<Void> asignarPsicologo(String reporteId, String psicologoId) {
        return getCollection().document(reporteId).update(
                "psicologoAsignado", psicologoId,
                "estado", "EN_SEGUIMIENTO",
                "ultimaActualizacion", System.currentTimeMillis()
        );
    }

    public Task<Void> deleteReporte(String reporteId) {
        return getCollection().document(reporteId).delete();
    }

    /** Agrega un avance del estudiante al plan de acción. */
    public Task<Void> agregarAvance(String reporteId, Map<String, Object> avance) {
        return getCollection().document(reporteId).update(
                "avances", FieldValue.arrayUnion(avance),
                "ultimaActualizacion", System.currentTimeMillis()
        );
    }

    /** Marca el plan de acción como cumplido por el estudiante. */
    public Task<Void> marcarPlanCumplido(String reporteId) {
        return getCollection().document(reporteId).update(
                "planCumplido", true,
                "estado", "ATENDIDO",
                "ultimaActualizacion", System.currentTimeMillis()
        );
    }
}