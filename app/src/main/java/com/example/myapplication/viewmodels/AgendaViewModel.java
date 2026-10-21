package com.example.myapplication.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.models.Cita;
import com.example.myapplication.repositories.AppointmentRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AgendaViewModel extends ViewModel {

    private final AppointmentRepository appointmentRepository = new AppointmentRepository();

    private final MutableLiveData<List<Cita>> citas = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private ListenerRegistration registration;
    private String psicologoId;

    public LiveData<List<Cita>> getCitas() {
        return citas;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void empezarEscucha(String uid) {
        if (uid == null) return;
        if (uid.equals(psicologoId) && registration != null) return;

        detenerEscucha();
        psicologoId = uid;
        loading.setValue(citas.getValue() == null);

        registration = appointmentRepository.escucharCitasDePsicologo(uid, (snapshots, e) -> {
            loading.setValue(false);
            if (e != null) {
                error.setValue("No se pudieron cargar las citas");
                return;
            }
            citas.setValue(parseCitas(snapshots));
        });
    }

    public void detenerEscucha() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    public void reintentar() {
        if (psicologoId == null) return;
        detenerEscucha();
        empezarEscucha(psicologoId);
    }

    public void clearError() {
        error.setValue(null);
    }

    @Override
    protected void onCleared() {
        detenerEscucha();
    }

    private static List<Cita> parseCitas(QuerySnapshot snapshots) {
        List<Cita> lista = new ArrayList<>();
        if (snapshots != null) {
            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                Cita c = doc.toObject(Cita.class);
                if (c != null) {
                    c.setId(doc.getId());
                    lista.add(c);
                }
            }
        }
        lista.sort((a, b) -> {
            boolean aCanc = "CANCELADA".equals(a.getEstado());
            boolean bCanc = "CANCELADA".equals(b.getEstado());
            if (aCanc != bCanc) return aCanc ? 1 : -1;
            int cmp = Comparator.nullsLast(String::compareTo)
                    .compare(a.getFecha(), b.getFecha());
            if (cmp != 0) return cmp;
            return Comparator.nullsLast(String::compareTo)
                    .compare(a.getHora(), b.getHora());
        });
        return lista;
    }
}
