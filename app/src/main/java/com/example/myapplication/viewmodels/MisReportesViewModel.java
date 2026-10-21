package com.example.myapplication.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.models.Reporte;
import com.example.myapplication.repositories.ReportRepository;
import com.example.myapplication.utils.Paginacion;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MisReportesViewModel extends ViewModel {

    private static final int TAM_PAGINA = Paginacion.PAGINA_POR_DEFECTO;

    private final ReportRepository reportRepository = new ReportRepository();

    private final MutableLiveData<List<Reporte>> reportes = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private final List<Reporte> acumulados = new ArrayList<>();
    private DocumentSnapshot cursor;
    private boolean cargado = false;
    private boolean cargando = false;
    private boolean haySiguiente = true;
    private int generacion = 0;
    private String estudianteId;

    public LiveData<List<Reporte>> getReportes() {
        return reportes;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public boolean isHaySiguiente() {
        return haySiguiente;
    }

    public void cargar(String uid) {
        if (uid == null || cargando || cargado) return;
        estudianteId = uid;
        generacion++;
        acumulados.clear();
        cursor = null;
        haySiguiente = true;
        cargarPagina(generacion);
    }

    public void cargarMas() {
        if (!cargado || cargando || !haySiguiente) return;
        cargarPagina(generacion);
    }

    private void cargarPagina(int gen) {
        cargando = true;
        loading.setValue(true);

        Paginacion.pagina(reportRepository.getReportesQuery(estudianteId), TAM_PAGINA, cursor,
                doc -> {
                    Reporte r = doc.toObject(Reporte.class);
                    if (r != null) r.setId(doc.getId());
                    return r;
                }).addOnSuccessListener(pagina -> {
            if (gen != generacion) return;
            acumulados.addAll(pagina.items);
            acumulados.sort((a, b) -> Long.compare(b.getFecha(), a.getFecha()));
            cursor = pagina.ultimo;
            haySiguiente = pagina.haySiguiente;
            cargando = false;
            cargado = true;
            loading.setValue(false);
            reportes.setValue(new ArrayList<>(acumulados));
        }).addOnFailureListener(e -> {
            if (gen != generacion) return;
            cargando = false;
            loading.setValue(false);
            error.setValue("No se pudieron cargar los registros");
        });
    }

    public void recargar() {
        if (estudianteId == null) return;
        generacion++;
        cargando = false;
        cargado = false;
        cursor = null;
        haySiguiente = true;
        acumulados.clear();
        cargar(estudianteId);
    }

    public void clearError() {
        error.setValue(null);
    }
}
