package com.example.myapplication.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;
import com.example.myapplication.models.Reporte;
import com.example.myapplication.repositories.ReportRepository;
import com.example.myapplication.services.SessionManager;

import java.util.HashMap;
import java.util.Map;

public class ReportProblematicaFragment extends Fragment {

    private static final String EXTRA_ESTUDIANTE_ID = "estudiante_id";
    private static final String EXTRA_ESTUDIANTE_NOMBRE = "estudiante_nombre";

    private String estudianteId;
    private String estudianteNombre;
    private ReportRepository reporteRepository;
    private SessionManager sessionManager;

    private Button btnReportar; // Placeholder - en implementación completa habría más campos

    public static ReportProblematicaFragment newInstance(String estudianteId, String estudianteNombre) {
        ReportProblematicaFragment fragment = new ReportProblematicaFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_ESTUDIANTE_ID, estudianteId);
        args.putString(EXTRA_ESTUDIANTE_NOMBRE, estudianteNombre);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report_problematica, container, false);

        estudianteId = getArguments() != null ? getArguments().getString(EXTRA_ESTUDIANTE_ID) : null;
        estudianteNombre = getArguments() != null ? getArguments().getString(EXTRA_ESTUDIANTE_NOMBRE) : null;

        sessionManager = new SessionManager(requireContext());
        reporteRepository = new ReportRepository();

        btnReportar = view.findViewById(R.id.btn_reportar);

        btnReportar.setOnClickListener(v -> reportarProblematica());

        return view;
    }

    private void reportarProblematica() {
        if (estudianteId == null) {
            Toast.makeText(requireContext(), "ID de estudiante no disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear reporte con datos básicos
        Reporte reporte = new Reporte();
        reporte.setEstudianteId(estudianteId);
        reporte.setEstudianteNombre(estudianteNombre);
        reporte.setTitulo("Problemática reportada");
        reporte.setTipo("PROBLEMATICA");
        reporte.setDescripcion("Problemática reportada por el estudiante");
        reporte.setNivelRiesgo("OTRO");
        reporte.setEstado("PENDIENTE");
        reporte.setPlanCumplido(false);

        reporteRepository.crearReporte(reporte)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(requireContext(), "Problemática reportada exitosamente", Toast.LENGTH_SHORT).show();
                    // Regresar al fragment anterior
                    if (getFragmentManager() != null) {
                        getFragmentManager().popBackStack();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error al reportar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}