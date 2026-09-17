package com.example.myapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.Usuario;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StudentsAdapter extends RecyclerView.Adapter<StudentsAdapter.StudentViewHolder> {

    // ============================================
    // CALLBACK
    // ============================================
    public interface OnStudentClickListener {
        void onStudentClick(Usuario estudiante);
    }

    // ============================================
    // CAMPOS
    // ============================================
    private final List<Usuario> estudiantes = new ArrayList<>();
    private final List<Usuario> filtrados = new ArrayList<>();
    private final Set<String> reportesActivos = new HashSet<>();
    private final OnStudentClickListener listener;

    // ============================================
    // CONSTRUCTOR
    // ============================================
    public StudentsAdapter(OnStudentClickListener listener) {
        this.listener = listener;
    }

    // ============================================
    // SETTER DE DATOS
    // ============================================
    public void setEstudiantes(List<Usuario> lista) {
        estudiantes.clear();
        estudiantes.addAll(lista);
        aplicarFiltro(null, null);
    }

    /**
     * Indica qué estudiantes tienen un plan de acción activo (dot en la tarjeta).
     */
    public void setReportesActivos(Set<String> activos) {
        reportesActivos.clear();
        if (activos != null) {
            reportesActivos.addAll(activos);
        }
        notifyDataSetChanged();
    }

    // ============================================
    // FILTRO
    // ============================================
    /**
     * Filtra por nivel de riesgo y/o texto.
     *
     * @param nivelRiesgo  null o "" para todos, o "BAJO", "MEDIO", "ALTO"
     * @param query        texto de búsqueda (nombre, código o documento)
     */
    public void aplicarFiltro(String nivelRiesgo, String query) {
        filtrados.clear();
        String q = (query != null) ? query.toLowerCase().trim() : "";

        for (Usuario u : estudiantes) {
            boolean coincideRiesgo = (nivelRiesgo == null
                    || nivelRiesgo.isEmpty()
                    || nivelRiesgo.equals(u.getNivelRiesgo()));

            boolean coincideTexto = q.isEmpty()
                    || (u.getNombre() != null && u.getNombre().toLowerCase().contains(q))
                    || (u.getCodigo() != null && u.getCodigo().toLowerCase().contains(q))
                    || (u.getDocumento() != null && u.getDocumento().contains(q));

            if (coincideRiesgo && coincideTexto) {
                filtrados.add(u);
            }
        }
        notifyDataSetChanged();
    }

    public int getTotalFiltrado() {
        return filtrados.size();
    }

    // ============================================
    // RECYCLER VIEW
    // ============================================
    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_card, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        Usuario u = filtrados.get(position);

        // Nombre e iniciales
        holder.txtNombre.setText(u.getNombre() != null ? u.getNombre() : "Sin nombre");
        holder.txtIniciales.setText(u.getIniciales());

        // Programa + semestre
        String programa = u.getPrograma() != null ? u.getPrograma() : "Sin programa";
        String semestre = u.getSemestre() > 0 ? "Semestre " + u.getSemestre() : "";
        holder.txtPrograma.setText(semestre.isEmpty()
                ? programa
                : programa + " · " + semestre);

        // Promedio
        double prom = u.getPromedio();
        holder.txtPromedio.setText(String.format("Prom: %.1f", prom));

        // Nivel de riesgo con color
        String riesgo = u.getNivelRiesgo() != null ? u.getNivelRiesgo() : "BAJO";
        holder.txtRiesgo.setText(riesgo);
        aplicarColorRiesgo(holder.txtRiesgo, riesgo);

        // Punto si tiene plan de acción activo
        holder.dotReporte.setVisibility(
                u.getUid() != null && reportesActivos.contains(u.getUid())
                        ? View.VISIBLE : View.GONE);

        // Click en la tarjeta
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onStudentClick(u);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filtrados.size();
    }

    // ============================================
    // HELPER: color según nivel de riesgo
    // ============================================
    private void aplicarColorRiesgo(TextView txt, String riesgo) {
        switch (riesgo) {
            case "ALTO":
                txt.setBackgroundResource(R.drawable.badge_risk_high);
                break;
            case "MEDIO":
                txt.setBackgroundResource(R.drawable.badge_risk_medium);
                break;
            case "BAJO":
            default:
                txt.setBackgroundResource(R.drawable.badge_risk_low);
                break;
        }
    }

    // ============================================
    // VIEW HOLDER
    // ============================================
    static class StudentViewHolder extends RecyclerView.ViewHolder {
        TextView txtIniciales, txtNombre, txtPrograma, txtRiesgo, txtPromedio;
        View dotReporte;

        StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            txtIniciales = itemView.findViewById(R.id.txt_iniciales);
            txtNombre = itemView.findViewById(R.id.txt_nombre);
            txtPrograma = itemView.findViewById(R.id.txt_programa);
            txtRiesgo = itemView.findViewById(R.id.txt_riesgo);
            txtPromedio = itemView.findViewById(R.id.txt_promedio);
            dotReporte = itemView.findViewById(R.id.dot_reporte);
        }
    }
}