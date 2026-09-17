package com.example.myapplication.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;
import com.example.myapplication.models.Alerta;
import com.example.myapplication.models.Cita;
import com.example.myapplication.models.Tutoria;
import com.example.myapplication.models.Usuario;
import com.example.myapplication.repositories.AlertRepository;
import com.example.myapplication.repositories.AppointmentRepository;
import com.example.myapplication.repositories.CourseRepository;
import com.example.myapplication.repositories.ReportRepository;
import com.example.myapplication.repositories.TutoriaRepository;
import com.example.myapplication.repositories.UserRepository;
import com.example.myapplication.services.SessionManager;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;

public class StatsFragment extends Fragment {

    private PieChart pieChartUsers;
    private BarChart barChartBienestar;
    private PieChart pieChartAcademico;

    private UserRepository userRepository;
    private AppointmentRepository appointmentRepository;
    private AlertRepository alertRepository;
    private CourseRepository courseRepository;
    private TutoriaRepository tutoriaRepository;
    private ReportRepository reportRepository;

    private int countCitasTotal = 0, countCitasAtendidas = 0;
    private int countAlertasTotal = 0, countAlertasPendientes = 0, countAlertasAtendidas = 0;
    private int countCursos = 0, countTutoriasActivas = 0, countReportes = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pieChartUsers = view.findViewById(R.id.pie_chart_users);
        barChartBienestar = view.findViewById(R.id.bar_chart_bienestar);
        pieChartAcademico = view.findViewById(R.id.pie_chart_academico);

        userRepository = new UserRepository();
        appointmentRepository = new AppointmentRepository();
        alertRepository = new AlertRepository();
        courseRepository = new CourseRepository();
        tutoriaRepository = new TutoriaRepository();
        reportRepository = new ReportRepository();

