package com.example.myapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.Usuario;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.List;

public class EstudiantesMatriculaAdapter extends RecyclerView.Adapter<EstudiantesMatriculaAdapter.VH> {

    public interface OnEstadoCambioListener {
        void onEstadoCambiado();
    }

    private final List<Usuario> estudiantes = new ArrayList<>();
    private final List<String> inscritos;
    private OnEstadoCambioListener listener;

    public EstudiantesMatriculaAdapter(List<String> inscritos) {
        this.inscritos = inscritos != null ? inscritos : new ArrayList<>();
    }

    public void setOnEstadoCambioListener(OnEstadoCambioListener listener) {
        this.listener = listener;
    }

    public void setEstudiantes(List<Usuario> lista) {
        estudiantes.clear();
        if (lista != null) estudiantes.addAll(lista);
        notifyDataSetChanged();
    }

    public List<String> getInscritos() {
        return inscritos;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_matricula_estudiante, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Usuario u = estudiantes.get(position);
        String nombre = u.getNombre() != null ? u.getNombre() : "Estudiante";
        String documento = u.getDocumento() != null && !u.getDocumento().isEmpty()
                ? " · " + u.getDocumento() : "";
        holder.check.setText(nombre + documento);
        holder.check.setChecked(inscritos.contains(u.getUid()));
        holder.check.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!inscritos.contains(u.getUid())) inscritos.add(u.getUid());
            } else {
                inscritos.remove(u.getUid());
            }
            if (listener != null) listener.onEstadoCambiado();
        });
    }

    @Override
    public int getItemCount() {
        return estudiantes.size();
    }

    public static class VH extends RecyclerView.ViewHolder {
        MaterialCheckBox check;

        public VH(@NonNull View itemView) {
            super(itemView);
            check = itemView.findViewById(R.id.check_matricula);
        }
    }
}