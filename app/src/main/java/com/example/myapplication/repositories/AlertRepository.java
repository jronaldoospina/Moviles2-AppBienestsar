package com.example.myapplication.repositories;

import com.example.myapplication.models.Alerta;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Map;

public class AlertRepository {

    private static final String COLLECTION = "alertas";
    private final FirebaseFirestore db;

    public AlertRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    private CollectionReference getCollection() {
        return db.collection(COLLECTION);
    }

    public Task<DocumentReference> createAlerta(Alerta alerta) {
        return getCollection().add(alerta);
    }

    /** Alertas pendientes (para psicólogo) */
    public Task<QuerySnapshot> getAlertaEmocionalActiva(String estudianteId) {
        return getCollection()
                .whereEqualTo("estudianteId", estudianteId)
                .whereEqualTo("motivo", "EMOCIONAL")
                .whereEqualTo("estado", "PENDIENTE")
                .get();
    }

    /** Todas las alertas */
    public Task<QuerySnapshot> getAllAlertas() {
        return getCollection()
                .orderBy("fecha", Query.Direction.DESCENDING)
                .get();
    }

    /** Escucha en tiempo real todas las alertas (ordenadas por fecha) */
    public ListenerRegistration escucharAlertas(EventListener<QuerySnapshot> listener) {
        return getCollection()
                .orderBy("fecha", Query.Direction.DESCENDING)
                .addSnapshotListener(listener);
    }

    /** Escucha en tiempo real las alertas que reportó un docente */
    public ListenerRegistration escucharAlertasDeDocente(String docenteId,
                                                         EventListener<QuerySnapshot> listener) {
        return getCollection()
                .whereEqualTo("docenteId", docenteId)
                .addSnapshotListener(listener);
    }

    public Task<Void> updateAlerta(String alertaId, Map<String, Object> updates) {
        return getCollection().document(alertaId).update(updates);
    }

    public Task<Void> atenderAlerta(String alertaId, String psicologoId) {
        return getCollection().document(alertaId).update(
                "estado", "ATENDIDA",
                "atendidaPor", psicologoId,
                "fechaAtencion", System.currentTimeMillis()
        );
    }

    public Task<Void> deleteAlerta(String alertaId) {
        return getCollection().document(alertaId).delete();
    }

    /** Alertas reportadas por un docente en específico */
    public Task<QuerySnapshot> getAlertasByDocente(String docenteId) {
        return getCollection()
                .whereEqualTo("docenteId", docenteId)
                .orderBy("fecha", Query.Direction.DESCENDING)
                .get();
    }
}