package com.example.myapplication.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.myapplication.LoginActivity;
import com.example.myapplication.R;
import com.example.myapplication.RegisterUserActivity;
import com.example.myapplication.databinding.FragmentProfileBinding;
import com.example.myapplication.models.Nota;
import com.example.myapplication.models.Usuario;
import com.example.myapplication.repositories.GradeRepository;
import com.example.myapplication.repositories.UserRepository;
import com.example.myapplication.services.SessionManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private SessionManager sessionManager;
    private UserRepository userRepository;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        userRepository = new UserRepository();
        mAuth = FirebaseAuth.getInstance();

        cargarDatos();
        setupBotones();
    }

    private void cargarDatos() {
        String uid = sessionManager.getUserId();
        if (uid == null) return;

        userRepository.getUserById(uid)
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    Usuario u = doc.toObject(Usuario.class);
                    if (u == null) return;
                    u.setUid(doc.getId());
                    pintarUsuario(u);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Error cargando perfil: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void pintarUsuario(Usuario u) {
        binding.txtNombre.setText(u.getNombre() != null ? u.getNombre() : "Sin nombre");
        binding.txtEmail.setText(u.getEmail() != null ? u.getEmail() : "");
        binding.txtRol.setText(u.getRol() != null ? u.getRol() : "SIN ROL");
        binding.txtIniciales.setText(u.getIniciales());
        binding.txtUsername.setText(u.getUsuario() != null ? u.getUsuario() : "-");
        binding.txtDocumento.setText(u.getDocumento() != null ? u.getDocumento() : "-");
        binding.txtTelefono.setText(u.getTelefono() != null ? u.getTelefono() : "-");

        // Si es estudiante cargamos notas para calcular promedio y nivel de riesgo
        if (SessionManager.ROLE_ESTUDIANTE.equals(u.getRol())) {
            cargarNotasPromedio(u.getUid());
        } else {
            // Ocultar layout de Promedio y Riesgo si no es estudiante
            binding.layoutPromedio.setVisibility(View.GONE);
            binding.layoutRiesgo.setVisibility(View.GONE);
        }
    }

    private void cargarNotasPromedio(String uid) {
        GradeRepository gradeRepo = new GradeRepository();
        // Cargar todas las notas del estudiante
        gradeRepo.getNotasByEstudiante(uid)
                .addOnSuccessListener(snap -> {
                    List<Double> valores = new ArrayList<>();
                    for (var doc : snap.getDocuments()) {
                        Nota n = doc.toObject(Nota.class);
                        if (n != null && n.getValor() > 0) {
                            valores.add(n.getValor());
                        }
                    }
                    if (!valores.isEmpty()) {
                        double suma = 0;
                        for (double v : valores) suma += v;
                        double promedio = suma / valores.size();

                        // Determinar nivel de riesgo
                        String nivelRiesgo;
                        if (promedio >= 4.0) nivelRiesgo = "BAJO";
                        else if (promedio >= 3.0) nivelRiesgo = "MEDIO";
                        else nivelRiesgo = "ALTO";

                        binding.txtPromedio.setText(String.format("Prom: %.1f", promedio));
                        binding.txtNivelRiesgo.setText(nivelRiesgo);

                        // Color según nivel
                        int color;
                        switch (nivelRiesgo) {
                            case "ALTO":  color = 0xFFD32F2F; break;
                            case "MEDIO": color = 0xFFF57F17; break;
                            default:      color = 0xFF388E3C; break;
                        }
                        binding.txtNivelRiesgo.setTextColor(color);
                        binding.layoutPromedio.setVisibility(View.VISIBLE);
                        binding.layoutRiesgo.setVisibility(View.VISIBLE);
                    } else {
                        binding.layoutPromedio.setVisibility(View.GONE);
                        binding.layoutRiesgo.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Error cargando notas: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void setupBotones() {
        binding.btnChangePhone.setOnClickListener(v -> showChangePhoneDialog());
        binding.btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        binding.btnLogout.setOnClickListener(v -> confirmarCerrarSesion());
    }

    private void confirmarCerrarSesion() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Cerrar Sesión")
                .setMessage("¿Estás seguro de que deseas cerrar sesión?")
                .setPositiveButton("Sí, cerrar", (d, w) -> cerrarSesion())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void cerrarSesion() {
        mAuth.signOut();
        sessionManager.logout();

        Intent i = new Intent(requireContext(), LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        requireActivity().finish();
    }
    private void showChangePhoneDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_change_phone, null);

        TextInputLayout layoutPhone = dialogView.findViewById(R.id.layout_phone);
        TextInputEditText editPhone = dialogView.findViewById(R.id.edit_phone);

        // Precargar teléfono actual
        String currentPhone = binding.txtTelefono.getText().toString();
        if (!currentPhone.equals("-")) {
            editPhone.setText(currentPhone);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Cambiar Teléfono")
                .setView(dialogView)
                .setPositiveButton("Guardar", null) // Lo seteamos después para controlar validación
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String nuevo = editPhone.getText() != null
                        ? editPhone.getText().toString().trim() : "";

                if (nuevo.isEmpty() || !nuevo.matches("\\d{7,15}")) {
                    layoutPhone.setError("Teléfono inválido (7-15 dígitos)");
                    return;
                }

                String uid = sessionManager.getUserId();
                if (uid == null) return;

                userRepository.updatePhone(uid, nuevo)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(requireContext(),
                                    "✅ Teléfono actualizado", Toast.LENGTH_SHORT).show();
                            binding.txtTelefono.setText(nuevo);
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(requireContext(),
                                    "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            });
        });

        dialog.show();
    }
    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_change_password, null);

        TextInputLayout layoutActual = dialogView.findViewById(R.id.layout_actual);
        TextInputLayout layoutNueva = dialogView.findViewById(R.id.layout_nueva);
        TextInputLayout layoutConfirmar = dialogView.findViewById(R.id.layout_confirmar);
        TextInputEditText editActual = dialogView.findViewById(R.id.edit_actual);
        TextInputEditText editNueva = dialogView.findViewById(R.id.edit_nueva);
        TextInputEditText editConfirmar = dialogView.findViewById(R.id.edit_confirmar);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Cambiar Contraseña")
                .setView(dialogView)
                .setPositiveButton("Cambiar", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                layoutActual.setError(null);
                layoutNueva.setError(null);
                layoutConfirmar.setError(null);

                String actual = editActual.getText() != null ? editActual.getText().toString() : "";
                String nueva = editNueva.getText() != null ? editNueva.getText().toString() : "";
                String confirmar = editConfirmar.getText() != null ? editConfirmar.getText().toString() : "";

                if (actual.isEmpty()) {
                    layoutActual.setError("Ingresa tu contraseña actual");
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
                if (nueva.equals(actual)) {
                    layoutNueva.setError("La nueva debe ser diferente a la actual");
                    return;
                }

                // Reautenticar con la contraseña actual y luego cambiar
                reautenticarYCambiar(actual, nueva, dialog);
            });
        });

        dialog.show();
    }

    private void reautenticarYCambiar(String passwordActual, String passwordNueva, AlertDialog dialog) {
        com.google.firebase.auth.FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(requireContext(), "Sesión no válida", Toast.LENGTH_SHORT).show();
            return;
        }

        com.google.firebase.auth.AuthCredential credential =
                com.google.firebase.auth.EmailAuthProvider.getCredential(user.getEmail(), passwordActual);

        user.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    // Ahora sí, cambiar contraseña
                    user.updatePassword(passwordNueva)
                            .addOnSuccessListener(aVoid2 -> {
                                // Actualizar Firestore
                                userRepository.markPasswordChanged(user.getUid());
                                Toast.makeText(requireContext(),
                                        "✅ Contraseña actualizada", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(requireContext(),
                                        "Error al cambiar: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(),
                            "Contraseña actual incorrecta",
                            Toast.LENGTH_LONG).show();
                });
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}