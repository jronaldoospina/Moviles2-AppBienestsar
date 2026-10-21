package com.example.myapplication.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.models.Conversacion;
import com.example.myapplication.models.Notificacion;
import com.example.myapplication.repositories.ChatRepository;
import com.example.myapplication.repositories.NotificacionRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

public class MainViewModel extends ViewModel {

    private final NotificacionRepository notificacionRepository = new NotificacionRepository();
    private final ChatRepository chatRepository = new ChatRepository();

    private final MutableLiveData<Integer> noLeidas = new MutableLiveData<>(0);

    private ListenerRegistration notificacionRegistration;
    private ListenerRegistration chatRegistration;
    private String uid;
    private int noLeidasNotif;
    private int noLeidasChat;

    public LiveData<Integer> getNoLeidas() {
        return noLeidas;
    }

    public void empezar(String uid) {
        if (uid == null) return;
        if (uid.equals(this.uid) && notificacionRegistration != null && chatRegistration != null) {
            return;
        }

        detener();
        if (!uid.equals(this.uid)) {
            noLeidasNotif = 0;
            noLeidasChat = 0;
            noLeidas.setValue(0);
        }
        this.uid = uid;

        notificacionRegistration = notificacionRepository.escuchar(uid, (snapshot, e) -> {
            if (e != null || snapshot == null) return;
            noLeidasNotif = contarNotificaciones(snapshot);
            actualizarTotal();
        });

        chatRegistration = chatRepository.escucharConversaciones(uid, (snapshot, e) -> {
            if (e != null || snapshot == null) return;
            noLeidasChat = contarPendientes(snapshot, uid);
            actualizarTotal();
        });
    }

    public void detener() {
        if (notificacionRegistration != null) {
            notificacionRegistration.remove();
            notificacionRegistration = null;
        }
        if (chatRegistration != null) {
            chatRegistration.remove();
            chatRegistration = null;
        }
    }

    @Override
    protected void onCleared() {
        detener();
    }

    private void actualizarTotal() {
        noLeidas.setValue(noLeidasNotif + noLeidasChat);
    }

    private static int contarNotificaciones(QuerySnapshot snapshot) {
        int total = 0;
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Notificacion n = doc.toObject(Notificacion.class);
            if (n != null && !n.isLeida()) total++;
        }
        return total;
    }

    private static int contarPendientes(QuerySnapshot snapshot, String uid) {
        int total = 0;
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Conversacion c = doc.toObject(Conversacion.class);
            if (c == null || c.getPendientes() == null) continue;
            Long v = c.getPendientes().get(uid);
            if (v != null) total += v;
        }
        return total;
    }
}
