package com.example.myapplication.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.models.Usuario;
import com.example.myapplication.repositories.UserRepository;
import com.example.myapplication.utils.Paginacion;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UsersViewModel extends ViewModel {

    private static final int TAM_PAGINA = Paginacion.PAGINA_POR_DEFECTO;

    private final UserRepository userRepository = new UserRepository();

    private final MutableLiveData<List<Usuario>> usuarios = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private final List<Usuario> acumulados = new ArrayList<>();
    private DocumentSnapshot cursor;
    private boolean cargado = false;
    private boolean cargando = false;
    private boolean haySiguiente = true;
    private int generacion = 0;
    private String rol = null;
    private String query = "";

    public LiveData<List<Usuario>> getUsuarios() {
        return usuarios;
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

    public void cargar() {
        if (cargando || cargado) return;
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

        Paginacion.pagina(userRepository.getUsuariosQuery(), TAM_PAGINA, cursor, doc -> {
            Usuario u = doc.toObject(Usuario.class);
            if (u != null) u.setUid(doc.getId());
            return u;
        }).addOnSuccessListener(pagina -> {
            if (gen != generacion) return;
            acumulados.addAll(pagina.items);
            cursor = pagina.ultimo;
            haySiguiente = pagina.haySiguiente;
            cargando = false;
            cargado = true;
            loading.setValue(false);
            usuarios.setValue(new ArrayList<>(acumulados));
        }).addOnFailureListener(e -> {
            if (gen != generacion) return;
            cargando = false;
            loading.setValue(false);
            error.setValue("No se pudieron cargar los usuarios");
        });
    }

    public void recargar() {
        generacion++;
        cargando = false;
        cargado = false;
        cursor = null;
        haySiguiente = true;
        acumulados.clear();
        cargar();
    }

    public void clearError() {
        error.setValue(null);
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query != null ? query : "";
    }
}
