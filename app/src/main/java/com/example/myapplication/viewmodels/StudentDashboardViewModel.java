package com.example.myapplication.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.models.Cita;
import com.example.myapplication.models.Curso;
import com.example.myapplication.models.Nota;
import com.example.myapplication.repositories.AppointmentRepository;
import com.example.myapplication.repositories.CourseRepository;
import com.example.myapplication.repositories.GradeRepository;
import com.example.myapplication.utils.SemaforoUtils;
import com.example.myapplication.utils.Resource;
import com.example.myapplication.utils.SemaforoUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentDashboardViewModel extends ViewModel {

    public static class Resumen {
        public final double promedio;
        public final int materias;
        public final int enRiesgo;
        public final int aprobadas;
        public final boolean sinDatos;

        public Resumen(double promedio, int materias, int enRiesgo, int aprobadas, boolean sinDatos) {
            this.promedio = promedio;
            this.materias = materias;
            this.enRiesgo = enRiesgo;
            this.aprobadas = aprobadas;
            this.sinDatos = sinDatos;
        }

        public static Resumen vacio() {
            return new Resumen(0, 0, 0, 0, true);
        }
    }

    private final GradeRepository gradeRepository = new GradeRepository();
    private final CourseRepository courseRepository = new CourseRepository();
    private final AppointmentRepository appointmentRepository = new AppointmentRepository();

    private final MutableLiveData<Resource<Resumen>> resumen = new MutableLiveData<>();
    private final MutableLiveData<Resource<Cita>> proximaCita = new MutableLiveData<>();

    private String uid;
    private int generacion = 0;

    public LiveData<Resource<Resumen>> getResumen() {
        return resumen;
    }

    public LiveData<Resource<Cita>> getProximaCita() {
        return proximaCita;
    }

    public void cargar(String uid) {
        if (this.uid != null && this.uid.equals(uid)) {
            // Ya está cargado o cargando
            return;
        }

        if (uid == null) {
            resumen.setValue(Resource.success(Resumen.vacio()));
            proximaCita.setValue(Resource.success(null));
            return;
        }

        this.uid = uid;
        generacion++;
        cargarResumen(generacion, uid);
        cargarProximaCita(generacion, uid);
    }

    public void recargar() {
        if (uid == null) return;
        generacion++;
        cargarResumen(generacion, uid);
        cargarProximaCita(generacion, uid);
    }

    private void cargarResumen(int gen, String uid) {
        resumen.setValue(Resource.loading(null));

        gradeRepository.getNotasByEstudiante(uid)
                .addOnSuccessListener(snapshot -> {
                    if (gen != generacion) return;
                    List<Nota> notas = snapshot != null ? snapshot.toObjects(Nota.class) : null;
                    if (notas == null || notas.isEmpty()) {
                        resumen.setValue(Resource.success(Resumen.vacio()));
                        return;
                    }
                    cargarCursos(gen, notas);
                })
                .addOnFailureListener(e -> {
                    if (gen != generacion) return;
                    resumen.setValue(Resource.error("Error al cargar notas: " + e.getMessage(), null));
                });
    }

    private void cargarCursos(int gen, List<Nota> notas) {
        Map<String, List<Nota>> byCourse = new HashMap<>();
        for (Nota n : notas) {
            if (n.getCursoId() == null) continue;
            byCourse.computeIfAbsent(n.getCursoId(), k -> new ArrayList<>()).add(n);
        }

        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (String cursoId : byCourse.keySet()) {
            tasks.add(courseRepository.getCursoById(cursoId));
        }

        Tasks.whenAllComplete(tasks).addOnCompleteListener(completado -> {
            if (gen != generacion) return;
            Map<String, Curso> cursos = new HashMap<>();
            for (Task<DocumentSnapshot> t : tasks) {
                if (t.isSuccessful() && t.getResult() != null && t.getResult().exists()) {
                    Curso c = t.getResult().toObject(Curso.class);
                    if (c != null) {
                        c.setId(t.getResult().getId());
                        cursos.put(c.getId(), c);
                    }
                }
            }

            List<SemaforoUtils.PromedioMateria> items = new ArrayList<>();
            for (Map.Entry<String, List<Nota>> e : byCourse.entrySet()) {
                items.add(SemaforoUtils.computePromedioMateria(
                        e.getKey(), e.getValue(), cursos.get(e.getKey())));
            }
            items.sort((a, b) -> a.nombre.compareToIgnoreCase(b.nombre));

            resumen.setValue(Resource.success(new Resumen(
                    SemaforoUtils.promedioGeneral(items),
                    items.size(),
                    SemaforoUtils.enRiesgo(items),
                    SemaforoUtils.aprobadas(items),
                    false)));
        });
    }

    private void cargarProximaCita(int gen, String uid) {
        proximaCita.setValue(Resource.loading(null));
        appointmentRepository.getCitasByEstudiante(uid)
                .addOnSuccessListener(snapshot -> {
                    if (gen != generacion) return;
                    List<Cita> citas = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Cita c = doc.toObject(Cita.class);
                        if (c != null) {
                            c.setId(doc.getId());
                            citas.add(c);
                        }
                    }

                    String hoy = LocalDate.now().toString();
                    Cita proxima = null;
                    for (Cita c : citas) {
                        String estado = c.getEstado();
                        if (!"PENDIENTE".equals(estado) && !"CONFIRMADA".equals(estado)) continue;
                        if (c.getFecha() == null || c.getFecha().compareTo(hoy) < 0) continue;
                        if (proxima == null || esAntes(c, proxima)) {
                            proxima = c;
                        }
                    }
                    proximaCita.setValue(Resource.success(proxima));
                })
                .addOnFailureListener(e -> {
                    if (gen != generacion) return;
                    proximaCita.setValue(Resource.error("Error al cargar citas", null));
                });
    }

    private static boolean esAntes(Cita a, Cita b) {
        int cmp = a.getFecha().compareTo(b.getFecha());
        if (cmp != 0) return cmp < 0;
        String ha = a.getHora() != null ? a.getHora() : "00:00";
        String hb = b.getHora() != null ? b.getHora() : "00:00";
        return ha.compareTo(hb) < 0;
    }
}
