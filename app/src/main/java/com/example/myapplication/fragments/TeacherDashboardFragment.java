package com.example.myapplication.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapters.AlertsAdapter;
import com.example.myapplication.models.Alerta;
import com.example.myapplication.models.Curso;
import com.example.myapplication.models.Tutoria;
import com.example.myapplication.repositories.AlertRepository;
import com.example.myapplication.repositories.CourseRepository;
import com.example.myapplication.repositories.TutoriaRepository;
import com.example.myapplication.services.SessionManager;
import com.example.myapplication.utils.FechaUtils;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class TeacherDashboardFragment extends Fragment {

    private final List<Alerta> recientes = new ArrayList<>();
    private AlertsAdapter alertsAdapter;
    private ListenerRegistration alertasReg;

    private TextView textDate;
    private TextView textKpi1Value, textKpi2Value, textKpi3Value, textKpi4Value;
    private TextView txtKpi1Label, txtKpi2Label;

    private SessionManager sessionManager;
    private CourseRepository courseRepository;
    private TutoriaRepository tutoriaRepository;
    private AlertRepository alertRepository;

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
        txtKpi1Label = view.findViewById(R.id.txt_kpi1_label);
        txtKpi2Label = view.findViewById(R.id.txt_kpi2_label);

        txtKpi1Label.setText("Mis Cursos");
        txtKpi2Label.setText("Tutorías Activas");

        TextView txtKpi4Label = view.findViewById(R.id.txt_kpi4_label);
        if (txtKpi4Label != null) {
            txtKpi4Label.setText("Atendidas");
        }

        TextView txtKpi3Label = view.findViewById(R.id.txt_kpi3_label);
        if (txtKpi3Label != null) {
            txtKpi3Label.setText("Estudiantes");
        }

        TextView txtAaEstudiantes = view.findViewById(R.id.txt_aa_estudiantes);
        if (txtAaEstudiantes != null) txtAaEstudiantes.setText("Cursos");
        TextView txtAaCita = view.findViewById(R.id.txt_aa_cita);
        if (txtAaCita != null) txtAaCita.setText("Tutorías");
        TextView txtAaAlertas = view.findViewById(R.id.txt_aa_alertas);
        if (txtAaAlertas != null) txtAaAlertas.setText("Alertas");


        RecyclerView recyclerAlertas = view.findViewById(R.id.recycler_recent_alerts);
        alertsAdapter = new AlertsAdapter();
        recyclerAlertas.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerAlertas.setAdapter(alertsAdapter);
        alertsAdapter.setOnAlertaClickListener(this::mostrarDetalle);

        sessionManager = new SessionManager(requireContext());
        courseRepository = new CourseRepository();
        tutoriaRepository = new TutoriaRepository();
        alertRepository = new AlertRepository();

        pintarSaludo();
        configurarAccesosRapidos(view);
        cargarCursosYEstudiantes();
        cargarTutorias();
    }

    private void pintarSaludo() {
        if (textDate != null) {
            textDate.setText("Hoy, " + FechaUtils.fechaBonitaConAnio(FechaUtils.hoyIso()));
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

    private void cargarCursosYEstudiantes() {
        String uid = sessionManager.getUserId();
        if (uid == null) return;
        courseRepository.getCursosByDocente(uid).addOnSuccessListener(snap -> {
            if (!isAdded() || getView() == null) return;
            int cursos = 0, estudiantes = 0;
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Curso c = doc.toObject(Curso.class);
                if (c == null) continue;
                cursos++;
                if (c.getEstudiantesIds() != null) {
                    estudiantes += c.getEstudiantesIds().size();
                }
            }
            if (textKpi1Value != null) textKpi1Value.setText(String.valueOf(cursos));
            if (textKpi3Value != null) textKpi3Value.setText(String.valueOf(estudiantes));
        }).addOnFailureListener(e -> { /* mantener valores por defecto */ });
    }

    private void cargarTutorias() {
        String uid = sessionManager.getUserId();
        if (uid == null) return;
        tutoriaRepository.getTutoriasByDocente(uid).addOnSuccessListener(snap -> {
            if (!isAdded() || getView() == null) return;
            int activas = 0;
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Tutoria t = doc.toObject(Tutoria.class);
                if (t != null && t.isActiva()) activas++;
            }
            if (textKpi2Value != null) textKpi2Value.setText(String.valueOf(activas));
        }).addOnFailureListener(e -> { /* mantener valores por defecto */ });
    }

    @Override
    public void onResume() {
        super.onResume();
        String uid = sessionManager != null ? sessionManager.getUserId() : null;
        if (alertasReg == null && uid != null) {
            alertasReg = alertRepository.escucharAlertasDeDocente(uid, this::onAlertasActualizadas);
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
                                       @Nullable com.google.firebase.firestore.FirebaseFirestoreException e) {
        if (e != null || !isAdded() || getView() == null) return;

        int total = 0, pendientes = 0, atendidas = 0;
        recientes.clear();

        if (snapshots != null) {
            total = snapshots.size();
            List<Alerta> items = new ArrayList<>();
            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                Alerta a = doc.toObject(Alerta.class);
                if (a == null) continue;
                a.setId(doc.getId());
                items.add(a);
                if ("PENDIENTE".equals(a.getEstado())) pendientes++;
                if ("ATENDIDA".equals(a.getEstado())) atendidas++;
            }
            items.sort((x, y) -> Long.compare(y.getFecha(), x.getFecha()));
            recientes.addAll(items.subList(0, Math.min(5, items.size())));
        }

        if (alertsAdapter != null) alertsAdapter.setData(recientes);
        int mejora = total > 0 ? (int) Math.round(atendidas * 100.0 / total) : -1;
        if (textKpi4Value != null) textKpi4Value.setText(mejora >= 0 ? mejora + "%" : "--");
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