package com.example.myapplication.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapters.AlertsAdapter;
import com.example.myapplication.models.Alerta;
import com.example.myapplication.repositories.AlertRepository;
import com.example.myapplication.repositories.AuditRepository;
import com.example.myapplication.services.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class AlertsFragment extends Fragment {

    private final List<Alerta> alertas = new ArrayList<>();
    private AlertsAdapter adapter;
    private AlertRepository repo;
    private SessionManager sessionManager;
    private ListenerRegistration registration;

    private RecyclerView recycler;
    private TextView txtEmpty;
    private ProgressBar progress;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alerts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recycler = view.findViewById(R.id.recycler_alertas);
        txtEmpty = view.findViewById(R.id.txt_alerts_empty);
        progress = view.findViewById(R.id.progress_alertas);

        repo = new AlertRepository();
        sessionManager = new SessionManager(requireContext());

        adapter = new AlertsAdapter();
        adapter.setOnAlertaClickListener(this::mostrarOpciones);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        progress.setVisibility(View.VISIBLE);
        registration = repo.escucharAlertas(this::onAlertasActualizadas);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    private void onAlertasActualizadas(@Nullable QuerySnapshot snapshots, @Nullable com.google.firebase.firestore.FirebaseFirestoreException e) {
        progress.setVisibility(View.GONE);
        if (e != null) {
            txtEmpty.setText("No se pudieron cargar las alertas.\n" + e.getMessage());
            txtEmpty.setVisibility(View.VISIBLE);
            return;
        }
        alertas.clear();
        if (snapshots != null) {
            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                Alerta a = doc.toObject(Alerta.class);
                if (a != null) {
                    a.setId(doc.getId());
                    alertas.add(a);
                }
            }
        }
        adapter.setData(alertas);
        txtEmpty.setVisibility(alertas.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void mostrarOpciones(Alerta alerta) {
        String[] opciones = alerta.getEstado() != null && alerta.getEstado().equals("ATENDIDA")
                ? new String[]{"Eliminar alerta"}
                : new String[]{"Marcar como atendida", "Eliminar alerta"};

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(alerta.getEstudianteNombre() != null
                        ? "Alerta de " + alerta.getEstudianteNombre()
                        : "Alerta")
                .setItems(opciones, (d, which) -> {
                    if (opciones[which].startsWith("Marcar")) {
                        atender(alerta);
                    } else {
                        eliminar(alerta);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void atender(Alerta alerta) {
        repo.atenderAlerta(alerta.getId(), sessionManager.getUserId())
                .addOnSuccessListener(v -> {
                    Toast.makeText(requireContext(),
                            "Alerta atendida", Toast.LENGTH_SHORT).show();
                    new AuditRepository().registrar("ALERTA_ATENDIDA",
                            "Atendió alerta de "
                                    + (alerta.getEstudianteNombre() != null
                                    ? alerta.getEstudianteNombre() : "-"),
                            sessionManager);
                })
                .addOnFailureListener(err -> Toast.makeText(requireContext(),
                        "Error: " + err.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void eliminar(Alerta alerta) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Eliminar alerta")
                .setMessage("¿Eliminar la alerta de " + alerta.getEstudianteNombre() + "?")
                .setPositiveButton("Eliminar", (d, which) ->
                        repo.deleteAlerta(alerta.getId())
                                .addOnSuccessListener(v -> {
                                    Toast.makeText(requireContext(),
                                            "Alerta eliminada", Toast.LENGTH_SHORT).show();
                                    new AuditRepository().registrar("ALERTA_ELIMINADA",
                                            "Eliminó alerta de "
                                                    + (alerta.getEstudianteNombre() != null
                                                    ? alerta.getEstudianteNombre() : "-"),
                                            sessionManager);
                                })
                                .addOnFailureListener(err -> Toast.makeText(requireContext(),
                                        "Error: " + err.getMessage(), Toast.LENGTH_LONG).show()))
                .setNegativeButton("Cancelar", null)
                .show();
    }
}