package com.example.myapplication.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapters.CourseAdapter;
import com.example.myapplication.models.Curso;
import com.example.myapplication.models.Nota;
import com.example.myapplication.models.Tutoria;
import com.example.myapplication.repositories.CourseRepository;
import com.example.myapplication.repositories.GradeRepository;
import com.example.myapplication.repositories.TutoriaRepository;
import com.example.myapplication.services.SessionManager;
import com.example.myapplication.utils.FechaUtils;
import com.example.myapplication.utils.SemaforoUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;

import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MisCursosEstudianteFragment extends Fragment
        implements CourseAdapter.OnCourseClickListener {

    private RecyclerView recycler;
    private TextView txtEmpty;
    private ProgressBar progress;

    private CourseAdapter adapter;
    private CourseRepository courseRepository;
    private TutoriaRepository tutoriaRepository;
    private GradeRepository gradeRepository;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mis_cursos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recycler = view.findViewById(R.id.recycler_mis_cursos);
        txtEmpty = view.findViewById(R.id.layout_mis_cursos_empty);
        progress = view.findViewById(R.id.progress_mis_cursos);

        courseRepository = new CourseRepository();
        tutoriaRepository = new TutoriaRepository();
        gradeRepository = new GradeRepository();
        sessionManager = new SessionManager(requireContext());

        adapter = new CourseAdapter(this);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        cargarCursos();
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarCursos();
    }

    private void cargarCursos() {
        String uid = sessionManager.getUserId();
        if (uid == null) return;

        progress.setVisibility(View.VISIBLE);
        courseRepository.getCursosByEstudiante(uid)
                .addOnSuccessListener(snap -> {
                    progress.setVisibility(View.GONE);
                    List<Curso> lista = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Curso c = doc.toObject(Curso.class);
                        if (c != null) {
                            c.setId(doc.getId());
                            lista.add(c);
                        }
                    }
                    Collections.sort(lista, (a, b) ->
                            a.getNombre().compareToIgnoreCase(b.getNombre()));
                    adapter.setCursos(lista);
                    txtEmpty.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(requireContext(),
                            "Error al cargar cursos: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onCourseClick(Curso curso) {
        StringBuilder msg = new StringBuilder();
        msg.append("Código: ").append(nvl(curso.getCodigo(), "-")).append("\n");
        msg.append("Programa: ").append(nvl(curso.getPrograma(), "-")).append("\n");
        msg.append("Semestre: ").append(curso.getSemestre() > 0 ? curso.getSemestre() : "-")
                .append("\n");
        msg.append("Periodo: ").append(nvl(curso.getPeriodo(), "-")).append("\n");
        msg.append("Docente: ").append(nvl(curso.getDocenteNombre(), "-"));

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(curso.getNombre())
                .setMessage(msg)
                .setPositiveButton("Ver tutorías", (d, w) -> cargarTutorias(curso))
                .setNeutralButton("Mis notas", (d, w) -> cargarMisNotas(curso))
                .setNegativeButton("Cerrar", null)
                .show();
    }

    private void cargarMisNotas(Curso curso) {
        String uid = sessionManager.getUserId();
        if (uid == null) return;

        progress.setVisibility(View.VISIBLE);
        gradeRepository.getNotasByEstudianteCurso(uid, curso.getId())
                .addOnSuccessListener(snap -> {
                    progress.setVisibility(View.GONE);
                    List<Nota> notas = snap.toObjects(Nota.class);
                    if (notas == null || notas.isEmpty()) {
                        Toast.makeText(requireContext(),
                                "Aún no tienes notas registradas en este curso",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    mostrarDialogoNotas(curso, notas);
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(requireContext(),
                            "Error al cargar notas: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void mostrarDialogoNotas(Curso curso, List<Nota> notas) {
        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_curso_notas, null);

        View semaforo = v.findViewById(R.id.semaforo_notas_circle);
        TextView txtPromedio = v.findViewById(R.id.txt_notas_promedio);
        TextView txtNivel = v.findViewById(R.id.txt_notas_nivel);
        LinearLayout container = v.findViewById(R.id.container_cortes_notas);

        Map<String, Nota> latest = new HashMap<>();
        for (Nota n : notas) {
            if (n.getCorteId() == null) continue;
            Nota actual = latest.get(n.getCorteId());
            if (actual == null || n.getFechaRegistro() > actual.getFechaRegistro()) {
                latest.put(n.getCorteId(), n);
            }
        }

        double suma = 0;
        int count = 0;
        for (int i = 1; i <= 3; i++) {
            Nota n = latest.get("corte" + i);
            container.addView(buildCorteRow(i, n));
            if (n != null) {
                suma += n.getValor();
                count++;
            }
        }
        double promedio = count > 0 ? suma / count : 0;

        semaforo.setBackgroundResource(SemaforoUtils.drawableFor(promedio));
        txtPromedio.setText(SemaforoUtils.format(promedio));
        txtPromedio.setTextColor(SemaforoUtils.colorFor(requireContext(), promedio));
        txtNivel.setText("Riesgo: " + SemaforoUtils.nivelFor(promedio));
        txtNivel.setTextColor(SemaforoUtils.colorFor(requireContext(), promedio));

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(curso.getNombre())
                .setView(v)
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private View buildCorteRow(int indice, Nota n) {
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_curso_corte, null);

        TextView txtNombre = row.findViewById(R.id.txt_corte_nombre);
        TextView txtValor = row.findViewById(R.id.txt_corte_valor);
        TextView txtObs = row.findViewById(R.id.txt_corte_observacion);
        TextView txtFecha = row.findViewById(R.id.txt_corte_fecha);

        txtNombre.setText("Corte " + indice);
        if (n == null) {
            txtValor.setText("—");
            txtValor.setBackgroundResource(R.drawable.badge_info);
            txtValor.setTextColor(requireContext().getColor(R.color.info_text));
            return row;
        }

        txtValor.setText(SemaforoUtils.format(n.getValor()));
        double valor = n.getValor();
        if (valor >= 4.0) {
            txtValor.setBackgroundResource(R.drawable.badge_success);
            txtValor.setTextColor(requireContext().getColor(R.color.success_text));
        } else if (valor >= 3.0) {
            txtValor.setBackgroundResource(R.drawable.badge_risk_medium);
            txtValor.setTextColor(requireContext().getColor(R.color.risk_medium_text));
        } else {
            txtValor.setBackgroundResource(R.drawable.badge_risk_high);
            txtValor.setTextColor(requireContext().getColor(R.color.risk_high_text));
        }
        txtObs.setTextColor(requireContext().getColor(
                n.getNivelRiesgo() != null && "ALTO".equals(n.getNivelRiesgo())
                        ? R.color.risk_high_text
                        : R.color.text_secondary));
        txtObs.setText(n.getObservacion() != null && !n.getObservacion().isEmpty()
                ? n.getObservacion() : "");
        txtObs.setVisibility(txtObs.getText().toString().isEmpty() ? View.GONE : View.VISIBLE);

        if (n.getFechaRegistro() > 0) {
            txtFecha.setText("Registrada: " + FechaUtils.fechaBonitaLong(n.getFechaRegistro()));
            txtFecha.setVisibility(View.VISIBLE);
        }
        return row;
    }

    private void cargarTutorias(Curso curso) {
        progress.setVisibility(View.VISIBLE);
        tutoriaRepository.getTutoriasByCurso(curso.getId())
                .addOnSuccessListener(snap -> {
                    progress.setVisibility(View.GONE);
                    List<String> lineas = new ArrayList<>();
                    for (Tutoria t : snap.toObjects(Tutoria.class)) {
                        if (t == null) continue;
                        lineas.add(diaLabel(t.getDiaSemana()) + " "
                                + nvl(t.getHoraInicio(), "--") + "–"
                                + nvl(t.getHoraFin(), "--")
                                + "\nAula: " + nvl(t.getAula(), "-")
                                + " · Prof. " + nvl(t.getDocenteNombre(), "-"));
                    }
                    if (lineas.isEmpty()) {
                        Toast.makeText(requireContext(),
                                "Este curso no tiene tutorías activas",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Tutorías · " + curso.getNombre())
                            .setItems(lineas.toArray(new String[0]),
                                    (d, i) -> mostrarDetalleTutoria(snap.getDocuments().get(i)
                                            .toObject(Tutoria.class)))
                            .setNegativeButton("Cerrar", null)
                            .show();
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(requireContext(),
                            "Error al cargar tutorías: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void mostrarDetalleTutoria(Tutoria t) {
        if (t == null) return;
        StringBuilder msg = new StringBuilder();
        msg.append("Materia: ").append(nvl(t.getMateria(), "-")).append("\n");
        msg.append("Día: ").append(diaLabel(t.getDiaSemana())).append("\n");
        msg.append("Horario: ").append(nvl(t.getHoraInicio(), "--")).append(" – ")
                .append(nvl(t.getHoraFin(), "--")).append("\n");
        msg.append("Aula: ").append(nvl(t.getAula(), "-")).append("\n");
        msg.append("Curso: ").append(nvl(t.getCursoNombre(), "-")).append(" ")
                .append(nvl(t.getCodigoCurso(), ""));

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Tutoría de " + nvl(t.getDocenteNombre(), "docente"))
                .setMessage(msg)
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private String diaLabel(String dia) {
        if (dia == null) return "-";
        try {
            java.time.DayOfWeek dw = java.time.DayOfWeek.valueOf(dia);
            String s = dw.getDisplayName(TextStyle.FULL, new Locale("es", "CO"));
            return s.substring(0, 1).toUpperCase() + s.substring(1);
        } catch (Exception e) {
            return dia;
        }
    }

    private String nvl(String valor, String def) {
        return valor != null && !valor.isEmpty() ? valor : def;
    }
}