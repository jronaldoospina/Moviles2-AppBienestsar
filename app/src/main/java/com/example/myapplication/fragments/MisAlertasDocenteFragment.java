package com.example.myapplication.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapters.AlertsAdapter;
import com.example.myapplication.models.Alerta;
import com.example.myapplication.repositories.AlertRepository;
import com.example.myapplication.services.SessionManager;
import com.example.myapplication.utils.FechaUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class MisAlertasDocenteFragment extends Fragment {

    private final List<Alerta> alertas = new ArrayList<>();
    private AlertsAdapter adapter;
    private AlertRepository repo;
    private SessionManager sessionManager;
    private ListenerRegistration registration;

    private RecyclerView recycler;
    private TextView txtEmpty;
    private ProgressBar progress;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alerts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recycler = view.findViewById(R.id.recycler_alertas);
        txtEmpty = view.findViewById(R.id.txt_alerts_empty);
        progress = view.findViewById(R.id.progress_alertas);

        repo = new AlertRepository();
        sessionManager = new SessionManager(requireContext());

        adapter = new AlertsAdapter();
        adapter.setOnAlertaClickListener(this::mostrarDetalle);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        String uid = sessionManager.getUserId();
        if (uid == null) return;
        progress.setVisibility(View.VISIBLE);
        registration = repo.escucharAlertasDeDocente(uid, this::onAlertasActualizadas);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    private void onAlertasActualizadas(@Nullable QuerySnapshot snapshots,
                                       @Nullable com.google.firebase.firestore.FirebaseFirestoreException e) {
        progress.setVisibility(View.GONE);
        if (e != null) {
            txtEmpty.setText("No se pudieron cargar las alertas.\n" + e.getMessage());
            txtEmpty.setVisibility(View.VISIBLE);
            return;
        }
        alertas.clear();
        if (snapshots != null) {
            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                Alerta a = doc.toObject(Alerta.class);
                if (a != null) {
                    a.setId(doc.getId());
                    alertas.add(a);
                }
            }
        }
        alertas.sort((x, y) -> Long.compare(y.getFecha(), x.getFecha()));
        adapter.setData(alertas);
        txtEmpty.setText("Aún no has reportado alertas.");
        txtEmpty.setVisibility(alertas.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void mostrarDetalle(Alerta a) {
        StringBuilder msg = new StringBuilder();
        msg.append("Estudiante: ").append(a.getEstudianteNombre() != null
                ? a.getEstudianteNombre() : "-").append("\n");
        msg.append("Curso: ").append(a.getCursoNombre() != null ? a.getCursoNombre() : "-")
                .append("\n");
        msg.append("Motivo: ").append(motivoLabel(a.getMotivo())).append("\n");
        if (a.getDescripcion() != null && !a.getDescripcion().isEmpty()) {
            msg.append("Descripción: ").append(a.getDescripcion()).append("\n");
        }
        msg.append("Enviada: ").append(a.getFecha() > 0
                ? FechaUtils.fechaBonitaLong(a.getFecha()) : "-").append("\n");

        String estado = a.getEstado() != null ? a.getEstado() : "PENDIENTE";
        if (!"PENDIENTE".equals(estado) && a.getFechaAtencion() > 0) {
            msg.append("Estado: ").append(estado)
                    .append(" · ").append(FechaUtils.fechaBonitaLong(a.getFechaAtencion()));
        } else {
            msg.append("Estado: ").append(estado);
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Alerta de " + a.getEstudianteNombre())
                .setMessage(msg)
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private String motivoLabel(String motivo) {
        if (motivo == null) return "";
        switch (motivo) {
            case "INASISTENCIA":   return "Inasistencia";
            case "COMPORTAMIENTO": return "Comportamiento";
            case "OTRO":           return "Otro";
            default:               return "Bajo rendimiento";
        }
    }
}