package com.example.myapplication.repositories;

import com.example.myapplication.models.AuditoriaLog;
import com.example.myapplication.services.SessionManager;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class AuditRepository {

    private static final String COLLECTION = "auditoria";
    private final FirebaseFirestore db;

    public AuditRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    private CollectionReference getCollection() {
        return db.collection(COLLECTION);
    }

    /** Registra una acción con el actor actual (usuario de la sesión). */
    public Task<com.google.firebase.firestore.DocumentReference> registrar(
            String tipo, String detalle, SessionManager sessionManager) {
        String userId = sessionManager != null ? sessionManager.getUserId() : null;
        String nombre = sessionManager != null ? sessionManager.getUserName() : null;
        String rol = sessionManager != null ? sessionManager.getUserRole() : null;
        AuditoriaLog log = new AuditoriaLog(userId, nombre, rol, tipo, detalle);
        return getCollection().add(log);
    }

    /** Todos los registros, del más reciente al más antiguo. */
    public Task<QuerySnapshot> getLogs() {
        return getCollection()
                .orderBy("fecha", Query.Direction.DESCENDING)
                .get();
    }

    /** Escucha en tiempo real los registros de auditoría. */
    public ListenerRegistration escucharLogs(EventListener<QuerySnapshot> listener) {
        return getCollection()
                .orderBy("fecha", Query.Direction.DESCENDING)
                .addSnapshotListener(listener);
    }
}