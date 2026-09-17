package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.databinding.ActivityUserDetailBinding;
import com.example.myapplication.models.Usuario;
import com.example.myapplication.repositories.AuditRepository;
import com.example.myapplication.repositories.UserRepository;
import com.example.myapplication.services.SessionManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserDetailActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "user_id";

    private ActivityUserDetailBinding binding;
    private UserRepository userRepository;
    private SessionManager sessionManager;
    private String userId;
    private Usuario usuarioActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        userRepository = new UserRepository();
        sessionManager = new SessionManager(this);

        userId = getIntent().getStringExtra(EXTRA_USER_ID);
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Usuario no especificado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.toolbar.setTitle("Detalle de Usuario");

        cargarUsuario();

        // Botones solo para Admin
        boolean esAdmin = SessionManager.ROLE_ADMIN.equals(sessionManager.getUserRole());
        binding.btnEditarUsuario.setVisibility(esAdmin ? View.VISIBLE : View.GONE);
        binding.btnResetPassword.setVisibility(esAdmin ? View.VISIBLE : View.GONE);
        binding.btnToggleActive.setVisibility(esAdmin ? View.VISIBLE : View.GONE);

        binding.btnEditarUsuario.setOnClickListener(v -> mostrarDialogoEditar());
        binding.btnResetPassword.setOnClickListener(v -> confirmarResetPassword());
        binding.btnToggleActive.setOnClickListener(v -> confirmarToggleActivo());
    }

    private void cargarUsuario() {
        userRepository.getUserById(userId)
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Usuario no encontrado", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    usuarioActual = doc.toObject(Usuario.class);
                    if (usuarioActual != null) {
                        usuarioActual.setUid(doc.getId());
                        pintarUsuario();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void pintarUsuario() {
        if (usuarioActual == null) return;

        binding.txtIniciales.setText(usuarioActual.getIniciales());
        binding.txtNombre.setText(usuarioActual.getNombre());
        binding.txtEmail.setText(usuarioActual.getEmail());
        binding.txtRol.setText(usuarioActual.getRol());
        binding.txtUsuario.setText(usuarioActual.getUsuario());
        binding.txtDocumento.setText(usuarioActual.getDocumento());
        binding.txtTelefono.setText(usuarioActual.getTelefono());

        // Estado
        if (usuarioActual.isActivo()) {
            binding.txtEstado.setText("ACTIVO");
            binding.txtEstado.setTextColor(getColor(R.color.success_text));
            binding.btnToggleActive.setText("Desactivar Usuario");
        } else {
            binding.txtEstado.setText("INACTIVO");
            binding.txtEstado.setTextColor(getColor(R.color.risk_high_text));
            binding.btnToggleActive.setText("Activar Usuario");
        }

        // Cambio de contraseña pendiente
        if (usuarioActual.isRequiereCambioContrasena()) {
            binding.txtRequiereCambio.setVisibility(View.VISIBLE);
        } else {
            binding.txtRequiereCambio.setVisibility(View.GONE);
        }

        // Campos específicos de estudiante
        if ("ESTUDIANTE".equals(usuarioActual.getRol())) {
            binding.cardEstudiante.setVisibility(View.VISIBLE);
            binding.txtCodigo.setText(usuarioActual.getCodigo() != null ? usuarioActual.getCodigo() : "-");
            binding.txtPrograma.setText(usuarioActual.getPrograma() != null ? usuarioActual.getPrograma() : "-");
            binding.txtSemestre.setText(String.valueOf(usuarioActual.getSemestre()));
            binding.txtNivelRiesgo.setText(usuarioActual.getNivelRiesgo() != null ? usuarioActual.getNivelRiesgo() : "-");
        } else {
            binding.cardEstudiante.setVisibility(View.GONE);
        }
    }

    private void mostrarDialogoEditar() {
        if (usuarioActual == null) return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_user, null);
        
        TextInputEditText editNombre = dialogView.findViewById(R.id.edit_nombre);
        TextInputEditText editDocumento = dialogView.findViewById(R.id.edit_documento);
        TextInputEditText editEmail = dialogView.findViewById(R.id.edit_email);
        TextInputEditText editTelefono = dialogView.findViewById(R.id.edit_telefono);
        
        LinearLayout layoutEstudiante = dialogView.findViewById(R.id.layout_estudiante_fields);
        TextInputEditText editCodigo = dialogView.findViewById(R.id.edit_codigo);
        TextInputEditText editPrograma = dialogView.findViewById(R.id.edit_programa);
        TextInputEditText editSemestre = dialogView.findViewById(R.id.edit_semestre);
        
        TextInputLayout layoutEspecialidad = dialogView.findViewById(R.id.layout_especialidad);
        TextInputEditText editEspecialidad = dialogView.findViewById(R.id.edit_especialidad);

        // Prellenar datos
        editNombre.setText(usuarioActual.getNombre() != null ? usuarioActual.getNombre() : "");
        editDocumento.setText(usuarioActual.getDocumento() != null ? usuarioActual.getDocumento() : "");
        editEmail.setText(usuarioActual.getEmail() != null ? usuarioActual.getEmail() : "");
        editTelefono.setText(usuarioActual.getTelefono() != null ? usuarioActual.getTelefono() : "");

        if (SessionManager.ROLE_ESTUDIANTE.equals(usuarioActual.getRol())) {
            layoutEstudiante.setVisibility(View.VISIBLE);
            editCodigo.setText(usuarioActual.getCodigo() != null ? usuarioActual.getCodigo() : "");
            editPrograma.setText(usuarioActual.getPrograma() != null ? usuarioActual.getPrograma() : "");
            editSemestre.setText(String.valueOf(usuarioActual.getSemestre()));
        } else if (SessionManager.ROLE_PSICOLOGO.equals(usuarioActual.getRol())) {
            layoutEspecialidad.setVisibility(View.VISIBLE);
            editEspecialidad.setText(usuarioActual.getEspecialidad() != null ? usuarioActual.getEspecialidad() : "");
        }

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String nuevoNombre = editNombre.getText() != null ? editNombre.getText().toString().trim() : "";
                    String nuevoDoc = editDocumento.getText() != null ? editDocumento.getText().toString().trim() : "";
                    String nuevoEmail = editEmail.getText() != null ? editEmail.getText().toString().trim() : "";
                    String nuevoTel = editTelefono.getText() != null ? editTelefono.getText().toString().trim() : "";
                    
                    if (nuevoNombre.isEmpty() || nuevoDoc.isEmpty() || nuevoEmail.isEmpty()) {
                        Toast.makeText(this, "Nombre, documento y correo son obligatorios", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    usuarioActual.setNombre(nuevoNombre);
                    usuarioActual.setDocumento(nuevoDoc);
                    usuarioActual.setEmail(nuevoEmail);
                    usuarioActual.setUsuario(nuevoEmail.split("@")[0]);
                    usuarioActual.setTelefono(nuevoTel);

                    if (SessionManager.ROLE_ESTUDIANTE.equals(usuarioActual.getRol())) {
                        usuarioActual.setCodigo(editCodigo.getText().toString().trim());
                        usuarioActual.setPrograma(editPrograma.getText().toString().trim());
                        String semStr = editSemestre.getText().toString().trim();
                        usuarioActual.setSemestre(semStr.isEmpty() ? 1 : Integer.parseInt(semStr));
                    } else if (SessionManager.ROLE_PSICOLOGO.equals(usuarioActual.getRol())) {
                        usuarioActual.setEspecialidad(editEspecialidad.getText().toString().trim());
                    }

                    guardarCambiosUsuario();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void guardarCambiosUsuario() {
        FirebaseFirestore.getInstance().collection("usuarios").document(usuarioActual.getUid())
                .set(usuarioActual)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Información actualizada", Toast.LENGTH_SHORT).show();
                    pintarUsuario();
                    new AuditRepository().registrar(
                            "USUARIO_EDITADO",
                            "Se editó la información de " + usuarioActual.getNombre(),
                            sessionManager);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void confirmarResetPassword() {
        if (usuarioActual == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Resetear Contraseña")
                .setMessage("Se reseteará la contraseña de " + usuarioActual.getNombre() +
                        " a su número de documento: " + usuarioActual.getDocumento() +
                        "\n\nEl usuario deberá cambiarla en su próximo inicio de sesión.")
                .setPositiveButton("Resetear", (d, w) -> resetearPassword())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void resetearPassword() {
        if (usuarioActual == null) return;

        // NOTA: Firebase Auth no permite cambiar la contraseña de OTRO usuario
        // desde el cliente. Esto requiere Admin SDK (Cloud Function).
        // Por ahora, solo marcamos requiereCambioContrasena = true.
        // Y avisamos al admin que debe resetearla manualmente desde Firebase Console.

        userRepository.resetPasswordRequirement(usuarioActual.getUid())
                .addOnSuccessListener(aVoid -> {
                    new AlertDialog.Builder(this)
                            .setTitle("✅ Marcado para cambio")
                            .setMessage("El usuario ha sido marcado para cambiar contraseña.\n\n" +
                                    "⚠️ IMPORTANTE: Firebase no permite resetear contraseñas de otros usuarios " +
                                    "desde la app. Debes hacerlo manualmente desde Firebase Console → Authentication.")
                            .setPositiveButton("Entendido", null)
                            .show();
                    usuarioActual.setRequiereCambioContrasena(true);
                    binding.txtRequiereCambio.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void confirmarToggleActivo() {
        if (usuarioActual == null) return;

        boolean esActivo = usuarioActual.isActivo();
        String accion = esActivo ? "desactivar" : "activar";

        new AlertDialog.Builder(this)
                .setTitle((esActivo ? "Desactivar" : "Activar") + " Usuario")
                .setMessage("¿Seguro que deseas " + accion + " a " + usuarioActual.getNombre() + "?")
                .setPositiveButton("Sí", (d, w) -> toggleActivo())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void toggleActivo() {
        if (usuarioActual == null) return;

        boolean nuevoEstado = !usuarioActual.isActivo();
        userRepository.setUserActive(usuarioActual.getUid(), nuevoEstado)
                .addOnSuccessListener(aVoid -> {
                    usuarioActual.setActivo(nuevoEstado);
                    pintarUsuario();
                    Toast.makeText(this,
                            "Usuario " + (nuevoEstado ? "activado" : "desactivado"),
                            Toast.LENGTH_SHORT).show();
                    new AuditRepository().registrar(
                            nuevoEstado ? "USUARIO_ACTIVADO" : "USUARIO_DESACTIVADO",
                            "Cambió estado de " + (usuarioActual.getNombre() != null
                                    ? usuarioActual.getNombre() : "-")
                                    + " (" + (usuarioActual.getRol() != null
                                    ? usuarioActual.getRol() : "-") + ")",
                            sessionManager);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}