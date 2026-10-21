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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapters.AlertsAdapter;
import com.example.myapplication.models.Alerta;
import com.example.myapplication.repositories.AlertRepository;
import com.example.myapplication.repositories.AuditRepository;
import com.example.myapplication.services.SessionManager;
import com.example.myapplication.utils.UiUtils;
import com.example.myapplication.viewmodels.AlertsViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class AlertsFragment extends Fragment {

    private AlertsAdapter adapter;
    private AlertRepository repo;
    private SessionManager sessionManager;
    private AlertsViewModel viewModel;

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
        viewModel = new ViewModelProvider(this).get(AlertsViewModel.class);

        adapter = new AlertsAdapter();
        adapter.setOnAlertaClickListener(this::mostrarOpciones);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        observarViewModel();
    }

    private void observarViewModel() {
        viewModel.getAlertas().observe(getViewLifecycleOwner(), alertas -> {
            adapter.setData(alertas);
            txtEmpty.setVisibility(alertas.isEmpty() ? View.VISIBLE : View.GONE);
        });
        viewModel.getLoading().observe(getViewLifecycleOwner(), cargando ->
                progress.setVisibility(
                        Boolean.TRUE.equals(cargando) ? View.VISIBLE : View.GONE));
        viewModel.getError().observe(getViewLifecycleOwner(), mensaje -> {
            if (mensaje == null || !isAdded() || getView() == null) return;
            UiUtils.showRetry(requireView(), mensaje, viewModel::reintentar);
            viewModel.clearError();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        viewModel.empezarEscucha();
    }

    @Override
    public void onStop() {
        super.onStop();
        viewModel.detenerEscucha();
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