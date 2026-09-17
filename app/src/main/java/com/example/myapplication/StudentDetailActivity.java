package com.example.myapplication;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.myapplication.models.Reporte;
import com.example.myapplication.models.Notificacion;
import com.example.myapplication.models.ReporteEmocional;
import com.example.myapplication.models.Conversacion;
import com.example.myapplication.models.Usuario;
import com.example.myapplication.repositories.ChatRepository;
import com.example.myapplication.repositories.EmotionalReportRepository;
import com.example.myapplication.repositories.AuditRepository;
import com.example.myapplication.repositories.NotificacionRepository;
import com.example.myapplication.repositories.ReportRepository;
import com.example.myapplication.repositories.UserRepository;
import com.example.myapplication.services.SessionManager;
import com.example.myapplication.utils.AnimoUtils;
import com.example.myapplication.utils.FechaUtils;
import com.example.myapplication.utils.PdfExporter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ESTUDIANTE_ID = "estudiante_id";

    private static final int REQ_EXPORTAR_PDF = 987;
    private static final int REQ_EXPORTAR_PLAN_PDF = 988;
    private static final String[] ESTADOS = {"ABIERTO", "EN_SEGUIMIENTO", "ATENDIDO"};
    private static final String[] ESTADOS_LABEL = {"Abierto", "En seguimiento", "Atendido"};

    private String nombreArchivo = "expediente";
    private List<String> lineasExportar = new ArrayList<>();
    private List<String> lineasExportarPlan = new ArrayList<>();

    private MaterialToolbar toolbar;
    private TextView txtIniciales, txtNombre, txtEmail, txtRiesgo;
    private TextView txtCodigo, txtPrograma, txtSemestre, txtPromedio, txtTelefono;
    private TextView txtReporteCount;
    private View progressDetail;
    private View emocionalContent, dotEmocional;
    private TextView txtEmocionalNone, txtEmocionalAnimo, txtEmocionalFecha, txtEmocionalDesc;
    private MaterialButton btnNuevoReporte, btnNuevoPlan, btnSemaforo;
    private LinearLayout reportesContainer;

    private UserRepository userRepository;
    private EmotionalReportRepository emotionalRepository;
    private ReportRepository reportRepository;
    private SessionManager sessionManager;
    private String estudianteId;
    private String estudianteNombre = "";

    private final List<Reporte> reportes = new ArrayList<>();
    private final List<ReporteEmocional> historialEmocional = new ArrayList<>();

    private int selectedTipo = 0;       // 0 = REPORTE, 1 = PLAN
    private String selectedRiesgo = "MEDIO";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_detail);

        estudianteId = getIntent().getStringExtra(EXTRA_ESTUDIANTE_ID);
        if (estudianteId == null) {
            Toast.makeText(this, "Estudiante no encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userRepository = new UserRepository();
        emotionalRepository = new EmotionalReportRepository();
        reportRepository = new ReportRepository();
        sessionManager = new SessionManager(this);

        initViews();
        configurarToolbar();
        cargarEstudiante();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        txtIniciales = findViewById(R.id.txt_iniciales);
        txtNombre = findViewById(R.id.txt_nombre);
        txtEmail = findViewById(R.id.txt_email);
        txtRiesgo = findViewById(R.id.txt_riesgo);
        txtCodigo = findViewById(R.id.txt_codigo);
        txtPrograma = findViewById(R.id.txt_programa);
        txtSemestre = findViewById(R.id.txt_semestre);
        txtPromedio = findViewById(R.id.txt_promedio);
        txtTelefono = findViewById(R.id.txt_telefono);
        txtReporteCount = findViewById(R.id.txt_reporte_count);
        progressDetail = findViewById(R.id.progress_detail);
        btnNuevoReporte = findViewById(R.id.btn_nuevo_reporte);
        btnNuevoPlan = findViewById(R.id.btn_nuevo_plan);
        btnSemaforo = findViewById(R.id.btn_semaforo);
        reportesContainer = findViewById(R.id.reportes_container);
        emocionalContent = findViewById(R.id.emocional_content);
        dotEmocional = findViewById(R.id.dot_emocional);
        txtEmocionalNone = findViewById(R.id.txt_emocional_none);
        txtEmocionalAnimo = findViewById(R.id.txt_emocional_animo);
        txtEmocionalFecha = findViewById(R.id.txt_emocional_fecha);
        txtEmocionalDesc = findViewById(R.id.txt_emocional_desc);

        btnNuevoReporte.setOnClickListener(v -> mostrarDialogoCrear(0));
        btnNuevoPlan.setOnClickListener(v -> mostrarDialogoCrear(1));
        btnSemaforo.setOnClickListener(v -> abrirSemaforoPdf());
    }

    private void abrirSemaforoPdf() {
        Intent i = new Intent(this, SemaforoPdfActivity.class);
        i.putExtra(SemaforoPdfActivity.EXTRA_ESTUDIANTE_ID, estudianteId);
        startActivity(i);
    }

    // ============================================================
    // CHAT CON EL ESTUDIANTE
    // ============================================================

    private void abrirOCrearConversacion() {
        // Lógica de chat eliminada
    }

    private void configurarToolbar() {
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.inflateMenu(R.menu.menu_export);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_export_pdf) {
                exportarExpediente();
                return true;
            }
            return false;
        });
    }

    private void exportarExpediente() {
        List<String> lineas = new ArrayList<>();

        lineas.add("Expediente del estudiante");
        lineas.add("Nombre: " + txtNombre.getText());
        lineas.add("Código: " + txtCodigo.getText());
        lineas.add("Programa: " + txtPrograma.getText());
        lineas.add("Semestre: " + txtSemestre.getText());
        lineas.add("Promedio: " + txtPromedio.getText());
        lineas.add("Nivel de riesgo: " + txtRiesgo.getText());
        lineas.add("");

        lineas.add("Estado emocional: " + txtEmocionalAnimo.getText());
        lineas.add("Última actualización: " + txtEmocionalFecha.getText());
        if (txtEmocionalDesc.getVisibility() == View.VISIBLE) {
            lineas.add("Detalle: " + txtEmocionalDesc.getText());
        }
        lineas.add("");

        long corte30 = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000L;
        lineas.add("Historial emocional (últimos 30 días):");
        int nHist = 0;
        for (ReporteEmocional re : historialEmocional) {
            if (re.getFecha() < corte30) continue;
            String desc = re.getDescripcion() != null ? re.getDescripcion() : "";
            if (desc.length() > 120) desc = desc.substring(0, 120) + "…";
            lineas.add("· " + FechaUtils.fechaBonitaLong(re.getFecha())
                    + " · Ánimo: " + AnimoUtils.label(re.getAnimo())
                    + (desc.isEmpty() ? "" : " · " + desc));
            nHist++;
        }
        if (nHist == 0) {
            lineas.add("Sin registros en los últimos 30 días.");
        }
        lineas.add("");

        lineas.add("Registros (" + txtReporteCount.getText() + "):");
        for (Reporte r : reportes) {
            boolean esPlan = "PLAN".equals(r.getTipo());
            lineas.add((esPlan ? "PLAN: " : "REPORTE: ") + r.getTitulo());
            lineas.add("   Estado: " + (r.isPlanCumplido() ? "CUMPLIDO" : estadoLabel(r.getEstado()))
                    + " · Riesgo: " + (r.getNivelRiesgo() != null ? r.getNivelRiesgo() : "-")
                    + " · " + FechaUtils.fechaBonitaLong(r.getFecha()));
            if (r.getDescripcion() != null && !r.getDescripcion().isEmpty()) {
                lineas.add("   " + r.getDescripcion());
            }
            if (r.getAvances() != null) {
                for (Map<String, Object> a : r.getAvances()) {
                    Object f = a != null ? a.get("fecha") : null;
                    Object t = a != null ? a.get("texto") : null;
                    long ms = f instanceof Long ? (Long) f : 0L;
                    lineas.add("   · " + (ms > 0 ? FechaUtils.fechaBonitaLong(ms) : "")
                            + " — " + (t != null ? t : ""));
                }
            }
            lineas.add("");
        }

        lineasExportar = lineas;
        nombreArchivo = "Expediente_" + estudianteNombre.replaceAll("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ _-]", "")
                .replace(' ', '_') + ".pdf";

        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/pdf");
        i.putExtra(Intent.EXTRA_TITLE, nombreArchivo);
        startActivityForResult(i, REQ_EXPORTAR_PDF);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQ_EXPORTAR_PDF || requestCode == REQ_EXPORTAR_PLAN_PDF)
                && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) return;
            boolean expediente = requestCode == REQ_EXPORTAR_PDF;
            final List<String> lineas = expediente ? lineasExportar : lineasExportarPlan;
            String titulo = expediente
                    ? "Expediente de " + (estudianteNombre != null ? estudianteNombre : "Estudiante")
                    : "Plan de acción de " + (estudianteNombre != null ? estudianteNombre : "Estudiante");
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out == null) return;
                PdfExporter.escribirExpediente(out,
                        titulo,
                        "Generado por " + sessionManager.getUserName()
                                + " · " + FechaUtils.fechaBonitaLong(System.currentTimeMillis()),
                        lineas);
                Toast.makeText(this, "PDF exportado correctamente", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(this, "Error al exportar: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void exportarPlanPdf(Reporte r) {
        List<String> lineas = new ArrayList<>();
        lineas.add("Estudiante: " + estudianteNombre);
        lineas.add("Código: " + txtCodigo.getText());
        lineas.add("Creado: " + FechaUtils.fechaBonitaLong(r.getFecha()));
        lineas.add("Riesgo: " + (r.getNivelRiesgo() != null ? r.getNivelRiesgo() : "-"));
        lineas.add("Estado: " + (r.isPlanCumplido() ? "CUMPLIDO" : estadoLabel(r.getEstado())));
        lineas.add("");
        if (r.getDescripcion() != null && !r.getDescripcion().isEmpty()) {
            lineas.add("Descripción:");
            lineas.add(r.getDescripcion());
            lineas.add("");
        }
        lineas.add("Avances registrados:");
        List<Map<String, Object>> avances = r.getAvances();
        if (avances != null && !avances.isEmpty()) {
            for (Map<String, Object> a : avances) {
                Object f = a != null ? a.get("fecha") : null;
                Object t = a != null ? a.get("texto") : null;
                long ms = f instanceof Long ? (Long) f : 0L;
                lineas.add("· " + (ms > 0 ? FechaUtils.fechaBonitaLong(ms) : "")
                        + " — " + (t != null ? t : ""));
            }
        } else {
            lineas.add("Aún no hay avances registrados.");
        }

        lineasExportarPlan = lineas;
        nombreArchivo = "PlanAccion_" + estudianteNombre.replaceAll("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ _-]", "")
                .replace(' ', '_') + ".pdf";

        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/pdf");
        i.putExtra(Intent.EXTRA_TITLE, nombreArchivo);
        startActivityForResult(i, REQ_EXPORTAR_PLAN_PDF);
    }

    private void cargarEstudiante() {
        progressDetail.setVisibility(View.VISIBLE);

        userRepository.getUserById(estudianteId)
                .addOnSuccessListener(doc -> {
                    progressDetail.setVisibility(View.GONE);

                    if (!doc.exists()) {
                        Toast.makeText(this, "Estudiante no encontrado", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    Usuario u = doc.toObject(Usuario.class);
                    if (u == null) return;
                    u.setUid(doc.getId());
                    estudianteNombre = u.getNombre() != null ? u.getNombre() : "";
                    pintarEstudiante(u);
                    cargarEmocional();
                    cargarReportes();
                })
                .addOnFailureListener(e -> {
                    progressDetail.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void pintarEstudiante(Usuario u) {
        txtIniciales.setText(u.getIniciales());
        txtNombre.setText(u.getNombre() != null ? u.getNombre() : "Sin nombre");
        txtEmail.setText(u.getEmail() != null ? u.getEmail() : "");

        String riesgo = u.getNivelRiesgo() != null ? u.getNivelRiesgo() : "BAJO";
        txtRiesgo.setText(riesgo);
        aplicarColorRiesgo(riesgo);

        txtCodigo.setText(u.getCodigo() != null && !u.getCodigo().isEmpty() ? u.getCodigo() : "-");
        txtPrograma.setText(u.getPrograma() != null && !u.getPrograma().isEmpty() ? u.getPrograma() : "-");
        txtSemestre.setText(u.getSemestre() > 0 ? String.valueOf(u.getSemestre()) : "-");
        txtPromedio.setText(u.getPromedio() > 0 ? String.format("%.2f", u.getPromedio()) : "-");
        txtTelefono.setText(u.getTelefono() != null && !u.getTelefono().isEmpty() ? u.getTelefono() : "-");
    }

    // ============================================================
    // ESTADO EMOCIONAL
    // ============================================================

    private void cargarEmocional() {
        emotionalRepository.getByEstudiante(estudianteId)
                .addOnSuccessListener(snapshot -> {
                    List<ReporteEmocional> lista =
                            snapshot != null ? snapshot.toObjects(ReporteEmocional.class)
                                    : new ArrayList<>();
                    historialEmocional.clear();
                    if (lista != null && !lista.isEmpty()) {
                        lista.sort((a, b) -> Long.compare(b.getFecha(), a.getFecha()));
                        historialEmocional.addAll(lista);
                        ReporteEmocional r = lista.get(0);
                        dotEmocional.setBackgroundResource(AnimoUtils.circle(r.getAnimo()));
                        txtEmocionalAnimo.setText("Último reporte: " + AnimoUtils.label(r.getAnimo()));
                        txtEmocionalFecha.setText("Fecha: " + FechaUtils.fechaBonitaConAnio(r.getFechaDia()));
                        if (r.getDescripcion() != null && !r.getDescripcion().isEmpty()) {
                            txtEmocionalDesc.setText("\"" + r.getDescripcion() + "\"");
                            txtEmocionalDesc.setVisibility(View.VISIBLE);
                        } else {
                            txtEmocionalDesc.setVisibility(View.GONE);
                        }
                        txtEmocionalNone.setVisibility(View.GONE);
                        emocionalContent.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    // se mantiene el estado "sin datos"
                });
    }

    // ============================================================
    // REPORTES Y PLANES DE SEGUIMIENTO
    // ============================================================

    private void cargarReportes() {
        reportes.clear();
        reportRepository.getReportesByEstudianteSinOrden(estudianteId)
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Reporte r = doc.toObject(Reporte.class);
                            if (r != null) {
                                r.setId(doc.getId());
                                reportes.add(r);
                            }
                        }
                    }
                    reportes.sort((a, b) -> Long.compare(b.getFecha(), a.getFecha()));
                    pintarReportes();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al cargar reportes: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void pintarReportes() {
        int activos = 0;
        for (Reporte r : reportes) {
            if ("ABIERTO".equals(r.getEstado()) || "EN_SEGUIMIENTO".equals(r.getEstado())) {
                activos++;
            }
        }
        txtReporteCount.setText(reportes.size() + " en total · " + activos + " activos");

        reportesContainer.removeAllViews();
        if (reportes.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Aún no hay reportes ni planes para este estudiante.");
            empty.setTextColor(getColor(R.color.text_secondary));
            empty.setTextSize(13f);
            reportesContainer.addView(empty);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (Reporte r : reportes) {
            View row = inflater.inflate(R.layout.item_reporte_card, reportesContainer, false);

            TextView txtTitulo = row.findViewById(R.id.txt_reporte_titulo);
            String tipo = "PLAN".equals(r.getTipo()) ? "Plan" : "Reporte";
            txtTitulo.setText(tipo + ": " + (r.getTitulo() != null ? r.getTitulo() : "Sin título"));

            TextView txtFecha = row.findViewById(R.id.txt_reporte_fecha);
            String fecha = FechaUtils.fechaBonitaLong(r.getFecha());
            String riesgo = r.getNivelRiesgo() != null ? r.getNivelRiesgo() : "-";
            txtFecha.setText(fecha + " · Riesgo " + riesgo);

            boolean esPlan = "PLAN".equals(r.getTipo());
            if (esPlan) {
                int nAvances = r.getAvances() != null ? r.getAvances().size() : 0;
                txtFecha.setText(txtFecha.getText() + " · " + nAvances + " avances");
            }

            TextView txtEstado = row.findViewById(R.id.txt_reporte_estado);
            pintarEstadoBadge(txtEstado, r);

            row.setOnClickListener(v -> {
                if (esPlan) {
                    mostrarAvances(r);
                } else {
                    mostrarDialogoEstado(r);
                }
            });
            row.setOnLongClickListener(v -> {
                confirmarEliminar(r);
                return true;
            });
            reportesContainer.addView(row);
        }
    }

    private void pintarEstadoBadge(TextView tv, Reporte r) {
        if (r.isPlanCumplido()) {
            tv.setBackgroundResource(R.drawable.badge_success);
            tv.setText("CUMPLIDO");
        } else if ("EN_SEGUIMIENTO".equals(r.getEstado())) {
            tv.setBackgroundResource(R.drawable.badge_risk_medium);
            tv.setText("EN SEGUIMIENTO");
        } else if ("ATENDIDO".equals(r.getEstado())) {
            tv.setBackgroundResource(R.drawable.badge_success);
            tv.setText("ATENDIDO");
        } else {
            tv.setBackgroundResource(R.drawable.badge_info);
            tv.setText("ABIERTO");
        }
    }

    private void mostrarAvances(Reporte r) {
        StringBuilder msg = new StringBuilder();
        if (r.getDescripcion() != null && !r.getDescripcion().isEmpty()) {
            msg.append(r.getDescripcion()).append("\n\n");
        }
        msg.append(r.isPlanCumplido() ? "Plan cumplido"
                : "En proceso (" + estadoLabel(r.getEstado()) + ")\n");

        List<Map<String, Object>> avances = r.getAvances();
        if (avances != null && !avances.isEmpty()) {
            msg.append("\nAvances del estudiante:\n");
            for (Map<String, Object> a : avances) {
                Object f = a != null ? a.get("fecha") : null;
                Object t = a != null ? a.get("texto") : null;
                long ms = f instanceof Long ? (Long) f : 0L;
                msg.append("· ").append(ms > 0 ? FechaUtils.fechaBonitaLong(ms) : "")
                        .append(" — ").append(t != null ? t : "").append("\n");
            }
        } else {
            msg.append("\nAún no hay avances registrados.");
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Plan: " + r.getTitulo())
                .setMessage(msg)
                .setPositiveButton("Cerrar", null)
                .setNeutralButton("Exportar PDF", (d, w) -> exportarPlanPdf(r))
                .show();
    }

    private String estadoLabel(String estado) {
        if (estado == null) return "Abierto";
        switch (estado) {
            case "EN_SEGUIMIENTO": return "En seguimiento";
            case "ATENDIDO":       return "Atendido";
            default:               return "Abierto";
        }
    }

    private void mostrarDialogoCrear(int tipo) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_new_reporte, null);

        TextInputEditText editTitulo = v.findViewById(R.id.edit_titulo);
        TextInputEditText editDescripcion = v.findViewById(R.id.edit_descripcion_reporte);
        MaterialButton btnTipoReporte = v.findViewById(R.id.btn_tipo_reporte);
        MaterialButton btnTipoPlan = v.findViewById(R.id.btn_tipo_plan);
        MaterialButton btnRiesgoBajo = v.findViewById(R.id.btn_riesgo_bajo);
        MaterialButton btnRiesgoMedio = v.findViewById(R.id.btn_riesgo_medio);
        MaterialButton btnRiesgoAlto = v.findViewById(R.id.btn_riesgo_alto);

        selectedTipo = tipo;
        selectedRiesgo = "MEDIO";
        pintarToggleTipo(btnTipoReporte, btnTipoPlan);
        pintarToggleRiesgo(btnRiesgoBajo, btnRiesgoMedio, btnRiesgoAlto);

        btnTipoReporte.setOnClickListener(x -> {
            selectedTipo = 0;
            pintarToggleTipo(btnTipoReporte, btnTipoPlan);
        });
        btnTipoPlan.setOnClickListener(x -> {
            selectedTipo = 1;
            pintarToggleTipo(btnTipoReporte, btnTipoPlan);
        });

        btnRiesgoBajo.setOnClickListener(x -> {
            selectedRiesgo = "BAJO";
            pintarToggleRiesgo(btnRiesgoBajo, btnRiesgoMedio, btnRiesgoAlto);
        });
        btnRiesgoMedio.setOnClickListener(x -> {
            selectedRiesgo = "MEDIO";
            pintarToggleRiesgo(btnRiesgoBajo, btnRiesgoMedio, btnRiesgoAlto);
        });
        btnRiesgoAlto.setOnClickListener(x -> {
            selectedRiesgo = "ALTO";
            pintarToggleRiesgo(btnRiesgoBajo, btnRiesgoMedio, btnRiesgoAlto);
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(tipo == 0 ? "Nuevo Reporte de Seguimiento" : "Nuevo Plan de Acción")
                .setView(v)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(x -> guardarReporte(editTitulo, editDescripcion, dialog));
    }

    private void pintarToggleTipo(MaterialButton reporte, MaterialButton plan) {
        pintarToggle(reporte, selectedTipo == 0);
        pintarToggle(plan, selectedTipo == 1);
    }

    private void pintarToggleRiesgo(MaterialButton bajo, MaterialButton medio, MaterialButton alto) {
        pintarToggle(bajo, "BAJO".equals(selectedRiesgo));
        pintarToggle(medio, "MEDIO".equals(selectedRiesgo));
        pintarToggle(alto, "ALTO".equals(selectedRiesgo));
    }

    private void pintarToggle(MaterialButton btn, boolean activo) {
        if (activo) {
            btn.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.green_primary)));
            btn.setTextColor(getColor(R.color.white_card));
            btn.setStrokeWidth(dp(0));
        } else {
            btn.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.white_card)));
            btn.setTextColor(getColor(R.color.text_secondary));
            btn.setStrokeWidth(dp(1));
            btn.setStrokeColor(ColorStateList.valueOf(getColor(R.color.divider_color)));
        }
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private void guardarReporte(TextInputEditText editTitulo, TextInputEditText editDescripcion, AlertDialog dialog) {
        String titulo = editTitulo.getText() != null ? editTitulo.getText().toString().trim() : "";
        String descripcion = editDescripcion.getText() != null ? editDescripcion.getText().toString().trim() : "";

        if (titulo.isEmpty()) {
            Toast.makeText(this, "Escribe un título", Toast.LENGTH_SHORT).show();
            return;
        }

        Reporte r = new Reporte();
        r.setEstudianteId(estudianteId);
        r.setEstudianteNombre(estudianteNombre);
        r.setPsicologoAsignado(sessionManager.getUserId());
        r.setPsicologoNombre(sessionManager.getUserName());
        r.setTitulo(titulo);
        r.setTipo(selectedTipo == 0 ? "REPORTE" : "PLAN");
        r.setDescripcion(descripcion);
        r.setNivelRiesgo(selectedRiesgo);
        r.setEstado("ABIERTO");
        r.setFecha(System.currentTimeMillis());
        r.setUltimaActualizacion(System.currentTimeMillis());

        reportRepository.createReporte(r)
                .addOnSuccessListener(doc -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Registro creado", Toast.LENGTH_SHORT).show();
                    boolean esPlan = "PLAN".equals(r.getTipo());
                    new AuditRepository().registrar(esPlan ? "PLAN_CREADO" : "REPORTE_CREADO",
                            "Creó " + (esPlan ? "plan" : "reporte") + " '" + titulo
                                    + "' para " + estudianteNombre,
                            sessionManager);
                    notificarEstudiante(r);
                    cargarReportes();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void notificarEstudiante(Reporte r) {
        boolean esPlan = "PLAN".equals(r.getTipo());
        Notificacion n = new Notificacion();
        n.setReceptorId(estudianteId);
        n.setCreadaPor(sessionManager.getUserId());
        n.setOrigenNombre(sessionManager.getUserName());
        n.setTipo(esPlan ? "PLAN" : "REPORTE");
        n.setTitulo(esPlan ? "Nuevo plan de acción"
                : "Nuevo reporte de bienestar");
        String cuerpo = r.getTitulo() != null ? r.getTitulo() : "";
        if (r.getDescripcion() != null && !r.getDescripcion().isEmpty()) {
            cuerpo += " — " + r.getDescripcion();
        }
        if (cuerpo.length() > 140) cuerpo = cuerpo.substring(0, 140) + "…";
        n.setCuerpo(cuerpo);
        n.setLeida(false);
        n.setFecha(System.currentTimeMillis());
        new NotificacionRepository().crear(n);
    }

    private void mostrarDialogoEstado(Reporte r) {
        int seleccionado = 0;
        for (int i = 0; i < ESTADOS.length; i++) {
            if (ESTADOS[i].equals(r.getEstado())) {
                seleccionado = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Estado del registro")
                .setSingleChoiceItems(ESTADOS_LABEL, seleccionado, (d, which) -> {
                    String estado = ESTADOS[which];
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("estado", estado);
                    reportRepository.updateReporte(r.getId(), updates)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Estado actualizado", Toast.LENGTH_SHORT).show();
                                cargarReportes();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    d.dismiss();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmarEliminar(Reporte r) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Eliminar registro")
                .setMessage("¿Eliminar \"" + r.getTitulo() + "\"? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (d, w) ->
                        reportRepository.deleteReporte(r.getId())
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Registro eliminado", Toast.LENGTH_SHORT).show();
                                    cargarReportes();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void aplicarColorRiesgo(String riesgo) {
        switch (riesgo) {
            case "ALTO":
                txtRiesgo.setBackgroundResource(R.drawable.badge_risk_high);
                break;
            case "MEDIO":
                txtRiesgo.setBackgroundResource(R.drawable.badge_risk_medium);
                break;
            case "BAJO":
            default:
                txtRiesgo.setBackgroundResource(R.drawable.badge_risk_low);
                break;
        }
    }
}