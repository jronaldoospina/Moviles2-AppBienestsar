package com.example.myapplication.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapplication.R;
import com.example.myapplication.RegisterUserActivity;
import com.example.myapplication.UserDetailActivity;
import com.example.myapplication.adapters.UsersAdapter;
import com.example.myapplication.databinding.FragmentUsersBinding;
import com.example.myapplication.models.Usuario;
import com.example.myapplication.repositories.AuditRepository;
import com.example.myapplication.repositories.UserRepository;
import com.example.myapplication.services.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class UsersFragment extends Fragment {

    private static final String[] ROLES = {
            SessionManager.ROLE_ADMIN,
            SessionManager.ROLE_PSICOLOGO,
            SessionManager.ROLE_DOCENTE,
            SessionManager.ROLE_ESTUDIANTE
    };
    private static final String[] ROLES_LABEL = {"Administrador", "Psicólogo", "Docente", "Estudiante"};

    private FragmentUsersBinding binding;
    private UserRepository userRepository;
    private SessionManager sessionManager;
    private UsersAdapter adapter;

    private String rolActual = null;    // filtro actual (null = todos)
    private String queryActual = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentUsersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userRepository = new UserRepository();
        sessionManager = new SessionManager(requireContext());

        configurarRecycler();
        configurarBuscador();
        configurarChips();
        configurarFAB();

        cargarUsuarios();
    }

    private void configurarRecycler() {
        adapter = new UsersAdapter(
                requireContext(),
                usuario -> {
                    Intent i = new Intent(requireContext(), UserDetailActivity.class);
                    i.putExtra(UserDetailActivity.EXTRA_USER_ID, usuario.getUid());
                    startActivity(i);
                },
                (usuario, anchor) -> mostrarMenuOpciones(usuario, anchor)
        );
        binding.recyclerUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerUsers.setAdapter(adapter);
    }

    private void configurarBuscador() {
        binding.editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                queryActual = s.toString();
                aplicarFiltros();
            }
        });
    }

    private void configurarChips() {
        binding.chipGroupRoles.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                rolActual = null;
            } else {
                int id = checkedIds.get(0);
                if (id == R.id.chip_admin) rolActual = SessionManager.ROLE_ADMIN;
                else if (id == R.id.chip_psicologo) rolActual = SessionManager.ROLE_PSICOLOGO;
                else if (id == R.id.chip_docente) rolActual = SessionManager.ROLE_DOCENTE;
                else if (id == R.id.chip_estudiante) rolActual = SessionManager.ROLE_ESTUDIANTE;
                else rolActual = null;
            }
            aplicarFiltros();
        });
    }

    private void configurarFAB() {
        binding.fabAddUser.setOnClickListener(v -> {
            Intent i = new Intent(requireContext(), RegisterUserActivity.class);
            startActivity(i);
        });
    }

    private void aplicarFiltros() {
        adapter.aplicarFiltro(rolActual, queryActual);
        int total = adapter.getTotalFiltrado();
        binding.txtContador.setText(total + (total == 1 ? " usuario" : " usuarios"));
        binding.layoutEmpty.setVisibility(total == 0 ? View.VISIBLE : View.GONE);
    }

    private void cargarUsuarios() {
        binding.progressUsers.setVisibility(View.VISIBLE);

        userRepository.getAllUsers()
                .addOnSuccessListener(snapshot -> {
                    binding.progressUsers.setVisibility(View.GONE);

                    List<Usuario> lista = new ArrayList<>();
                    snapshot.forEach(doc -> {
                        Usuario u = doc.toObject(Usuario.class);
                        if (u != null) {
                            u.setUid(doc.getId());
                            lista.add(u);
                        }
                    });

                    adapter.setUsuarios(lista);
                    aplicarFiltros();
                })
                .addOnFailureListener(e -> {
                    binding.progressUsers.setVisibility(View.GONE);
                    Toast.makeText(requireContext(),
                            "Error cargando usuarios: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Click corto en la tarjeta → detalle del usuario.
     */
    private void mostrarOpcionesUsuario(Usuario usuario) {
        StringBuilder detalle = new StringBuilder();
        detalle.append("Rol: ").append(rolLabel(usuario.getRol())).append("\n");
        if (usuario.getEmail() != null && !usuario.getEmail().isEmpty()) {
            detalle.append("Correo: ").append(usuario.getEmail()).append("\n");
        }
        if (usuario.getDocumento() != null && !usuario.getDocumento().isEmpty()) {
            detalle.append("Documento: ").append(usuario.getDocumento()).append("\n");
        }
        if (usuario.getTelefono() != null && !usuario.getTelefono().isEmpty()) {
            detalle.append("Teléfono: ").append(usuario.getTelefono()).append("\n");
        }
        if (SessionManager.ROLE_ESTUDIANTE.equals(usuario.getRol())) {
            if (usuario.getCodigo() != null && !usuario.getCodigo().isEmpty()) {
                detalle.append("Código: ").append(usuario.getCodigo()).append("\n");
            }
            if (usuario.getPrograma() != null && !usuario.getPrograma().isEmpty()) {
                detalle.append("Programa: ").append(usuario.getPrograma()).append("\n");
            }
        } else if (SessionManager.ROLE_PSICOLOGO.equals(usuario.getRol())
                && usuario.getEspecialidad() != null && !usuario.getEspecialidad().isEmpty()) {
            detalle.append("Especialidad: ").append(usuario.getEspecialidad()).append("\n");
        }
        detalle.append("Estado: ").append(usuario.isActivo() ? "Activo" : "Inactivo").append("\n");
        if (usuario.getFechaRegistro() > 0) {
            detalle.append("Registro: ").append(
                    com.example.myapplication.utils.FechaUtils.fechaBonitaLong(usuario.getFechaRegistro()));
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(usuario.getNombre() != null ? usuario.getNombre() : "Usuario")
                .setMessage(detalle)
                .setPositiveButton("Cambiar rol", (d, w) -> cambiarRol(usuario))
                .setNeutralButton("Activar / Desactivar", (d, w) -> toggleActivo(usuario))
                .setNegativeButton("Cerrar", null)
                .show();
    }

    private void cambiarRol(Usuario usuario) {
        String miUid = sessionManager.getUserId();
        if (miUid != null && miUid.equals(usuario.getUid())) {
            Toast.makeText(requireContext(), "No puedes cambiar tu propio rol",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        int seleccionado = 0;
        for (int i = 0; i < ROLES.length; i++) {
            if (ROLES[i].equals(usuario.getRol())) {
                seleccionado = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cambiar rol de " + (usuario.getNombre() != null ? usuario.getNombre() : "-"))
                .setSingleChoiceItems(ROLES_LABEL, seleccionado, (d, which) -> {
                    String nuevoRol = ROLES[which];
                    java.util.Map<String, Object> updates = new java.util.HashMap<>();
                    updates.put("rol", nuevoRol);
                    userRepository.updateUser(usuario.getUid(), updates)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(requireContext(),
                                        "Rol actualizado a " + ROLES_LABEL[which],
                                        Toast.LENGTH_SHORT).show();
                                new AuditRepository().registrar(
                                        "USUARIO_ROL_CAMBIADO",
                                        "Cambió el rol de " + (usuario.getNombre() != null ? usuario.getNombre() : "-")
                                                + " a " + ROLES_LABEL[which],
                                        sessionManager);
                                cargarUsuarios();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(requireContext(),
                                            "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    d.dismiss();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private String rolLabel(String rol) {
        for (int i = 0; i < ROLES.length; i++) {
            if (ROLES[i].equals(rol)) return ROLES_LABEL[i];
        }
        return rol != null ? rol : "-";
    }

    /**
     * Click en los 3 puntitos → menú contextual.
     */
    private void mostrarMenuOpciones(Usuario usuario, View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenu().add(0, 1, 0, "Ver detalles");
        popup.getMenu().add(0, 2, 1, "Activar / Desactivar");
        popup.getMenu().add(0, 3, 2, "Cambiar rol");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    mostrarOpcionesUsuario(usuario);
                    return true;
                case 2:
                    toggleActivo(usuario);
                    return true;
                case 3:
                    cambiarRol(usuario);
                    return true;
                default:
                    return false;
            }
        });
        popup.show();
    }

    private void toggleActivo(Usuario usuario) {
        boolean nuevoEstado = !usuario.isActivo();
        userRepository.setUserActive(usuario.getUid(), nuevoEstado)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(),
                            nuevoEstado ? "Usuario activado" : "Usuario desactivado",
                            Toast.LENGTH_SHORT).show();
                    new AuditRepository().registrar(
                            nuevoEstado ? "USUARIO_ACTIVADO" : "USUARIO_DESACTIVADO",
                            "Cambió estado de " + (usuario.getNombre() != null ? usuario.getNombre() : "-")
                                    + " (" + (usuario.getRol() != null ? usuario.getRol() : "-") + ")",
                            sessionManager);
                    cargarUsuarios();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    @Override
    public void onResume() {
        super.onResume();
        // Recargar al volver de RegisterUserActivity
        cargarUsuarios();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}