package com.example.myapplication.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.myapplication.R;
import com.example.myapplication.models.Cita;
import com.example.myapplication.models.Curso;
import com.example.myapplication.models.Nota;
import com.example.myapplication.repositories.AppointmentRepository;
import com.example.myapplication.repositories.CourseRepository;
import com.example.myapplication.repositories.GradeRepository;
import com.example.myapplication.services.SessionManager;
import com.example.myapplication.utils.FechaUtils;
import com.example.myapplication.utils.SemaforoUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.firestore.DocumentSnapshot;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StudentDashboardFragment extends Fragment {

    private TextView txtDate;
    private TextView txtPromedio, txtNivel, txtRecomendacion;
    private TextView txtMaterias, txtRiesgo, txtAprobadas;
    private TextView txtProximaCita, txtEmpty;
    private View semaforoCircle;
    private CircularProgressIndicator progressLoading;

    private final GradeRepository gradeRepository = new GradeRepository();
    private final CourseRepository courseRepository = new CourseRepository();
    private final AppointmentRepository appointmentRepository = new AppointmentRepository();
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        txtDate = view.findViewById(R.id.text_date);
        txtPromedio = view.findViewById(R.id.txt_promedio);
        txtNivel = view.findViewById(R.id.txt_nivel);
        txtRecomendacion = view.findViewById(R.id.txt_recomendacion);
        txtMaterias = view.findViewById(R.id.txt_materias);
        txtRiesgo = view.findViewById(R.id.txt_riesgo);
        txtAprobadas = view.findViewById(R.id.txt_aprobadas);
        txtProximaCita = view.findViewById(R.id.txt_proxima_cita);
        txtEmpty = view.findViewById(R.id.txt_empty);
        semaforoCircle = view.findViewById(R.id.semaforo_circle);
        progressLoading = view.findViewById(R.id.progress_loading);

        sessionManager = new SessionManager(requireContext());

        setupHeader();
        setupQuickAccess(view);
        loadResumen();
        loadProximaCita();
    }

    private void setupHeader() {
        if (txtDate != null) {
            // Saludo mostrado en la barra superior, no en el fragmento
            LocalDate hoy = LocalDate.now();
            String fecha = hoy.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("es", "CO"))
                    + ", " + hoy.getDayOfMonth() + " de "
                    + hoy.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "CO"))
                    + " de " + hoy.getYear();
            txtDate.setText(fecha.substring(0, 1).toUpperCase() + fecha.substring(1));
        }
    }

    private void setupQuickAccess(View view) {
        View quickProgress = view.findViewById(R.id.quick_progress);
        if (quickProgress != null) quickProgress.setOnClickListener(v -> navigateTo(v, R.id.nav_est_progress));
        
        View quickAppointments = view.findViewById(R.id.quick_appointments);
        if (quickAppointments != null) quickAppointments.setOnClickListener(v -> navigateTo(v, R.id.nav_est_appointments));

        View quickEmotion = view.findViewById(R.id.quick_emotion);
        if (quickEmotion != null) quickEmotion.setOnClickListener(v -> navigateTo(v, R.id.nav_est_emotion));
        
        View quickBienestar = view.findViewById(R.id.quick_bienestar);
        if (quickBienestar != null) quickBienestar.setOnClickListener(v -> navigateTo(v, R.id.nav_est_bienestar));
        
        View quickCourses = view.findViewById(R.id.quick_courses);
        if (quickCourses != null) quickCourses.setOnClickListener(v -> navigateTo(v, R.id.nav_est_courses));
    }

    private void navigateTo(View v, int destinationId) {
        try {
            NavController navController = Navigation.findNavController(v);
            if (navController.getCurrentDestination() != null) {
                int currentId = navController.getCurrentDestination().getId();
                if (currentId == R.id.nav_est_home) {
                    navController.navigate(destinationId);
                }
            }
        } catch (IllegalArgumentException e) {
            // Ignorar clics múltiples/rápidos
        }
    }

    // ============================================================
    // RESUMEN ACADÉMICO
    // ============================================================

    private void loadResumen() {
        String uid = sessionManager.getUserId();
        if (uid == null) {
            pinNoDatos();
            return;
        }

        if (progressLoading != null) {
            progressLoading.setVisibility(View.VISIBLE);
        }

        gradeRepository.getNotasByEstudiante(uid)
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded() || getView() == null) return;
                    List<Nota> notas = snapshot.toObjects(Nota.class);
                    if (notas == null || notas.isEmpty()) {
                        progressLoading.setVisibility(View.GONE);
                        pinNoDatos();
                        return;
                    }
                    fetchCursos(notas);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || getView() == null) return;
                    progressLoading.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Error al cargar notas: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    pinNoDatos();
                });
    }

    private void fetchCursos(List<Nota> notas) {
        Map<String, List<Nota>> byCourse = new HashMap<>();
        for (Nota n : notas) {
            if (n.getCursoId() == null) continue;
            byCourse.computeIfAbsent(n.getCursoId(), k -> new ArrayList<>()).add(n);
        }

        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (String cursoId : byCourse.keySet()) {
            tasks.add(courseRepository.getCursoById(cursoId));
        }

        Tasks.whenAllComplete(tasks).addOnSuccessListener(completed -> {
            if (!isAdded() || getView() == null) return;
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
            progressLoading.setVisibility(View.GONE);
            pintarResumen(byCourse, cursos);
        }).addOnFailureListener(e -> {
            if (!isAdded() || getView() == null) return;
            progressLoading.setVisibility(View.GONE);
            Toast.makeText(requireContext(), "Error al cargar materias: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            pinNoDatos();
        });
    }

    private void pintarResumen(Map<String, List<Nota>> byCourse, Map<String, Curso> cursos) {
        List<SemaforoUtils.PromedioMateria> items = new ArrayList<>();
        for (Map.Entry<String, List<Nota>> e : byCourse.entrySet()) {
            items.add(SemaforoUtils.computePromedioMateria(
                    e.getKey(), e.getValue(), cursos.get(e.getKey())));
        }
        Collections.sort(items, (a, b) -> a.nombre.compareToIgnoreCase(b.nombre));

        double promedio = SemaforoUtils.promedioGeneral(items);
        int enRiesgo = SemaforoUtils.enRiesgo(items);
        int aprobadas = SemaforoUtils.aprobadas(items);

        txtPromedio.setText(SemaforoUtils.format(promedio));
        txtMaterias.setText(String.valueOf(items.size()));
        txtRiesgo.setText(String.valueOf(enRiesgo));
        txtAprobadas.setText(String.valueOf(aprobadas));

        semaforoCircle.setBackgroundResource(SemaforoUtils.drawableFor(promedio));
        int color = SemaforoUtils.colorFor(requireContext(), promedio);
        txtNivel.setText("Nivel de riesgo: " + SemaforoUtils.nivelFor(promedio));
        txtNivel.setTextColor(color);
        txtRecomendacion.setTextColor(color);
        txtRecomendacion.setText(recomendacionPara(promedio, enRiesgo));
    }

    private String recomendacionPara(double promedio, int enRiesgo) {
        if (promedio >= 4.0) {
            return "¡Excelente! Tu rendimiento está muy bien. Sigue así.";
        }
        if (promedio >= 3.0) {
            if (enRiesgo > 0) {
                return "Tienes materias por debajo de tu promedio. Revisa tu progreso y habla con tus docentes.";
            }
            return "Buen rendimiento. Puedes mejorar un poco más tus materias.";
        }
        return "Tu rendimiento requiere atención. Agenda una cita con Bienestar Universitario.";
    }

    private void pinNoDatos() {
        txtEmpty.setVisibility(View.VISIBLE);
    }

    // ============================================================
    // PRÓXIMA CITA
    // ============================================================

    private void loadProximaCita() {
        String uid = sessionManager.getUserId();
        if (uid == null) {
            txtProximaCita.setText("No tienes citas próximas");
            return;
        }

        appointmentRepository.getCitasByEstudiante(uid)
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded() || getView() == null) return;
                    List<Cita> citas = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Cita c = doc.toObject(Cita.class);
                        if (c != null) {
                            c.setId(doc.getId());
                            citas.add(c);
                        }
                    }
                    
                    Collections.sort(citas, (a, b) -> {
                        String f1 = a.getFecha() != null ? a.getFecha() : "";
                        String f2 = b.getFecha() != null ? b.getFecha() : "";
                        int cmp = f2.compareTo(f1);
                        if (cmp != 0) return cmp;
                        String h1 = a.getHora() != null ? a.getHora() : "";
                        String h2 = b.getHora() != null ? b.getHora() : "";
                        return h2.compareTo(h1);
                    });
                            
                    String hoy = LocalDate.now().toString(); // yyyy-MM-dd

                    Cita proxima = null;
                    for (Cita c : citas) {
                        String estado = c.getEstado();
                        if (!"PENDIENTE".equals(estado) && !"CONFIRMADA".equals(estado)) continue;
                        if (c.getFecha() == null || c.getFecha().compareTo(hoy) < 0) continue;
                        if (proxima == null || esAntes(c, proxima)) {
                            proxima = c;
                        }
                    }

                    if (proxima == null) {
                        txtProximaCita.setText("No tienes citas próximas");
                    } else {
                        txtProximaCita.setText(FechaUtils.fechaBonita(proxima.getFecha()) + " · " + proxima.getHora()
                                + (proxima.getPsicologoNombre() != null
                                    ? "\nCon: " + proxima.getPsicologoNombre() : ""));
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || getView() == null) return;
                    txtProximaCita.setText("No se pudo cargar tu próxima cita");
                });
    }

    private boolean esAntes(Cita a, Cita b) {
        int cmp = a.getFecha().compareTo(b.getFecha());
        if (cmp != 0) return cmp < 0;
        String ha = a.getHora() != null ? a.getHora() : "00:00";
        String hb = b.getHora() != null ? b.getHora() : "00:00";
        return ha.compareTo(hb) < 0;
    }
}