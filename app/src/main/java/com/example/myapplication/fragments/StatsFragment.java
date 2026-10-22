package com.example.myapplication.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.R;
import com.example.myapplication.models.Alerta;
import com.example.myapplication.models.Cita;
import com.example.myapplication.models.Tutoria;
import com.example.myapplication.models.Usuario;
import com.example.myapplication.services.SessionManager;
import com.example.myapplication.viewmodels.StatsViewModel;
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

import java.util.ArrayList;

public class StatsFragment extends Fragment {

    private PieChart pieChartUsers;
    private BarChart barChartBienestar;
    private PieChart pieChartAcademico;

    private StatsViewModel viewModel;

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

        viewModel = new ViewModelProvider(this).get(StatsViewModel.class);

        configurarGraficos();
        observarViewModel();
        viewModel.cargar();
    }

    private void observarViewModel() {
        viewModel.getEstado().observe(getViewLifecycleOwner(), estado -> {
            if (estado == null) return;
            actualizarGraficoUsuarios(estado.usuariosEstudiantes, estado.usuariosDocentes,
                    estado.usuariosPsicologos, estado.usuariosAdmins);
            intentarActualizarBarChart(estado.citasTotal, estado.citasAtendidas,
                    estado.alertasTotal, estado.alertasPendientes, estado.alertasAtendidas);
            intentarActualizarPieAcademico(estado.cursosTotal, estado.tutoriasActivas, estado.reportesTotal);
        });
        viewModel.getLoading().observe(getViewLifecycleOwner(), cargando -> {
            // Optional: show/hide progress if you have a progress bar in layout
        });
        viewModel.getError().observe(getViewLifecycleOwner(), mensaje -> {
            if (mensaje != null) {
                // Show error toast or snackbar
            }
        });
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

    private void intentarActualizarBarChart(int citasTotal, int citasAtendidas,
                                           int alertasTotal, int alertasPendientes, int alertasAtendidas) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        entries.add(new BarEntry(0, citasTotal));
        labels.add("Citas Total");
        
        entries.add(new BarEntry(1, citasAtendidas));
        labels.add("Citas Atend.");
        
        entries.add(new BarEntry(2, alertasTotal));
        labels.add("Alertas Total");
        
        entries.add(new BarEntry(3, alertasPendientes));
        labels.add("Alerta Pend.");
        
        entries.add(new BarEntry(4, alertasAtendidas));
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

    private void intentarActualizarPieAcademico(int cursosTotal, int tutoriasActivas, int reportesTotal) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        if (cursosTotal > 0) entries.add(new PieEntry(cursosTotal, "Cursos"));
        if (tutoriasActivas > 0) entries.add(new PieEntry(tutoriasActivas, "Tutorías (Activas)"));
        if (reportesTotal > 0) entries.add(new PieEntry(reportesTotal, "Reportes"));

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
