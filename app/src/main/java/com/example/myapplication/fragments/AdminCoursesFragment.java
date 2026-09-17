package com.example.myapplication.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
import com.example.myapplication.adapters.EstudiantesMatriculaAdapter;
import com.example.myapplication.models.Curso;
import com.example.myapplication.models.Notificacion;
import com.example.myapplication.models.Usuario;
import com.example.myapplication.repositories.AuditRepository;
import com.example.myapplication.repositories.CourseRepository;
import com.example.myapplication.repositories.NotificacionRepository;
import com.example.myapplication.repositories.UserRepository;
import com.example.myapplication.services.SessionManager;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AdminCoursesFragment extends Fragment {

    private static final String[] SEMESTRES = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};

    private final List<String> docentesNombres = new ArrayList<>();
    private final List<String> docentesIds = new ArrayList<>();
    private final List<Usuario> estudiantesList = new ArrayList<>();

    private RecyclerView recycler;
    private View txtEmpty;
    private TextView txtEmptyMsg;
    private TextView txtContador;
    private ProgressBar progress;

    private CourseAdapter adapter;
    private CourseRepository courseRepository;
    private UserRepository userRepository;
    private NotificacionRepository notificacionRepository;
    private SessionManager sessionManager;

    private String queryActual = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_courses, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recycler = view.findViewById(R.id.recycler_admin_courses);
        txtEmpty = view.findViewById(R.id.layout_courses_empty);
        txtEmptyMsg = view.findViewById(R.id.txt_courses_empty_msg);
        txtContador = view.findViewById(R.id.txt_courses_count);
        progress = view.findViewById(R.id.progress_admin_courses);
        FloatingActionButton fab = view.findViewById(R.id.fab_nuevo_curso);

        courseRepository = new CourseRepository();
        userRepository = new UserRepository();
        notificacionRepository = new NotificacionRepository();
        sessionManager = new SessionManager(requireContext());

        adapter = new CourseAdapter(this::abrirDetalleCurso);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        TextInputEditText editBusqueda = view.findViewById(R.id.edit_search_courses);
        editBusqueda.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                queryActual = s.toString();
                aplicarFiltro();
            }
        });

        fab.setOnClickListener(v -> mostrarDialogoCrearCurso());
    }

    private void aplicarFiltro() {
        adapter.aplicarFiltro(queryActual);
        int total = adapter.getTotalFiltrado();
        txtContador.setText(total + (total == 1 ? " curso" : " cursos"));
        boolean hayBusqueda = queryActual != null && !queryActual.trim().isEmpty();
        boolean sinResultados = total == 0;
        txtEmpty.setVisibility(sinResultados ? View.VISIBLE : View.GONE);
        txtEmptyMsg.setText(hayBusqueda
                ? "No hay cursos que coincidan con tu búsqueda."
                : "Aún no hay cursos.\nToca + para crear el primero.");
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarCursos();
    }

    private void cargarCursos() {
        progress.setVisibility(View.VISIBLE);
        courseRepository.getAllCursos()
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
                            Long.compare(b.getFechaCreacion(), a.getFechaCreacion()));
                    adapter.setCursos(lista);
                    aplicarFiltro();
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(requireContext(),
                            "Error al cargar cursos: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // ============================================================
    // CREAR CURSO
    // ============================================================

    private void mostrarDialogoCrearCurso() {
        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_create_curso, null);

        TextInputEditText txtNombre = v.findViewById(R.id.txt_curso_nombre);
        TextInputEditText txtCodigo = v.findViewById(R.id.txt_curso_codigo);
        TextInputEditText txtCreditos = v.findViewById(R.id.txt_curso_creditos);
        TextInputEditText txtPrograma = v.findViewById(R.id.txt_curso_programa);
        TextInputEditText txtPeriodo = v.findViewById(R.id.txt_curso_periodo);
        MaterialAutoCompleteTextView actSemestre = v.findViewById(R.id.act_curso_semestre);
        MaterialAutoCompleteTextView actDocente = v.findViewById(R.id.act_curso_docente);

        actSemestre.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, SEMESTRES));
        actSemestre.setText("1", false);

        cargarDocentes(actDocente);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Crear curso")
                .setView(v)
                .setPositiveButton("Crear", null)
                .setNegativeButton("Cancelar", null)
                .create();
        dialog.show();

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(x -> {
                    String nombre = txtNombre.getText() != null
                            ? txtNombre.getText().toString().trim() : "";
                    String codigo = txtCodigo.getText() != null
                            ? txtCodigo.getText().toString().trim() : "";
                    String programa = txtPrograma.getText() != null
                            ? txtPrograma.getText().toString().trim() : "";
                    String periodo = txtPeriodo.getText() != null
                            ? txtPeriodo.getText().toString().trim() : "";
                    String docenteTexto = actDocente.getText() != null
                            ? actDocente.getText().toString().trim() : "";
                    if (crearCurso(nombre, codigo, programa, periodo, docenteTexto,
                            actSemestre, txtCreditos)) {
                        dialog.dismiss();
                    }
                });
    }

    private void cargarDocentes(MaterialAutoCompleteTextView actDocente) {
        userRepository.getAllUsers().addOnSuccessListener(snaps -> {
            docentesNombres.clear();
            docentesIds.clear();
            List<Usuario> docentes = new ArrayList<>();
            for (DocumentSnapshot doc : snaps.getDocuments()) {
                Usuario u = doc.toObject(Usuario.class);
                if (u != null && SessionManager.ROLE_DOCENTE.equals(u.getRol())
                        && u.getNombre() != null && !u.getNombre().trim().isEmpty()) {
                    u.setUid(doc.getId());
                    docentes.add(u);
                }
            }
            docentes.sort(Comparator.comparing(u -> u.getNombre().trim()));
            for (Usuario u : docentes) {
                docentesNombres.add(u.getNombre().trim());
                docentesIds.add(u.getUid());
            }
            actDocente.setAdapter(new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_list_item_1, docentesNombres));
        }).addOnFailureListener(err -> Toast.makeText(requireContext(),
                "No se pudieron cargar los docentes: " + err.getMessage(),
                Toast.LENGTH_LONG).show());
    }

    private boolean crearCurso(String nombre, String codigo, String programa, String periodo,
                               String docenteTexto, MaterialAutoCompleteTextView actSemestre,
                               TextInputEditText txtCreditos) {
        if (nombre.isEmpty()) {
            Toast.makeText(requireContext(), "Ingresa el nombre del curso", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (codigo.isEmpty()) {
            Toast.makeText(requireContext(), "Ingresa el código", Toast.LENGTH_SHORT).show();
            return false;
        }
        int idx = docentesNombres.indexOf(docenteTexto);
        if (docenteTexto.isEmpty() || idx < 0) {
            Toast.makeText(requireContext(), "Selecciona un docente de la lista", Toast.LENGTH_SHORT).show();
            return false;
        }
        String docenteId = docentesIds.get(idx);
        String docenteNombre = docentesNombres.get(idx);

        int semestre;
        try {
            semestre = Integer.parseInt(actSemestre.getText() != null
                    ? actSemestre.getText().toString() : "1");
        } catch (Exception e) {
            semestre = 1;
        }
        int creditos;
        try {
            creditos = Integer.parseInt(txtCreditos.getText() != null
                    ? txtCreditos.getText().toString().trim() : "3");
        } catch (Exception e) {
            creditos = 3;
        }

        Curso curso = new Curso(nombre, codigo, docenteId, docenteNombre);
        curso.setPrograma(programa.isEmpty() ? "Sin programa" : programa);
        curso.setPeriodo(periodo.isEmpty() ? "2025-2" : periodo);
        curso.setSemestre(semestre);
        curso.setCreditos(creditos);

        courseRepository.createCurso(curso)
                .addOnSuccessListener(v -> {
                    Toast.makeText(requireContext(), "Curso creado", Toast.LENGTH_SHORT).show();
                    new AuditRepository().registrar("CURSO_CREADO",
                            "Creó el curso " + nombre + " (" + codigo + ") asignado a "
                                    + docenteNombre,
                            sessionManager);
                    notificarDocente(docenteId, nombre, codigo);
                    cargarCursos();
                })
                .addOnFailureListener(err -> Toast.makeText(requireContext(),
                        "Error al crear: " + err.getMessage(), Toast.LENGTH_LONG).show());
        return true;
    }

    private void notificarDocente(String docenteId, String nombre, String codigo) {
        Notificacion n = new Notificacion();
        n.setReceptorId(docenteId);
        n.setCreadaPor(sessionManager.getUserId());
        n.setOrigenNombre(sessionManager.getUserName());
        n.setTipo("CURSO");
        n.setTitulo("Nuevo curso asignado");
        n.setCuerpo("Se te asignó " + nombre + " (" + codigo + ").");
        n.setLeida(false);
        n.setFecha(System.currentTimeMillis());
        notificacionRepository.crear(n);
    }

    // ============================================================
    // DETALLE + MATRÍCULA
    // ============================================================

    private void abrirDetalleCurso(Curso curso) {
        int inscritos = curso.getEstudiantesIds() != null ? curso.getEstudiantesIds().size() : 0;

        StringBuilder msg = new StringBuilder();
        msg.append("Código: ").append(nvl(curso.getCodigo(), "-")).append("\n");
        msg.append("Programa: ").append(nvl(curso.getPrograma(), "-")).append("\n");
        msg.append("Semestre: ").append(curso.getSemestre() > 0 ? curso.getSemestre() : "-")
                .append("\n");
        msg.append("Créditos: ").append(curso.getCreditos() > 0 ? curso.getCreditos() : "-")
                .append("\n");
        msg.append("Periodo: ").append(nvl(curso.getPeriodo(), "-")).append("\n");
        msg.append("Docente: ").append(nvl(curso.getDocenteNombre(), "-")).append("\n");
        msg.append("\nEstudiantes inscritos: ").append(inscritos);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(curso.getNombre())
                .setMessage(msg)
                .setPositiveButton("Matrícula de estudiantes", (d, w) -> abrirMatricula(curso))
                .setNeutralButton("Eliminar", (d, w) -> eliminarCurso(curso))
                .setNegativeButton("Cerrar", null)
                .show();
    }

    private void eliminarCurso(Curso curso) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Eliminar curso")
                .setMessage("¿Eliminar \"" + curso.getNombre() + "\"? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (d, w) ->
                        courseRepository.deleteCurso(curso.getId())
                                .addOnSuccessListener(v -> {
                                    Toast.makeText(requireContext(), "Curso eliminado",
                                            Toast.LENGTH_SHORT).show();
                                    new AuditRepository().registrar("CURSO_ELIMINADO",
                                            "Eliminó el curso " + nvl(curso.getNombre(), "-"),
                                            sessionManager);
                                    cargarCursos();
                                })
                                .addOnFailureListener(err -> Toast.makeText(requireContext(),
                                        "Error: " + err.getMessage(), Toast.LENGTH_LONG).show()))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void abrirMatricula(Curso curso) {
        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_inscribir_estudiantes, null);

        RecyclerView recyclerMat = v.findViewById(R.id.recycler_matricula);
        ProgressBar bar = v.findViewById(R.id.progress_matricula);
        TextView txtEmptyMat = v.findViewById(R.id.txt_matricula_empty);

        List<String> inscritos = new ArrayList<>();
        if (curso.getEstudiantesIds() != null) inscritos.addAll(curso.getEstudiantesIds());

        EstudiantesMatriculaAdapter adapterMat =
                new EstudiantesMatriculaAdapter(inscritos);
        recyclerMat.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerMat.setAdapter(adapterMat);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Matrícula · " + curso.getNombre())
                .setView(v)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();
        dialog.show();

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(x -> {
                    guardarMatricula(curso, inscritos, dialog);
                });

        bar.setVisibility(View.VISIBLE);
        txtEmptyMat.setVisibility(View.GONE);
        userRepository.getAllEstudiantes().addOnSuccessListener(snaps -> {
            bar.setVisibility(View.GONE);
            estudiantesList.clear();
            List<Usuario> ordenados = new ArrayList<>();
            for (DocumentSnapshot doc : snaps.getDocuments()) {
                Usuario u = doc.toObject(Usuario.class);
                if (u != null && u.getNombre() != null && !u.getNombre().trim().isEmpty()) {
                    u.setUid(doc.getId());
                    ordenados.add(u);
                }
            }
            ordenados.sort(Comparator.comparing(u -> u.getNombre()));
            estudiantesList.addAll(ordenados);
            adapterMat.setEstudiantes(estudiantesList);
            txtEmptyMat.setVisibility(estudiantesList.isEmpty() ? View.VISIBLE : View.GONE);
        }).addOnFailureListener(err -> {
            bar.setVisibility(View.GONE);
            txtEmptyMat.setVisibility(View.VISIBLE);
            Toast.makeText(requireContext(), "Error: " + err.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void guardarMatricula(Curso curso, List<String> nuevos, androidx.appcompat.app.AlertDialog dialog) {
        List<String> originales = new ArrayList<>();
        if (curso.getEstudiantesIds() != null) originales.addAll(curso.getEstudiantesIds());

        List<String> aAgregar = new ArrayList<>();
        List<String> aQuitar = new ArrayList<>();
        for (String id : nuevos) {
            if (!originales.contains(id)) aAgregar.add(id);
        }
        for (String id : originales) {
            if (!nuevos.contains(id)) aQuitar.add(id);
        }

        if (aAgregar.isEmpty() && aQuitar.isEmpty()) {
            dialog.dismiss();
            return;
        }

        List<Task<Void>> tasks = new ArrayList<>();
        for (String id : aAgregar) tasks.add(courseRepository.addEstudiante(curso.getId(), id));
        for (String id : aQuitar) tasks.add(courseRepository.removeEstudiante(curso.getId(), id));

        Tasks.whenAllComplete(tasks).addOnSuccessListener(completed -> {
            dialog.dismiss();
            Toast.makeText(requireContext(),
                    "Matrícula actualizada: +" + aAgregar.size() + " inscritos, -"
                            + aQuitar.size() + " retirados",
                    Toast.LENGTH_SHORT).show();
            new AuditRepository().registrar("MATRICULA",
                    "Actualizó matrícula de " + nvl(curso.getNombre(), "-") + ": +"
                            + aAgregar.size() + " inscritos, -" + aQuitar.size() + " retirados",
                    sessionManager);
            notificarNuevosInscritos(aAgregar, curso.getNombre());
            cargarCursos();
        }).addOnFailureListener(err -> Toast.makeText(requireContext(),
                "Error al guardar matrícula: " + err.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void notificarNuevosInscritos(List<String> aAgregar, String cursoNombre) {
        if (aAgregar.isEmpty()) return;
        Notificacion n = new Notificacion();
        n.setCreadaPor(sessionManager.getUserId());
        n.setOrigenNombre(sessionManager.getUserName());
        n.setTipo("CURSO");
        n.setTitulo("Nueva inscripción");
        n.setCuerpo("Fuiste inscrito(a) en " + cursoNombre + ".");
        n.setLeida(false);
        n.setFecha(System.currentTimeMillis());
        for (String estudianteId : aAgregar) {
            Notificacion copia = new Notificacion();
            copia.setReceptorId(estudianteId);
            copia.setCreadaPor(n.getCreadaPor());
            copia.setOrigenNombre(n.getOrigenNombre());
            copia.setTipo("CURSO");
            copia.setTitulo(n.getTitulo());
            copia.setCuerpo(n.getCuerpo());
            copia.setLeida(false);
            copia.setFecha(n.getFecha());
            notificacionRepository.crear(copia);
        }
    }

    private String nvl(String valor, String def) {
        return valor != null && !valor.isEmpty() ? valor : def;
    }
}