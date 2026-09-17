package com.example.myapplication.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapters.AppointmentsAdapter;
import com.example.myapplication.models.Cita;
import com.example.myapplication.repositories.AppointmentRepository;
import com.example.myapplication.repositories.AuditRepository;
import com.example.myapplication.services.SessionManager;
import com.example.myapplication.utils.CitaReminderScheduler;
import com.example.myapplication.utils.FechaUtils;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MyAppointmentsFragment extends Fragment implements AppointmentsAdapter.OnAppointmentListener {

    private RecyclerView recycler;
    private View layoutEmpty;
    private TextView txtEmpty;
    private CircularProgressIndicator progressLoading;
    private final AppointmentsAdapter adapter = new AppointmentsAdapter();

    private final AppointmentRepository appointmentRepository = new AppointmentRepository();
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_appointments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recycler = view.findViewById(R.id.recycler_citas);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        txtEmpty = view.findViewById(R.id.txt_empty);
        progressLoading = view.findViewById(R.id.progress_loading);

        adapter.setListener(this);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        sessionManager = new SessionManager(requireContext());
        load();
    }

    private void load() {
        String uid = sessionManager.getUserId();
        if (uid == null) {
            showEmpty();
            return;
        }

        progressLoading.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);

        appointmentRepository.getCitasByEstudiante(uid)
                .addOnSuccessListener(snapshot -> {
                    progressLoading.setVisibility(View.GONE);
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

                    adapter.setCitas(citas);
                    if (citas.isEmpty()) showEmpty();
                    for (Cita c : citas) {
                        CitaReminderScheduler.sincronizar(requireContext(), c);
                    }
                })
                .addOnFailureListener(e -> {
                    progressLoading.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Error al cargar citas: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    showEmpty();
                });
    }

    private void showEmpty() {
        layoutEmpty.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCancelClick(Cita cita) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Cancelar cita")
                .setMessage("¿Seguro que deseas cancelar la cita del "
                        + FechaUtils.fechaBonitaConAnio(cita.getFecha())
                        + " a las " + cita.getHora() + "?")
                .setPositiveButton("Sí, cancelar", (d, w) -> cancelarCita(cita))
                .setNegativeButton("No", null)
                .show();
    }

    private void cancelarCita(Cita cita) {
        appointmentRepository.cambiarEstado(cita.getId(), "CANCELADA")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Cita cancelada", Toast.LENGTH_SHORT).show();
                    new AuditRepository().registrar("CITA_CANCELADA",
                            "Canceló su cita del " + (cita.getFecha() != null ? cita.getFecha() : "-")
                                    + " a las " + (cita.getHora() != null ? cita.getHora() : "-"),
                            sessionManager);
                    load();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }
}