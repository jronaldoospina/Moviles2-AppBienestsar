package com.example.myapplication.repositories;

import com.example.myapplication.models.Curso;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class CourseRepository {

    private static final String COLLECTION = "cursos";
    private final FirebaseFirestore db;

    public CourseRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    private CollectionReference getCollection() {
        return db.collection(COLLECTION);
    }

    /**
     * Crea un nuevo curso. Retorna el ID generado.
     */
    public Task<DocumentReference> createCurso(Curso curso) {
        return getCollection().add(curso);
    }

    /**
     * Obtiene un curso por ID.
     */
    public Task<com.google.firebase.firestore.DocumentSnapshot> getCursoById(String cursoId) {
        return getCollection().document(cursoId).get();
    }

    /**
     * Lista los cursos de un docente.
     */
    public Task<QuerySnapshot> getCursosByDocente(String docenteId) {
        return getCollection()
                .whereEqualTo("docenteId", docenteId)
                .get();
    }

    /**
     * Lista todos los cursos.
     */
    public Task<QuerySnapshot> getAllCursos() {
        return getCollection()
                .orderBy("fechaCreacion", Query.Direction.DESCENDING)
                .get();
    }

    /**
     * Lista los cursos de un estudiante por su matrícula (array-contains).
     */
    public Task<QuerySnapshot> getCursosByEstudiante(String estudianteId) {
        return getCollection()
                .whereArrayContains("estudiantesIds", estudianteId)
                .get();
    }

    /**
     * Actualiza campos de un curso.
     */
    public Task<Void> updateCurso(String cursoId, Map<String, Object> updates) {
        updates.put("ultimaActualizacion", System.currentTimeMillis());
        return getCollection().document(cursoId).update(updates);
    }

    /**
     * Agrega un estudiante al curso.
     */
    public Task<Void> addEstudiante(String cursoId, String estudianteId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("ultimaActualizacion", System.currentTimeMillis());
        // Usamos FieldValue.arrayUnion para no pisar el array
        return getCollection().document(cursoId)
                .update("estudiantesIds",
                        com.google.firebase.firestore.FieldValue.arrayUnion(estudianteId),
                        "ultimaActualizacion", System.currentTimeMillis());
    }

    /**
     * Elimina un estudiante del curso.
     */
    public Task<Void> removeEstudiante(String cursoId, String estudianteId) {
        return getCollection().document(cursoId)
                .update("estudiantesIds",
                        com.google.firebase.firestore.FieldValue.arrayRemove(estudianteId),
                        "ultimaActualizacion", System.currentTimeMillis());
    }

    /**
     * Actualiza la lista completa de cortes.
     */
    public Task<Void> updateCortes(String cursoId, java.util.List<Curso.Corte> cortes) {
        return getCollection().document(cursoId)
                .update("cortes", cortes,
                        "ultimaActualizacion", System.currentTimeMillis());
    }

    /**
     * Elimina un curso.
     */
    public Task<Void> deleteCurso(String cursoId) {
        return getCollection().document(cursoId).delete();
    }
}