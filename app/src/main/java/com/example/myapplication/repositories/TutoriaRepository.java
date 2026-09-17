package com.example.myapplication.repositories;

import com.example.myapplication.models.Tutoria;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Map;

public class TutoriaRepository {

    private static final String COLLECTION = "tutorias";
    private final FirebaseFirestore db;

    public TutoriaRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    private CollectionReference getCollection() {
        return db.collection(COLLECTION);
    }

    public Task<DocumentReference> createTutoria(Tutoria tutoria) {
        return getCollection().add(tutoria);
    }

    /** Tutorías de un docente */
    public Task<QuerySnapshot> getTutoriasByDocente(String docenteId) {
        return getCollection()
                .whereEqualTo("docenteId", docenteId)
                .get();
    }

    /** Tutorías de un curso */
    public Task<QuerySnapshot> getTutoriasByCurso(String cursoId) {
        return getCollection()
                .whereEqualTo("cursoId", cursoId)
                .whereEqualTo("activa", true)
                .get();
    }

    /** Tutorías activas para estudiantes (todas) */
    public Task<QuerySnapshot> getTutoriasActivas() {
        return getCollection()
                .whereEqualTo("activa", true)
                .orderBy("fechaCreacion", Query.Direction.DESCENDING)
                .get();
    }

    /** Todas las tutorías (para Admin) */
    public Task<QuerySnapshot> getAllTutorias() {
        return getCollection().get();
    }

    public Task<Void> updateTutoria(String tutoriaId, Map<String, Object> updates) {
        updates.put("ultimaActualizacion", System.currentTimeMillis());
        return getCollection().document(tutoriaId).update(updates);
    }

    public Task<Void> deleteTutoria(String tutoriaId) {
        return getCollection().document(tutoriaId).delete();
    }
}