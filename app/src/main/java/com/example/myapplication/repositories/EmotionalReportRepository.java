package com.example.myapplication.repositories;

import com.example.myapplication.models.ReporteEmocional;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Map;

public class EmotionalReportRepository {

    private static final String COLLECTION = "reportes_emocionales";
    private final FirebaseFirestore db;

    public EmotionalReportRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    private CollectionReference getCollection() {
        return db.collection(COLLECTION);
    }

    /** Reportes de un estudiante, sin orden server-side (se ordena en cliente). */
    public Task<QuerySnapshot> getByEstudiante(String estudianteId) {
        return getCollection().whereEqualTo("estudianteId", estudianteId).get();
    }

    public Task<DocumentReference> create(ReporteEmocional reporte) {
        return getCollection().add(reporte);
    }

    public Task<Void> update(String reporteId, Map<String, Object> updates) {
        updates.put("fecha", System.currentTimeMillis());
        return getCollection().document(reporteId).update(updates);
    }
}