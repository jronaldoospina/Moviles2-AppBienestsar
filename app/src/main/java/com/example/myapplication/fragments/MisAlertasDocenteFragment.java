package com.example.myapplication.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapters.AlertsAdapter;
import com.example.myapplication.models.Alerta;
import com.example.myapplication.services.SessionManager;
import com.example.myapplication.utils.FechaUtils;
import com.example.myapplication.utils.UiUtils;
import com.example.myapplication.viewmodels.TeacherAlertsViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MisAlertasDocenteFragment extends Fragment {

    private AlertsAdapter adapter;
    private SessionManager sessionManager;
    private TeacherAlertsViewModel viewModel;

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

        sessionManager = new SessionManager(requireContext());
        viewModel = new ViewModelProvider(this).get(TeacherAlertsViewModel.class);

        adapter = new AlertsAdapter();
        adapter.setOnAlertaClickListener(this::mostrarDetalle);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        observarViewModel();
    }

    private void observarViewModel() {
        viewModel.getAlertas().observe(getViewLifecycleOwner(), alertas -> {
            adapter.setData(alertas);
            txtEmpty.setText(R.string.mis_alertas_vacio);
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
        viewModel.empezarEscucha(sessionManager.getUserId());
    }

    @Override
    public void onStop() {
        super.onStop();
        viewModel.detenerEscucha();
    }

    private void mostrarDetalle(Alerta a) {
        StringBuilder msg = new StringBuilder();
        msg.append("Estudiante: ").append(a.getEstudianteNombre() != null
                ? a.getEstudianteNombre() : "-").append("\n");
        msg.append("Curso: ").append(a.getCursoNombre() != null ? a.getCursoNombre() : "-")
                .append("\n");
        msg.append("Motivo: ").append(motivoLabel(a.getMotivo())).append("\n");
        if (a.getDescripcion() != null && !a.getDescripcion().isEmpty()) {
            msg.append("Descripción: ").append(a.getDescripcion()).append("\n");
        }
        msg.append("Enviada: ").append(a.getFecha() > 0
                ? FechaUtils.fechaBonitaLong(a.getFecha()) : "-").append("\n");

        String estado = a.getEstado() != null ? a.getEstado() : "PENDIENTE";
        if (!"PENDIENTE".equals(estado) && a.getFechaAtencion() > 0) {
            msg.append("Estado: ").append(estado)
                    .append(" · ").append(FechaUtils.fechaBonitaLong(a.getFechaAtencion()));
        } else {
            msg.append("Estado: ").append(estado);
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Alerta de " + a.getEstudianteNombre())
                .setMessage(msg)
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private String motivoLabel(String motivo) {
        if (motivo == null) return "";
        switch (motivo) {
            case "INASISTENCIA":   return "Inasistencia";
            case "COMPORTAMIENTO": return "Comportamiento";
            case "OTRO":           return "Otro";
            default:               return "Bajo rendimiento";
        }
    }
}