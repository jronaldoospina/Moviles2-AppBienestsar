package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapters.NotificationsAdapter;
import com.example.myapplication.models.Notificacion;
import com.example.myapplication.repositories.NotificacionRepository;
import com.example.myapplication.services.SessionManager;
import com.example.myapplication.utils.UiUtils;
import com.example.myapplication.viewmodels.NotificationsViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.CircularProgressIndicator;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private TextView txtEmpty;
    private CircularProgressIndicator progress;

    private final NotificacionRepository repositorio = new NotificacionRepository();
    private SessionManager sessionManager;
    private NotificationsAdapter adapter;
    private NotificationsViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        String uid = new SessionManager(this).getUserId();
        if (uid == null) {
            Toast.makeText(this, "Sesión no válida", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        sessionManager = new SessionManager(this);
        viewModel = new ViewModelProvider(this).get(NotificationsViewModel.class);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_notifs);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.inflateMenu(R.menu.menu_notifications);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_mark_all) {
                marcarTodasLeidas();
                return true;
            }
            return false;
        });

        recycler = findViewById(R.id.recycler_notificaciones);
        txtEmpty = findViewById(R.id.txt_empty_notifs);
        progress = findViewById(R.id.progress_notifs);

        adapter = new NotificationsAdapter(this::abrirNotificacion);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
        recycler.setItemAnimator(null);

        observarViewModel();
        viewModel.empezarEscucha(uid);
    }

    private void observarViewModel() {
        viewModel.getNotificaciones().observe(this, lista -> {
            adapter.setNotificaciones(lista);
            txtEmpty.setVisibility(lista.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
        });
        viewModel.getLoading().observe(this, cargando ->
                progress.setVisibility(
                        Boolean.TRUE.equals(cargando) ? android.view.View.VISIBLE : android.view.View.GONE));
        viewModel.getError().observe(this, mensaje -> {
            if (mensaje == null) return;
            UiUtils.showRetry(findViewById(android.R.id.content), mensaje, viewModel::reintentar);
            viewModel.clearError();
        });
    }

    private void marcarTodasLeidas() {
        repositorio.marcarTodasLeidas(sessionManager.getUserId())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void abrirNotificacion(Notificacion n) {
        if (!n.isLeida()) {
            repositorio.marcarLeida(n.getId())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }

        if (n.getConversacionId() != null) {
            Intent i = new Intent(this, ChatDetailActivity.class);
            i.putExtra(ChatDetailActivity.EXTRA_CONVERSACION_ID, n.getConversacionId());
            String nombre = n.getOrigenNombre() != null ? n.getOrigenNombre() : "Conversación";
            i.putExtra(ChatDetailActivity.EXTRA_OTRO_NOMBRE, nombre);
            if (n.getCreadaPor() != null) {
                i.putExtra(ChatDetailActivity.EXTRA_OTRO_ID, n.getCreadaPor());
            }
            startActivity(i);
            return;
        }

        String destino = destinoPara(n.getTipo());
        if (destino != null) {
            Intent i = new Intent(this, MainActivity.class);
            i.putExtra(MainActivity.EXTRA_NAV_DESTINO, destino);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
            finish();
        }
    }

    private String destinoPara(String tipo) {
        if (tipo == null) return null;
        switch (tipo) {
            case "CITA":      return "citas";
            case "CURSO":     return "cursos";
            case "REPORTE":   return "bienestar";
            case "PLAN":      return "bienestar";
            case "ALERTA":    return "alertas";
            case "TUTORIA":   return "tutorias";
            default:          return null;
        }
    }

    @Override
    protected void onDestroy() {
        viewModel.detenerEscucha();
        super.onDestroy();
    }
}