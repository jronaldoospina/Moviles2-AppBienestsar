package com.example.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapters.CourseStudentAdapter;
import com.example.myapplication.adapters.MassGradeAdapter;
import com.example.myapplication.databinding.ActivityCourseDetailBinding;
import com.example.myapplication.models.Alerta;
import com.example.myapplication.models.Curso;
import com.example.myapplication.models.Nota;
import com.example.myapplication.models.Notificacion;
import com.example.myapplication.FcmNotificationSender;
import com.example.myapplication.repositories.AlertRepository;
import com.example.myapplication.repositories.AuditRepository;
import com.example.myapplication.repositories.CourseRepository;
import com.example.myapplication.repositories.GradeRepository;
import com.example.myapplication.repositories.NotificacionRepository;
import com.example.myapplication.repositories.UserRepository;
import com.example.myapplication.services.SessionManager;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.Map;

public class CourseDetailActivity extends AppCompatActivity {

    public static final String EXTRA_CURSO_ID = "curso_id";

    private static final String[] MOTIVOS_LABEL = {
            "Bajo rendimiento", "Inasistencia", "Comportamiento", "Otro"
    };
    private static final String[] MOTIVOS_CODE = {
            "BAJO_RENDIMIENTO", "INASISTENCIA", "COMPORTAMIENTO", "OTRO"
    };

    private ActivityCourseDetailBinding binding;
    private CourseRepository courseRepository;
    private GradeRepository gradeRepository;
    private UserRepository userRepository;
    private AlertRepository alertRepository;
    private SessionManager sessionManager;

    private String cursoId;
    private Curso cursoActual;
    private String corteSeleccionadoId = "corte1";

    private CourseStudentAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCourseDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        courseRepository = new CourseRepository();
        gradeRepository = new GradeRepository();
        userRepository = new UserRepository();
        alertRepository = new AlertRepository();
        sessionManager = new SessionManager(this);

        cursoId = getIntent().getStringExtra(EXTRA_CURSO_ID);
        if (cursoId == null) {
            Toast.makeText(this, "Curso no especificado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new CourseStudentAdapter();
        adapter.setOnReportarListener(this::abrirDialogoAlerta);
        binding.recyclerEstudiantes.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerEstudiantes.setAdapter(adapter);

        binding.fabRegistrarNotas.setOnClickListener(v -> abrirDialogoRegistroMasivo());

        cargarCurso();
    }

    private void cargarCurso() {
        binding.progressDetail.setVisibility(View.VISIBLE);
        courseRepository.getCursoById(cursoId)
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        binding.progressDetail.setVisibility(View.GONE);
                        Toast.makeText(this, "Curso no encontrado", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    cursoActual = doc.toObject(Curso.class);
                    if (cursoActual != null) {
                        cursoActual.setId(doc.getId());
                    }

                    // Si no tiene cortes, agregar 3 por defecto
                    if (cursoActual.getCortes() == null || cursoActual.getCortes().isEmpty()) {
                        List<Curso.Corte> cortes = new ArrayList<>();
                        cortes.add(new Curso.Corte("corte1", "Corte 1", 0.33));
                        cortes.add(new Curso.Corte("corte2", "Corte 2", 0.33));
                        cortes.add(new Curso.Corte("corte3", "Corte 3", 0.34));
                        cursoActual.setCortes(cortes);
                    }

                    pintarInfo();
                    pintarCortes();
                    cargarEstudiantesYNotas();
                })
                .addOnFailureListener(e -> {
                    binding.progressDetail.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void pintarInfo() {
        binding.txtCursoNombre.setText(cursoActual.getNombre());
        binding.txtCursoCodigo.setText(cursoActual.getCodigo() + " · " + cursoActual.getPeriodo());
        binding.txtCursoEstudiantes.setText(cursoActual.getTotalEstudiantes() + " estudiantes");
    }

    private void pintarCortes() {
        binding.chipGroupCortes.removeAllViews();

        for (Curso.Corte corte : cursoActual.getCortes()) {
            Chip chip = new Chip(this);
            chip.setText(corte.getNombre());
            chip.setCheckable(true);
            chip.setId(View.generateViewId());
            chip.setTag(corte.getId());
            chip.setChecked(corte.getId().equals(corteSeleccionadoId));

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    corteSeleccionadoId = (String) buttonView.getTag();
                    cargarEstudiantesYNotas();
                }
            });

            binding.chipGroupCortes.addView(chip);
        }
    }

