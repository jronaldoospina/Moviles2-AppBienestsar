package com.example.myapplication.services;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

/**
 * Servicio Singleton para centralizar el acceso a Firebase.
 *
 * Uso:
 *   FirebaseService.getInstance().getAuth()
 *   FirebaseService.getInstance().getFirestore()
 */
public class FirebaseService {

    private static FirebaseService instance;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private FirebaseDatabase database;
    private FirebaseStorage storage;

    /**
     * Constructor privado (Singleton)
     */
    private FirebaseService() {
        // Inicializar servicios de Firebase
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    /**
     * Obtiene la instancia única del servicio
     */
    public static synchronized FirebaseService getInstance() {
        if (instance == null) {
            instance = new FirebaseService();
        }
        return instance;
    }

    // ============================================
    // GETTERS
    // ============================================

    public FirebaseAuth getAuth() {
        return auth;
    }

    public FirebaseFirestore getFirestore() {
        return firestore;
    }

    public FirebaseDatabase getDatabase() {
        return database;
    }

    public FirebaseStorage getStorage() {
        return storage;
    }

    // ============================================
    // UTILIDADES
    // ============================================

    /**
     * Verifica si hay un usuario autenticado
     */
    public boolean isUserLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    /**
     * Obtiene el ID del usuario actual
     */
    public String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    /**
     * Obtiene el email del usuario actual
     */
    public String getCurrentUserEmail() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : null;
    }

    /**
     * Cierra la sesión en Firebase
     */
    public void signOut() {
        auth.signOut();
    }
}