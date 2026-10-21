package com.example.myapplication.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapplication.R;
import com.example.myapplication.StudentDetailActivity;
import com.example.myapplication.adapters.StudentsAdapter;
import com.example.myapplication.databinding.FragmentStudentsBinding;
import com.example.myapplication.utils.Debouncer;
import com.example.myapplication.utils.UiUtils;
import com.example.myapplication.viewmodels.StudentsViewModel;

public class StudentsFragment extends Fragment {

    private FragmentStudentsBinding binding;
    private StudentsAdapter adapter;
    private StudentsViewModel viewModel;

    private String riesgoActual = null;
    private String queryActual = "";
    private boolean primeraResume = true;

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

        viewModel = new ViewModelProvider(this).get(StudentsViewModel.class);
        riesgoActual = viewModel.getRiesgo();
        queryActual = viewModel.getQuery();

        configurarRecycler();
        configurarBuscador();
        configurarChips();
        observarViewModel();
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

    private final Debouncer searchDebouncer = new Debouncer(300);

    private void configurarBuscador() {
        binding.editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString();
                searchDebouncer.submit(() -> {
                    queryActual = query;
                    viewModel.setQuery(query);
                    aplicarFiltros();
                });
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
                else if (id == R.id.chip_high) riesgoActual = "ALTO";
                else if (id == R.id.chip_critical) riesgoActual = "CRITICO";
                else riesgoActual = null;
            }
            viewModel.setRiesgo(riesgoActual);
            aplicarFiltros();
        });
    }

    private void aplicarFiltros() {
        adapter.aplicarFiltro(riesgoActual, queryActual);
        int total = adapter.getTotalFiltrado();
        binding.txtContador.setText(total + (total == 1 ? " estudiante" : " estudiantes"));
        binding.layoutEmpty.setVisibility(total == 0 ? View.VISIBLE : View.GONE);
    }

    private void observarViewModel() {
        viewModel.getEstudiantes().observe(getViewLifecycleOwner(), lista -> {
            adapter.setEstudiantes(lista);
            aplicarFiltros();
        });
        viewModel.getReportesActivos().observe(getViewLifecycleOwner(),
                activos -> adapter.setReportesActivos(activos));
        viewModel.getLoading().observe(getViewLifecycleOwner(), cargando ->
                binding.progressStudents.setVisibility(
                        Boolean.TRUE.equals(cargando) ? View.VISIBLE : View.GONE));
        viewModel.getError().observe(getViewLifecycleOwner(), mensaje -> {
            if (mensaje == null) return;
            UiUtils.showRetry(binding.getRoot(), mensaje, viewModel::recargar);
            viewModel.clearError();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (primeraResume) {
            primeraResume = false;
            viewModel.cargar();
        } else {
            viewModel.recargar();
        }
    }

    @Override
    public void onDestroyView() {
        searchDebouncer.cancel();
        super.onDestroyView();
        binding = null;
    }
}