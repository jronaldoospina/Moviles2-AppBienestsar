package com.example.myapplication.repositories;

import com.example.myapplication.models.Cita;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Map;

public class AppointmentRepository {

    private static final String COLLECTION = "citas";
    private final FirebaseFirestore db;

    public AppointmentRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    private CollectionReference getCollection() {
        return db.collection(COLLECTION);
    }

    public Task<DocumentReference> createCita(Cita cita) {
        return getCollection().add(cita);
    }

    /** Citas de un estudiante */
    public Task<QuerySnapshot> getCitasByEstudiante(String estudianteId) {
        return getCollection()
                .whereEqualTo("estudianteId", estudianteId)
                .get();
    }

    /** Citas de un psicólogo, sin orden server-side (se ordena en cliente, sin índice compuesto). */
    public Task<QuerySnapshot> getCitasByPsicologoSinOrden(String psicologoId) {
        return getCollection()
                .whereEqualTo("psicologoId", psicologoId)
                .get();
    }

    /** Escucha en tiempo real las citas de un psicólogo. */
    public com.google.firebase.firestore.ListenerRegistration escucharCitasDePsicologo(
            String psicologoId,
            com.google.firebase.firestore.EventListener<QuerySnapshot> listener) {
        return getCollection()
                .whereEqualTo("psicologoId", psicologoId)
                .addSnapshotListener(listener);
    }

    /** Todas las citas (para Admin) */
    public Task<QuerySnapshot> getAllCitas() {
        return getCollection().get();
    }

    public Task<Void> updateCita(String citaId, Map<String, Object> updates) {
        updates.put("ultimaActualizacion", System.currentTimeMillis());
        return getCollection().document(citaId).update(updates);
    }

    public Task<Void> cambiarEstado(String citaId, String nuevoEstado) {
        return getCollection().document(citaId).update(
                "estado", nuevoEstado,
                "ultimaActualizacion", System.currentTimeMillis()
        );
    }

    public Task<Void> confirmarCita(String citaId) {
        return cambiarEstado(citaId, "CONFIRMADA");
    }

    public Task<Void> atenderCita(String citaId, String notas) {
        Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("estado", "ATENDIDA");
        updates.put("ultimaActualizacion", System.currentTimeMillis());
        if (notas != null && !notas.trim().isEmpty()) {
            updates.put("notasPsicologo", notas.trim());
        }
        return getCollection().document(citaId).update(updates);
    }

    public Task<Void> deleteCita(String citaId) {
        return getCollection().document(citaId).delete();
    }
}