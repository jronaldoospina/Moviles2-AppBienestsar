package com.example.myapplication.fragments;

import android.os.Bundle;
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
import com.example.myapplication.models.Alerta;
import com.example.myapplication.models.ReporteEmocional;
import com.example.myapplication.repositories.AlertRepository;
import com.example.myapplication.repositories.EmotionalReportRepository;
import com.example.myapplication.services.SessionManager;
import com.example.myapplication.utils.AnimoUtils;
import com.example.myapplication.utils.FechaUtils;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.QuerySnapshot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmotionalReportFragment extends Fragment {

    private static final int[] MOOD_IDS = {
            R.id.mood_1, R.id.mood_2, R.id.mood_3, R.id.mood_4, R.id.mood_5
    };
    private static final int[] LABEL_IDS = {
            R.id.label_1, R.id.label_2, R.id.label_3, R.id.label_4, R.id.label_5
    };

    private final TextView[] moodViews = new TextView[5];
    private final TextView[] labelViews = new TextView[5];

    private TextInputEditText editDescripcion;
    private MaterialButton btnSave;
    private LinearLayout histContainer;
    private TextView txtHistEmpty;

    private final List<ReporteEmocional> reportes = new ArrayList<>();
    private int selectedMood = 0;
    private String editingId;      // id del reporte de hoy si ya existe
    private boolean todayExists = false;

    private final EmotionalReportRepository repo = new EmotionalReportRepository();
    private final AlertRepository alertRepo = new AlertRepository();
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_emotional_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        for (int i = 0; i < 5; i++) {
            moodViews[i] = view.findViewById(MOOD_IDS[i]);
            final int mood = i + 1;
            moodViews[i].setOnClickListener(v -> selectMood(mood));
        }
        for (int i = 0; i < 5; i++) {
            labelViews[i] = view.findViewById(LABEL_IDS[i]);
        }

        editDescripcion = view.findViewById(R.id.edit_descripcion);
        btnSave = view.findViewById(R.id.btn_save);
        histContainer = view.findViewById(R.id.hist_container);
        txtHistEmpty = view.findViewById(R.id.txt_hist_empty);

        btnSave.setOnClickListener(v -> guardar());

        sessionManager = new SessionManager(requireContext());
        load();
    }

    private void selectMood(int mood) {
        selectedMood = mood;
        pintarSelector();
    }

    private void pintarSelector() {
        int white = ContextCompat.getColor(requireContext(), R.color.text_hint);
        for (int i = 0; i < 5; i++) {
            if (selectedMood == i + 1) {
                moodViews[i].setBackgroundResource(AnimoUtils.circle(selectedMood));
                moodViews[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.white_card));
                labelViews[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            } else {
                moodViews[i].setBackgroundResource(R.drawable.mood_circle_none);
                moodViews[i].setTextColor(white);
                labelViews[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            }
        }
    }

    private void load() {
        String uid = sessionManager.getUserId();
        if (uid == null) {
            Toast.makeText(requireContext(), "Sesión no válida", Toast.LENGTH_SHORT).show();
            return;
        }

        repo.getByEstudiante(uid)
                .addOnSuccessListener(snapshot -> {
                    reportes.clear();
                    if (snapshot != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                            ReporteEmocional r = doc.toObject(ReporteEmocional.class);
                            if (r != null) {
                                r.setId(doc.getId());
                                reportes.add(r);
                            }
                        }
                    }
                    reportes.sort(Comparator.comparingLong(ReporteEmocional::getFecha).reversed());
                    prefillHoy();
                    pintarHistorial();
                    pintarTendencia();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Error al cargar: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void prefillHoy() {
        String hoy = LocalDate.now().toString();
        todayExists = false;
        editingId = null;
        for (ReporteEmocional r : reportes) {
            if (hoy.equals(r.getFechaDia())) {
                todayExists = true;
                editingId = r.getId();
                selectedMood = r.getAnimo();
                if (r.getDescripcion() != null) {
                    editDescripcion.setText(r.getDescripcion());
                }
                pintarSelector();
                btnSave.setText("Actualizar reporte de hoy");
                return;
            }
        }
        selectedMood = 0;
        editingId = null;
        editDescripcion.setText("");
        btnSave.setText("Guardar reporte");
        pintarSelector();
    }

    private void pintarHistorial() {
        histContainer.removeAllViews();
        txtHistEmpty.setVisibility(reportes.isEmpty() ? View.VISIBLE : View.GONE);

        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (int i = 0; i < Math.min(reportes.size(), 14); i++) {
            ReporteEmocional r = reportes.get(i);
            View row = inflater.inflate(R.layout.item_emotional_history, histContainer, false);

            View dot = row.findViewById(R.id.dot_emotion);
            dot.setBackgroundResource(AnimoUtils.circle(r.getAnimo()));

            TextView txtFecha = row.findViewById(R.id.txt_hist_fecha);
            txtFecha.setText(esHoy(r) ? "Hoy" : FechaUtils.fechaBonitaConAnio(r.getFechaDia()));

            TextView txtAnimo = row.findViewById(R.id.txt_hist_animo);
            String texto = "Ánimo: " + AnimoUtils.label(r.getAnimo());
            if (r.getDescripcion() != null && !r.getDescripcion().isEmpty()) {
                texto += " — " + r.getDescripcion();
            }
            txtAnimo.setText(texto);

            MaterialButton btnEdit = row.findViewById(R.id.btn_edit_day);
            if (esHoy(r)) {
                btnEdit.setVisibility(View.VISIBLE);
                btnEdit.setOnClickListener(v -> editarDesdeHistorial(r));
            } else {
                btnEdit.setVisibility(View.GONE);
            }
        }
    }

    private void pintarTendencia() {
        View v = getView();
        if (v == null) return;
        MaterialCardView card = v.findViewById(R.id.card_mood_trend);
        LineChart chart = v.findViewById(R.id.chart_mood_trend);
        if (card == null || chart == null) return;

        List<ReporteEmocional> asc = new ArrayList<>(reportes);
        asc.sort(Comparator.comparingLong(ReporteEmocional::getFecha));

        if (asc.size() < 2) {
            card.setVisibility(View.GONE);
            return;
        }
        card.setVisibility(View.VISIBLE);

        java.util.List<Entry> entries = new ArrayList<>();
        java.util.List<Integer> circleColors = new ArrayList<>();
        java.util.List<String> labels = new ArrayList<>();
        for (int i = 0; i < Math.min(asc.size(), 14); i++) {
            ReporteEmocional r = asc.get(i);
            entries.add(new Entry(i, r.getAnimo()));
            circleColors.add(ContextCompat.getColor(requireContext(), AnimoUtils.color(r.getAnimo())));
            labels.add(fechaCorta(r.getFechaDia()));
        }

        LineDataSet set = new LineDataSet(entries, "Ánimo");
        set.setColor(ContextCompat.getColor(requireContext(), R.color.green_primary));
        set.setLineWidth(2.5f);
        set.setCircleRadius(6f);
        set.setCircleHoleRadius(3f);
        set.setDrawCircleHole(true);
        set.setCircleColors(circleColors);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawFilled(true);
        set.setFillColor(ContextCompat.getColor(requireContext(), R.color.green_primary));
        set.setFillAlpha(45);
        set.setHighlightEnabled(false);

        LineData data = new LineData(set);
        chart.setData(data);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);

        XAxis x = chart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setValueFormatter(new IndexAxisValueFormatter(labels.toArray(new String[0])));
        x.setGranularity(1f);
        x.setDrawGridLines(false);
        x.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        x.setTextSize(9f);
        x.setAvoidFirstLastClipping(true);

        chart.getAxisLeft().setAxisMinimum(1f);
        chart.getAxisLeft().setAxisMaximum(5f);
        chart.getAxisLeft().setGranularity(1f);
        chart.getAxisLeft().setDrawGridLines(true);
        chart.getAxisLeft().setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        chart.getAxisLeft().setTextSize(9f);
        chart.getAxisRight().setEnabled(false);

        chart.animateX(300);
        chart.invalidate();
    }

    private String fechaCorta(String fechaDia) {
        if (fechaDia == null || fechaDia.isEmpty()) return "";
        try {
            String[] p = fechaDia.split("-");
            return (p.length >= 3) ? p[2] + "/" + p[1] : fechaDia;
        } catch (Exception e) {
            return fechaDia;
        }
    }

    private void editarDesdeHistorial(ReporteEmocional r) {
        selectedMood = r.getAnimo();
        editingId = r.getId();
        todayExists = true;
        editDescripcion.setText(r.getDescripcion() != null ? r.getDescripcion() : "");
        btnSave.setText("Actualizar reporte de hoy");
        pintarSelector();

        View v = getView();
        if (v != null) {
            v.post(() -> {
                androidx.core.widget.NestedScrollView sv = v.findViewById(R.id.scroll_emotion);
                if (sv != null) sv.smoothScrollTo(0, 0);
            });
        }
    }

    private boolean esHoy(ReporteEmocional r) {
        return LocalDate.now().toString().equals(r.getFechaDia());
    }

    private void guardar() {
        if (selectedMood == 0) {
            Toast.makeText(requireContext(), "Selecciona cómo te sientes hoy", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = sessionManager.getUserId();
        if (uid == null) return;

        String descripcion = editDescripcion.getText() != null
                ? editDescripcion.getText().toString().trim() : "";

        btnSave.setEnabled(false);

        if (editingId != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("animo", selectedMood);
            updates.put("descripcion", descripcion);
            updates.put("fechaDia", LocalDate.now().toString());
            repo.update(editingId, updates)
                    .addOnSuccessListener(aVoid -> {
                        btnSave.setEnabled(true);
                        Toast.makeText(requireContext(), "Reporte actualizado", Toast.LENGTH_SHORT).show();
                        load();
                        evaluarAlertaTemprana(uid);
                    })
                    .addOnFailureListener(e -> {
                        btnSave.setEnabled(true);
                        Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } else {
            ReporteEmocional r = new ReporteEmocional();
            r.setEstudianteId(uid);
            r.setAnimo(selectedMood);
            r.setDescripcion(descripcion);
            r.setFechaDia(LocalDate.now().toString());
            r.setFecha(System.currentTimeMillis());
            repo.create(r)
                    .addOnSuccessListener(doc -> {
                        btnSave.setEnabled(true);
                        Toast.makeText(requireContext(), "Reporte guardado", Toast.LENGTH_SHORT).show();
                        load();
                        evaluarAlertaTemprana(uid);
                    })
                    .addOnFailureListener(e -> {
                        btnSave.setEnabled(true);
                        Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }

    private void evaluarAlertaTemprana(String uid) {
        long desde = System.currentTimeMillis() - 7L * 24 * 3600 * 1000;

        repo.getByEstudiante(uid)
                .addOnSuccessListener(this::onHistorialAlerta)
                .addOnFailureListener(e -> {
                });
    }

    private void onHistorialAlerta(QuerySnapshot snapshot) {
        long desde = System.currentTimeMillis() - 7L * 24 * 3600 * 1000;
        int bajos = 0;
        if (snapshot != null) {
            for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                ReporteEmocional r = doc.toObject(ReporteEmocional.class);
                if (r != null && r.getFecha() >= desde && r.getAnimo() <= 2) {
                    bajos++;
                }
            }
        }
        if (bajos >= 3) {
            crearAlertaEmocional(bajos);
        }
    }

    private void crearAlertaEmocional(int bajos) {
        String uid = sessionManager.getUserId();
        if (uid == null) return;

        alertRepo.getAlertaEmocionalActiva(uid)
                .addOnSuccessListener(snap -> {
                    if (snap != null && !snap.isEmpty()) {
                        return;
                    }
                    Alerta a = new Alerta();
                    a.setDocenteNombre("Sistema");
                    a.setEstudianteId(uid);
                    a.setEstudianteNombre(sessionManager.getUserName());
                    a.setMotivo("EMOCIONAL");
                    a.setDescripcion("Ánimo bajo (1-2) registrado " + bajos
                            + " días en la última semana. El estudiante necesita acompañamiento.");
                    a.setEstado("PENDIENTE");
                    a.setFecha(System.currentTimeMillis());
                    alertRepo.createAlerta(a)
                            .addOnSuccessListener(doc ->
                                    Toast.makeText(requireContext(),
                                            "Te apoyaremos: Bienestar ha sido notificado.",
                                            Toast.LENGTH_LONG).show())
                            .addOnFailureListener(e -> {
                            });
                });
    }
}