    /**
     * Carga los estudiantes del curso y las notas del corte seleccionado,
     * y los combina en el adapter.
     */
    private void cargarEstudiantesYNotas() {
        binding.progressDetail.setVisibility(View.VISIBLE);

        List<String> estudiantesIds = cursoActual.getEstudiantesIds();
        if (estudiantesIds == null) estudiantesIds = new ArrayList<>();

        if (estudiantesIds.isEmpty()) {
            adapter.setData(new ArrayList<>(), new HashMap<>());
            binding.progressDetail.setVisibility(View.GONE);
            return;
        }

        // Cargar notas del corte
        final List<String> finalEstIds = estudiantesIds;
        // Se usa getNotasByCurso para evitar requerimiento de índice compuesto (FAILED_PRECONDITION) y se filtra en código
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

                    // Cargar datos de los estudiantes
                    cargarDatosEstudiantes(finalEstIds, notasPorEstudiante);
                })
                .addOnFailureListener(e -> {
                    binding.progressDetail.setVisibility(View.GONE);
                    Toast.makeText(this, "Error notas: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void cargarDatosEstudiantes(List<String> ids, Map<String, Nota> notasPorEstudiante) {
        // Cargamos cada estudiante uno a uno (simple)
        List<CourseStudentAdapter.StudentRow> filas = new ArrayList<>();
        final int[] pendientes = {ids.size()};

        if (ids.isEmpty()) {
            binding.progressDetail.setVisibility(View.GONE);
            return;
        }

        for (String estId : ids) {
            userRepository.getUserById(estId)
                    .addOnSuccessListener(doc -> {
                        String nombre = "Estudiante";
                        String codigo = "-";
                        if (doc.exists()) {
                            nombre = doc.getString("nombre");
                            codigo = doc.getString("codigo");
                        }

                        Nota nota = notasPorEstudiante.get(estId);
                        filas.add(new CourseStudentAdapter.StudentRow(
                                estId, nombre, codigo, nota
                        ));

                        pendientes[0]--;
                        if (pendientes[0] == 0) {
                            // Ordenar por nombre
                            filas.sort((a, b) -> a.nombre.compareToIgnoreCase(b.nombre));
                            adapter.setData(filas, notasPorEstudiante);
                            binding.progressDetail.setVisibility(View.GONE);
                        }
                    })
                    .addOnFailureListener(e -> {
                        pendientes[0]--;
                        if (pendientes[0] == 0) {
                            adapter.setData(filas, notasPorEstudiante);
                            binding.progressDetail.setVisibility(View.GONE);
                        }
                    });
        }
    }

    // ============================================================
    // ALERTA AL BIENESTAR
    // ============================================================

    private void abrirDialogoAlerta(CourseStudentAdapter.StudentRow fila) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_alerta, null);
        TextInputEditText editDesc = v.findViewById(R.id.edit_alerta_desc);
        final int[] seleccion = {0};

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Reportar: " + fila.nombre)
                .setView(v)
                .setSingleChoiceItems(MOTIVOS_LABEL, 0, (d, which) -> seleccion[0] = which)
                .setPositiveButton("Enviar al bienestar", null)
                .setNegativeButton("Cancelar", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(x -> guardarAlerta(MOTIVOS_CODE[seleccion[0]], fila,
                        editDesc, dialog));
    }

    private void abrirDialogoReportarAlerta(CourseStudentAdapter.StudentRow fila) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_report_alert, null);
        TextInputEditText editDesc = v.findViewById(R.id.edit_alerta_desc);

        // Configurar chips de motivo
        int[] seleccionMotivo = {0}; // 0=Bajo rendimiento, 1=Inasistencia, 2=Comportamiento, 3=Otro
        Chip chipBajo = v.findViewById(R.id.chip_bajo);
        Chip chipInasistencia = v.findViewById(R.id.chip_inasistencia);
        Chip chipComportamiento = v.findViewById(R.id.chip_comportamiento);
        Chip chipOtro = v.findViewById(R.id.chip_otro);

        // Establecer listeners para los chips
        chipBajo.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) seleccionMotivo[0] = 0;
        });
        chipInasistencia.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) seleccionMotivo[0] = 1;
        });
        chipComportamiento.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) seleccionMotivo[0] = 2;
        });
        chipOtro.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) seleccionMotivo[0] = 3;
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Reportar: " + fila.nombre)
                .setView(v)
                .setPositiveButton("Enviar al bienestar", null)
                .setNegativeButton("Cancelar", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(x -> enviarAlertaAlumno(fila, seleccionMotivo[0],
                        editDesc, dialog));
    }

    private void enviarAlertaAlumno(CourseStudentAdapter.StudentRow fila, int motivoCodigo,
                                    TextInputEditText editDesc, AlertDialog dialog) {
        String motivo;
        switch (motivoCodigo) {
            case 1: motivo = "INASISTENCIA"; break;
            case 2: motivo = "COMPORTAMIENTO"; break;
            case 3: motivo = "OTRO"; break;
            default: motivo = "BAJO_RENDIMIENTO";
        }

        String descripcion = editDesc.getText() != null
                ? editDesc.getText().toString().trim() : "";

        Alerta a = new Alerta(
                sessionManager.getUserId(),
                sessionManager.getUserName(),
                fila.estudianteId,
                fila.nombre,
                cursoId,
                cursoActual != null ? cursoActual.getNombre() : null,
                motivo,
                descripcion
        );

        alertRepository.createAlerta(a)
                .addOnSuccessListener(doc -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Alerta enviada a bienestar", Toast.LENGTH_SHORT).show();
                    new AuditRepository().registrar("ALERTA_CREADA",
                            "Reportó alerta de " + fila.nombre
                                    + " en '" + (cursoActual != null ? cursoActual.getNombre() : "-") + "'",
                            sessionManager);
                    notificarPsicologos(a);
                    
                    // Enviar notificación FCM al psicólogo
                    String tokenPsi = sessionManager.getFcmToken();
                    if (tokenPsi != null && !tokenPsi.isEmpty()) {
                        String motivoTexto = FcmNotificationSender.formatMotivo(motivo);
                        FcmNotificationSender.enviarAlertaNotificacion(
                                tokenPsi,
                                fila.nombre,
                                motivoTexto,
                                cursoActual != null ? cursoActual.getNombre() : null,
                                descripcion
                        );
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }
    private void guardarAlerta(String motivo, CourseStudentAdapter.StudentRow fila,
                              TextInputEditText editDesc, AlertDialog dialog) {
        String descripcion = editDesc.getText() != null
                ? editDesc.getText().toString().trim() : "";

        Alerta a = new Alerta(
                sessionManager.getUserId(),
                sessionManager.getUserName(),
                fila.estudianteId,
                fila.nombre,
                cursoId,
                cursoActual != null ? cursoActual.getNombre() : null,
                motivo,
                descripcion
        );

        alertRepository.createAlerta(a)
                .addOnSuccessListener(doc -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Alerta enviada a bienestar", Toast.LENGTH_SHORT).show();
                    new AuditRepository().registrar("ALERTA_CREADA",
                            "Reportó alerta de " + fila.nombre
                                    + " en '" + (cursoActual != null ? cursoActual.getNombre() : "-") + "'",
                            sessionManager);
                    notificarPsicologos(a);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void notificarPsicologos(Alerta alerta) {
        userRepository.getAllUsers().addOnSuccessListener(snap -> {
            List<String> destinatarios = new ArrayList<>();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                if ("PSICOLOGO".equals(doc.getString("rol"))) {
                    Object asignadosObj = doc.get("estudiantesAsignados");
                    if (asignadosObj instanceof List
                            && ((List<?>) asignadosObj).contains(alerta.getEstudianteId())) {
                        destinatarios.add(doc.getId());
                    }
                }
            }
            // Fallback: si nadie está asignado, notificar a todos los psicólogos
            if (destinatarios.isEmpty()) {
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    if ("PSICOLOGO".equals(doc.getString("rol"))) {
                        destinatarios.add(doc.getId());
                    }
                }
            }

            NotificacionRepository notifRepo = new NotificacionRepository();
            String cuerpo = "Motivo: " + motivoLabel(alerta.getMotivo())
                    + (alerta.getDescripcion() != null && !alerta.getDescripcion().isEmpty()
                    ? " — " + alerta.getDescripcion() : "");
            if (cuerpo.length() > 140) cuerpo = cuerpo.substring(0, 140) + "…";

            for (String psiId : destinatarios) {
                Notificacion n = new Notificacion();
                n.setReceptorId(psiId);
                n.setCreadaPor(alerta.getDocenteId());
                n.setOrigenNombre(alerta.getDocenteNombre());
                n.setTipo("ALERTA");
                n.setTitulo("Nueva alerta de "
                        + (alerta.getEstudianteNombre() != null
                        ? alerta.getEstudianteNombre() : "un estudiante"));
                n.setCuerpo(cuerpo);
                n.setLeida(false);
                n.setFecha(System.currentTimeMillis());
                notifRepo.crear(n);
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "No se pudo notificar: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
    }

    private String motivoLabel(String motivo) {
        if (motivo == null) return "";
        switch (motivo) {
            case "INASISTENCIA":   return "Inasistencia";
            case "COMPORTAMIENTO": return "Comportamiento";
            case "OTRO":           return "Otro";
            default:               return "Bajo rendimiento";
        }
    }

    // ============================================================
    // REGISTRO MASIVO DE NOTAS
    // ============================================================

    private MassGradeAdapter massGradeAdapter;

private void abrirDialogoRegistroMasivo() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_registro_masivo, null);
        TextView txtCorte = v.findViewById(R.id.txt_corte_titulo);
        RecyclerView recyclerEstudiantes = v.findViewById(R.id.recycler_estudiantes_notas);
        MaterialButton btnGuardarTodas = v.findViewById(R.id.btn_guardar_todas);
        ProgressBar progressCargando = v.findViewById(R.id.progress_cargando);
        TextView txtVacio = v.findViewById(R.id.txt_vacio);

        String nombreCorte = "Corte " + corteSeleccionadoId.replace("corte", "");
        if (cursoActual != null && cursoActual.getCortes() != null) {
            for (Curso.Corte c : cursoActual.getCortes()) {
                if (corteSeleccionadoId.equals(c.getId())) {
                    nombreCorte = c.getNombre();
                    break;
                }
            }
        }
        txtCorte.setText("Corte: " + nombreCorte);

        // Obtener datos de estudiantes
        List<String> estudianteIds = cursoActual != null ? cursoActual.getEstudiantesIds() : new ArrayList<>();
        final List<String> finalEstudianteIds = estudianteIds;
        final List<String> estudianteNombres = new ArrayList<>();
        final List<String> estudianteCodigos = new ArrayList<>();

        if (estudianteIds != null && !estudianteIds.isEmpty()) {
            int total = estudianteIds.size();
            AtomicInteger cargados = new AtomicInteger(0);
            progressCargando.setVisibility(View.VISIBLE);
            txtVacio.setVisibility(View.GONE);
            for (String estId : estudianteIds) {
                UserRepository userRepo = new UserRepository();
                userRepo.getUserById(estId).addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String nombre = doc.getString("nombre");
                        String codigo = doc.getString("codigo");
                        if (nombre != null) estudianteNombres.add(nombre);
                        else estudianteNombres.add("Estudiante " + estId);
                        if (codigo != null) estudianteCodigos.add(codigo.toUpperCase());
                        else estudianteCodigos.add(estId);
                    }
                    int currentLoaded = cargados.incrementAndGet();
                    if (currentLoaded >= total) {
                        // Todos los datos cargados, crear adapter
                        progressCargando.setVisibility(View.GONE);
                        massGradeAdapter = new MassGradeAdapter(
                                finalEstudianteIds, estudianteNombres, estudianteCodigos, cursoActual,
                                new MassGradeAdapter.OnNotaChangedListener() {
                                    @Override
                                    public void onAllNotesReady(Map<String, Double> notas) {}
                                    @Override
                                    public void onNoteInvalid(String codigo, String error) {}
                                }
                        );
                        recyclerEstudiantes.setLayoutManager(new LinearLayoutManager(CourseDetailActivity.this));
                        recyclerEstudiantes.setAdapter(massGradeAdapter);
                        btnGuardarTodas.setEnabled(true);
                        txtVacio.setVisibility(View.GONE);
                    }
                });
            }
        } else {
            // Sin estudiantes, crear adapter vacío inmediatamente
            progressCargando.setVisibility(View.GONE);
            massGradeAdapter = new MassGradeAdapter(
                    new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(), cursoActual,
                    new MassGradeAdapter.OnNotaChangedListener() {
                        @Override
                        public void onAllNotesReady(Map<String, Double> notas) {}
                        @Override
                        public void onNoteInvalid(String codigo, String error) {}
                    }
            );
            recyclerEstudiantes.setLayoutManager(new LinearLayoutManager(CourseDetailActivity.this));
            recyclerEstudiantes.setAdapter(massGradeAdapter);
            btnGuardarTodas.setEnabled(true);
            txtVacio.setVisibility(View.VISIBLE);
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Registro masivo de notas")
                .setView(v)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();

        // Handler para esperar carga de datos (fallback)
        final Handler handler = new Handler();
        handler.postDelayed(() -> {
            // Si el adapter aún no fue creado (carga lenta), crearlo ahora
            if (massGradeAdapter == null) {
                progressCargando.setVisibility(View.GONE);
                massGradeAdapter = new MassGradeAdapter(
                        estudianteIds, estudianteNombres, estudianteCodigos, cursoActual,
                        new MassGradeAdapter.OnNotaChangedListener() {
                            @Override
                            public void onAllNotesReady(Map<String, Double> notas) {}
                            @Override
                            public void onNoteInvalid(String codigo, String error) {}
                        }
                );
                recyclerEstudiantes.setLayoutManager(new LinearLayoutManager(CourseDetailActivity.this));
                recyclerEstudiantes.setAdapter(massGradeAdapter);
                btnGuardarTodas.setEnabled(true);
                txtVacio.setVisibility(estudianteIds == null || estudianteIds.isEmpty() ? View.VISIBLE : View.GONE);
            }
        }, 2000);

        btnGuardarTodas.setOnClickListener(btnView -> {
            if (massGradeAdapter == null) {
                Toast.makeText(this, "Cargando estudiantes...", Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, Double> notas = massGradeAdapter.getNotasTemp();
            guardarNotasMasivasConMapa(notas, dialog);
        });

        dialog.show();
        
        // Forzar que el teclado se muestre por encima del diálogo
        if (dialog.getWindow() != null) {
            dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    private void guardarNotasMasivasConMapa(Map<String, Double> notas, AlertDialog dialog) {
        if (notas.isEmpty()) {
            Toast.makeText(this, "No hay notas para guardar", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> ids = cursoActual != null ? cursoActual.getEstudiantesIds() : null;
        if (ids == null || ids.isEmpty()) {
            Toast.makeText(this, "Este curso no tiene estudiantes", Toast.LENGTH_SHORT).show();
            return;
        }

        // Filtrar solo las que tienen valores válidos
        List<Map.Entry<String, Double>> paraGuardar = new ArrayList<>();
        for (Map.Entry<String, Double> entry : notas.entrySet()) {
            if (entry.getValue() != null && entry.getValue() >= 0.0 && entry.getValue() <= 5.0) {
                paraGuardar.add(entry);
            }
        }

        if (paraGuardar.isEmpty()) {
            Toast.makeText(this, "No hay notas válidas para guardar (deben ser 0.0 - 5.0)", 
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Cargar notas existentes usando solo cursoId para evitar FAILED_PRECONDITION por falta de índice compuesto
        gradeRepository.getNotasByCurso(cursoId)
                .addOnSuccessListener(notasSnap -> {
                    // Construir mapa de notas existentes: estudianteId -> Nota
                    Map<String, Nota> notasExistentes = new HashMap<>();
                    for (DocumentSnapshot doc : notasSnap.getDocuments()) {
                        Nota n = doc.toObject(Nota.class);
                        if (n != null && corteSeleccionadoId.equals(n.getCorteId())) {
                            n.setId(doc.getId());
                            notasExistentes.put(n.getEstudianteId(), n);
                        }
                    }

                    final int total = paraGuardar.size();
                    final int[] creadas = {0};
                    final int[] actualizadas = {0};
                    final int[] errores = {0};
                    final int[] terminadas = {0};

                    for (Map.Entry<String, Double> entry : paraGuardar) {
                        String estId = entry.getKey();
                        double valor = entry.getValue();

                        boolean yaExiste = notasExistentes.containsKey(estId);
                        final boolean esActualizacion = yaExiste;

                        Map<String, Object> up = new HashMap<>();
                        up.put("valor", valor);
                        up.put("nivelRiesgo", semaforoDe(valor));
                        up.put("registradoPor", sessionManager.getUserId());

                        Task<Void> tarea;
                        if (esActualizacion && notasExistentes.get(estId) != null) {
                            // Actualizar existente
                            tarea = gradeRepository.updateNota(notasExistentes.get(estId).getId(), up);
                        } else {
                            // Crear nueva nota
                            Nota n = new Nota(estId, cursoId, corteSeleccionadoId, valor,
                                    sessionManager.getUserId());
                            n.setNivelRiesgo(semaforoDe(valor));
                            tarea = gradeRepository.createNota(n).continueWith(t -> null);
                        }

                        tarea.addOnSuccessListener(v -> {
                            if (esActualizacion) actualizadas[0]++; else creadas[0]++;
                            terminadas[0]++;
                            if (terminadas[0] == total) {
                                terminarRegistroMasivo(dialog, creadas[0], actualizadas[0], errores[0]);
                            }
                        }).addOnFailureListener(e -> {
                            errores[0]++;
                            terminadas[0]++;
                            if (terminadas[0] == total) {
                                terminarRegistroMasivo(dialog, creadas[0], actualizadas[0], errores[0]);
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al cargar notas existentes: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    if (dialog != null && dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    }
                });
    }

    private void terminarRegistroMasivo(AlertDialog dialog, int creadas, int actualizadas, int errores) {
        if (dialog != null) dialog.dismiss();
        String msg = "Guardadas: " + creadas;
        if (actualizadas > 0) msg += " · Actualizadas: " + actualizadas;
        if (errores > 0) msg += " · Errores: " + errores;
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        cargarEstudiantesYNotas();
    }

    private void guardarNotasMasivas(TextInputEditText editNotas, AlertDialog dialog) {
        String texto = editNotas.getText() != null
                ? editNotas.getText().toString().trim() : "";
        List<Object[]> pares = parsearPares(texto);
        if (pares.isEmpty()) {
            Toast.makeText(this, "No hay datos válidos. Formato: CODIGO NOTA",
                    Toast.LENGTH_LONG).show();
            return;
        }

        List<String> ids = cursoActual != null
                ? cursoActual.getEstudiantesIds() : null;
        if (ids == null || ids.isEmpty()) {
            Toast.makeText(this, "Este curso no tiene estudiantes", Toast.LENGTH_SHORT).show();
            return;
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

        Map<String, String> codigoAId = new HashMap<>();
        final int[] pendientes = {ids.size()};
        for (String estId : ids) {
            userRepository.getUserById(estId)
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String codigo = doc.getString("codigo");
                            if (codigo != null && !codigo.trim().isEmpty()) {
                                codigoAId.put(codigo.trim().toUpperCase(), estId);
                            }
                        }
                        pendientes[0]--;
                        if (pendientes[0] == 0) {
                            aplicarNotasMasivas(pares, codigoAId, dialog);
                        }
                    })
                    .addOnFailureListener(e -> {
                        pendientes[0]--;
                        if (pendientes[0] == 0) {
                            aplicarNotasMasivas(pares, codigoAId, dialog);
                        }
                    });
        }
    }

    private List<Object[]> parsearPares(String texto) {
        List<Object[]> pares = new ArrayList<>();
        for (String linea : texto.split("\\r?\\n")) {
            linea = linea.trim();
            if (linea.isEmpty()) continue;
            String[] tokens = linea.split("[\\s;,]+");
            if (tokens.length < 2) continue;
            String codigo = tokens[0].trim();
            double valor;
            try {
                valor = Double.parseDouble(tokens[tokens.length - 1].replace(',', '.'));
            } catch (NumberFormatException ex) {
                continue;
            }
            if (valor < 0.0 || valor > 5.0) continue;
            pares.add(new Object[]{codigo.toUpperCase(), valor});
        }
        return pares;
    }

    private void aplicarNotasMasivas(List<Object[]> pares, Map<String, String> codigoAId,
                                     AlertDialog dialog) {
        gradeRepository.getNotasByCursoAndCorte(cursoId, corteSeleccionadoId)
                .addOnSuccessListener(notasSnap -> {
                    Map<String, Nota> existentes = new HashMap<>();
                    for (DocumentSnapshot doc : notasSnap.getDocuments()) {
                        Nota n = doc.toObject(Nota.class);
                        if (n != null) {
                            n.setId(doc.getId());
                            existentes.put(n.getEstudianteId(), n);
                        }
                    }

                    final int total = pares.size();
                    final int[] creadas = {0}, actualizadas = {0}, sinMatch = {0}, errores = {0};
                    final int[] terminadas = {0};

                    for (Object[] par : pares) {
                        String codigo = (String) par[0];
                        double valor = (Double) par[1];
                        String estId = codigoAId.get(codigo);

                        if (estId == null) {
                            sinMatch[0]++;
                            terminadas[0]++;
                            if (terminadas[0] == total) {
                                terminarRegistro(dialog, creadas[0], actualizadas[0],
                                        sinMatch[0], errores[0]);
                            }
                            continue;
                        }

                        Nota existente = existentes.get(estId);
                        final boolean esActualizacion = existente != null;
                        Task<Void> tarea;
                        if (esActualizacion) {
                            Map<String, Object> up = new HashMap<>();
                            up.put("valor", valor);
                            up.put("nivelRiesgo", semaforoDe(valor));
                            up.put("registradoPor", sessionManager.getUserId());
                            tarea = gradeRepository.updateNota(existente.getId(), up);
                        } else {
                            Nota n = new Nota(estId, cursoId, corteSeleccionadoId, valor,
                                    sessionManager.getUserId());
                            n.setNivelRiesgo(semaforoDe(valor));
                            tarea = gradeRepository.createNota(n).continueWith(t -> null);
                        }

                        tarea.addOnSuccessListener(v -> {
                            if (esActualizacion) actualizadas[0]++; else creadas[0]++;
                            terminadas[0]++;
                            if (terminadas[0] == total) {
                                terminarRegistro(dialog, creadas[0], actualizadas[0],
                                        sinMatch[0], errores[0]);
                            }
                        }).addOnFailureListener(e -> {
                            errores[0]++;
                            terminadas[0]++;
                            if (terminadas[0] == total) {
                                terminarRegistro(dialog, creadas[0], actualizadas[0],
                                        sinMatch[0], errores[0]);
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String semaforoDe(double valor) {
        if (valor >= 4.0) return "BAJO";
        if (valor >= 3.0) return "MEDIO";
        return "ALTO";
    }

    private void terminarRegistro(AlertDialog dialog, int creadas, int actualizadas,
                                  int sinMatch, int errores) {
        dialog.dismiss();
        Toast.makeText(this,
                "Guardadas: " + creadas + " · Actualizadas: " + actualizadas
                        + " · Sin coincidir: " + sinMatch
                        + (errores > 0 ? " · Errores: " + errores : ""),
                Toast.LENGTH_LONG).show();
        cargarEstudiantesYNotas();
    }

    // ============================================================
    // REGISTRO INDIVIDUAL DE NOTAS
    // ============================================================

    private void abrirDialogoRegistroIndividual(CourseStudentAdapter.StudentRow fila) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_register_grade, null);
        TextView txtEstudiante = v.findViewById(R.id.txt_estudiante_nombre);
        TextInputEditText editNota = v.findViewById(R.id.edit_nota_valor);
        TextInputEditText editObservacion = v.findViewById(R.id.edit_observacion);

        txtEstudiante.setText(fila.nombre);
        editNota.setText("");

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Registrar nota para " + fila.nombre)
                .setView(v)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(x -> guardarNotaIndividual(fila, editNota, editObservacion, dialog));
    }

    private void guardarNotaIndividual(CourseStudentAdapter.StudentRow fila,
                                       TextInputEditText editNota, TextInputEditText editObservacion,
                                       AlertDialog dialog) {
        String textoNota = editNota.getText() != null
                ? editNota.getText().toString().trim() : "";
        String observacion = editObservacion.getText() != null
                ? editObservacion.getText().toString().trim() : "";

        if (textoNota.isEmpty()) {
            Toast.makeText(this, "Ingrese una nota", Toast.LENGTH_SHORT).show();
            return;
        }

        double valor;
        try {
            valor = Double.parseDouble(textoNota);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Formato de número inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        if (valor < 0.0 || valor > 5.0) {
            Toast.makeText(this, "La nota debe estar entre 0.0 y 5.0", Toast.LENGTH_SHORT).show();
            return;
        }

        Nota n = new Nota(
                fila.estudianteId,
                cursoId,
                corteSeleccionadoId,
                valor,
                sessionManager.getUserId()
        );
        n.setObservacion(observacion);
        n.setNivelRiesgo(semaforoDe(valor));

        gradeRepository.createNota(n)
                .addOnSuccessListener(doc -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Nota guardada correctamente", Toast.LENGTH_SHORT).show();
                    cargarEstudiantesYNotas();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al guardar nota: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_course_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_registrar_nota_individual) {
            List<CourseStudentAdapter.StudentRow> filas = adapter.getFilas();
            if (filas != null && !filas.isEmpty()) {
                abrirDialogoRegistroIndividual(filas.get(0));
            } else {
                Toast.makeText(this, "No hay estudiantes en este corte", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        if (item.getItemId() == R.id.action_registro_masivo) {
            abrirDialogoRegistroMasivo();
            return true;
        }
        if (item.getItemId() == R.id.action_reporte_alerta) {
            // Obtener el primer estudiante de la lista visible
            List<CourseStudentAdapter.StudentRow> filas = adapter.getFilas();
            if (filas != null && !filas.isEmpty()) {
                abrirDialogoReportarAlerta(filas.get(0));
            } else {
                Toast.makeText(this, "No hay estudiantes en este corte", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}