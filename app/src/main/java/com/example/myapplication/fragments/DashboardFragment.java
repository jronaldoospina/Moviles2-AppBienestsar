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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.StudentDetailActivity;
import com.example.myapplication.adapters.AlertsAdapter;
import com.example.myapplication.adapters.PacienteResumenAdapter;
import com.example.myapplication.models.Alerta;
import com.example.myapplication.models.Cita;
import com.example.myapplication.models.PacienteResumen;
import com.example.myapplication.models.ReporteEmocional;
import com.example.myapplication.models.Usuario;
import com.example.myapplication.repositories.AlertRepository;
import com.example.myapplication.repositories.AppointmentRepository;
import com.example.myapplication.repositories.EmotionalReportRepository;
import com.example.myapplication.repositories.UserRepository;
import com.example.myapplication.services.SessionManager;
import com.example.myapplication.utils.FechaUtils;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardFragment extends Fragment {

    private final List<Alerta> recientes = new ArrayList<>();
    private AlertsAdapter alertsAdapter;
    private ListenerRegistration alertasReg;

    private final List<Usuario> estudiantes = new ArrayList<>();
    private final Map<String, Integer> alertasActivasPorEstudiante = new HashMap<>();

    private PacienteResumenAdapter pacientesAdapter;
    private View layoutPacientes;
    private TextView txtPacientesEmpty;

    private View cardKpis;
    private ProgressBar progressKpis;
    private TextView textDate;
    private TextView textRiskCount, textAppointmentsToday, textTotalStudents, textImprovement;

    private View cardCita;
    private TextView txtAaEstudiantes, txtAaCita, txtAaAlertas;
    private TextView txtKpi2Label;

    private SessionManager sessionManager;
    private UserRepository userRepository;
    private AppointmentRepository appointmentRepository;
    private AlertRepository alertRepository;
    private EmotionalReportRepository emotionalRepository;
    private boolean esPsicologo;
    private int totalAlertas, alertasPendientes, alertasAtendidas;

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
        txtKpi2Label = view.findViewById(R.id.txt_kpi2_label);

        cardKpis = view.findViewById(R.id.card_kpis);
        progressKpis = view.findViewById(R.id.progress_kpis);

        RecyclerView recyclerAlertas = view.findViewById(R.id.recycler_recent_alerts);
        alertsAdapter = new AlertsAdapter();
        recyclerAlertas.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerAlertas.setAdapter(alertsAdapter);

        sessionManager = new SessionManager(requireContext());
        userRepository = new UserRepository();
        appointmentRepository = new AppointmentRepository();
        alertRepository = new AlertRepository();
        emotionalRepository = new EmotionalReportRepository();

        String rol = sessionManager.getUserRole();
        esPsicologo = SessionManager.ROLE_PSICOLOGO.equals(rol);

        pintarSaludo();
        configurarAccesosRapidos(view);
        configurarPacientes(view);
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

    private void pintarSaludo() {
        if (textDate != null) {
            textDate.setText("Hoy, " + FechaUtils.fechaBonitaConAnio(FechaUtils.hoyIso()));
        }
    }

    private void configurarAccesosRapidos(View view) {
        if (esPsicologo) {
            View cardEstudiantes = view.findViewById(R.id.card_aa_estudiantes);
            if (cardEstudiantes != null) cardEstudiantes.setOnClickListener(v -> navegar(R.id.nav_psi_students));
            
            View cardCita = view.findViewById(R.id.card_aa_cita);
            if (cardCita != null) cardCita.setOnClickListener(v -> navegar(R.id.nav_psi_agenda));
            
            View cardAlertas = view.findViewById(R.id.card_aa_alertas);
            if (cardAlertas != null) cardAlertas.setOnClickListener(v -> navegar(R.id.nav_psi_alerts));
            
            View viewAll = view.findViewById(R.id.text_view_all);
            if (viewAll != null) viewAll.setOnClickListener(v -> navegar(R.id.nav_psi_alerts));
        } else {
            if (txtAaEstudiantes != null) txtAaEstudiantes.setText("Usuarios");
            
            View cardEstudiantes = view.findViewById(R.id.card_aa_estudiantes);
            if (cardEstudiantes != null) cardEstudiantes.setOnClickListener(v -> navegar(R.id.nav_admin_users));

            if (cardCita != null) cardCita.setVisibility(View.GONE);

            if (txtAaAlertas != null) txtAaAlertas.setText("Estadísticas");
            
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
    public void onResume() {
        super.onResume();
        if (alertasReg == null) {
            alertasReg = alertRepository.escucharAlertas(this::onAlertasActualizadas);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (alertasReg != null) {
            alertasReg.remove();
            alertasReg = null;
        }
    }

    private void onAlertasActualizadas(@Nullable QuerySnapshot snapshots,
                                       @Nullable FirebaseFirestoreException e) {
        if (e != null || !isAdded() || getView() == null) return;

        totalAlertas = 0;
        alertasPendientes = 0;
        alertasAtendidas = 0;
        recientes.clear();
        alertasActivasPorEstudiante.clear();

        if (snapshots != null) {
            totalAlertas = snapshots.size();
            int contador = 0;
            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                Alerta a = doc.toObject(Alerta.class);
                if (a == null) continue;
                if ("PENDIENTE".equals(a.getEstado())) {
                    alertasPendientes++;
                    if (a.getEstudianteId() != null) {
                        alertasActivasPorEstudiante.merge(a.getEstudianteId(), 1, Integer::sum);
                    }
                }
                if ("ATENDIDA".equals(a.getEstado())) alertasAtendidas++;
                if (contador < 5) {
                    a.setId(doc.getId());
                    recientes.add(a);
                    contador++;
                }
            }
        }

        alertsAdapter.setData(recientes);
        pintarPacientes();

        if (!esPsicologo) {
            textAppointmentsToday.setText(String.valueOf(alertasPendientes));
            int mejora = totalAlertas > 0
                    ? (int) Math.round(alertasAtendidas * 100.0 / totalAlertas) : -1;
            textImprovement.setText(mejora >= 0 ? mejora + "%" : "--");
        }
    }

    private void pintarPacientes() {
        if (!esPsicologo || pacientesAdapter == null) return;

        List<PacienteResumen> lista = new ArrayList<>();
        for (Usuario u : estudiantes) {
            Integer n = alertasActivasPorEstudiante.get(u.getUid());
            if (n != null && n > 0) {
                lista.add(new PacienteResumen(u.getUid(), u.getNombre(), u.getNivelRiesgo(), n));
            }
        }
        lista.sort((a, b) -> Integer.compare(b.getAlertasActivas(), a.getAlertasActivas()));

        pacientesAdapter.setData(lista);
        boolean vacio = lista.isEmpty();
        layoutPacientes.setVisibility(vacio ? View.GONE : View.VISIBLE);
        txtPacientesEmpty.setVisibility(vacio ? View.VISIBLE : View.GONE);
        cargarAnimos(lista);
    }

    private void cargarAnimos(List<PacienteResumen> lista) {
        for (PacienteResumen p : lista) {
            emotionalRepository.getByEstudiante(p.getUid())
                    .addOnSuccessListener(snap -> {
                        if (!isAdded() || getView() == null) return;
                        int mejor = 0;
                        long mejorFecha = 0;
                        if (snap != null) {
                            for (DocumentSnapshot doc : snap.getDocuments()) {
                                ReporteEmocional re = doc.toObject(ReporteEmocional.class);
                                if (re != null && re.getFecha() >= mejorFecha) {
                                    mejorFecha = re.getFecha();
                                    mejor = re.getAnimo();
                                }
                            }
                        }
                        if (pacientesAdapter != null) {
                            pacientesAdapter.updateAnimo(p.getUid(), mejor);
                        }
                    });
        }
    }
}