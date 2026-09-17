package com.example.myapplication.fragments;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;
import com.example.myapplication.models.Curso;
import com.example.myapplication.models.Nota;
import com.example.myapplication.repositories.CourseRepository;
import com.example.myapplication.repositories.GradeRepository;
import com.example.myapplication.services.SessionManager;
import com.example.myapplication.utils.SemaforoUtils;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyProgressFragment extends Fragment {

    private View semaforoCircle;
    private TextView txtPromedio, txtNivel;
    private TextView txtCountCursos, txtCountRiesgo, txtCountAprobadas;
    private TextView txtEmpty;
    private LinearLayout containerCursos;
    private BarChart chart;
    private CircularProgressIndicator progressLoading;

    private final GradeRepository gradeRepository = new GradeRepository();
    private final CourseRepository courseRepository = new CourseRepository();
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_progress, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        semaforoCircle = view.findViewById(R.id.semaforo_circle);
        txtPromedio = view.findViewById(R.id.txt_promedio);
        txtNivel = view.findViewById(R.id.txt_nivel);
        txtCountCursos = view.findViewById(R.id.txt_count_cursos);
        txtCountRiesgo = view.findViewById(R.id.txt_count_riesgo);
        txtCountAprobadas = view.findViewById(R.id.txt_count_aprobadas);
        txtEmpty = view.findViewById(R.id.txt_empty);
        containerCursos = view.findViewById(R.id.container_cursos);
        chart = view.findViewById(R.id.chart_progress);
        progressLoading = view.findViewById(R.id.progress_loading);

        sessionManager = new SessionManager(requireContext());
        load();
    }

    private void load() {
        String uid = sessionManager.getUserId();
        if (uid == null) {
            showEmpty();
            return;
        }

        progressLoading.setVisibility(View.VISIBLE);
        containerCursos.removeAllViews();
        txtEmpty.setVisibility(View.GONE);

        gradeRepository.getNotasByEstudiante(uid)
                .addOnSuccessListener(snapshot -> {
                    List<Nota> notas = snapshot.toObjects(Nota.class);
                    if (notas == null || notas.isEmpty()) {
                        progressLoading.setVisibility(View.GONE);
                        showEmpty();
                        return;
                    }
                    fetchCursos(notas);
                })
                .addOnFailureListener(e -> {
                    progressLoading.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Error al cargar notas: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    showEmpty();
                });
    }

    private void fetchCursos(List<Nota> notas) {
        Map<String, List<Nota>> byCourse = new HashMap<>();
        for (Nota n : notas) {
            if (n.getCursoId() == null) continue;
            byCourse.computeIfAbsent(n.getCursoId(), k -> new ArrayList<>()).add(n);
        }

        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (String cursoId : byCourse.keySet()) {
            tasks.add(courseRepository.getCursoById(cursoId));
        }

        Tasks.whenAllComplete(tasks).addOnSuccessListener(completed -> {
            Map<String, Curso> cursos = new HashMap<>();
            for (Task<DocumentSnapshot> t : tasks) {
                if (t.isSuccessful() && t.getResult() != null && t.getResult().exists()) {
                    Curso c = t.getResult().toObject(Curso.class);
                    if (c != null) {
                        c.setId(t.getResult().getId());
                        cursos.put(c.getId(), c);
                    }
                }
            }
            progressLoading.setVisibility(View.GONE);
            buildUi(byCourse, cursos);
        }).addOnFailureListener(e -> {
            progressLoading.setVisibility(View.GONE);
            Toast.makeText(requireContext(), "Error al cargar materias: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            showEmpty();
        });
    }

    private void buildUi(Map<String, List<Nota>> byCourse, Map<String, Curso> cursos) {
        List<SemaforoUtils.PromedioMateria> items = new ArrayList<>();
        for (Map.Entry<String, List<Nota>> e : byCourse.entrySet()) {
            items.add(SemaforoUtils.computePromedioMateria(
                    e.getKey(), e.getValue(), cursos.get(e.getKey())));
        }
        Collections.sort(items,
                (a, b) -> a.nombre.compareToIgnoreCase(b.nombre));

        renderHeader(items);
        renderChart(items);
        renderCourses(items);
    }

    // ============================================================
    // CÁLCULOS
    // ============================================================

    private void renderHeader(List<SemaforoUtils.PromedioMateria> items) {
        double promedio = SemaforoUtils.promedioGeneral(items);

        txtPromedio.setText(SemaforoUtils.format(promedio));
        txtCountCursos.setText(String.valueOf(items.size()));
        txtCountRiesgo.setText(String.valueOf(SemaforoUtils.enRiesgo(items)));
        txtCountAprobadas.setText(String.valueOf(SemaforoUtils.aprobadas(items)));

        semaforoCircle.setBackgroundResource(SemaforoUtils.drawableFor(promedio));
        int color = SemaforoUtils.colorFor(requireContext(), promedio);
        txtNivel.setText("Nivel de riesgo: " + SemaforoUtils.nivelFor(promedio));
        txtNivel.setTextColor(color);
    }

    private void renderChart(List<SemaforoUtils.PromedioMateria> items) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            SemaforoUtils.PromedioMateria cp = items.get(i);
            entries.add(new BarEntry(i, (float) cp.promedio));
            colors.add(SemaforoUtils.colorFor(requireContext(), cp.promedio));
            labels.add(SemaforoUtils.shortName(cp.nombre));
        }

        int textColor = ContextCompat.getColor(requireContext(), R.color.text_primary);

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(textColor);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.5f);
        chart.setData(data);

        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setDrawBarShadow(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(textColor);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelCount(labels.size());
        if (labels.size() > 4) {
            xAxis.setLabelRotationAngle(45f);
            chart.setExtraOffsets(6f, 6f, 6f, 18f);
        }

        com.github.mikephil.charting.components.YAxis left = chart.getAxisLeft();
        left.setAxisMinimum(0f);
        left.setAxisMaximum(5f);
        left.setGranularity(1f);
        left.setTextColor(textColor);

        chart.getAxisRight().setEnabled(false);
        chart.setFitBars(true);
        chart.animateY(600);
        chart.invalidate();
    }

    private void renderCourses(List<SemaforoUtils.PromedioMateria> items) {
        containerCursos.removeAllViews();
        for (SemaforoUtils.PromedioMateria cp : items) {
            containerCursos.addView(buildCourseCard(cp));
        }
    }

    // ============================================================
    // VISTAS DE MATERIA
    // ============================================================

    private View buildCourseCard(SemaforoUtils.PromedioMateria cp) {
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, dp(8));
        card.setLayoutParams(cardLp);
        card.setRadius(dp(14));
        card.setCardElevation(dp(1));
        card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white_card));

        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.addView(content);

        // Fila 1: nombre + promedio
        LinearLayout row1 = new LinearLayout(requireContext());
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(row1);

        TextView name = new TextView(requireContext());
        name.setText(cp.nombre);
        name.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        name.setTextSize(15f);
        name.setTypeface(null, Typeface.BOLD);
        name.setMaxLines(2);
        name.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        row1.addView(name, nameLp);

        TextView avg = new TextView(requireContext());
        avg.setText(SemaforoUtils.format(cp.promedio));
        avg.setTextColor(SemaforoUtils.colorFor(requireContext(), cp.promedio));
        avg.setTextSize(18f);
        avg.setTypeface(null, Typeface.BOLD);
        row1.addView(avg);

        // Código (opcional)
        if (!TextUtils.isEmpty(cp.codigo)) {
            TextView code = new TextView(requireContext());
            code.setText(cp.codigo);
            code.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            code.setTextSize(12f);
            content.addView(code);
        }

        // Chips de cortes
        LinearLayout chips = new LinearLayout(requireContext());
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(chips);

        for (int i = 0; i < SemaforoUtils.NUM_CORTES; i++) {
            Double v = cp.cortes[i];
            String text = "C" + (i + 1) + " "
                    + (v != null ? SemaforoUtils.format(v) : "—");
            chips.addView(buildChip(text, v != null ? v : -1));
        }

        return card;
    }

    private TextView buildChip(String text, double valor) {
        TextView chip = new TextView(requireContext());
        chip.setText(text);
        chip.setTextSize(12f);
        chip.setTypeface(null, Typeface.BOLD);
        chip.setPadding(dp(12), dp(6), dp(12), dp(6));

        int bgRes;
        int fgColor;
        if (valor >= 4.0) {
            bgRes = R.color.green_primary;
            fgColor = Color.WHITE;
        } else if (valor >= 3.0) {
            bgRes = R.color.risk_medium_text;
            fgColor = Color.WHITE;
        } else if (valor > 0) {
            bgRes = R.color.risk_high_text;
            fgColor = Color.WHITE;
        } else {
            bgRes = R.color.divider_color;
            fgColor = ContextCompat.getColor(requireContext(), R.color.text_secondary);
        }

        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dp(14));
        drawable.setColor(ContextCompat.getColor(requireContext(), bgRes));
        chip.setBackground(drawable);
        chip.setTextColor(fgColor);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(dp(6));
        chip.setLayoutParams(lp);
        return chip;
    }

    // ============================================================
    // UTILIDADES
    // ============================================================

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private void showEmpty() {
        txtEmpty.setVisibility(View.VISIBLE);
        containerCursos.removeAllViews();
    }
}