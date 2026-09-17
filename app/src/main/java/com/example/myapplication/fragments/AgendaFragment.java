package com.example.myapplication.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
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
import com.example.myapplication.StudentDetailActivity;
import com.example.myapplication.adapters.AgendaAdapter;
import com.example.myapplication.models.Cita;
import com.example.myapplication.models.Notificacion;
import com.example.myapplication.models.Usuario;
import com.example.myapplication.repositories.AppointmentRepository;
import com.example.myapplication.repositories.AuditRepository;
import com.example.myapplication.repositories.NotificacionRepository;
import com.example.myapplication.repositories.UserRepository;
import com.example.myapplication.services.SessionManager;
import com.example.myapplication.utils.CitaReminderScheduler;
import com.example.myapplication.utils.FechaUtils;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AgendaFragment extends Fragment
        implements AgendaAdapter.OnCitaClickListener {

    private static final String[] MOTIVOS = {
            "Seguimiento psicológico", "Evaluación de riesgo", "Orientación académica",
            "Ansiedad y rendimiento", "Seguimiento de plan", "Otro"
    };
    private static final String[] DURACIONES = {"30 min", "45 min", "60 min"};

    private final List<Cita> citas = new ArrayList<>();
    private final List<String> nombresEstudiantes = new ArrayList<>();
    private final List<String> idsEstudiantes = new ArrayList<>();

    private AgendaAdapter adapter;
    private AppointmentRepository appointmentRepository;
    private UserRepository userRepository;
    private NotificacionRepository notificacionRepository;
    private SessionManager sessionManager;
    private ListenerRegistration registration;

    private RecyclerView recycler;
    private TextView txtEmpty;
    private TextView txtResumenSemana;
    private ProgressBar progress;

    private String fechaSeleccionada;
    private String horaSeleccionada;

    private boolean vistaSemana = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_agenda, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recycler = view.findViewById(R.id.recycler_citas_psi);
        txtEmpty = view.findViewById(R.id.txt_agenda_empty);
        txtResumenSemana = view.findViewById(R.id.txt_resumen_semana);
        progress = view.findViewById(R.id.progress_agenda);
        FloatingActionButton fab = view.findViewById(R.id.fab_nueva_cita);

        appointmentRepository = new AppointmentRepository();
        userRepository = new UserRepository();
        notificacionRepository = new NotificacionRepository();
        sessionManager = new SessionManager(requireContext());

        adapter = new AgendaAdapter();
        adapter.setOnCitaClickListener(this);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        fechaSeleccionada = LocalDate.now().toString();
        horaSeleccionada = "";

        MaterialButtonToggleGroup grupoPeriodo = view.findViewById(R.id.grupo_periodo);
        grupoPeriodo.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            vistaSemana = checkedId == R.id.btn_periodo_semana;
            aplicarPeriodo();
        });
        grupoPeriodo.check(R.id.btn_periodo_todas);

        fab.setOnClickListener(v -> mostrarDialogoNuevaCita());
    }

    @Override
    public void onStart() {
        super.onStart();
        String uid = sessionManager.getUserId();
        if (uid == null) return;
        progress.setVisibility(View.VISIBLE);
        registration = appointmentRepository.escucharCitasDePsicologo(uid, this::onCitasActualizadas);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    private void onCitasActualizadas(@Nullable QuerySnapshot snapshots,
                                     @Nullable com.google.firebase.firestore.FirebaseFirestoreException e) {
        progress.setVisibility(View.GONE);
        if (e != null) {
            txtEmpty.setText("No se pudieron cargar las citas.\n" + e.getMessage());
            txtEmpty.setVisibility(View.VISIBLE);
            return;
        }
        citas.clear();
        if (snapshots != null) {
            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                Cita c = doc.toObject(Cita.class);
                if (c != null) {
                    c.setId(doc.getId());
                    citas.add(c);
                }
            }
        }
        citas.sort((a, b) -> {
            boolean aCanc = "CANCELADA".equals(a.getEstado());
            boolean bCanc = "CANCELADA".equals(b.getEstado());
            if (aCanc != bCanc) return aCanc ? 1 : -1;
            int cmp = Comparator.nullsLast(String::compareTo)
                    .compare(a.getFecha(), b.getFecha());
            if (cmp != 0) return cmp;
            return Comparator.nullsLast(String::compareTo)
                    .compare(a.getHora(), b.getHora());
        });
        adapter.setCitas(citas);
        txtEmpty.setVisibility(citas.isEmpty() ? View.VISIBLE : View.GONE);
        sincronizarRecordatorios();
    }

    private void aplicarPeriodo() {
        List<Cita> visibles;
        if (vistaSemana) {
            String hoyIso = LocalDate.now().toString();
            String finIso = LocalDate.now().plusDays(7).toString();
            visibles = new ArrayList<>();
            for (Cita c : citas) {
                if ("CANCELADA".equals(c.getEstado()) || "ATENDIDA".equals(c.getEstado())) continue;
                String f = c.getFecha();
                if (f != null && f.compareTo(hoyIso) >= 0 && f.compareTo(finIso) <= 0) {
                    visibles.add(c);
                }
            }
            visibles.sort((a, b) -> {
                int cmp = Comparator.nullsLast(String::compareTo).compare(a.getFecha(), b.getFecha());
                if (cmp != 0) return cmp;
                return Comparator.nullsLast(String::compareTo).compare(a.getHora(), b.getHora());
            });
        } else {
            visibles = citas;
        }

        adapter.setCitas(visibles);
        txtEmpty.setVisibility(visibles.isEmpty() ? View.VISIBLE : View.GONE);
        actualizarResumen(visibles.size());
    }

    private void actualizarResumen(int visibles) {
        if (txtResumenSemana == null) return;
        if (vistaSemana) {
            txtResumenSemana.setText("Próximos 7 días: " + visibles
                    + (visibles == 1 ? " cita programada" : " citas programadas"));
        } else {
            txtResumenSemana.setText("Agenda completa · " + visibles + " registros");
        }
    }

    private void sincronizarRecordatorios() {
        for (Cita c : citas) {
            CitaReminderScheduler.sincronizar(requireContext(), c);
        }
    }

    @Override
    public void onCitaClick(Cita cita) {
        List<String> opciones = new ArrayList<>();
        opciones.add("Ver detalles");
        if (cita.getEstudianteId() != null && !cita.getEstudianteId().isEmpty()) {
            opciones.add("Ver expediente del estudiante");
        }
        String estado = cita.getEstado() != null ? cita.getEstado() : "PENDIENTE";
        if ("PENDIENTE".equals(estado)) {
            opciones.add("Confirmar cita");
            opciones.add("Marcar como atendida");
            opciones.add("Cancelar cita");
        } else if ("CONFIRMADA".equals(estado)) {
            opciones.add("Marcar como atendida");
            opciones.add("Cancelar cita");
        }
        String[] arr = opciones.toArray(new String[0]);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(tituloCita(cita))
                .setItems(arr, (d, which) -> {
                    String accion = arr[which];
                    if (accion.startsWith("Ver d")) {
                        verDetalles(cita);
                    } else if (accion.startsWith("Ver e")) {
                        abrirExpediente(cita);
                    } else if (accion.startsWith("Confirmar")) {
                        confirmar(cita);
                    } else if (accion.startsWith("Marcar")) {
                        pedirNotasYAtender(cita);
                    } else if (accion.startsWith("Cancelar")) {
                        cancelar(cita);
                    }
                })
                .setNegativeButton("Cerrar", null)
                .show();
    }

    private void abrirExpediente(Cita cita) {
        if (cita.getEstudianteId() == null || cita.getEstudianteId().isEmpty()) return;
        Intent i = new Intent(requireContext(), StudentDetailActivity.class);
        i.putExtra(StudentDetailActivity.EXTRA_ESTUDIANTE_ID, cita.getEstudianteId());
        startActivity(i);
    }

    private String tituloCita(Cita cita) {
        return "Cita con " + (cita.getEstudianteNombre() != null
                ? cita.getEstudianteNombre() : "estudiante");
    }

    private void verDetalles(Cita cita) {
        StringBuilder msg = new StringBuilder();
        msg.append("Fecha: ").append(cita.getFecha() != null
                ? FechaUtils.fechaBonitaConAnio(cita.getFecha()) : "-").append("\n");
        msg.append("Hora: ").append(cita.getHora() != null ? cita.getHora() : "-").append("\n");
        msg.append("Duración: ").append(cita.getDuracionMinutos() > 0
                ? cita.getDuracionMinutos() + " min" : "-").append("\n");
        msg.append("Motivo: ").append(cita.getMotivo() != null ? cita.getMotivo() : "-")
                .append("\n");
        msg.append("Estado: ").append(estadoLabel(cita.getEstado()));
        if (cita.getNotasPsicologo() != null && !cita.getNotasPsicologo().isEmpty()) {
            msg.append("\n\nNotas:\n").append(cita.getNotasPsicologo());
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(tituloCita(cita))
                .setMessage(msg)
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private void confirmar(Cita cita) {
        appointmentRepository.confirmarCita(cita.getId())
                .addOnSuccessListener(v -> {
                    Toast.makeText(requireContext(), "Cita confirmada", Toast.LENGTH_SHORT).show();
                    new AuditRepository().registrar("CITA_CONFIRMADA",
                            "Confirmó cita con " + (cita.getEstudianteNombre() != null
                                    ? cita.getEstudianteNombre() : "-"),
                            sessionManager);
                })
                .addOnFailureListener(err -> Toast.makeText(requireContext(),
                        "Error: " + err.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void pedirNotasYAtender(Cita cita) {
        TextInputEditText input = new TextInputEditText(requireContext());
        input.setHint("Notas de la sesión (opcional)");
        input.setMinLines(2);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Marcar como atendida")
                .setMessage("Cita con " + (cita.getEstudianteNombre() != null
                        ? cita.getEstudianteNombre() : "estudiante") + " - " + cita.getFecha())
                .setView(input)
                .setPositiveButton("Atender", (d, w) -> {
                    String notas = input.getText() != null ? input.getText().toString() : "";
                    atender(cita, notas);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void atender(Cita cita, String notas) {
        appointmentRepository.atenderCita(cita.getId(), notas)
                .addOnSuccessListener(v -> {
                    Toast.makeText(requireContext(), "Cita atendida", Toast.LENGTH_SHORT).show();
                    new AuditRepository().registrar("CITA_ATENDIDA",
                            "Atendió cita con " + (cita.getEstudianteNombre() != null
                                    ? cita.getEstudianteNombre() : "-"),
                            sessionManager);
                })
                .addOnFailureListener(err -> Toast.makeText(requireContext(),
                        "Error: " + err.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void cancelar(Cita cita) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cancelar cita")
                .setMessage("¿Cancelar la cita con " + cita.getEstudianteNombre() + " del "
                        + cita.getFecha() + " a las " + cita.getHora() + "?")
                .setPositiveButton("Cancelar cita", (d, w) ->
                        appointmentRepository.cambiarEstado(cita.getId(), "CANCELADA")
                                .addOnSuccessListener(v -> {
                                    Toast.makeText(requireContext(), "Cita cancelada",
                                            Toast.LENGTH_SHORT).show();
                                    new AuditRepository().registrar("CITA_CANCELADA",
                                            "Canceló cita con " + (cita.getEstudianteNombre() != null
                                                    ? cita.getEstudianteNombre() : "-"),
                                            sessionManager);
                                })
                                .addOnFailureListener(err -> Toast.makeText(requireContext(),
                                        "Error: " + err.getMessage(), Toast.LENGTH_LONG).show()))
                .setNegativeButton("No", null)
                .show();
    }

    // ============================================================
    // NUEVA CITA
    // ============================================================

    private void mostrarDialogoNuevaCita() {
        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_nueva_cita, null);

        MaterialAutoCompleteTextView actEstudiante = v.findViewById(R.id.act_estudiante);
        TextInputEditText txtFecha = v.findViewById(R.id.txt_fecha);
        TextInputEditText txtHora = v.findViewById(R.id.txt_hora);
        MaterialAutoCompleteTextView actDuracion = v.findViewById(R.id.act_duracion);
        MaterialAutoCompleteTextView actMotivo = v.findViewById(R.id.act_motivo);
        TextInputEditText txtNotas = v.findViewById(R.id.txt_notas);

        cargarEstudiantes(actEstudiante);

        actDuracion.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, DURACIONES));
        actDuracion.setText(DURACIONES[1], false);
        actMotivo.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, MOTIVOS));

        txtFecha.setText(fechaSeleccionada);
        txtFecha.setOnClickListener(x -> {
            try {
                LocalDate hoy = LocalDate.now();
                String[] partes = fechaSeleccionada.split("-");
                int anio = Integer.parseInt(partes[0]);
                int mes = Integer.parseInt(partes[1]) - 1;
                int dia = Integer.parseInt(partes[2]);
                DatePickerDialog d = new DatePickerDialog(requireContext(),
                        (dp, y, m, d2) -> {
                            fechaSeleccionada = String.format("%04d-%02d-%02d", y, m + 1, d2);
                            txtFecha.setText(fechaSeleccionada);
                        }, anio, mes, dia);
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.set(hoy.getYear(), hoy.getMonthValue() - 1, hoy.getDayOfMonth(), 0, 0, 0);
                d.getDatePicker().setMinDate(cal.getTimeInMillis());
                d.show();
            } catch (Exception ex) {
                fechaSeleccionada = LocalDate.now().toString();
            }
        });

        txtHora.setText(horaSeleccionada);
        txtHora.setOnClickListener(x -> {
            int hh = 9, mm = 0;
            if (horaSeleccionada != null && horaSeleccionada.matches("\\d{1,2}:\\d{2}")) {
                String[] p = horaSeleccionada.split(":");
                hh = Integer.parseInt(p[0]);
                mm = Integer.parseInt(p[1]);
            }
            TimePickerDialog t = new TimePickerDialog(requireContext(),
                    (tp, h, m) -> {
                        horaSeleccionada = String.format("%02d:%02d", h, m);
                        txtHora.setText(horaSeleccionada);
                    }, hh, mm, true);
            t.show();
        });

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Nueva cita")
                .setView(v)
                .setPositiveButton("Programar", null)
                .setNegativeButton("Cancelar", null)
                .create();
        dialog.show();

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(x -> {
                    String estudianteTexto = actEstudiante.getText() != null
                            ? actEstudiante.getText().toString().trim() : "";
                    String duracionTexto = actDuracion.getText() != null
                            ? actDuracion.getText().toString().trim() : "";
                    String motivo = actMotivo.getText() != null
                            ? actMotivo.getText().toString().trim() : "";
                    String notas = txtNotas.getText() != null
                            ? txtNotas.getText().toString().trim() : "";
                    if (programarCita(estudianteTexto, duracionTexto, motivo, notas)) {
                        dialog.dismiss();
                    }
                });
    }

    private void cargarEstudiantes(MaterialAutoCompleteTextView actEstudiante) {
        userRepository.getAllEstudiantes().addOnSuccessListener(snaps -> {
            nombresEstudiantes.clear();
            idsEstudiantes.clear();
            for (DocumentSnapshot doc : snaps.getDocuments()) {
                Usuario u = doc.toObject(Usuario.class);
                if (u != null && u.getNombre() != null && !u.getNombre().trim().isEmpty()) {
                    nombresEstudiantes.add(u.getNombre().trim());
                    idsEstudiantes.add(doc.getId());
                }
            }
            ArrayAdapter<String> a = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_list_item_1, nombresEstudiantes);
            actEstudiante.setAdapter(a);
            actEstudiante.setEnabled(!nombresEstudiantes.isEmpty());
        }).addOnFailureListener(err -> Toast.makeText(requireContext(),
                "No se pudieron cargar los estudiantes: " + err.getMessage(),
                Toast.LENGTH_LONG).show());
    }

    private boolean programarCita(String estudianteTexto, String duracionTexto,
                                  String motivo, String notas) {
        if (estudianteTexto.isEmpty()) {
            Toast.makeText(requireContext(), "Selecciona un estudiante", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (horaSeleccionada.isEmpty()) {
            Toast.makeText(requireContext(), "Selecciona la hora de la cita", Toast.LENGTH_SHORT).show();
            return false;
        }
        int idx = nombresEstudiantes.indexOf(estudianteTexto);
        if (idx < 0) {
            Toast.makeText(requireContext(), "Selecciona un estudiante de la lista", Toast.LENGTH_SHORT).show();
            return false;
        }
        String estudianteId = idsEstudiantes.get(idx);
        int duracion;
        try {
            duracion = Integer.parseInt(duracionTexto.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            duracion = 45;
        }
        if (duracion <= 0) duracion = 45;

        String psiId = sessionManager.getUserId();
        String psiNombre = sessionManager.getUserName();
        if (psiId == null) return false;

        Cita cita = new Cita();
        cita.setEstudianteId(estudianteId);
        cita.setEstudianteNombre(estudianteTexto);
        cita.setPsicologoId(psiId);
        cita.setPsicologoNombre(psiNombre);
        cita.setFecha(fechaSeleccionada);
        cita.setHora(horaSeleccionada);
        cita.setDuracionMinutos(duracion);
        cita.setMotivo(motivo.isEmpty() ? "Seguimiento psicológico" : motivo);
        cita.setEstado("PENDIENTE");
        cita.setNotasPsicologo(notas);

        appointmentRepository.createCita(cita)
                .addOnSuccessListener(v -> {
                    Toast.makeText(requireContext(), "Cita programada", Toast.LENGTH_SHORT).show();
                    new AuditRepository().registrar("CITA_CREADA",
                            "Programó cita con " + estudianteTexto + " el " + fechaSeleccionada
                                    + " a las " + horaSeleccionada,
                            sessionManager);
                    notificarEstudiante(estudianteId, estudianteTexto, fechaSeleccionada, horaSeleccionada);
                })
                .addOnFailureListener(err -> Toast.makeText(requireContext(),
                        "Error al programar: " + err.getMessage(), Toast.LENGTH_LONG).show());
        return true;
    }

    private void notificarEstudiante(String estudianteId, String estudianteNombre,
                                     String fecha, String hora) {
        Notificacion n = new Notificacion();
        n.setReceptorId(estudianteId);
        n.setCreadaPor(sessionManager.getUserId());
        n.setOrigenNombre(sessionManager.getUserName() != null ? sessionManager.getUserName() : "Psicólogo");
        n.setTipo("CITA");
        n.setTitulo("Nueva cita programada");
        n.setCuerpo("Tienes una cita el " + FechaUtils.fechaBonitaConAnio(fecha)
                + " a las " + hora + ".");
        n.setLeida(false);
        n.setFecha(System.currentTimeMillis());
        notificacionRepository.crear(n);
    }

    private String estadoLabel(String estado) {
        if (estado == null) return "Pendiente";
        switch (estado) {
            case "CONFIRMADA": return "Confirmada";
            case "ATENDIDA": return "Atendida";
            case "CANCELADA": return "Cancelada";
            case "PENDIENTE":
            default: return "Pendiente";
        }
    }
}