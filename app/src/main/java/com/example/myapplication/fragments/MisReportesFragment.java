package com.example.myapplication.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapters.MisReportesAdapter;
import com.example.myapplication.models.Notificacion;
import com.example.myapplication.models.Reporte;
import com.example.myapplication.repositories.NotificacionRepository;
import com.example.myapplication.repositories.ReportRepository;
import com.example.myapplication.services.SessionManager;
import com.example.myapplication.utils.FechaUtils;
import com.example.myapplication.utils.PdfExporter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MisReportesFragment extends Fragment {

    private static final int REQ_EXPORTAR_PLAN_PDF = 989;

    private MisReportesAdapter adapter;
    private ReportRepository repo;
    private SessionManager sessionManager;

    private RecyclerView recycler;
    private TextView txtEmpty;
    private ProgressBar progress;
    private List<String> lineasExportarPlan = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mis_reportes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recycler = view.findViewById(R.id.recycler_reportes);
        txtEmpty = view.findViewById(R.id.txt_reportes_empty);
        progress = view.findViewById(R.id.progress_reportes);

        repo = new ReportRepository();
        sessionManager = new SessionManager(requireContext());

        adapter = new MisReportesAdapter();
        adapter.setOnReporteClickListener(this::mostrarDetalle);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        cargarReportes();
    }

    private void cargarReportes() {
        String uid = sessionManager.getUserId();
        if (uid == null) return;
        progress.setVisibility(View.VISIBLE);
        repo.getReportesByEstudianteSinOrden(uid)
                .addOnSuccessListener(this::onReportesCargados)
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    txtEmpty.setText("No se pudieron cargar los registros.\n" + e.getMessage());
                    txtEmpty.setVisibility(View.VISIBLE);
                });
    }

    private void onReportesCargados(@Nullable QuerySnapshot snapshots) {
        progress.setVisibility(View.GONE);
        List<Reporte> items = new ArrayList<>();
        if (snapshots != null) {
            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                Reporte r = doc.toObject(Reporte.class);
                if (r != null) {
                    r.setId(doc.getId());
                    items.add(r);
                }
            }
        }
        items.sort((a, b) -> Long.compare(b.getFecha(), a.getFecha()));
        adapter.setData(items);
        txtEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void mostrarDetalle(Reporte r) {
        boolean esPlan = "PLAN".equals(r.getTipo());
        StringBuilder msg = new StringBuilder();
        if (r.getDescripcion() != null && !r.getDescripcion().isEmpty()) {
            msg.append(r.getDescripcion()).append("\n\n");
        }

        if (esPlan) {
            List<Map<String, Object>> avances = r.getAvances();
            if (avances != null && !avances.isEmpty()) {
                msg.append(r.isPlanCumplido() ? "Plan cumplido" : "En proceso")
                        .append("\n\n").append("Avances registrados:\n");
                for (Map<String, Object> a : avances) {
                    Object f = a != null ? a.get("fecha") : null;
                    Object t = a != null ? a.get("texto") : null;
                    long ms = f instanceof Long ? (Long) f : 0L;
                    msg.append("· ").append(ms > 0 ? FechaUtils.fechaBonitaLong(ms) : "")
                            .append(" — ").append(t != null ? t : "").append("\n");
                }
            } else {
                msg.append("En proceso\n").append("\nAún no has registrado avances.");
            }
            msg.append("\n\n");
        }

        msg.append("Estado: ").append(r.isPlanCumplido() ? "Cumplido"
                : estadoLabel(r.getEstado())).append("\n");
        msg.append("Riesgo: ").append(riesgoLabel(r.getNivelRiesgo())).append("\n");
        msg.append("Creada: ").append(r.getFecha() > 0
                ? FechaUtils.fechaBonitaLong(r.getFecha()) : "-").append("\n");
        msg.append("Por: ").append(r.getPsicologoNombre() != null
                ? r.getPsicologoNombre() : "Bienestar Univ.");

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle((esPlan ? "Plan de acción" : "Reporte") + ": " + r.getTitulo())
                .setMessage(msg)
                .setPositiveButton("Cerrar", null);

        if (esPlan) {
            if (!r.isPlanCumplido()) {
                builder.setNeutralButton("Agregar avance", (d, w) -> registrarAvance(r));
                builder.setNegativeButton("Marcar cumplido", (d, w) -> marcarCumplido(r));
            } else {
                builder.setNeutralButton("Exportar PDF", (d, w) -> exportarPlanPdf(r));
            }
        }
        builder.show();
    }

    private void exportarPlanPdf(Reporte r) {
        List<String> lineas = new ArrayList<>();
        lineas.add("Título: " + (r.getTitulo() != null ? r.getTitulo() : "Sin título"));
        lineas.add("Estudiante: " + (r.getEstudianteNombre() != null
                ? r.getEstudianteNombre() : sessionManager.getUserName()));
        lineas.add("Creado: " + (r.getFecha() > 0 ? FechaUtils.fechaBonitaLong(r.getFecha()) : "-"));
        lineas.add("Riesgo: " + riesgoLabel(r.getNivelRiesgo()));
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
        String nombreArchivo = "PlanAccion_"
                + (r.getEstudianteNombre() != null
                ? r.getEstudianteNombre().replaceAll("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ _-]", "").replace(' ', '_')
                : "Estudiante")
                + ".pdf";

        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/pdf");
        i.putExtra(Intent.EXTRA_TITLE, nombreArchivo);
        startActivityForResult(i, REQ_EXPORTAR_PLAN_PDF);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_EXPORTAR_PLAN_PDF && resultCode == getActivity().RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) return;
            try (OutputStream out = getActivity().getContentResolver().openOutputStream(uri)) {
                if (out == null) return;
                PdfExporter.escribirExpediente(out,
                        "Plan de acción",
                        "Generado por " + (sessionManager.getUserName() != null
                                ? sessionManager.getUserName() : "Estudiante")
                                + " · " + FechaUtils.fechaBonitaLong(System.currentTimeMillis()),
                        lineasExportarPlan);
                Toast.makeText(requireContext(), "PDF exportado correctamente", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(requireContext(), "Error al exportar: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void registrarAvance(Reporte r) {
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_avance_plan, null);
        TextInputEditText edit = v.findViewById(R.id.edit_avance);

        AlertDialog dialogo = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Registrar avance del plan")
                .setView(v)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();
        dialogo.show();
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(x -> {
            String texto = edit.getText() != null ? edit.getText().toString().trim() : "";
            if (texto.isEmpty()) {
                Toast.makeText(requireContext(), "Escribe tu avance", Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, Object> avance = new HashMap<>();
            avance.put("fecha", System.currentTimeMillis());
            avance.put("texto", texto);
            repo.agregarAvance(r.getId(), avance)
                    .addOnSuccessListener(aVoid -> {
                        dialogo.dismiss();
                        Toast.makeText(requireContext(), "Avance registrado", Toast.LENGTH_SHORT).show();
                        cargarReportes();
                        notificarPsi(r, "Registró un avance en el plan: " + texto, true);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });
    }

    private void marcarCumplido(Reporte r) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Marcar plan como cumplido")
                .setMessage("¿Confirmas que cumpliste el plan \"" + r.getTitulo() + "\"?")
                .setPositiveButton("Sí, cumplido", (d, w) ->
                        repo.marcarPlanCumplido(r.getId())
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(requireContext(), "¡Felicidades! Plan cumplido",
                                            Toast.LENGTH_SHORT).show();
                                    cargarReportes();
                                    notificarPsi(r, "Marcó el plan como cumplido", false);
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(requireContext(), "Error: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show()))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void notificarPsi(Reporte r, String cuerpo, boolean esAvance) {
        if (r.getPsicologoAsignado() == null || r.getPsicologoAsignado().isEmpty()) return;

        Notificacion n = new Notificacion();
        n.setReceptorId(r.getPsicologoAsignado());
        n.setCreadaPor(sessionManager.getUserId());
        n.setOrigenNombre(sessionManager.getUserName());
        n.setTipo("REPORTE");
        n.setTitulo(esAvance ? "Avance del plan" : "Plan cumplido");
        n.setCuerpo(cuerpo.length() > 140 ? cuerpo.substring(0, 140) + "…" : cuerpo);
        n.setLeida(false);
        n.setFecha(System.currentTimeMillis());
        new NotificacionRepository().crear(n);
    }

    private String estadoLabel(String estado) {
        if (estado == null) return "Abierto";
        switch (estado) {
            case "EN_SEGUIMIENTO": return "En seguimiento";
            case "ATENDIDO":       return "Atendido";
            default:               return "Abierto";
        }
    }

    private String riesgoLabel(String nivel) {
        if (nivel == null) return "-";
        switch (nivel) {
            case "ALTO":  return "Alto";
            case "MEDIO": return "Medio";
            case "BAJO":  return "Bajo";
            default:      return nivel;
        }
    }
}