        configurarGraficos();
        cargarDatos();
    }

    private void configurarGraficos() {
        // PieChart Usuarios
        pieChartUsers.getDescription().setEnabled(false);
        pieChartUsers.setDrawHoleEnabled(true);
        pieChartUsers.setHoleColor(Color.TRANSPARENT);
        pieChartUsers.setEntryLabelTextSize(12f);
        pieChartUsers.setEntryLabelColor(Color.BLACK);
        pieChartUsers.getLegend().setWordWrapEnabled(true);

        // BarChart Bienestar
        barChartBienestar.getDescription().setEnabled(false);
        barChartBienestar.setDrawGridBackground(false);
        barChartBienestar.setFitBars(true);
        barChartBienestar.getAxisRight().setEnabled(false);
        barChartBienestar.getAxisLeft().setAxisMinimum(0f);
        barChartBienestar.getAxisLeft().setGranularity(1f);
        XAxis xAxis = barChartBienestar.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        // PieChart Académico
        pieChartAcademico.getDescription().setEnabled(false);
        pieChartAcademico.setDrawHoleEnabled(true);
        pieChartAcademico.setHoleColor(Color.TRANSPARENT);
        pieChartAcademico.setEntryLabelTextSize(12f);
        pieChartAcademico.setEntryLabelColor(Color.BLACK);
        pieChartAcademico.getLegend().setWordWrapEnabled(true);
    }

    private void cargarDatos() {
        cargarUsuarios();
        cargarBienestar();
        cargarAcademico();
    }

    private void cargarUsuarios() {
        userRepository.getAllUsers().addOnSuccessListener(snap -> {
            if (!isAdded()) return;
            int estudiantes = 0, docentes = 0, psicologos = 0, admins = 0;
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Usuario u = doc.toObject(Usuario.class);
                if (u == null) continue;
                String rol = u.getRol();
                if (SessionManager.ROLE_ESTUDIANTE.equals(rol)) {
                    estudiantes++;
                } else if (SessionManager.ROLE_DOCENTE.equals(rol)) {
                    docentes++;
                } else if (SessionManager.ROLE_PSICOLOGO.equals(rol)) {
                    psicologos++;
                } else if (SessionManager.ROLE_ADMIN.equals(rol)) {
                    admins++;
                }
            }
            actualizarGraficoUsuarios(estudiantes, docentes, psicologos, admins);
        });
    }

    private void actualizarGraficoUsuarios(int est, int doc, int psi, int adm) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        if (est > 0) entries.add(new PieEntry(est, "Estudiantes"));
        if (doc > 0) entries.add(new PieEntry(doc, "Docentes"));
        if (psi > 0) entries.add(new PieEntry(psi, "Psicólogos"));
        if (adm > 0) entries.add(new PieEntry(adm, "Administradores"));

        PieDataSet dataSet = new PieDataSet(entries, "Roles");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        PieData data = new PieData(dataSet);
        pieChartUsers.setData(data);
        pieChartUsers.invalidate();
    }

    private void cargarBienestar() {
        // Se cargan Citas y Alertas para unirlas en el BarChart
        appointmentRepository.getAllCitas().addOnSuccessListener(snap -> {
            if (!isAdded()) return;
            countCitasTotal = snap.size();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Cita c = doc.toObject(Cita.class);
                if (c != null && "ATENDIDA".equals(c.getEstado())) {
                    countCitasAtendidas++;
                }
            }
            intentarActualizarBarChart();
        });

        alertRepository.getAllAlertas().addOnSuccessListener(snap -> {
            if (!isAdded()) return;
            countAlertasTotal = snap.size();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Alerta a = doc.toObject(Alerta.class);
                if (a == null) continue;
                if ("PENDIENTE".equals(a.getEstado())) countAlertasPendientes++;
                if ("ATENDIDA".equals(a.getEstado())) countAlertasAtendidas++;
            }
            intentarActualizarBarChart();
        });
    }

    private void intentarActualizarBarChart() {
        // Se llama cada vez que termina de cargar Citas o Alertas
        // Se actualiza la gráfica con los últimos valores disponibles
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        entries.add(new BarEntry(0, countCitasTotal));
        labels.add("Citas Total");
        
        entries.add(new BarEntry(1, countCitasAtendidas));
        labels.add("Citas Atend.");
        
        entries.add(new BarEntry(2, countAlertasTotal));
        labels.add("Alertas Total");
        
        entries.add(new BarEntry(3, countAlertasPendientes));
        labels.add("Alerta Pend.");
        
        entries.add(new BarEntry(4, countAlertasAtendidas));
        labels.add("Alerta Atend.");

        BarDataSet dataSet = new BarDataSet(entries, "Gestión");
        dataSet.setColors(ColorTemplate.PASTEL_COLORS);
        dataSet.setValueTextSize(12f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        BarData data = new BarData(dataSet);
        barChartBienestar.setData(data);

        barChartBienestar.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChartBienestar.getXAxis().setLabelCount(labels.size());

        barChartBienestar.invalidate();
    }

    private void cargarAcademico() {
        courseRepository.getAllCursos().addOnSuccessListener(snap -> {
            if (!isAdded()) return;
            countCursos = snap.size();
            intentarActualizarPieAcademico();
        });

        tutoriaRepository.getAllTutorias().addOnSuccessListener(snap -> {
            if (!isAdded()) return;
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Tutoria t = doc.toObject(Tutoria.class);
                if (t != null && t.isActiva()) countTutoriasActivas++;
            }
            intentarActualizarPieAcademico();
        });

        reportRepository.getAllReportes().addOnSuccessListener(snap -> {
            if (!isAdded()) return;
            countReportes = snap.size();
            intentarActualizarPieAcademico();
        });
    }

    private void intentarActualizarPieAcademico() {
        ArrayList<PieEntry> entries = new ArrayList<>();
        if (countCursos > 0) entries.add(new PieEntry(countCursos, "Cursos"));
        if (countTutoriasActivas > 0) entries.add(new PieEntry(countTutoriasActivas, "Tutorías (Activas)"));
        if (countReportes > 0) entries.add(new PieEntry(countReportes, "Reportes"));

        if (entries.isEmpty()) return;

        PieDataSet dataSet = new PieDataSet(entries, "Actividad");
        dataSet.setColors(ColorTemplate.JOYFUL_COLORS);
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        PieData data = new PieData(dataSet);
        pieChartAcademico.setData(data);
        pieChartAcademico.invalidate();
    }
}
