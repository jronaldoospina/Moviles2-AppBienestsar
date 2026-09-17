package com.example.myapplication.repositories;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.example.myapplication.models.Usuario;

/**
 * Repositorio para operaciones con usuarios en Firestore.
 */
public class UserRepository {

    private static final String COLLECTION_USUARIOS = "usuarios";

    private final FirebaseFirestore db;
    private final CollectionReference usuariosRef;

    public UserRepository() {
        db = FirebaseFirestore.getInstance();
        usuariosRef = db.collection(COLLECTION_USUARIOS);
    }

    /**
     * Crea un nuevo documento de usuario en Firestore
     */
    public Task<Void> createUser(String uid, Usuario usuario) {
        return usuariosRef.document(uid).set(usuario);
    }

    /**
     * Obtiene un usuario por su UID
     */
    public Task<DocumentSnapshot> getUserById(String uid) {
        return usuariosRef.document(uid).get();
    }

    /**
     * Actualiza campos específicos de un usuario
     */
    public Task<Void> updateUser(String uid, java.util.Map<String, Object> updates) {
        updates.put("ultimaActualizacion", System.currentTimeMillis());
        return usuariosRef.document(uid).update(updates);
    }

    /**
     * Actualiza el teléfono del usuario
     */
    public Task<Void> updatePhone(String uid, String telefono) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("telefono", telefono);
        return updateUser(uid, updates);
    }

    /**
     * Marca que el usuario ya cambió su contraseña
     */
    public Task<Void> markPasswordChanged(String uid) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("requiereCambioContrasena", false);
        updates.put("ultimoCambioContrasena", System.currentTimeMillis());
        return updateUser(uid, updates);
    }

    /**
     * Admin resetea la contraseña de un usuario (marca requiereCambio)
     */
    public Task<Void> resetPasswordRequirement(String uid) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("requiereCambioContrasena", true);
        return updateUser(uid, updates);
    }

    /**
     * Verifica si ya existe un documento con ese número de documento
     */
    public Task<QuerySnapshot> checkDocumentoExists(String documento) {
        return usuariosRef.whereEqualTo("documento", documento).get();
    }

    /**
     * Verifica si ya existe un usuario con ese nombre de usuario
     */
    public Task<QuerySnapshot> checkUsuarioExists(String usuario) {
        return usuariosRef.whereEqualTo("usuario", usuario).get();
    }

    /**
     * Obtiene todos los usuarios (para Admin)
     */
    public Task<QuerySnapshot> getAllUsers() {
        return usuariosRef.orderBy("nombre", Query.Direction.ASCENDING).get();
    }

    /**
     * Obtiene estudiantes (para Psicólogo)
     */

    public Task<QuerySnapshot> getAllEstudiantes() {
        return db.collection("usuarios")
                .whereEqualTo("rol", "ESTUDIANTE")
                .get();
    }

    /**
     * Activa o desactiva un usuario
     */
    public Task<Void> setUserActive(String uid, boolean activo) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("activo", activo);
        return updateUser(uid, updates);
    }
}