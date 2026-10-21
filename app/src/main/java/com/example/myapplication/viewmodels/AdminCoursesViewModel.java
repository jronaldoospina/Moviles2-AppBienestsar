package com.example.myapplication.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.models.Curso;
import com.example.myapplication.repositories.CourseRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminCoursesViewModel extends ViewModel {

    private final CourseRepository courseRepository = new CourseRepository();

    private final MutableLiveData<List<Curso>> cursos = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private boolean cargado = false;
    private boolean cargando = false;
    private String query = "";

    public LiveData<List<Curso>> getCursos() {
        return cursos;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void cargar() {
        if (cargando || cargado) return;
        cargando = true;
        loading.setValue(true);

        courseRepository.getAllCursos()
                .addOnSuccessListener(snapshot -> {
                    List<Curso> lista = parseCursos(snapshot);
                    Collections.sort(lista, (a, b) ->
                            Long.compare(b.getFechaCreacion(), a.getFechaCreacion()));
                    cursos.setValue(lista);
                    cargando = false;
                    cargado = true;
                    loading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    cargando = false;
                    loading.setValue(false);
                    error.setValue("No se pudieron cargar los cursos");
                });
    }

    public void recargar() {
        cargado = false;
        cargar();
    }

    public void clearError() {
        error.setValue(null);
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query != null ? query : "";
    }

    private static List<Curso> parseCursos(QuerySnapshot snapshot) {
        List<Curso> lista = new ArrayList<>();
        if (snapshot == null) return lista;
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Curso c = doc.toObject(Curso.class);
            if (c != null) {
                c.setId(doc.getId());
                lista.add(c);
            }
        }
        return lista;
    }
}
