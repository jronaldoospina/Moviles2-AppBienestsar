package com.example.myapplication.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.models.Alerta;
import com.example.myapplication.models.Cita;
import com.example.myapplication.models.PacienteResumen;
import com.example.myapplication.models.ReporteEmocional;
import com.example.myapplication.models.Usuario;
import com.example.myapplication.repositories.AlertRepository;
import com.example.myapplication.repositories.AppointmentRepository;
import com.example.myapplication.repositories.EmotionalReportRepository;
import com.example.myapplication.repositories.UserRepository;
import com.example.myapplication.utils.Resource;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DashboardViewModel extends ViewModel {

    public static class Resumen {
        public final int alertasPendientes;
        public final int citasHoy;
        public final int totalEstudiantes;
        public final int porcentajeAtendidas;
        public final List<Alerta> recientes;
        public final List<PacienteResumen> pacientes;

        public Resumen(int alertasPendientes, int citasHoy, int totalEstudiantes,
                       int porcentajeAtendidas, List<Alerta> recientes,
                       List<PacienteResumen> pacientes) {
            this.alertasPendientes = alertasPendientes;
            this.citasHoy = citasHoy;
            this.totalEstudiantes = totalEstudiantes;
            this.porcentajeAtendidas = porcentajeAtendidas;
            this.recientes = recientes;
            this.pacientes = pacientes;
        }
    }

    private final AlertRepository alertRepository = new AlertRepository();
    private final AppointmentRepository appointmentRepository = new AppointmentRepository();
    private final UserRepository userRepository = new UserRepository();
    private final EmotionalReportRepository emotionalRepository = new EmotionalReportRepository();

    private final MutableLiveData<Resource<Resumen>> resumen = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private final List<Usuario> estudiantes = new ArrayList<>();
    private final Map<String, Integer> alertasActivasPorEstudiante = new HashMap<>();
    private final Map<String, Integer> animos = new HashMap<>();
    private final Set<String> animosCargando = new HashSet<>();

    private ListenerRegistration registration;
    private List<Alerta> recientes = new ArrayList<>();
    private List<PacienteResumen> pacientes = new ArrayList<>();
    private String uid;
    private boolean esPsicologo;
    private int totalAlertas;
    private int alertasPendientes;
    private int alertasAtendidas;
    private int citasHoy;
    private boolean estudiantesCargados = false;
    private boolean citasCargadas = false;
    private boolean cargandoDatos = false;

    public LiveData<Resource<Resumen>> getResumen() {
        return resumen;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void empezar(boolean esPsicologo, String uid) {
        this.esPsicologo = esPsicologo;
        this.uid = uid;

        if (!estudiantesCargados || !citasCargadas) {
            cargarDatosPuntuales();
        }

        if (registration == null) {
            registration = alertRepository.escucharAlertas(this::onAlertas);
        }
    }

    public void detener() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    public void reintentar() {
        estudiantesCargados = false;
        citasCargadas = false;
        cargandoDatos = false;
        detener();
        empezar(esPsicologo, uid);
    }

    public void clearError() {
        error.setValue(null);
    }

    @Override
    protected void onCleared() {
        detener();
    }

    private void cargarDatosPuntuales() {
        if (cargandoDatos) return;
        cargandoDatos = true;
        resumen.setValue(Resource.loading(resumen.getValue() != null ? resumen.getValue().data : null));

        if (!estudiantesCargados) {
            userRepository.getAllEstudiantes()
                    .addOnSuccessListener(snapshot -> {
                        estudiantes.clear();
                        if (snapshot != null) {
                            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                Usuario u = doc.toObject(Usuario.class);
                                if (u != null) {
                                    u.setUid(doc.getId());
                                    estudiantes.add(u);
                                }
                            }
                        }
                        estudiantesCargados = true;
                        alTerminarCarga();
                    })
                    .addOnFailureListener(e -> {
                        estudiantesCargados = true;
                        error.setValue("No se pudieron cargar los estudiantes");
                        alTerminarCarga();
                    });
        }

        if (!citasCargadas) {
            String hoy = LocalDate.now().toString();
            var tarea = esPsicologo && uid != null
                    ? appointmentRepository.getCitasByPsicologoYFecha(uid, hoy)
                    : appointmentRepository.getCitasByFecha(hoy);
            tarea.addOnSuccessListener(snapshot -> {
                citasHoy = 0;
                if (snapshot != null) {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Cita c = doc.toObject(Cita.class);
                        if (c != null && !"CANCELADA".equals(c.getEstado())) citasHoy++;
                    }
                }
                citasCargadas = true;
                alTerminarCarga();
            }).addOnFailureListener(e -> {
                citasCargadas = true;
                error.setValue("No se pudieron cargar las citas");
                alTerminarCarga();
            });
        }
    }

    private void alTerminarCarga() {
        if (estudiantesCargados && citasCargadas) {
            cargandoDatos = false;
            emitir();
            if (esPsicologo) recomputarPacientes();
        }
    }

    private void onAlertas(QuerySnapshot snapshot,
                           FirebaseFirestoreException e) {
        if (e != null) {
            error.setValue("No se pudieron cargar las alertas");
            return;
        }

        totalAlertas = 0;
        alertasPendientes = 0;
        alertasAtendidas = 0;
        alertasActivasPorEstudiante.clear();
        List<Alerta> items = new ArrayList<>();

        if (snapshot != null) {
            totalAlertas = snapshot.size();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Alerta a = doc.toObject(Alerta.class);
                if (a == null) continue;
                a.setId(doc.getId());
                items.add(a);
                if ("PENDIENTE".equals(a.getEstado())) {
                    alertasPendientes++;
                    if (a.getEstudianteId() != null) {
                        alertasActivasPorEstudiante.merge(a.getEstudianteId(), 1, Integer::sum);
                    }
                }
                if ("ATENDIDA".equals(a.getEstado())) alertasAtendidas++;
            }
        }

        items.sort((a, b) -> Long.compare(b.getFecha(), a.getFecha()));
        recientes = new ArrayList<>(items.subList(0, Math.min(5, items.size())));
        emitir();
        if (esPsicologo) recomputarPacientes();
    }

    private void recomputarPacientes() {
        Map<String, Integer> animosPrevios = new HashMap<>(animos);
        List<PacienteResumen> lista = new ArrayList<>();
        for (Usuario u : estudiantes) {
            Integer n = alertasActivasPorEstudiante.get(u.getUid());
            if (n == null || n <= 0) continue;
            PacienteResumen p = new PacienteResumen(u.getUid(), u.getNombre(), u.getNivelRiesgo(), n);
            Integer animo = animosPrevios.get(u.getUid());
            p.setAnimo(animo != null ? animo : 0);
            lista.add(p);
        }
        lista.sort((a, b) -> Integer.compare(b.getAlertasActivas(), a.getAlertasActivas()));
        pacientes = lista;
        emitir();

        for (PacienteResumen p : lista) {
            if (animos.containsKey(p.getUid()) || !animosCargando.add(p.getUid())) continue;
            emotionalRepository.getByEstudiante(p.getUid())
                    .addOnSuccessListener(snap -> {
                        int mejor = 0;
                        long mejorFecha = 0;
                        if (snap != null) {
                            for (DocumentSnapshot doc : snap.getDocuments()) {
                                ReporteEmocional re = doc.toObject(ReporteEmocional.class);
                                if (re != null && re.getFecha() >= mejorFecha) {
                                    mejorFecha = re.getFecha();
                                    mejor = re.getAnimo();
                                }
                            }
                        }
                        animos.put(p.getUid(), mejor);
                        animosCargando.remove(p.getUid());
                        actualizarAnimo(p.getUid(), mejor);
                    })
                    .addOnFailureListener(e -> animosCargando.remove(p.getUid()));
        }
    }

    private void actualizarAnimo(String uid, int animo) {
        boolean cambio = false;
        for (PacienteResumen p : pacientes) {
            if (p.getUid() != null && p.getUid().equals(uid) && p.getAnimo() != animo) {
                p.setAnimo(animo);
                cambio = true;
            }
        }
        if (cambio) emitir();
    }

    private void emitir() {
        int porcentaje = totalAlertas > 0
                ? (int) Math.round(alertasAtendidas * 100.0 / totalAlertas) : -1;
        resumen.setValue(Resource.success(new Resumen(alertasPendientes, citasHoy, estudiantes.size(),
                porcentaje, new ArrayList<>(recientes), new ArrayList<>(pacientes))));
    }
}
