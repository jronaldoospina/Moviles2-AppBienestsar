package com.example.myapplication.repositories;

import com.example.myapplication.models.Message;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {

    private static final String COLLECTION = "mensajes";
    private final FirebaseFirestore db;

    public MessageRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Escucha los mensajes asociados a un usuario en tiempo real.
     *
     * @param userId UID del usuario
     * @param callback Callback que recibe la lista de mensajes
     * @return ListenerRegistration para poder remover el listener
     */
    public ListenerRegistration escucharMensajes(String userId, MessageCallback callback) {
        return db.collection(COLLECTION)
                .whereGreaterThan("fecha", 0)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        if (callback != null) {
                            callback.onError(error);
                        }
                        return;
                    }

                    if (querySnapshot != null) {
                        List<Message> resultList = new ArrayList<>();
                        for (DocumentChange change : querySnapshot.getDocumentChanges()) {
                            Message message = change.getDocument().toObject(Message.class);
                            if (message != null) {
                                message.setId(change.getDocument().getId());
                                if (message.getRemitenteId() != null && message.getRemitenteId().equals(userId)) {
                                    resultList.add(message);
                                } else if (message.getReceptorId() != null && message.getReceptorId().equals(userId)) {
                                    resultList.add(message);
                                }
                            }
                        }
                        if (callback != null) {
                            callback.onMessagesLoaded(resultList);
                        }
                    }
                });
    }

    /** Interfaz callback para recibir mensajes en tiempo real */
    public interface MessageCallback {
        void onMessagesLoaded(List<Message> messages);
        default void onError(Exception error) {}
    }

    /**
     * Crea un nuevo mensaje en Firestore.
     *
     * @param message Mensaje a crear
     * @param callback Callback que se ejecuta después de crear el mensaje
     * @return Task<Void> para operación asíncrona
     */
    public Task<Void> crearMensaje(Message message, MessageCallbackVoid callback) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        db.collection(COLLECTION).add(message)
                .addOnSuccessListener(documentReference -> {
                    message.setId(documentReference.getId());
                    if (callback != null) {
                        callback.onSuccess();
                    }
                    tcs.trySetResult(null);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e);
                    }
                    tcs.trySetException(e);
                });

        return tcs.getTask();
    }

    /** Interfaz callback void para operaciones de mensaje */
    public interface MessageCallbackVoid {
        void onSuccess();
        default void onError(Exception error) {}
    }

    /**
     * Actualiza el estado de lectura de un mensaje.
     *
     * @param messageId ID del mensaje a actualizar
     * @param leido Si fue leído
     * @return Task<Void> para operación asíncrona
     */
    public Task<Void> marcarComoLeido(String messageId, boolean leido) {
        return db.collection(COLLECTION).document(messageId)
                .update("leido", leido);
    }

    /**
     * Obtiene un mensaje por su ID.
     *
     * @param messageId ID del mensaje
     * @param callback Callback que recibe el mensaje cargado
     * @return Task para operación asíncrona
     */
    public Task<Message> getMessageById(String messageId, MessageGetCallback callback) {
        TaskCompletionSource<Message> tcs = new TaskCompletionSource<>();
        db.collection(COLLECTION).document(messageId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Message message = documentSnapshot.toObject(Message.class);
                        if (message != null) {
                            message.setId(documentSnapshot.getId());
                        }
                        if (callback != null) {
                            callback.onMessageLoaded(message);
                        }
                        tcs.trySetResult(message);
                    } else {
                        Exception ex = new Exception("Mensaje no encontrado");
                        if (callback != null) {
                            callback.onError(ex);
                        }
                        tcs.trySetException(ex);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e);
                    }
                    tcs.trySetException(e);
                });
        return tcs.getTask();
    }

    /** Interfaz callback para obtener un mensaje por ID */
    public interface MessageGetCallback {
        void onMessageLoaded(Message message);
        default void onError(Exception error) {}
    }
}
