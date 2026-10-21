package com.example.myapplication.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.models.Alerta;
import com.example.myapplication.repositories.AlertRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class TeacherAlertsViewModel extends ViewModel {

    private final AlertRepository alertRepository = new AlertRepository();

    private final MutableLiveData<List<Alerta>> alertas = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private ListenerRegistration registration;
    private String docenteId;

    public LiveData<List<Alerta>> getAlertas() {
        return alertas;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void empezarEscucha(String uid) {
        if (uid == null) return;
        if (uid.equals(docenteId) && registration != null) return;

        detenerEscucha();
        docenteId = uid;
        loading.setValue(alertas.getValue() == null);

        registration = alertRepository.escucharAlertasDeDocente(uid, (snapshots, e) -> {
            loading.setValue(false);
            if (e != null) {
                error.setValue("No se pudieron cargar las alertas");
                return;
            }
            alertas.setValue(parseAlertas(snapshots));
        });
    }

    public void detenerEscucha() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    public void reintentar() {
        if (docenteId == null) return;
        detenerEscucha();
        empezarEscucha(docenteId);
    }

    public void clearError() {
        error.setValue(null);
    }

    @Override
    protected void onCleared() {
        detenerEscucha();
    }

    private static List<Alerta> parseAlertas(QuerySnapshot snapshots) {
        List<Alerta> lista = new ArrayList<>();
        if (snapshots != null) {
            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                Alerta a = doc.toObject(Alerta.class);
                if (a != null) {
                    a.setId(doc.getId());
                    lista.add(a);
                }
            }
        }
        lista.sort((x, y) -> Long.compare(y.getFecha(), x.getFecha()));
        return lista;
    }
}
