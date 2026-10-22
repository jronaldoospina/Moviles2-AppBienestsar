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
import androidx.lifecycle.ViewModelProvider;
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
import com.example.myapplication.utils.UiUtils;
import com.example.myapplication.viewmodels.MyAppointmentsViewModel;
import com.google.android.material.progressindicator.CircularProgressIndicator;

public class MyAppointmentsFragment extends Fragment implements AppointmentsAdapter.OnAppointmentListener {

    private RecyclerView recycler;
    private View layoutEmpty;
    private TextView txtEmpty;
    private CircularProgressIndicator progressLoading;
    private final AppointmentsAdapter adapter = new AppointmentsAdapter();

    private SessionManager sessionManager;
    private MyAppointmentsViewModel viewModel;
    private final AppointmentRepository appointmentRepository = new AppointmentRepository();

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
        viewModel = new ViewModelProvider(this).get(MyAppointmentsViewModel.class);

        observarViewModel();
        viewModel.cargar(sessionManager.getUserId());
    }

    private void observarViewModel() {
        viewModel.getCitas().observe(getViewLifecycleOwner(), citas -> {
            adapter.setCitas(citas);
            if (citas.isEmpty()) showEmpty();
            for (Cita c : citas) {
                CitaReminderScheduler.sincronizar(requireContext(), c);
            }
        });
        viewModel.getLoading().observe(getViewLifecycleOwner(), cargando ->
                progressLoading.setVisibility(
                        Boolean.TRUE.equals(cargando) ? View.VISIBLE : View.GONE));
        viewModel.getError().observe(getViewLifecycleOwner(), mensaje -> {
            if (mensaje == null) return;
            UiUtils.showRetry(requireView(), mensaje, viewModel::recargar);
            viewModel.clearError();
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
                    viewModel.recargar();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }
}