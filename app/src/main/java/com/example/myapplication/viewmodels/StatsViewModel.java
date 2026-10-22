package com.example.myapplication.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.models.Alerta;
import com.example.myapplication.models.Cita;
import com.example.myapplication.models.Reporte;
import com.example.myapplication.models.Tutoria;
import com.example.myapplication.models.Usuario;
import com.example.myapplication.repositories.AlertRepository;
import com.example.myapplication.repositories.AppointmentRepository;
import com.example.myapplication.repositories.CourseRepository;
import com.example.myapplication.repositories.ReportRepository;
import com.example.myapplication.repositories.TutoriaRepository;
import com.example.myapplication.repositories.UserRepository;
import com.example.myapplication.services.SessionManager;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class StatsViewModel extends ViewModel {

    public static class Estado {
        public final int usuariosEstudiantes;
        public final int usuariosDocentes;
        public final int usuariosPsicologos;
        public final int usuariosAdmins;

        public final int citasTotal;
        public final int citasAtendidas;
        public final int alertasTotal;
        public final int alertasPendientes;
        public final int alertasAtendidas;

        public final int cursosTotal;
        public final int tutoriasActivas;
        public final int reportesTotal;

        public Estado(int usuariosEstudiantes, int usuariosDocentes, int usuariosPsicologos, int usuariosAdmins,
                      int citasTotal, int citasAtendidas, int alertasTotal, int alertasPendientes, int alertasAtendidas,
                      int cursosTotal, int tutoriasActivas, int reportesTotal) {
            this.usuariosEstudiantes = usuariosEstudiantes;
            this.usuariosDocentes = usuariosDocentes;
            this.usuariosPsicologos = usuariosPsicologos;
            this.usuariosAdmins = usuariosAdmins;
            this.citasTotal = citasTotal;
            this.citasAtendidas = citasAtendidas;
            this.alertasTotal = alertasTotal;
            this.alertasPendientes = alertasPendientes;
            this.alertasAtendidas = alertasAtendidas;
            this.cursosTotal = cursosTotal;
            this.tutoriasActivas = tutoriasActivas;
            this.reportesTotal = reportesTotal;
        }

        public static Estado vacio() {
            return new Estado(0,0,0,0, 0,0,0,0,0, 0,0,0);
        }
    }

    private final UserRepository userRepository = new UserRepository();
    private final AppointmentRepository appointmentRepository = new AppointmentRepository();
    private final AlertRepository alertRepository = new AlertRepository();
    private final CourseRepository courseRepository = new CourseRepository();
    private final TutoriaRepository tutoriaRepository = new TutoriaRepository();
    private final ReportRepository reportRepository = new ReportRepository();

    private final MutableLiveData<Estado> estado = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private boolean cargado = false;
    private boolean cargando = false;
    private int generacion = 0;

    public LiveData<Estado> getEstado() {
        return estado;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void cargar() {
        if (cargando || cargado) return;
        generacion++;
        cargando = true;
        loading.setValue(true);

        Task<QuerySnapshot> usuariosTask = userRepository.getAllUsers();
        Task<QuerySnapshot> citasTask = appointmentRepository.getAllCitas();
        Task<QuerySnapshot> alertasTask = alertRepository.getAllAlertas();
        Task<QuerySnapshot> cursosTask = courseRepository.getAllCursos();
        Task<QuerySnapshot> tutoriasTask = tutoriaRepository.getAllTutorias();
        Task<QuerySnapshot> reportesTask = reportRepository.getAllReportes();

        List<Task<?>> tareas = new ArrayList<>();
        tareas.add(usuariosTask);
        tareas.add(citasTask);
        tareas.add(alertasTask);
        tareas.add(cursosTask);
        tareas.add(tutoriasTask);
        tareas.add(reportesTask);

        Tasks.whenAllComplete(tareas).addOnCompleteListener(task -> {
            if (generacion != this.generacion) return;

            int est = 0, doc = 0, psi = 0, adm = 0;
            if (usuariosTask.isSuccessful() && usuariosTask.getResult() != null) {
                for (DocumentSnapshot docSnap : usuariosTask.getResult().getDocuments()) {
                    Usuario u = docSnap.toObject(Usuario.class);
                    if (u == null) continue;
                    String rol = u.getRol();
                    if (SessionManager.ROLE_ESTUDIANTE.equals(rol)) est++;
                    else if (SessionManager.ROLE_DOCENTE.equals(rol)) doc++;
                    else if (SessionManager.ROLE_PSICOLOGO.equals(rol)) psi++;
                    else if (SessionManager.ROLE_ADMIN.equals(rol)) adm++;
                }
            }

            int citasTotal = 0, citasAtendidas = 0;
            if (citasTask.isSuccessful() && citasTask.getResult() != null) {
                citasTotal = citasTask.getResult().size();
                for (DocumentSnapshot docSnap : citasTask.getResult().getDocuments()) {
                    Cita c = docSnap.toObject(Cita.class);
                    if (c != null && "ATENDIDA".equals(c.getEstado())) citasAtendidas++;
                }
            }

            int alertasTotal = 0, alertasPendientes = 0, alertasAtendidas = 0;
            if (alertasTask.isSuccessful() && alertasTask.getResult() != null) {
                alertasTotal = alertasTask.getResult().size();
                for (DocumentSnapshot docSnap : alertasTask.getResult().getDocuments()) {
                    Alerta a = docSnap.toObject(Alerta.class);
                    if (a == null) continue;
                    if ("PENDIENTE".equals(a.getEstado())) alertasPendientes++;
                    if ("ATENDIDA".equals(a.getEstado())) alertasAtendidas++;
                }
            }

            int cursosTotal = 0;
            if (cursosTask.isSuccessful() && cursosTask.getResult() != null) {
                cursosTotal = cursosTask.getResult().size();
            }

            int tutoriasActivas = 0;
            if (tutoriasTask.isSuccessful() && tutoriasTask.getResult() != null) {
                for (DocumentSnapshot docSnap : tutoriasTask.getResult().getDocuments()) {
                    Tutoria t = docSnap.toObject(Tutoria.class);
                    if (t != null && t.isActiva()) tutoriasActivas++;
                }
            }

            int reportesTotal = 0;
            if (reportesTask.isSuccessful() && reportesTask.getResult() != null) {
                reportesTotal = reportesTask.getResult().size();
            }

            estado.setValue(new Estado(est, doc, psi, adm,
                    citasTotal, citasAtendidas, alertasTotal, alertasPendientes, alertasAtendidas,
                    cursosTotal, tutoriasActivas, reportesTotal));

            cargando = false;
            cargado = true;
            loading.setValue(false);
        });
    }

    public void recargar() {
        generacion++;
        cargando = false;
        cargado = false;
        estado.setValue(Estado.vacio());
        cargar();
    }

    public void clearError() {
        error.setValue(null);
    }
}