package com.example.myapplication.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.models.Tutoria;
import com.example.myapplication.repositories.TutoriaRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class TutoriasViewModel extends ViewModel {

    private final TutoriaRepository tutoriaRepository = new TutoriaRepository();

    private final MutableLiveData<List<Tutoria>> tutorias = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private boolean cargado = false;
    private boolean cargando = false;
    private String docenteId;

    public LiveData<List<Tutoria>> getTutorias() {
        return tutorias;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void cargar(String uid) {
        if (uid == null) return;
        if (cargando) return;
        if (cargado && uid.equals(docenteId)) return;

        docenteId = uid;
        cargando = true;
        loading.setValue(true);

        tutoriaRepository.getTutoriasByDocente(uid)
                .addOnSuccessListener(snapshot -> {
                    tutorias.setValue(parseTutorias(snapshot));
                    cargando = false;
                    cargado = true;
                    loading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    cargando = false;
                    loading.setValue(false);
                    error.setValue("No se pudieron cargar las tutorías");
                });
    }

    public void recargar() {
        cargado = false;
        cargar(docenteId);
    }

    public void clearError() {
        error.setValue(null);
    }

    private static List<Tutoria> parseTutorias(QuerySnapshot snapshot) {
        List<Tutoria> lista = new ArrayList<>();
        if (snapshot != null) {
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Tutoria t = doc.toObject(Tutoria.class);
                if (t != null) {
                    t.setId(doc.getId());
                    lista.add(t);
                }
            }
        }
        return lista;
    }
}
