package com.example.myapplication.repositories;

import com.example.myapplication.models.Nota;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class GradeRepository {

    private static final String COLLECTION = "notas";
    private final FirebaseFirestore db;

    public GradeRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    private CollectionReference getCollection() {
        return db.collection(COLLECTION);
    }

    /** Crea o reemplaza una nota */
    public Task<DocumentReference> createNota(Nota nota) {
        return getCollection().add(nota);
    }

    /** Obtiene notas de un estudiante en un curso */
    public Task<QuerySnapshot> getNotasByEstudianteCurso(String estudianteId, String cursoId) {
        return getCollection()
                .whereEqualTo("estudianteId", estudianteId)
                .whereEqualTo("cursoId", cursoId)
                .get();
    }

    /** Obtiene todas las notas de un estudiante */
    public Task<QuerySnapshot> getNotasByEstudiante(String estudianteId) {
        // Se remueve el orderBy para evitar requerimiento de índice compuesto (FAILED_PRECONDITION)
        return getCollection()
                .whereEqualTo("estudianteId", estudianteId)
                .get();
    }

    /** Obtiene todas las notas de un curso y un corte específico */
    public Task<QuerySnapshot> getNotasByCursoAndCorte(String cursoId, String corteId) {
        return getCollection()
                .whereEqualTo("cursoId", cursoId)
                .whereEqualTo("corteId", corteId)
                .get();
    }
    public Task<QuerySnapshot> getNotasByCurso(String cursoId) {
        return getCollection()
                .whereEqualTo("cursoId", cursoId)
                .get();
    }

    /** Actualiza una nota */
    public Task<Void> updateNota(String notaId, Map<String, Object> updates) {
        updates.put("ultimaActualizacion", System.currentTimeMillis());
        return getCollection().document(notaId).update(updates);
    }

    /** Elimina una nota */
    public Task<Void> deleteNota(String notaId) {
        return getCollection().document(notaId).delete();
    }

    /** Guarda o actualiza notas masivamente */
    public Task<Void> guardarNotasMasivas(Map<String, Double> notasMap, String cursoId, String corteId) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        if (notasMap == null || notasMap.isEmpty()) {
            tcs.trySetResult(null);
            return tcs.getTask();
        }
        getNotasByCurso(cursoId).addOnSuccessListener(snap -> {
            Map<String, String> existingIds = new HashMap<>();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Nota n = doc.toObject(Nota.class);
                if (n != null && n.getEstudianteId() != null && corteId.equals(n.getCorteId())) {
                    existingIds.put(n.getEstudianteId(), doc.getId());
                }
            }
            final int total = notasMap.size();
            final int[] completed = {0};
            final boolean[] hasError = {false};

            for (Map.Entry<String, Double> entry : notasMap.entrySet()) {
                String estId = entry.getKey();
                Double valor = entry.getValue();
                String notaId = existingIds.get(estId);

                if (notaId != null) {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("valor", valor);
                    updates.put("ultimaActualizacion", System.currentTimeMillis());
                    updateNota(notaId, updates).addOnCompleteListener(task -> {
                        completed[0]++;
                        if (task.isSuccessful() && !hasError[0]) {
                            if (completed[0] == total) tcs.trySetResult(null);
                        } else if (!hasError[0]) {
                            hasError[0] = true;
                            tcs.trySetException(task.getException() != null ? task.getException() : new Exception("Error saving note"));
                        }
                    });
                } else {
                    Nota n = new Nota(estId, cursoId, corteId, valor, null);
                    createNota(n).addOnCompleteListener(task -> {
                        completed[0]++;
                        if (task.isSuccessful() && !hasError[0]) {
                            if (completed[0] == total) tcs.trySetResult(null);
                        } else if (!hasError[0]) {
                            hasError[0] = true;
                            tcs.trySetException(task.getException() != null ? task.getException() : new Exception("Error creating note"));
                        }
                    });
                }
            }
        }).addOnFailureListener(e -> tcs.trySetException(e));
        return tcs.getTask();
    }
}