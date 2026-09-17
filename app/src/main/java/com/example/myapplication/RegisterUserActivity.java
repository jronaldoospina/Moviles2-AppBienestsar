package com.example.myapplication;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.models.Usuario;
import com.example.myapplication.repositories.AuditRepository;
import com.example.myapplication.repositories.UserRepository;
import com.example.myapplication.services.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class RegisterUserActivity extends AppCompatActivity {

    // Toolbar
    private MaterialToolbar toolbar;

    // Selector de rol
    private TextInputLayout layoutRol;
    private MaterialAutoCompleteTextView dropdownRol;

    // Campos básicos
    private TextInputLayout layoutNombre, layoutDocumento, layoutUsuario, layoutTelefono;
    private TextInputEditText editNombre, editDocumento, editUsuario, editTelefono;

    // Sección estudiante
    private View sectionEstudiante;
    private TextInputLayout layoutCodigo, layoutPrograma, layoutSemestre;
    private TextInputEditText editCodigo, editPrograma, editSemestre;

    // Sección docente
    private View sectionDocente;

    // Sección psicólogo
    private View sectionPsicologo;
    private TextInputLayout layoutEspecialidad;
    private TextInputEditText editEspecialidad;

    // Botones y progress
    private MaterialButton btnRegister, btnCancel;
    private CircularProgressIndicator progressRegister;

    // Firebase / repos
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private UserRepository userRepository;
    private SessionManager sessionManager;

    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_user);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userRepository = new UserRepository();
        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserId();

        // Validar permisos
        if (!SessionManager.ROLE_ADMIN.equals(sessionManager.getUserRole()) && 
            !SessionManager.ROLE_PSICOLOGO.equals(sessionManager.getUserRole())) {
            Toast.makeText(this, "No tienes permisos para registrar usuarios", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();
        setupRoleDropdown();
        setupRoleListener();
        setupButtons();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);

        layoutRol = findViewById(R.id.layout_rol);
        dropdownRol = findViewById(R.id.dropdown_rol);

        layoutNombre = findViewById(R.id.layout_nombre);
        layoutDocumento = findViewById(R.id.layout_documento);
        layoutUsuario = findViewById(R.id.layout_usuario);
        layoutTelefono = findViewById(R.id.layout_telefono);
        editNombre = findViewById(R.id.edit_nombre);
        editDocumento = findViewById(R.id.edit_documento);
        editUsuario = findViewById(R.id.edit_usuario);
        editTelefono = findViewById(R.id.edit_telefono);

        sectionEstudiante = findViewById(R.id.section_estudiante);
        layoutCodigo = findViewById(R.id.layout_codigo);
        layoutPrograma = findViewById(R.id.layout_programa);
        layoutSemestre = findViewById(R.id.layout_semestre);
        editCodigo = findViewById(R.id.edit_codigo);
        editPrograma = findViewById(R.id.edit_programa);
        editSemestre = findViewById(R.id.edit_semestre);

        sectionDocente = findViewById(R.id.section_docente);

        sectionPsicologo = findViewById(R.id.section_psicologo);
        layoutEspecialidad = findViewById(R.id.layout_especialidad);
        editEspecialidad = findViewById(R.id.edit_especialidad);

        btnRegister = findViewById(R.id.btn_register);
        btnCancel = findViewById(R.id.btn_cancel);
        progressRegister = findViewById(R.id.progress_register);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRoleDropdown() {
        List<String> roles = new ArrayList<>();

        if (SessionManager.ROLE_ADMIN.equals(sessionManager.getUserRole())) {
            roles.add("ESTUDIANTE");
            roles.add("DOCENTE");
            roles.add("PSICOLOGO");
            roles.add("ADMIN");
        } else if (SessionManager.ROLE_PSICOLOGO.equals(sessionManager.getUserRole())) {
            // Psicólogo solo puede registrar estudiantes
            roles.add("ESTUDIANTE");
            layoutRol.setEnabled(false);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, roles);
        dropdownRol.setAdapter(adapter);

        // Seleccionar el primero por defecto (sin abrir el dropdown)
        dropdownRol.setText(roles.get(0), false);

        // Estado inicial de secciones
        updateSectionsVisibility(roles.get(0));
    }

    private void setupRoleListener() {
        dropdownRol.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            updateSectionsVisibility(selected);
        });
    }

    private void updateSectionsVisibility(String role) {
        sectionEstudiante.setVisibility(View.GONE);
        sectionDocente.setVisibility(View.GONE);
        sectionPsicologo.setVisibility(View.GONE);

        if (role == null) return;
        switch (role) {
            case "ESTUDIANTE": sectionEstudiante.setVisibility(View.VISIBLE); break;
            case "DOCENTE":    sectionDocente.setVisibility(View.VISIBLE);    break;
            case "PSICOLOGO":  sectionPsicologo.setVisibility(View.VISIBLE);  break;
            case "ADMIN":      /* sin sección */                              break;
        }
    }

    private void setupButtons() {
        btnRegister.setOnClickListener(v -> attemptRegister());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void clearErrors() {
        layoutNombre.setError(null);
        layoutDocumento.setError(null);
        layoutUsuario.setError(null);
        layoutTelefono.setError(null);
        layoutCodigo.setError(null);
        layoutPrograma.setError(null);
        layoutSemestre.setError(null);
        layoutEspecialidad.setError(null);
    }

    private void attemptRegister() {
        clearErrors();

        String nombre = editNombre.getText() != null ? editNombre.getText().toString().trim() : "";
        String documento = editDocumento.getText() != null ? editDocumento.getText().toString().trim() : "";
        String usuario = editUsuario.getText() != null ? editUsuario.getText().toString().trim().toLowerCase() : "";
        String telefono = editTelefono.getText() != null ? editTelefono.getText().toString().trim() : "";
        String rol = dropdownRol.getText().toString().trim();

        // Validaciones básicas
        if (TextUtils.isEmpty(nombre)) {
            layoutNombre.setError("Ingresa nombres y apellidos");
            editNombre.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(documento) || !documento.matches("\\d{6,15}")) {
            layoutDocumento.setError("Documento inválido (solo números, 6-15 dígitos)");
            editDocumento.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(usuario) || !usuario.matches("[a-z0-9._]{3,30}")) {
            layoutUsuario.setError("Usuario inválido (mín. 3, solo letras/números/._ )");
            editUsuario.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(telefono) || !telefono.matches("\\d{7,15}")) {
            layoutTelefono.setError("Teléfono inválido");
            editTelefono.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(rol)) {
            Toast.makeText(this, "Selecciona un rol", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validaciones específicas por rol
        String codigo = null, programa = null, especialidad = null;
        Integer semestre = null;

        if ("ESTUDIANTE".equals(rol)) {
            codigo = editCodigo.getText() != null ? editCodigo.getText().toString().trim() : "";
            programa = editPrograma.getText() != null ? editPrograma.getText().toString().trim() : "";
            String semestreStr = editSemestre.getText() != null ? editSemestre.getText().toString().trim() : "";

            if (TextUtils.isEmpty(codigo)) {
                layoutCodigo.setError("Ingresa el código estudiantil");
                editCodigo.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(programa)) {
                layoutPrograma.setError("Ingresa el programa");
                editPrograma.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(semestreStr)) {
                layoutSemestre.setError("Ingresa el semestre");
                editSemestre.requestFocus();
                return;
            }
            try {
                semestre = Integer.parseInt(semestreStr);
                if (semestre < 1 || semestre > 15) {
                    layoutSemestre.setError("Semestre entre 1 y 15");
                    editSemestre.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                layoutSemestre.setError("Semestre inválido");
                editSemestre.requestFocus();
                return;
            }
        } else if ("PSICOLOGO".equals(rol)) {
            especialidad = editEspecialidad.getText() != null ? editEspecialidad.getText().toString().trim() : "";
        }

        // Guardar valores finales para callbacks
        final String fNombre = nombre;
        final String fDocumento = documento;
        final String fUsuario = usuario;
        final String fTelefono = telefono;
        final String fRol = rol;
        final String fCodigo = codigo;
        final String fPrograma = programa;
        final String fEspecialidad = especialidad;
        final Integer fSemestre = semestre;

        showLoading(true);

        // 1. Verificar documento único
        userRepository.checkDocumentoExists(documento)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        showLoading(false);
                        layoutDocumento.setError("Este documento ya está registrado");
                        editDocumento.requestFocus();
                        return;
                    }
                    // 2. Verificar usuario único
                    userRepository.checkUsuarioExists(usuario)
                            .addOnSuccessListener(queryUserSnapshots -> {
                                if (queryUserSnapshots != null && !queryUserSnapshots.isEmpty()) {
                                    showLoading(false);
                                    layoutUsuario.setError("Este usuario ya está en uso");
                                    editUsuario.requestFocus();
                                    return;
                                }
                                // 3. Crear en Auth + Firestore
                                createAuthUser(fUsuario, fDocumento, fNombre, fTelefono, fRol,
                                        fCodigo, fPrograma, fSemestre, fEspecialidad);
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Toast.makeText(this, "Error verificando usuario: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error verificando documento: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void createAuthUser(String usuario, String documento, String nombre,
                                String telefono, String rol, String codigo,
                                String programa, Integer semestre, String especialidad) {

        String email = usuario + "@unicesar.edu.co";
        String password = documento; // Contraseña inicial = documento

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        showLoading(false);
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Error creando usuario";
                        Toast.makeText(this, "Error Auth: " + msg, Toast.LENGTH_LONG).show();
                        return;
                    }

                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    if (firebaseUser == null) {
                        showLoading(false);
                        Toast.makeText(this, "Error: usuario nulo", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String uid = firebaseUser.getUid();

                    // Crear modelo Usuario
                    Usuario nuevo = new Usuario();
                    nuevo.setUid(uid);
                    nuevo.setEmail(email);
                    nuevo.setUsuario(usuario);
                    nuevo.setDocumento(documento);
                    nuevo.setNombre(nombre);
                    nuevo.setRol(rol);
                    nuevo.setActivo(true);
                    nuevo.setTelefono(telefono);
                    nuevo.setFotoUrl("");
                    nuevo.setFechaRegistro(System.currentTimeMillis());
                    nuevo.setRequiereCambioContrasena(true);
                    nuevo.setUltimoCambioContrasena(0);
                    nuevo.setCreadoPor(currentUserId);
                    nuevo.setFechaCreacion(System.currentTimeMillis());
                    nuevo.setUltimaActualizacion(System.currentTimeMillis());

                    if ("ESTUDIANTE".equals(rol)) {
                        nuevo.setCodigo(codigo);
                        nuevo.setPrograma(programa);
                        nuevo.setSemestre(semestre != null ? semestre : 0);
                        nuevo.setNivelRiesgo("BAJO");
                    } else if ("PSICOLOGO".equals(rol)) {
                        nuevo.setEspecialidad(especialidad != null ? especialidad : "");
                    }

                    userRepository.createUser(uid, nuevo)
                            .addOnSuccessListener(aVoid -> {
                                showLoading(false);
                                new AuditRepository().registrar("USUARIO_CREADO",
                                        "Registró a " + nombre + " (" + rol + ")",
                                        sessionManager);
                                showSuccessDialog(email, password);
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Toast.makeText(this, "Error Firestore: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                });
    }

    private void showSuccessDialog(String email, String password) {
        new AlertDialog.Builder(this)
                .setTitle("✅ Usuario Registrado")
                .setMessage("El usuario fue creado exitosamente.\n\n" +
                        "📧 Correo: " + email + "\n" +
                        "🔑 Contraseña inicial: " + password + "\n\n" +
                        "El usuario deberá cambiar su contraseña en el primer inicio de sesión.")
                .setPositiveButton("Aceptar", (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void showLoading(boolean show) {
        progressRegister.setVisibility(show ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!show);
        btnCancel.setEnabled(!show);
    }
}