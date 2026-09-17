package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapters.NotificationsAdapter;
import com.example.myapplication.models.Notificacion;
import com.example.myapplication.repositories.NotificacionRepository;
import com.example.myapplication.services.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private TextView txtEmpty;
    private CircularProgressIndicator progress;

    private final NotificacionRepository repositorio = new NotificacionRepository();
    private SessionManager sessionManager;
    private NotificationsAdapter adapter;
    private ListenerRegistration registration;

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

        escuchar();
    }

    private void escuchar() {
        progress.setVisibility(android.view.View.VISIBLE);
        txtEmpty.setVisibility(android.view.View.GONE);

        registration = repositorio.escuchar(sessionManager.getUserId(), (snapshot, error) -> {
            progress.setVisibility(android.view.View.GONE);
            if (error != null) {
                txtEmpty.setText("No se pudieron cargar las notificaciones.");
                txtEmpty.setVisibility(android.view.View.VISIBLE);
                return;
            }

            List<Notificacion> lista = new ArrayList<>();
            if (snapshot != null) {
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Notificacion n = doc.toObject(Notificacion.class);
                    if (n != null) {
                        n.setId(doc.getId());
                        lista.add(n);
                    }
                }
            }
            lista.sort((a, b) -> Long.compare(b.getFecha(), a.getFecha()));
            adapter.setNotificaciones(lista);
            txtEmpty.setVisibility(lista.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
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
        if (registration != null) {
            registration.remove();
            registration = null;
        }
        super.onDestroy();
    }
}