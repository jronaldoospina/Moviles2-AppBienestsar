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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapters.AlertsAdapter;
import com.example.myapplication.models.Alerta;
import com.example.myapplication.services.SessionManager;
import com.example.myapplication.utils.FechaUtils;
import com.example.myapplication.utils.UiUtils;
import com.example.myapplication.viewmodels.TeacherDashboardViewModel;

public class TeacherDashboardFragment extends Fragment {

    private AlertsAdapter alertsAdapter;

    private TextView textDate;
    private TextView textKpi1Value, textKpi2Value, textKpi3Value, textKpi4Value;
    private ProgressBar progressKpis;

    private SessionManager sessionManager;
    private TeacherDashboardViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textDate = view.findViewById(R.id.text_date);
        textKpi1Value = view.findViewById(R.id.text_risk_count);
        textKpi2Value = view.findViewById(R.id.text_appointments_today);
        textKpi3Value = view.findViewById(R.id.text_total_students);
        textKpi4Value = view.findViewById(R.id.text_improvement);
        progressKpis = view.findViewById(R.id.progress_kpis);

        TextView txtKpi1Label = view.findViewById(R.id.txt_kpi1_label);
        txtKpi1Label.setText(R.string.teacher_mis_cursos);
        TextView txtKpi2Label = view.findViewById(R.id.txt_kpi2_label);
        txtKpi2Label.setText(R.string.teacher_tutorias_activas);

        TextView txtKpi4Label = view.findViewById(R.id.txt_kpi4_label);
        if (txtKpi4Label != null) {
            txtKpi4Label.setText(R.string.dashboard_atendidas);
        }

        TextView txtKpi3Label = view.findViewById(R.id.txt_kpi3_label);
        if (txtKpi3Label != null) {
            txtKpi3Label.setText(R.string.teacher_estudiantes);
        }

        TextView txtAaEstudiantes = view.findViewById(R.id.txt_aa_estudiantes);
        if (txtAaEstudiantes != null) txtAaEstudiantes.setText(R.string.teacher_cursos);
        TextView txtAaCita = view.findViewById(R.id.txt_aa_cita);
        if (txtAaCita != null) txtAaCita.setText(R.string.teacher_tutorias);
        TextView txtAaAlertas = view.findViewById(R.id.txt_aa_alertas);
        if (txtAaAlertas != null) txtAaAlertas.setText(R.string.teacher_alertas);

        RecyclerView recyclerAlertas = view.findViewById(R.id.recycler_recent_alerts);
        alertsAdapter = new AlertsAdapter();
        recyclerAlertas.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerAlertas.setAdapter(alertsAdapter);
        alertsAdapter.setOnAlertaClickListener(this::mostrarDetalle);

        sessionManager = new SessionManager(requireContext());
        viewModel = new ViewModelProvider(this).get(TeacherDashboardViewModel.class);

        pintarSaludo();
        configurarAccesosRapidos(view);
        observarViewModel();
    }

    private void observarViewModel() {
        viewModel.getResumen().observe(getViewLifecycleOwner(), resumen -> {
            if (textKpi1Value != null) textKpi1Value.setText(String.valueOf(resumen.cursos));
            if (textKpi2Value != null) textKpi2Value.setText(String.valueOf(resumen.tutoriasActivas));
            if (textKpi3Value != null) textKpi3Value.setText(String.valueOf(resumen.estudiantes));
            if (textKpi4Value != null) {
                textKpi4Value.setText(resumen.porcentajeAtendidas >= 0
                        ? resumen.porcentajeAtendidas + "%" : "--");
            }
            if (alertsAdapter != null) alertsAdapter.setData(resumen.recientes);
        });
        viewModel.getLoading().observe(getViewLifecycleOwner(), cargando ->
                progressKpis.setVisibility(Boolean.TRUE.equals(cargando)
                        ? View.VISIBLE : View.GONE));
        viewModel.getError().observe(getViewLifecycleOwner(), mensaje -> {
            if (mensaje == null) return;
            UiUtils.showRetry(requireView(), mensaje, viewModel::reintentar);
            viewModel.clearError();
        });
    }

    private void pintarSaludo() {
        if (textDate != null) {
            textDate.setText(getString(R.string.teacher_hoy) + FechaUtils.fechaBonitaConAnio(FechaUtils.hoyIso()));
        }
    }

    private void configurarAccesosRapidos(View view) {
        View cardEstudiantes = view.findViewById(R.id.card_aa_estudiantes);
        if (cardEstudiantes != null) cardEstudiantes.setOnClickListener(v -> navegar(R.id.nav_doc_courses));

        View cardCita = view.findViewById(R.id.card_aa_cita);
        if (cardCita != null) cardCita.setOnClickListener(v -> navegar(R.id.nav_doc_tutorias));

        View cardAlertas = view.findViewById(R.id.card_aa_alertas);
        if (cardAlertas != null) cardAlertas.setOnClickListener(v -> navegar(R.id.nav_doc_alerts));

        View viewAll = view.findViewById(R.id.text_view_all);
        if (viewAll != null) viewAll.setOnClickListener(v -> navegar(R.id.nav_doc_alerts));
    }

    private void navegar(int destinoId) {
        try {
            NavController navController = Navigation.findNavController(requireView());
            if (navController.getCurrentDestination() != null) {
                int currentId = navController.getCurrentDestination().getId();
                if (currentId == R.id.nav_doc_home) {
                    navController.navigate(destinoId);
                }
            }
        } catch (IllegalArgumentException e) {
            // Ignorar clics rápidos
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        viewModel.empezar(sessionManager.getUserId());
    }

    @Override
    public void onStop() {
        viewModel.detener();
        super.onStop();
    }

    private void mostrarDetalle(Alerta a) {
        StringBuilder msg = new StringBuilder();
        msg.append("Estudiante: ").append(a.getEstudianteNombre() != null
                ? a.getEstudianteNombre() : "-").append("\n");
        msg.append("Curso: ").append(a.getCursoNombre() != null ? a.getCursoNombre() : "-")
                .append("\n");
        if (a.getDescripcion() != null && !a.getDescripcion().isEmpty()) {
            msg.append("Descripción: ").append(a.getDescripcion()).append("\n");
        }
        msg.append("Enviada: ").append(a.getFecha() > 0
                ? FechaUtils.fechaBonitaLong(a.getFecha()) : "-").append("\n");
        String estado = a.getEstado() != null ? a.getEstado() : "PENDIENTE";
        msg.append("Estado: ");
        if (!"PENDIENTE".equals(estado) && a.getFechaAtencion() > 0) {
            msg.append(estado).append(" · ")
                    .append(FechaUtils.fechaBonitaLong(a.getFechaAtencion()));
        } else {
            msg.append(estado);
        }

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Alerta de " + a.getEstudianteNombre())
                .setMessage(msg)
                .setPositiveButton("Cerrar", null)
                .show();
    }
}
