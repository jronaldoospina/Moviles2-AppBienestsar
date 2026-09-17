package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.myapplication.services.FirebaseService;
import com.example.myapplication.services.SessionManager;
import com.example.myapplication.repositories.AuditRepository;

public class LoginActivity extends AppCompatActivity {

    // Vistas
    private TextInputLayout layoutEmail;
    private TextInputLayout layoutPassword;
    private TextInputEditText editEmail;
    private TextInputEditText editPassword;
    private MaterialButton btnLogin;
    private CircularProgressIndicator progressLogin;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseService.getInstance().getAuth();
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        // Si ya hay sesión activa, redirigir directo
        if (sessionManager.isLoggedIn()) {
            goToMain();
            return;
        }

        initViews();
        setupListeners();
    }

    private void initViews() {
        layoutEmail = findViewById(R.id.layout_email);
        layoutPassword = findViewById(R.id.layout_password);
        editEmail = findViewById(R.id.edit_email);
        editPassword = findViewById(R.id.edit_password);
        btnLogin = findViewById(R.id.btn_login);
        progressLogin = findViewById(R.id.progress_login);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());
        // ❌ ELIMINADO: textForgotPassword.setOnClickListener(...)
        // Regla del proyecto: NO hay recuperación de contraseña por correo
    }

    private void attemptLogin() {
        layoutEmail.setError(null);
        layoutPassword.setError(null);

        String email = editEmail.getText() != null ? editEmail.getText().toString().trim() : "";
        String password = editPassword.getText() != null ? editPassword.getText().toString().trim() : "";

        if (!validateInputs(email, password)) return;

        showLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            loadUserFromFirestore(user);
                        } else {
                            showLoading(false);
                            Toast.makeText(this, "Error: usuario nulo", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        showLoading(false);
                        Toast.makeText(this, getErrorMessage(task.getException()), Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Lee el documento del usuario en Firestore para obtener rol,
     * nombre y si requiere cambio de contraseña.
     */
    private void loadUserFromFirestore(FirebaseUser user) {
        db.collection("usuarios").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    showLoading(false);

                    if (!doc.exists()) {
                        Toast.makeText(this, "Usuario no encontrado en el sistema.", Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                        return;
                    }

                    String rol = doc.getString("rol");
                    String nombre = doc.getString("nombre");
                    Boolean requiereCambio = doc.getBoolean("requiereCambioContrasena");
                    Boolean activo = doc.getBoolean("activo");

                    // Validar que el usuario esté activo
                    if (activo != null && !activo) {
                        Toast.makeText(this,
                                "Tu cuenta está inactiva. Contacta al administrador.",
                                Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                        return;
                    }

                    // Validar que tenga rol asignado
                    if (rol == null || rol.isEmpty()) {
                        Toast.makeText(this, "Usuario sin rol asignado.", Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                        return;
                    }

// Guardar sesión con rol y nombre
                    sessionManager.createLoginSession(user.getUid(), user.getEmail(), rol, nombre);

                    new AuditRepository().registrar("LOGIN",
                            "Inició sesión (" + rol + ")", sessionManager);

                    Toast.makeText(this, "¡Bienvenido, " + nombre + "!", Toast.LENGTH_SHORT).show();

                    // Redirigir según corresponda
                    if (requiereCambio != null && requiereCambio) {
                        Intent i = new Intent(LoginActivity.this, ForceChangePasswordActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                    } else {
                        goToMain();
                    }
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error al cargar datos: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    mAuth.signOut();
                });
    }

    private void goToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private boolean validateInputs(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            layoutEmail.setError("Ingresa tu correo institucional");
            editEmail.requestFocus();
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.setError("Correo electrónico inválido");
            editEmail.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            layoutPassword.setError("Ingresa tu contraseña");
            editPassword.requestFocus();
            return false;
        }
        if (password.length() < 6) {
            layoutPassword.setError("La contraseña debe tener al menos 6 caracteres");
            editPassword.requestFocus();
            return false;
        }
        return true;
    }

    private void showLoading(boolean show) {
        if (show) {
            progressLogin.setVisibility(View.VISIBLE);
            btnLogin.setEnabled(false);
            btnLogin.setText("Iniciando sesión...");
        } else {
            progressLogin.setVisibility(View.GONE);
            btnLogin.setEnabled(true);
            btnLogin.setText(R.string.login_button);
        }
    }

    private String getErrorMessage(Exception exception) {
        if (exception == null) return "Error desconocido. Intenta de nuevo.";
        String message = exception.getMessage();
        if (message == null) return "Error al iniciar sesión. Verifica tus datos.";

        if (message.contains("no user record")) {
            return "No existe una cuenta con este correo.";
        } else if (message.contains("password is invalid") || message.contains("credential is incorrect")) {
            return "Contraseña incorrecta. Intenta de nuevo.";
        } else if (message.contains("network error")) {
            return "Sin conexión a internet. Verifica tu red.";
        } else if (message.contains("too many requests")) {
            return "Demasiados intentos. Espera un momento.";
        } else {
            return "Error al iniciar sesión. Verifica tus datos.";
        }
    }
}