package com.example.myapplication.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapplication.R;
import com.example.myapplication.StudentDetailActivity;
import com.example.myapplication.adapters.StudentsAdapter;
import com.example.myapplication.databinding.FragmentStudentsBinding;
import com.example.myapplication.models.Reporte;
import com.example.myapplication.models.Usuario;
import com.example.myapplication.repositories.ReportRepository;
import com.example.myapplication.repositories.UserRepository;
import com.example.myapplication.services.SessionManager;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StudentsFragment extends Fragment {

    private FragmentStudentsBinding binding;
    private UserRepository userRepository;
    private SessionManager sessionManager;
    private StudentsAdapter adapter;

    private String riesgoActual = null;
    private String queryActual = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userRepository = new UserRepository();
        sessionManager = new SessionManager(requireContext());

        configurarRecycler();
        configurarBuscador();
        configurarChips();

        cargarEstudiantes();
    }

    private void configurarRecycler() {
        adapter = new StudentsAdapter(estudiante -> {
            Intent i = new Intent(requireContext(), StudentDetailActivity.class);
            i.putExtra(StudentDetailActivity.EXTRA_ESTUDIANTE_ID, estudiante.getUid());
            startActivity(i);
        });
        binding.recyclerStudents.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerStudents.setAdapter(adapter);
    }

    private void configurarBuscador() {
        binding.editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                queryActual = s.toString();
                aplicarFiltros();
            }
        });
    }

    private void configurarChips() {
        binding.chipGroupRiesgo.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                riesgoActual = null;
            } else {
                int id = checkedIds.get(0);
                if (id == R.id.chip_low) riesgoActual = "BAJO";
                else if (id == R.id.chip_medium) riesgoActual = "MEDIO";
                else if (id == R.id.chip_high || id == R.id.chip_critical) riesgoActual = "ALTO";
                else riesgoActual = null;
            }
            aplicarFiltros();
        });
    }

    private void aplicarFiltros() {
        adapter.aplicarFiltro(riesgoActual, queryActual);
        int total = adapter.getTotalFiltrado();
        binding.txtContador.setText(total + (total == 1 ? " estudiante" : " estudiantes"));
        binding.layoutEmpty.setVisibility(total == 0 ? View.VISIBLE : View.GONE);
    }

    private void cargarEstudiantes() {
        binding.progressStudents.setVisibility(View.VISIBLE);

        userRepository.getAllEstudiantes()
                .addOnSuccessListener(snapshot -> {
                    binding.progressStudents.setVisibility(View.GONE);

                    List<Usuario> lista = new ArrayList<>();
                    snapshot.forEach(doc -> {
                        Usuario u = doc.toObject(Usuario.class);
                        if (u != null) {
                            u.setUid(doc.getId());
                            lista.add(u);
                        }
                    });

                    adapter.setEstudiantes(lista);
                    aplicarFiltros();
                })
                .addOnFailureListener(e -> {
                    binding.progressStudents.setVisibility(View.GONE);
                    Toast.makeText(requireContext(),
                            "Error cargando estudiantes: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });

        cargarReportesActivos();
    }

    private void cargarReportesActivos() {
        new ReportRepository().getAllReportes()
                .addOnSuccessListener(snapshot -> {
                    Set<String> activos = new HashSet<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Reporte r = doc.toObject(Reporte.class);
                            if (r != null && !r.isPlanCumplido() && r.getEstudianteId() != null) {
                                activos.add(r.getEstudianteId());
                            }
                        }
                    }
                    adapter.setReportesActivos(activos);
                })
                .addOnFailureListener(e -> {
                    binding.progressStudents.setVisibility(View.GONE);
                    Toast.makeText(requireContext(),
                            "Error cargando reportes: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarEstudiantes();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}