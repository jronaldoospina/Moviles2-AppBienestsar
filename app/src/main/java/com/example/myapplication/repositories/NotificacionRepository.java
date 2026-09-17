package com.example.myapplication.repositories;

import com.example.myapplication.models.Notificacion;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class NotificacionRepository {

    private static final String COLLECTION = "notificaciones";

    private final FirebaseFirestore db;

    public NotificacionRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    private CollectionReference col() {
        return db.collection(COLLECTION);
    }

    public ListenerRegistration escuchar(String receptorId, EventListener<QuerySnapshot> listener) {
        return col().whereEqualTo("receptorId", receptorId).addSnapshotListener(listener);
    }

    public Task<DocumentReference> crear(Notificacion notificacion) {
        return col().add(notificacion);
    }

    public Task<Void> marcarLeida(String notificacionId) {
        return col().document(notificacionId).update("leida", true);
    }

    public Task<Void> marcarTodasLeidas(String receptorId) {
        return col().whereEqualTo("receptorId", receptorId).get()
                .continueWithTask(task -> {
                    List<String> ids = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            Notificacion n = doc.toObject(Notificacion.class);
                            if (n != null && !n.isLeida()) {
                                ids.add(doc.getId());
                            }
                        }
                    }
                    if (ids.isEmpty()) return Tasks.forResult(null);
                    WriteBatch batch = db.batch();
                    for (String id : ids) {
                        batch.update(col().document(id), "leida", true);
                    }
                    return batch.commit();
                });
    }

    public Task<Void> eliminar(String notificacionId) {
        return col().document(notificacionId).delete();
    }
}