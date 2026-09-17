package com.example.myapplication.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapplication.CourseDetailActivity;
import com.example.myapplication.R;
import com.example.myapplication.adapters.CourseAdapter;
import com.example.myapplication.databinding.FragmentCoursesBinding;
import com.example.myapplication.models.Curso;
import com.example.myapplication.repositories.CourseRepository;
import com.example.myapplication.services.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class CoursesFragment extends Fragment {

    private FragmentCoursesBinding binding;
    private CourseRepository courseRepository;
    private SessionManager sessionManager;
    private CourseAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCoursesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        courseRepository = new CourseRepository();
        sessionManager = new SessionManager(requireContext());

        adapter = new CourseAdapter(curso -> {
            Intent i = new Intent(requireContext(), CourseDetailActivity.class);
            i.putExtra(CourseDetailActivity.EXTRA_CURSO_ID, curso.getId());
            startActivity(i);
        });

        binding.recyclerCourses.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerCourses.setAdapter(adapter);

        cargarCursos();
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarCursos();
    }

    private void cargarCursos() {
        binding.progressCourses.setVisibility(View.VISIBLE);

        String docenteId = sessionManager.getUserId();
        if (docenteId == null) {
            binding.progressCourses.setVisibility(View.GONE);
            return;
        }

        courseRepository.getCursosByDocente(docenteId)
                .addOnSuccessListener(snapshot -> {
                    binding.progressCourses.setVisibility(View.GONE);

                    List<Curso> lista = new ArrayList<>();
                    snapshot.forEach(doc -> {
                        Curso c = doc.toObject(Curso.class);
                        if (c != null) {
                            c.setId(doc.getId());
                            lista.add(c);
                        }
                    });

                    // ⬇️ ORDENAR EN MEMORIA (más reciente primero)
                    Collections.sort(lista, (a, b) ->
                            Long.compare(b.getFechaCreacion(), a.getFechaCreacion()));

                    adapter.setCursos(lista);
                    binding.layoutEmpty.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    binding.progressCourses.setVisibility(View.GONE);
                    Toast.makeText(requireContext(),
                            "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}