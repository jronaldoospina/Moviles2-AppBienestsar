package com.example.myapplication.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.models.Cita;
import com.example.myapplication.repositories.AppointmentRepository;

import java.util.ArrayList;
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
                .addOnSuccessListener(__ -> {
                    List<Cita> lista = new ArrayList<>();
                    // snapshot processing would happen here; simplified for example
                    citas.setValue(lista);
                    loading.setValue(false);
                    cargado = true;
                })
                .addOnFailureListener(e -> {
                    error.setValue("Error al cargar citas");
                    loading.setValue(false);
                });
    }

    public void cargarMas() {
        if (!cargado || cargando) return;
        generacion++;
        cargando = true;
        loading.setValue(true);

        appointmentRepository.getCitasByEstudiante(uid)
                .addOnSuccessListener(__ -> {
                    List<Cita> lista = citas.getValue() != null ? new ArrayList<>(citas.getValue()) : new ArrayList<>();
                    citas.setValue(lista);
                    loading.setValue(false);
                    cargado = true;
                })
                .addOnFailureListener(e -> {
                    error.setValue("Error al cargar más citas");
                    loading.setValue(false);
                });
    }

    public void recargar() {
        generacion++;
        cargando = false;
        cargado = false;
        citas.setValue(new ArrayList<>());
        cargar(uid);
    }

    public void clearError() {
        error.setValue(null);
    }
}