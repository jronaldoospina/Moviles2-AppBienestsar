package com.example.myapplication.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.models.Alerta;
import com.example.myapplication.models.Curso;
import com.example.myapplication.models.Tutoria;
import com.example.myapplication.repositories.AlertRepository;
import com.example.myapplication.repositories.CourseRepository;
import com.example.myapplication.repositories.TutoriaRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class TeacherDashboardViewModel extends ViewModel {

    public static class Resumen {
        public final int cursos;
        public final int estudiantes;
        public final int tutoriasActivas;
        public final int porcentajeAtendidas;
        public final List<Alerta> recientes;

        public Resumen(int cursos, int estudiantes, int tutoriasActivas,
                       int porcentajeAtendidas, List<Alerta> recientes) {
            this.cursos = cursos;
            this.estudiantes = estudiantes;
            this.tutoriasActivas = tutoriasActivas;
            this.porcentajeAtendidas = porcentajeAtendidas;
            this.recientes = recientes;
        }
    }

    private final CourseRepository courseRepository = new CourseRepository();
    private final TutoriaRepository tutoriaRepository = new TutoriaRepository();
    private final AlertRepository alertRepository = new AlertRepository();

    private final MutableLiveData<Resumen> resumen = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private ListenerRegistration registration;
    private String uid;
    private int cursos;
    private int estudiantes;
    private int tutoriasActivas;
    private int totalAlertas;
    private int alertasAtendidas;
    private List<Alerta> recientes = new ArrayList<>();
    private boolean datosCargados = false;
    private boolean cargandoDatos = false;

    public LiveData<Resumen> getResumen() {
        return resumen;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void empezar(String uid) {
        if (uid == null) return;
        if (!uid.equals(this.uid)) {
            detener();
            datosCargados = false;
            cargandoDatos = false;
            cursos = 0;
            estudiantes = 0;
            tutoriasActivas = 0;
            totalAlertas = 0;
            alertasAtendidas = 0;
            recientes = new ArrayList<>();
        }
        this.uid = uid;

        if (!datosCargados && !cargandoDatos) {
            cargarDatos();
        }

        if (registration == null) {
            registration = alertRepository.escucharAlertasDeDocente(uid, this::onAlertas);
        }
    }

    public void detener() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    public void reintentar() {
        datosCargados = false;
        detener();
        empezar(uid);
    }

    public void clearError() {
        error.setValue(null);
    }

    @Override
    protected void onCleared() {
        detener();
    }

    private void cargarDatos() {
        cargandoDatos = true;
        datosCargados = true;
        loading.setValue(true);

        Task<QuerySnapshot> cursosTask = courseRepository.getCursosByDocente(uid)
                .addOnSuccessListener(snapshot -> {
                    cursos = 0;
                    estudiantes = 0;
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Curso c = doc.toObject(Curso.class);
                        if (c == null) continue;
                        cursos++;
                        if (c.getEstudiantesIds() != null) {
                            estudiantes += c.getEstudiantesIds().size();
                        }
                    }
                    emitir();
                })
                .addOnFailureListener(e -> error.setValue("No se pudieron cargar los cursos"));

        Task<QuerySnapshot> tutoriasTask = tutoriaRepository.getTutoriasByDocente(uid)
                .addOnSuccessListener(snapshot -> {
                    tutoriasActivas = 0;
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Tutoria t = doc.toObject(Tutoria.class);
                        if (t != null && t.isActiva()) tutoriasActivas++;
                    }
                    emitir();
                })
                .addOnFailureListener(e -> error.setValue("No se pudieron cargar las tutorías"));

        Tasks.whenAllComplete(cursosTask, tutoriasTask).addOnCompleteListener(t -> {
            cargandoDatos = false;
            loading.setValue(false);
        });
    }

    private void onAlertas(QuerySnapshot snapshot, com.google.firebase.firestore.FirebaseFirestoreException e) {
        if (e != null) {
            error.setValue("No se pudieron cargar las alertas");
            return;
        }

        totalAlertas = 0;
        alertasAtendidas = 0;
        List<Alerta> items = new ArrayList<>();

        if (snapshot != null) {
            totalAlertas = snapshot.size();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Alerta a = doc.toObject(Alerta.class);
                if (a == null) continue;
                a.setId(doc.getId());
                items.add(a);
                if ("ATENDIDA".equals(a.getEstado())) alertasAtendidas++;
            }
        }

        items.sort((a, b) -> Long.compare(b.getFecha(), a.getFecha()));
        recientes = new ArrayList<>(items.subList(0, Math.min(5, items.size())));
        emitir();
    }

    private void emitir() {
        int porcentaje = totalAlertas > 0
                ? (int) Math.round(alertasAtendidas * 100.0 / totalAlertas) : -1;
        resumen.setValue(new Resumen(cursos, estudiantes, tutoriasActivas, porcentaje,
                new ArrayList<>(recientes)));
    }
}
