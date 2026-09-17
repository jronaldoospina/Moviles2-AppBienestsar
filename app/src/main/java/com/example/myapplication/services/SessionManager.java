package com.example.myapplication.services;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;

/**
 * Maneja la sesión del usuario usando SharedPreferences.
 * Incluye soporte para roles.
 */
public class SessionManager {

    // Nombre del archivo de preferencias
    private static final String PREF_NAME = "BienestarUPCSession";

    // Claves para guardar datos
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USER_ROLE = "userRole";
    private static final String KEY_USER_NAME = "userName";

    // Roles disponibles
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_PSICOLOGO = "PSICOLOGO";
    public static final String ROLE_DOCENTE = "DOCENTE";
    public static final String ROLE_ESTUDIANTE = "ESTUDIANTE";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.editor = prefs.edit();
    }

    public void createLoginSession(String userId, String email, String role, String name) {
        createLoginSession(userId, email, role, name, null);
    }

    /**
     * Crea una nueva sesión de login completa
     */
    public void createLoginSession(String userId, String email, String role, String name, String fcmToken) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_ROLE, role);
        editor.putString(KEY_USER_NAME, name);
        if (fcmToken != null && !fcmToken.isEmpty()) {
            editor.putString(KEY_FCM_TOKEN, fcmToken);
        }
        editor.apply();
    }

    /**
     * Verifica si el usuario está logueado
     */
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Obtiene el ID del usuario actual
     */
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    /**
     * Obtiene el email del usuario actual
     */
    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, null);
    }

    /**
     * Obtiene el rol del usuario actual
     */
    public String getUserRole() {
        return prefs.getString(KEY_USER_ROLE, null);
    }

    /**
     * Obtiene el nombre del usuario actual
     */
    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, null);
    }

    /**
     * Actualiza el rol del usuario
     */
    public void setUserRole(String role) {
        editor.putString(KEY_USER_ROLE, role);
        editor.apply();
    }

    /**
     * Actualiza el nombre del usuario
     */
    public void setUserName(String name) {
        editor.putString(KEY_USER_NAME, name);
        editor.apply();
    }

    /**
     * Obtiene todos los datos de la sesión
     */
    public HashMap<String, String> getUserDetails() {
        HashMap<String, String> user = new HashMap<>();
        user.put(KEY_USER_ID, prefs.getString(KEY_USER_ID, null));
        user.put(KEY_USER_EMAIL, prefs.getString(KEY_USER_EMAIL, null));
        user.put(KEY_USER_ROLE, prefs.getString(KEY_USER_ROLE, null));
        user.put(KEY_USER_NAME, prefs.getString(KEY_USER_NAME, null));
        return user;
    }

    private static final String KEY_FCM_TOKEN = "fcmToken";

    /**
     * Establece el token FCM del usuario
     */
    public void setFcmToken(String token) {
        editor.putString(KEY_FCM_TOKEN, token);
        editor.apply();
    }

    /**
     * Obtiene el token FCM del usuario
     */
    public String getFcmToken() {
        return prefs.getString(KEY_FCM_TOKEN, null);
    }

    /**
     * Cierra la sesión limpiando las preferencias
     */
    public void logout() {
        editor.clear();
        editor.apply();
    }
}