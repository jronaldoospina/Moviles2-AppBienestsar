package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.repositories.UserRepository;
import com.example.myapplication.services.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ForceChangePasswordActivity extends AppCompatActivity {

    private TextInputLayout layoutNueva, layoutConfirmar;
    private TextInputEditText editNueva, editConfirmar;
    private MaterialButton btnCambiar;
    private CircularProgressIndicator progressChange;

    private FirebaseAuth mAuth;
    private UserRepository userRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_force_change_password);

        mAuth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();
        sessionManager = new SessionManager(this);

        layoutNueva = findViewById(R.id.layout_nueva);
        layoutConfirmar = findViewById(R.id.layout_confirmar);
        editNueva = findViewById(R.id.edit_nueva);
        editConfirmar = findViewById(R.id.edit_confirmar);
        btnCambiar = findViewById(R.id.btn_cambiar);
        progressChange = findViewById(R.id.progress_change);

        btnCambiar.setOnClickListener(v -> attemptChange());
    }

    @Override
    public void onBackPressed() {
        // Bloqueado: no puede salir sin cambiar la contraseña
        Toast.makeText(this, "Debes cambiar tu contraseña para continuar", Toast.LENGTH_SHORT).show();
    }

    private void attemptChange() {
        layoutNueva.setError(null);
        layoutConfirmar.setError(null);

        String nueva = editNueva.getText() != null ? editNueva.getText().toString() : "";
        String confirmar = editConfirmar.getText() != null ? editConfirmar.getText().toString() : "";

        if (TextUtils.isEmpty(nueva)) {
            layoutNueva.setError("Ingresa la nueva contraseña");
            return;
        }
        if (nueva.length() < 6) {
            layoutNueva.setError("Mínimo 6 caracteres");
            return;
        }
        if (!nueva.equals(confirmar)) {
            layoutConfirmar.setError("Las contraseñas no coinciden");
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Sesión no válida. Vuelve a iniciar sesión.", Toast.LENGTH_SHORT).show();
            goToLogin();
            return;
        }

        showLoading(true);

        user.updatePassword(nueva)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        showLoading(false);
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Error al actualizar";
                        Toast.makeText(this, "Error: " + msg, Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Marcar en Firestore que ya no requiere cambio
                    userRepository.markPasswordChanged(user.getUid())
                            .addOnSuccessListener(aVoid -> {
                                showLoading(false);
                                Toast.makeText(this, "✅ Contraseña actualizada", Toast.LENGTH_LONG).show();
                                Intent i = new Intent(this, MainActivity.class);
                                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(i);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Toast.makeText(this, "Error Firestore: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                });
    }

    private void goToLogin() {
        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void showLoading(boolean show) {
        progressChange.setVisibility(show ? View.VISIBLE : View.GONE);
        btnCambiar.setEnabled(!show);
    }
}