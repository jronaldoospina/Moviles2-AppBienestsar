package com.example.myapplication.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapters.GradesAdapter;
import com.example.myapplication.models.Curso;
import com.example.myapplication.models.Nota;
import com.example.myapplication.repositories.CourseRepository;
import com.example.myapplication.repositories.GradeRepository;
import com.example.myapplication.services.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegisterGradesFragment extends Fragment {

    private static final String EXTRA_CURSO_ID = "curso_id";

    private String cursoId;
    private Curso cursoActual;
    private GradeRepository gradeRepository;
    private SessionManager sessionManager;

    private GradesAdapter adapter;
    private RecyclerView recyclerEstudiantes;
    private TextView txtCorteActual;
    private Button btnGuardarTodas;
    private ProgressBar progressBar;
    private List<String> estudianteIds;
    private List<String> estudianteNombres;
    private List<String> estudianteCodigos;

    public static RegisterGradesFragment newInstance(String cursoId) {
        RegisterGradesFragment fragment = new RegisterGradesFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_CURSO_ID, cursoId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register_grades, container, false);

        cursoId = getArguments() != null ? getArguments().getString(EXTRA_CURSO_ID) : null;

        sessionManager = new SessionManager(requireContext());
        gradeRepository = new GradeRepository();

        txtCorteActual = view.findViewById(R.id.txt_corte_actual);
        recyclerEstudiantes = view.findViewById(R.id.recycler_estudiantes);
        btnGuardarTodas = view.findViewById(R.id.btn_guardar_todas);
        progressBar = view.findViewById(R.id.progress_bar);

        recyclerEstudiantes.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new GradesAdapter(new ArrayList<>(), new HashMap<>());
        recyclerEstudiantes.setAdapter(adapter);

        btnGuardarTodas.setOnClickListener(v -> guardarNotas());

        cargarCursoYEstudiantes();

        return view;
    }

    private void cargarCursoYEstudiantes() {
        if (cursoId == null) {
            Toast.makeText(requireContext(), "Curso no especificado", Toast.LENGTH_SHORT).show();
            requireActivity().finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        CourseRepository courseRepository = new CourseRepository();
        courseRepository.getCursoById(cursoId)
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(requireContext(), "Curso no encontrado", Toast.LENGTH_SHORT).show();
                        requireActivity().finish();
                        return;
                    }
                    cursoActual = doc.toObject(Curso.class);
                    if (cursoActual != null) {
                        cursoActual.setId(doc.getId());
                    }
                    pintarInfo();
                    cargarEstudiantes();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void pintarInfo() {
        if (cursoActual != null) {
            String nombreCorte = "Corte actual";
            if (cursoActual.getCortes() != null) {
                for (Curso.Corte c : cursoActual.getCortes()) {
                    if (c.getId().equals(corteSeleccionadoId)) {
                        nombreCorte = c.getNombre();
                        break;
                    }
                }
            }
            txtCorteActual.setText("Corte: " + nombreCorte);
        }
    }

    private String corteSeleccionadoId = "corte1";

    private void cargarEstudiantes() {
        if (cursoActual == null || cursoActual.getEstudiantesIds() == null) {
            estudianteIds = new ArrayList<>();
            estudianteNombres = new ArrayList<>();
            estudianteCodigos = new ArrayList<>();
            adapter = new GradesAdapter(estudianteIds, estudianteNombres, estudianteCodigos, null, null);
            recyclerEstudiantes.setAdapter(adapter);
            progressBar.setVisibility(View.GONE);
            return;
        }

        estudianteIds = cursoActual.getEstudiantesIds();
        estudianteNombres = new ArrayList<>();
        estudianteCodigos = new ArrayList<>();

        if (estudianteIds != null) {
            for (String estId : estudianteIds) {
                // Cargar datos del usuario
                com.example.myapplication.repositories.UserRepository userRepo = 
                    new com.example.myapplication.repositories.UserRepository();
                userRepo.getUserById(estId).addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String nombre = doc.getString("nombre");
                        String codigo = doc.getString("codigo");
                        if (nombre != null) estudianteNombres.add(nombre);
                        else estudianteNombres.add("Estudiante " + estId);
                        if (codigo != null) estudianteCodigos.add(codigo.toUpperCase());
                        else estudianteCodigos.add(estId);
                    }
                    // Cuando hayamos cargado todos los estudiantes
                    if (estudianteNombres.size() >= estudianteIds.size()) {
                        // Cargar notas existentes
                        cargarNotasExistentes();
                    }
                });
            }
        }
    }

    private void cargarNotasExistentes() {
        if (cursoActual == null || cursoId == null || estudianteIds == null) return;

        gradeRepository.getNotasByCurso(cursoId)
                .addOnSuccessListener(notasSnap -> {
                    Map<String, Nota> notasPorEstudiante = new HashMap<>();
                    notasSnap.forEach(doc -> {
                        Nota n = doc.toObject(Nota.class);
                        if (n != null && corteSeleccionadoId.equals(n.getCorteId())) {
                            n.setId(doc.getId());
                            notasPorEstudiante.put(n.getEstudianteId(), n);
                        }
                    });

                    // Configurar adapter con datos
                    adapter = new GradesAdapter(estudianteIds, estudianteNombres, estudianteCodigos, cursoActual, notasPorEstudiante, (estudianteId, valor) -> {
                        // Callback opcional para validación en tiempo real
                    });
                    recyclerEstudiantes.setAdapter(adapter);
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Error cargando notas: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void guardarNotas() {
        if (adapter == null) {
            Toast.makeText(requireContext(), "No hay datos para guardar", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Double> notas = adapter.getNotas();
        if (notas.isEmpty()) {
            Toast.makeText(requireContext(), "No hay notas para guardar", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        GradeRepository gradeRepo = new GradeRepository();
        gradeRepo.guardarNotasMasivas(notas, cursoId, corteSeleccionadoId)
                .addOnSuccessListener(doc -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Notas guardadas exitosamente", Toast.LENGTH_SHORT).show();
                    requireActivity().onBackPressed();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Error guardando notas: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}