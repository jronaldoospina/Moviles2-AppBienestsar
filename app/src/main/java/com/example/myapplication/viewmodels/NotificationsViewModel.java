package com.example.myapplication.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.models.Notificacion;
import com.example.myapplication.repositories.NotificacionRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class NotificationsViewModel extends ViewModel {

    private static final int MAX_NOTIFICACIONES = 100;

    private final NotificacionRepository notificacionRepository = new NotificacionRepository();

    private final MutableLiveData<List<Notificacion>> notificaciones = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private ListenerRegistration registration;
    private String receptorId;

    public LiveData<List<Notificacion>> getNotificaciones() {
        return notificaciones;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void empezarEscucha(String uid) {
        if (uid == null) return;
        if (uid.equals(receptorId) && registration != null) return;

        detenerEscucha();
        receptorId = uid;
        loading.setValue(notificaciones.getValue() == null);

        registration = notificacionRepository.escuchar(uid, (snapshot, e) -> {
            loading.setValue(false);
            if (e != null) {
                error.setValue("No se pudieron cargar las notificaciones");
                return;
            }
            notificaciones.setValue(parseNotificaciones(snapshot));
        });
    }

    public void detenerEscucha() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    public void reintentar() {
        if (receptorId == null) return;
        detenerEscucha();
        empezarEscucha(receptorId);
    }

    public void clearError() {
        error.setValue(null);
    }

    @Override
    protected void onCleared() {
        detenerEscucha();
    }

    private static List<Notificacion> parseNotificaciones(QuerySnapshot snapshot) {
        List<Notificacion> lista = new ArrayList<>();
        if (snapshot != null) {
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Notificacion n = doc.toObject(Notificacion.class);
                if (n != null) {
                    n.setId(doc.getId());
                    lista.add(n);
                }
            }
        }
        lista.sort((a, b) -> Long.compare(b.getFecha(), a.getFecha()));
        if (lista.size() > MAX_NOTIFICACIONES) {
            return new ArrayList<>(lista.subList(0, MAX_NOTIFICACIONES));
        }
        return lista;
    }
}
