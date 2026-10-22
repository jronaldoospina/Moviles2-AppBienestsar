package com.example.myapplication.fragments;

import android.content.Intent;
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
import com.example.myapplication.StudentDetailActivity;
import com.example.myapplication.adapters.AlertsAdapter;
import com.example.myapplication.adapters.PacienteResumenAdapter;
import com.example.myapplication.services.SessionManager;
import com.example.myapplication.utils.FechaUtils;
import com.example.myapplication.utils.UiUtils;
import com.example.myapplication.viewmodels.DashboardViewModel;

public class DashboardFragment extends Fragment {

    private AlertsAdapter alertsAdapter;
    private PacienteResumenAdapter pacientesAdapter;
    private View layoutPacientes;
    private TextView txtPacientesEmpty;

    private ProgressBar progressKpis;
    private TextView textDate;
    private TextView textRiskCount, textAppointmentsToday, textTotalStudents, textImprovement;

    private View cardCita;
    private TextView txtAaEstudiantes, txtAaCita, txtAaAlertas;

    private SessionManager sessionManager;
    private DashboardViewModel viewModel;
    private boolean esPsicologo;

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
        textRiskCount = view.findViewById(R.id.text_risk_count);
        textAppointmentsToday = view.findViewById(R.id.text_appointments_today);
        textTotalStudents = view.findViewById(R.id.text_total_students);
        textImprovement = view.findViewById(R.id.text_improvement);

        cardCita = view.findViewById(R.id.card_aa_cita);
        txtAaEstudiantes = view.findViewById(R.id.txt_aa_estudiantes);
        txtAaCita = view.findViewById(R.id.txt_aa_cita);
        txtAaAlertas = view.findViewById(R.id.txt_aa_alertas);

        TextView txtKpi1Label = view.findViewById(R.id.txt_kpi1_label);
        if (txtKpi1Label != null) txtKpi1Label.setText(R.string.dashboard_alertas_activas);
        TextView txtKpi4Label = view.findViewById(R.id.txt_kpi4_label);
        if (txtKpi4Label != null) txtKpi4Label.setText(R.string.dashboard_atendidas);

        progressKpis = view.findViewById(R.id.progress_kpis);

        RecyclerView recyclerAlertas = view.findViewById(R.id.recycler_recent_alerts);
        alertsAdapter = new AlertsAdapter();
        recyclerAlertas.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerAlertas.setAdapter(alertsAdapter);

        sessionManager = new SessionManager(requireContext());
        esPsicologo = SessionManager.ROLE_PSICOLOGO.equals(sessionManager.getUserRole());
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        pintarSaludo();
        configurarAccesosRapidos(view);
        configurarPacientes(view);
        observarViewModel();
    }

    private void configurarPacientes(View view) {
        if (!esPsicologo) return;
        layoutPacientes = view.findViewById(R.id.layout_pacientes);
        txtPacientesEmpty = view.findViewById(R.id.txt_pacientes_empty);
        RecyclerView recyclerPacientes = view.findViewById(R.id.recycler_pacientes);
        if (recyclerPacientes != null) {
            pacientesAdapter = new PacienteResumenAdapter(paciente -> {
                Intent i = new Intent(requireContext(), StudentDetailActivity.class);
                i.putExtra(StudentDetailActivity.EXTRA_ESTUDIANTE_ID, paciente.getUid());
                startActivity(i);
            });
            recyclerPacientes.setLayoutManager(new LinearLayoutManager(requireContext()));
            recyclerPacientes.setAdapter(pacientesAdapter);
        }
        View btnVerPacientes = view.findViewById(R.id.text_ver_pacientes);
        if (btnVerPacientes != null) {
            btnVerPacientes.setOnClickListener(v -> navegar(R.id.nav_psi_students));
        }
    }

    private void observarViewModel() {
        viewModel.getResumen().observe(getViewLifecycleOwner(), resumen -> {
            if (textRiskCount != null) textRiskCount.setText(String.valueOf(resumen.alertasPendientes));
            if (textAppointmentsToday != null) textAppointmentsToday.setText(String.valueOf(resumen.citasHoy));
            if (textTotalStudents != null) textTotalStudents.setText(String.valueOf(resumen.totalEstudiantes));
            if (textImprovement != null) {
                textImprovement.setText(resumen.porcentajeAtendidas >= 0
                        ? resumen.porcentajeAtendidas + "%" : "--");
            }
            if (alertsAdapter != null) alertsAdapter.setData(resumen.recientes);
            if (esPsicologo && pacientesAdapter != null) {
                pacientesAdapter.setData(resumen.pacientes);
                boolean vacio = resumen.pacientes.isEmpty();
                if (layoutPacientes != null) {
                    layoutPacientes.setVisibility(vacio ? View.GONE : View.VISIBLE);
                }
                if (txtPacientesEmpty != null) {
                    txtPacientesEmpty.setVisibility(vacio ? View.VISIBLE : View.GONE);
                }
            }
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
            textDate.setText(getString(R.string.dashboard_hoy) + FechaUtils.fechaBonitaConAnio(FechaUtils.hoyIso()));
        }
    }

    private void configurarAccesosRapidos(View view) {
        if (esPsicologo) {
            View cardEstudiantes = view.findViewById(R.id.card_aa_estudiantes);
            if (cardEstudiantes != null) cardEstudiantes.setOnClickListener(v -> navegar(R.id.nav_psi_students));

            View cardCitaLocal = view.findViewById(R.id.card_aa_cita);
            if (cardCitaLocal != null) cardCitaLocal.setOnClickListener(v -> navegar(R.id.nav_psi_agenda));

            View cardAlertas = view.findViewById(R.id.card_aa_alertas);
            if (cardAlertas != null) cardAlertas.setOnClickListener(v -> navegar(R.id.nav_psi_alerts));

            View viewAll = view.findViewById(R.id.text_view_all);
            if (viewAll != null) viewAll.setOnClickListener(v -> navegar(R.id.nav_psi_alerts));
        } else {
            if (txtAaEstudiantes != null) txtAaEstudiantes.setText(R.string.dashboard_usuarios);

            View cardEstudiantes = view.findViewById(R.id.card_aa_estudiantes);
            if (cardEstudiantes != null) cardEstudiantes.setOnClickListener(v -> navegar(R.id.nav_admin_users));

            if (cardCita != null) cardCita.setVisibility(View.GONE);

            if (txtAaAlertas != null) txtAaAlertas.setText(R.string.dashboard_estadisticas);

            View cardAlertas = view.findViewById(R.id.card_aa_alertas);
            if (cardAlertas != null) cardAlertas.setOnClickListener(v -> navegar(R.id.nav_admin_stats));

            View viewAll = view.findViewById(R.id.text_view_all);
            if (viewAll != null) viewAll.setOnClickListener(v -> navegar(R.id.nav_admin_stats));
        }
    }

    private void navegar(int destinoId) {
        try {
            NavController navController = Navigation.findNavController(requireView());
            if (navController.getCurrentDestination() != null) {
                int currentId = navController.getCurrentDestination().getId();
                if (currentId == R.id.nav_admin_home || currentId == R.id.nav_psi_home) {
                    navController.navigate(destinoId);
                }
            }
        } catch (IllegalArgumentException e) {
            // Ignorar el error si se hace click muy rápido y la vista ya no es válida o el destino no existe desde aquí
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        viewModel.empezar(esPsicologo, sessionManager.getUserId());
    }

    @Override
    public void onStop() {
        viewModel.detener();
        super.onStop();
    }
}
