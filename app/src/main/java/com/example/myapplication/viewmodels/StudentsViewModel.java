package com.example.myapplication.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.models.Reporte;
import com.example.myapplication.models.Usuario;
import com.example.myapplication.repositories.ReportRepository;
import com.example.myapplication.repositories.UserRepository;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StudentsViewModel extends ViewModel {

    private final UserRepository userRepository = new UserRepository();
    private final ReportRepository reportRepository = new ReportRepository();

    private final MutableLiveData<List<Usuario>> estudiantes = new MutableLiveData<>();
    private final MutableLiveData<Set<String>> reportesActivos = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private boolean cargado = false;
    private boolean cargando = false;
    private String riesgo = null;
    private String query = "";

    public LiveData<List<Usuario>> getEstudiantes() {
        return estudiantes;
    }

    public LiveData<Set<String>> getReportesActivos() {
        return reportesActivos;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void cargar() {
        if (cargando || cargado) return;
        cargando = true;
        loading.setValue(true);

        var estudiantesTask = userRepository.getAllEstudiantes();
        var reportesTask = reportRepository.getAllReportes();

        Tasks.whenAll(estudiantesTask, reportesTask)
                .addOnSuccessListener(v -> {
                    estudiantes.setValue(parseEstudiantes(estudiantesTask.getResult()));
                    reportesActivos.setValue(parseReportes(reportesTask.getResult()));
                    cargando = false;
                    cargado = true;
                    loading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    cargando = false;
                    loading.setValue(false);
                    error.setValue("No se pudieron cargar los estudiantes");
                });
    }

    public void recargar() {
        cargado = false;
        cargar();
    }

    public void clearError() {
        error.setValue(null);
    }

    public String getRiesgo() {
        return riesgo;
    }

    public void setRiesgo(String riesgo) {
        this.riesgo = riesgo;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query != null ? query : "";
    }

    private static List<Usuario> parseEstudiantes(QuerySnapshot snapshot) {
        List<Usuario> lista = new ArrayList<>();
        if (snapshot == null) return lista;
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Usuario u = doc.toObject(Usuario.class);
            if (u != null) {
                u.setUid(doc.getId());
                lista.add(u);
            }
        }
        return lista;
    }

    private static Set<String> parseReportes(QuerySnapshot snapshot) {
        Set<String> activos = new HashSet<>();
        if (snapshot == null) return activos;
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Reporte r = doc.toObject(Reporte.class);
            if (r != null && !r.isPlanCumplido() && r.getEstudianteId() != null) {
                activos.add(r.getEstudianteId());
            }
        }
        return activos;
    }
}
