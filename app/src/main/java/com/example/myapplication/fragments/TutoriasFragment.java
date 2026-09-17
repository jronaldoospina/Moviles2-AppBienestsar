package com.example.myapplication.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapters.TutoriaAdapter;
import com.example.myapplication.models.Curso;
import com.example.myapplication.models.Tutoria;
import com.example.myapplication.repositories.AuditRepository;
import com.example.myapplication.repositories.CourseRepository;
import com.example.myapplication.repositories.TutoriaRepository;
import com.example.myapplication.services.SessionManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class TutoriasFragment extends Fragment {

    private final List<Tutoria> tutorias = new ArrayList<>();
    private final List<Curso> misCursos = new ArrayList<>();
    private TutoriaAdapter adapter;
    private TutoriaRepository repo;
    private CourseRepository courseRepository;
    private SessionManager sessionManager;

    private RecyclerView recycler;
    private TextView txtEmpty;
    private ProgressBar progress;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tutorias, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recycler = view.findViewById(R.id.recycler_tutorias);
        txtEmpty = view.findViewById(R.id.txt_tutorias_empty);
        progress = view.findViewById(R.id.progress_tutorias);

        repo = new TutoriaRepository();
        courseRepository = new CourseRepository();
        sessionManager = new SessionManager(requireContext());

        adapter = new TutoriaAdapter();
        adapter.setOnTutoriaClickListener(this::mostrarOpciones);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        view.findViewById(R.id.fab_nueva_tutoria).setOnClickListener(v -> abrirDialogoNueva());

        cargarTutorias();
    }

    private void cargarTutorias() {
        progress.setVisibility(View.VISIBLE);
        String uid = sessionManager.getUserId();
        if (uid == null) {
            progress.setVisibility(View.GONE);
            return;
        }
        repo.getTutoriasByDocente(uid)
                .addOnSuccessListener(this::onTutoriasCargadas)
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    txtEmpty.setText("No se pudieron cargar las tutorías.\n" + e.getMessage());
                    txtEmpty.setVisibility(View.VISIBLE);
                });
    }

    private void onTutoriasCargadas(QuerySnapshot snap) {
        progress.setVisibility(View.GONE);
        tutorias.clear();
        if (snap != null) {
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Tutoria t = doc.toObject(Tutoria.class);
                if (t != null) {
                    t.setId(doc.getId());
                    tutorias.add(t);
                }
            }
        }
        adapter.setData(tutorias);
        txtEmpty.setVisibility(tutorias.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void abrirDialogoNueva() {
        loadMisCursos();
    }

    private void loadMisCursos() {
        String uid = sessionManager.getUserId();
        if (uid == null) {
            Toast.makeText(requireContext(), "Sesión inválida", Toast.LENGTH_SHORT).show();
            return;
        }
        courseRepository.getCursosByDocente(uid)
                .addOnSuccessListener(snap -> {
                    misCursos.clear();
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            Curso c = doc.toObject(Curso.class);
                            if (c != null) {
                                c.setId(doc.getId());
                                misCursos.add(c);
                            }
                        }
                    }
                    if (misCursos.isEmpty()) {
                        Toast.makeText(requireContext(), "No tienes cursos asignados",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    mostrarDialogo();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void mostrarDialogo() {
        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_tutoria, null);

        AutoCompleteTextView spnCurso = v.findViewById(R.id.spn_tutoria_curso);
        TextInputEditText editMateria = v.findViewById(R.id.edit_tutoria_materia);
        ChipGroup chipDia = v.findViewById(R.id.chip_group_dia);
        TextInputEditText editInicio = v.findViewById(R.id.edit_tutoria_hora_inicio);
        TextInputEditText editFin = v.findViewById(R.id.edit_tutoria_hora_fin);
        TextInputEditText editAula = v.findViewById(R.id.edit_tutoria_aula);

        List<String> nombres = new ArrayList<>();
        for (Curso c : misCursos) {
            nombres.add(c.getNombre() + " (" + c.getCodigo() + " · " + c.getPeriodo() + ")");
        }
        ArrayAdapter<String> dropdown = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, nombres);
        spnCurso.setAdapter(dropdown);

        if (chipDia.getChildCount() > 0) {
            chipDia.check(chipDia.getChildAt(0).getId());
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Nueva tutoría")
                .setView(v)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(x -> guardarTutoria(
                        spnCurso, editMateria, chipDia, editInicio, editFin, editAula, dialog));
    }

    private void guardarTutoria(AutoCompleteTextView spnCurso, TextInputEditText editMateria,
                                ChipGroup chipDia, TextInputEditText editInicio,
                                TextInputEditText editFin, TextInputEditText editAula,
                                androidx.appcompat.app.AlertDialog dialog) {
        String materia = editMateria.getText() != null
                ? editMateria.getText().toString().trim() : "";
        String inicio = editInicio.getText() != null
                ? editInicio.getText().toString().trim() : "";
        String fin = editFin.getText() != null ? editFin.getText().toString().trim() : "";
        String aula = editAula.getText() != null ? editAula.getText().toString().trim() : "";
        String seleccion = spnCurso.getText() != null ? spnCurso.getText().toString().trim() : "";

        Chip chipDiaSel = chipDia.findViewById(chipDia.getCheckedChipId());
        String dia = chipDiaSel != null ? String.valueOf(chipDiaSel.getTag()) : "";

        if (materia.isEmpty() || seleccion.isEmpty() || dia.isEmpty()
                || inicio.isEmpty() || fin.isEmpty()) {
            Toast.makeText(requireContext(), "Completa todos los campos",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (!esHoraValida(inicio) || !esHoraValida(fin)) {
            Toast.makeText(requireContext(), "Hora inválida (formato 10:00)",
                    Toast.LENGTH_LONG).show();
            return;
        }

        Curso curso = null;
        for (Curso c : misCursos) {
            if ((c.getNombre() + " (" + c.getCodigo() + " · " + c.getPeriodo() + ")")
                    .equals(seleccion)) {
                curso = c;
                break;
            }
        }
        if (curso == null) {
            Toast.makeText(requireContext(), "Selecciona un curso válido",
                    Toast.LENGTH_LONG).show();
            return;
        }
        final Curso cursoSeleccionado = curso;

        Tutoria t = new Tutoria(
                sessionManager.getUserId(),
                sessionManager.getUserName(),
                cursoSeleccionado.getId(),
                cursoSeleccionado.getNombre(),
                materia,
                dia,
                inicio,
                fin,
                aula
        );
        t.setCodigoCurso(cursoSeleccionado.getCodigo());

        repo.createTutoria(t)
                .addOnSuccessListener(doc -> {
                    dialog.dismiss();
                    Toast.makeText(requireContext(), "Tutoría creada", Toast.LENGTH_SHORT).show();
                    new AuditRepository().registrar("TUTORIA_CREADA",
                            "Creó tutoría '" + materia + "' en "
                                    + cursoSeleccionado.getNombre(),
                            sessionManager);
                    cargarTutorias();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private boolean esHoraValida(String hora) {
        return hora.matches("\\d{1,2}:\\d{2}");
    }

    private void mostrarOpciones(Tutoria tutoria) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(tutoria.getMateria())
                .setMessage(diaLabel(tutoria.getDiaSemana()) + " · "
                        + tutoria.getHoraInicio() + " - " + tutoria.getHoraFin()
                        + "\n" + tutoria.getCursoNombre())
                .setPositiveButton("Eliminar", (d, which) ->
                        repo.deleteTutoria(tutoria.getId())
                                .addOnSuccessListener(v -> {
                                    Toast.makeText(requireContext(), "Tutoría eliminada",
                                            Toast.LENGTH_SHORT).show();
                                    new AuditRepository().registrar("TUTORIA_ELIMINADA",
                                            "Eliminó tutoría '"
                                                    + (tutoria.getMateria() != null
                                                    ? tutoria.getMateria() : "-") + "'",
                                            sessionManager);
                                    cargarTutorias();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(requireContext(), "Error: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show()))
                .setNegativeButton("Cerrar", null)
                .show();
    }

    private String diaLabel(String dia) {
        if (dia == null) return "";
        switch (dia) {
            case "MARTES": return "Martes";
            case "MIERCOLES": return "Miércoles";
            case "JUEVES": return "Jueves";
            case "VIERNES": return "Viernes";
            case "SABADO": return "Sábado";
            case "DOMINGO": return "Domingo";
            default: return "Lunes";
        }
    }
}