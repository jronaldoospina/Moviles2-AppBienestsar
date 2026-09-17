package com.example.myapplication.repositories;

import com.example.myapplication.models.Conversacion;
import com.example.myapplication.models.Mensaje;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class ChatRepository {

    private static final String CONVERSACIONES = "conversaciones";
    private static final String MENSAJES = "mensajes";

    private final FirebaseFirestore db;

    public ChatRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    private CollectionReference conv() {
        return db.collection(CONVERSACIONES);
    }

    private CollectionReference msj() {
        return db.collection(MENSAJES);
    }

    /** Llave única y ordenada entre dos participantes (insensible al orden). */
    public static String keyPar(String a, String b) {
        return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
    }

    /** Escucha en tiempo real las conversaciones del usuario. */
    public com.google.firebase.firestore.ListenerRegistration escucharConversaciones(
            String userId, EventListener<QuerySnapshot> listener) {
        return conv().whereArrayContains("participantes", userId).addSnapshotListener(listener);
    }

    /** Consulta una conversación por su llave única. */
    public Task<QuerySnapshot> buscarPorPar(String par) {
        return conv().whereEqualTo("par", par).get();
    }

    public Task<DocumentReference> crearConversacion(Conversacion conversacion) {
        return conv().add(conversacion);
    }

    public Task<Void> actualizarConversacion(String conversacionId, String ultimoMensaje) {
        return conv().document(conversacionId).update(
                "ultimoMensaje", ultimoMensaje,
                "ultimaActualizacion", System.currentTimeMillis()
        );
    }

    public Query getQueryMensajes(String conversacionId) {
        return msj().whereEqualTo("conversacionId", conversacionId);
    }

    public Task<QuerySnapshot> getMensajes(String conversacionId) {
        return msj().whereEqualTo("conversacionId", conversacionId).get();
    }

    public Task<DocumentReference> enviarMensaje(Mensaje mensaje) {
        return msj().add(mensaje);
    }

    /** Marca como leídos los mensajes de un usuario en la conversación. */
    public Task<Void> marcarLeidos(String conversacionId, String usuarioId) {
        return msj().whereEqualTo("conversacionId", conversacionId).get()
                .continueWithTask(task -> {
                    List<String> ids = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            Mensaje m = doc.toObject(Mensaje.class);
                            if (m != null && !m.isLeido() && !usuarioId.equals(m.getRemitenteId())) {
                                ids.add(doc.getId());
                            }
                        }
                    }
                    if (ids.isEmpty()) return Tasks.forResult(null);
                    WriteBatch batch = db.batch();
                    for (String id : ids) {
                        batch.update(msj().document(id), "leido", true);
                    }
                    return batch.commit();
                });
    }

    /** Incrementa el contador de no leídos del destinatario en la conversación. */
    public Task<Void> incrementarPendiente(String conversacionId, String receptorId) {
        if (conversacionId == null || receptorId == null) return Tasks.forResult(null);
        return conv().document(conversacionId)
                .update("pendientes." + receptorId, FieldValue.increment(1));
    }

    /** Pone en cero el contador de no leídos del usuario (al abrir el chat). */
    public Task<Void> limpiarPendiente(String conversacionId, String usuarioId) {
        if (conversacionId == null || usuarioId == null) return Tasks.forResult(null);
        return conv().document(conversacionId).update("pendientes." + usuarioId, 0L);
    }
}