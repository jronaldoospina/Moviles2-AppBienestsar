package com.example.myapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.Curso;

import java.util.ArrayList;
import java.util.List;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {

    public interface OnCourseClickListener {
        void onCourseClick(Curso curso);
    }

    private final List<Curso> cursos = new ArrayList<>();
    private final List<Curso> cursosFiltrados = new ArrayList<>();
    private final OnCourseClickListener listener;
    private String query = "";

    public CourseAdapter(OnCourseClickListener listener) {
        this.listener = listener;
    }

    public void setCursos(List<Curso> lista) {
        cursos.clear();
        if (lista != null) cursos.addAll(lista);
        aplicarFiltro(query);
    }

    /** Filtra los cursos por nombre, código, programa, periodo o docente. */
    public void aplicarFiltro(String texto) {
        query = (texto != null) ? texto : "";
        cursosFiltrados.clear();
        String q = query.trim().toLowerCase();
        for (Curso c : cursos) {
            boolean ok = q.isEmpty()
                    || contiene(c.getNombre(), q)
                    || contiene(c.getCodigo(), q)
                    || contiene(c.getPrograma(), q)
                    || contiene(c.getPeriodo(), q)
                    || contiene(c.getDocenteNombre(), q);
            if (ok) cursosFiltrados.add(c);
        }
        notifyDataSetChanged();
    }

    public int getTotalFiltrado() {
        return cursosFiltrados.size();
    }

    private boolean contiene(String valor, String q) {
        return valor != null && valor.toLowerCase().contains(q);
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_course_card, parent, false);
        return new CourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Curso curso = cursosFiltrados.get(position);

        holder.txtNombre.setText(curso.getNombre() != null ? curso.getNombre() : "Sin nombre");
        holder.txtCodigo.setText(
                (curso.getCodigo() != null ? curso.getCodigo() : "SIN-COD") +
                        " · " + (curso.getPeriodo() != null ? curso.getPeriodo() : "")
        );
        holder.txtTotalEstudiantes.setText(String.valueOf(curso.getTotalEstudiantes()));
        holder.txtTotalCortes.setText(String.valueOf(curso.getTotalCortes()));
        holder.txtPrograma.setText(curso.getPrograma() != null ? curso.getPrograma() : "-");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCourseClick(curso);
        });
    }

    @Override
    public int getItemCount() {
        return cursosFiltrados.size();
    }

    public static class CourseViewHolder extends RecyclerView.ViewHolder {
        TextView txtIcono, txtNombre, txtCodigo, txtTotalEstudiantes, txtTotalCortes, txtPrograma;

        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            txtIcono = itemView.findViewById(R.id.txt_icono);
            txtNombre = itemView.findViewById(R.id.txt_nombre);
            txtCodigo = itemView.findViewById(R.id.txt_codigo);
            txtTotalEstudiantes = itemView.findViewById(R.id.txt_total_estudiantes);
            txtTotalCortes = itemView.findViewById(R.id.txt_total_cortes);
            txtPrograma = itemView.findViewById(R.id.txt_programa);
        }
    }
}