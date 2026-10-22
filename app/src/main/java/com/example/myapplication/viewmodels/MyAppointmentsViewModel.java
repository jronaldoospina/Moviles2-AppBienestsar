package com.example.myapplication.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.models.Cita;
import com.example.myapplication.repositories.AppointmentRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MyAppointmentsViewModel extends ViewModel {

    private final AppointmentRepository appointmentRepository = new AppointmentRepository();

    private final MutableLiveData<List<Cita>> citas = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private String uid;
    private boolean cargado = false;
    private boolean cargando = false;
    private int generacion = 0;

    public LiveData<List<Cita>> getCitas() {
        return citas;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void cargar(String uid) {
        if (cargando || cargado) return;
        generacion++;
        this.uid = uid;
        cargando = true;
        loading.setValue(true);

        appointmentRepository.getCitasByEstudiante(uid)
                .addOnSuccessListener(snapshot -> {
                    if (generacion != this.generacion) return;
                    List<Cita> lista = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Cita c = doc.toObject(Cita.class);
                            if (c != null) {
                                c.setId(doc.getId());
                                lista.add(c);
                            }
                        }
                    }
                    // Ordenar: más recientes primero (fecha desc, luego hora desc)
                    Collections.sort(lista, (a, b) -> {
                        String f1 = a.getFecha() != null ? a.getFecha() : "";
                        String f2 = b.getFecha() != null ? b.getFecha() : "";
                        int cmp = f2.compareTo(f1);
                        if (cmp != 0) return cmp;
                        String h1 = a.getHora() != null ? a.getHora() : "";
                        String h2 = b.getHora() != null ? b.getHora() : "";
                        return h2.compareTo(h1);
                    });
                    citas.setValue(lista);
                    loading.setValue(false);
                    cargado = true;
                })
                .addOnFailureListener(e -> {
                    if (generacion != this.generacion) return;
                    error.setValue("Error al cargar citas");
                    loading.setValue(false);
                });
    }

    public void recargar() {
        generacion++;
        cargando = false;
        cargado = false;
        citas.setValue(new ArrayList<>());
        if (uid != null) cargar(uid);
    }

    public void clearError() {
        error.setValue(null);
    }
}