package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapters.SemaforoMateriaAdapter;
import com.example.myapplication.models.SemaforoPdf;
import com.example.myapplication.models.Usuario;
import com.example.myapplication.repositories.UserRepository;
import com.example.myapplication.utils.SemaforoPdfParser;
import com.example.myapplication.utils.SemaforoUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SemaforoPdfActivity extends AppCompatActivity {

    public static final String EXTRA_ESTUDIANTE_ID = "estudiante_id";

    private String estudianteId;
    private SemaforoPdf semaforo;
    private UserRepository userRepository;

    private MaterialToolbar toolbar;
    private MaterialCardView cardSumario;
    private TextView txtEstado, txtNombre, txtDocumento;
    private TextView txtPromedio, txtNivel, txtPrograma, txtPensum, txtSemestre, txtContadores;
    private View circulo;
    private RecyclerView rvMaterias;
    private MaterialButton btnAplicar;
    private CircularProgressIndicator progress;

    private final ActivityResultLauncher<String[]> seleccionarArchivo = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri == null) return;
                tomarPermiso(uri);
                procesarPdf(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_semaforo_pdf);

        estudianteId = getIntent().getStringExtra(EXTRA_ESTUDIANTE_ID);
        userRepository = new UserRepository();
        PDFBoxResourceLoader.init(getApplicationContext());

        toolbar = findViewById(R.id.toolbar_sem);
        cardSumario = findViewById(R.id.card_sumario);
        txtEstado = findViewById(R.id.txt_estado_sem);
        txtNombre = findViewById(R.id.txt_sf_nombre);
        txtDocumento = findViewById(R.id.txt_sf_documento);
        txtPromedio = findViewById(R.id.txt_sf_promedio);
        txtNivel = findViewById(R.id.txt_sf_nivel);
        txtPrograma = findViewById(R.id.txt_sf_programa);
        txtPensum = findViewById(R.id.txt_sf_pensum);
        txtSemestre = findViewById(R.id.txt_sf_semestre);
        txtContadores = findViewById(R.id.txt_sf_counters);
        circulo = findViewById(R.id.circulo_sem);
        rvMaterias = findViewById(R.id.rv_materias);
        btnAplicar = findViewById(R.id.btn_aplicar_sem);
        progress = findViewById(R.id.progress_sem);

        toolbar.setNavigationOnClickListener(v -> finish());

        findViewById(R.id.btn_seleccionar).setOnClickListener(v ->
                seleccionarArchivo.launch(new String[]{"application/pdf"}));

        btnAplicar.setOnClickListener(v -> guardarEnFicha());
    }

    private void tomarPermiso(Uri uri) {
        try {
            getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {
            // sin persistencia: el permiso aguanta mientras dura la actividad
        }
    }

    private void procesarPdf(Uri uri) {
        progress.setVisibility(View.VISIBLE);
        btnAplicar.setEnabled(false);
        txtEstado.setText("Leyendo el PDF...");

        new Thread(() -> {
            String texto = null;
            String error = null;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) throw new Exception("No se pudo abrir el archivo.");
                PDDocument doc = PDDocument.load(in);
                PDFTextStripper stripper = new PDFTextStripper();
                texto = stripper.getText(doc);
                doc.close();
            } catch (Exception e) {
                error = e.getMessage() != null ? e.getMessage() : "Error de lectura";
            }

            final String ft = texto;
            final String fe = error;
            runOnUiThread(() -> {
                progress.setVisibility(View.GONE);
                if (fe != null) {
                    txtEstado.setText("No se pudo leer el PDF: " + fe);
                    return;
                }
                try {
                    mostrarResultado(SemaforoPdfParser.parse(ft));
                } catch (IllegalArgumentException e) {
                    txtEstado.setText(e.getMessage());
                    cardSumario.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    private void mostrarResultado(SemaforoPdf r) {
        semaforo = r;

        txtNombre.setText(r.nombres != null ? r.nombres : "-");
        txtDocumento.setText("Documento: " + (r.documento != null ? r.documento : "-"));
        txtPrograma.setText(r.programa != null && !r.programa.isEmpty() ? r.programa : "-");
        txtPensum.setText(r.pensum != null ? r.pensum : "-");
        txtSemestre.setText(r.semestre > 0 ? String.valueOf(r.semestre) : "-");

        String formato = String.format(Locale.US, "%.2f", r.promedioAcumulado);
        txtPromedio.setText(formato);
        txtPromedio.setTextColor(SemaforoUtils.colorFor(this, r.promedioAcumulado));
        circulo.setBackgroundResource(SemaforoUtils.drawableFor(r.promedioAcumulado));

        pintarNivel(SemaforoUtils.nivelFor(r.promedioAcumulado));
        txtContadores.setText(r.contarMaterias() + " materias · "
                + r.contarRepetidas() + " repetidas · "
                + r.contarSinNota() + " sin nota");

        List<Object> items = new ArrayList<>();
        int pActual = -1;
        for (SemaforoPdf.Materia m : r.materias) {
            if (m.periodo != pActual) {
                items.add(m.periodo);
                pActual = m.periodo;
            }
            items.add(m);
        }
        rvMaterias.setLayoutManager(new LinearLayoutManager(this));
        rvMaterias.setAdapter(new SemaforoMateriaAdapter(items));

        cardSumario.setVisibility(View.VISIBLE);
        rvMaterias.setVisibility(View.VISIBLE);
        txtEstado.setText("Reporte leído correctamente.");
        btnAplicar.setEnabled(true);
    }

    private void pintarNivel(String nivel) {
        txtNivel.setText(nivel);
        int bg;
        switch (nivel) {
            case "ALTO":
                bg = R.drawable.badge_risk_high;
                break;
            case "MEDIO":
                bg = R.drawable.badge_risk_medium;
                break;
            default:
                bg = R.drawable.badge_risk_low;
                break;
        }
        txtNivel.setBackgroundResource(bg);
    }

    private void guardarEnFicha() {
        if (semaforo == null || estudianteId == null) return;

        btnAplicar.setEnabled(false);
        userRepository.getUserById(estudianteId)
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Estudiante no encontrado", Toast.LENGTH_SHORT).show();
                        btnAplicar.setEnabled(true);
                        return;
                    }
                    Usuario u = doc.toObject(Usuario.class);
                    confirmarAplicacion(u);
                })
                .addOnFailureListener(e -> {
                    btnAplicar.setEnabled(true);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void confirmarAplicacion(Usuario u) {
        String nivel = SemaforoUtils.nivelFor(semaforo.promedioAcumulado);
        String promedioTxt = String.format(Locale.US, "%.2f", semaforo.promedioAcumulado);

        String pdfDoc = semaforo.documento != null ? semaforo.documento.trim() : "";
        String userDoc = u.getDocumento() != null ? u.getDocumento().trim() : "";
        boolean coincide = !pdfDoc.isEmpty() && pdfDoc.equals(userDoc);

        StringBuilder msg = new StringBuilder();
        msg.append("Se actualizará la ficha del estudiante:\n\n");
        msg.append("· Promedio acumulado: ").append(promedioTxt).append("\n");
        msg.append("· Nivel de riesgo: ").append(nivel).append("\n");
        msg.append("· Materias: ").append(semaforo.contarMaterias())
                .append("  · Repetidas: ").append(semaforo.contarRepetidas()).append("\n");
        if (!coincide) {
            msg.append("\n⚠ El documento del PDF (").append(
                    pdfDoc.isEmpty() ? "sin dato" : pdfDoc)
                    .append(") NO coincide con el del estudiante abierto (").append(
                    userDoc.isEmpty() ? "sin dato" : userDoc).append(").");
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Aplicar Semáforo")
                .setMessage(msg.toString())
                .setPositiveButton("Aplicar", (d, w) -> aplicarEnFicha(nivel, promedioTxt))
                .setNegativeButton("Cancelar", (d, w) -> btnAplicar.setEnabled(true))
                .show();
    }

    private void aplicarEnFicha(String nivel, String promedioTxt) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("promedio", semaforo.promedioAcumulado);
        updates.put("nivelRiesgo", nivel);

        userRepository.updateUser(estudianteId, updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this,
                            "Semáforo aplicado: promedio " + promedioTxt
                                    + " · riesgo " + nivel,
                            Toast.LENGTH_LONG).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnAplicar.setEnabled(true);
                    Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}