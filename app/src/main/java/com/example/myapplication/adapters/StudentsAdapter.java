package com.example.myapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.Usuario;
import com.example.myapplication.utils.ListDiff;

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
    private String filtroRiesgo = "";
    private String filtroQuery = "";

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
        if (lista != null) estudiantes.addAll(lista);
        aplicarFiltro(filtroRiesgo, filtroQuery);
    }

    /**
     * Indica qué estudiantes tienen un plan de acción activo (dot en la tarjeta).
     */
    public void setReportesActivos(Set<String> activos) {
        reportesActivos.clear();
        if (activos != null) {
            reportesActivos.addAll(activos);
        }
        aplicarFiltro(filtroRiesgo, filtroQuery);
    }

    // ============================================
    // FILTRO
    // ============================================
    /**
     * Filtra por nivel de riesgo y/o texto.
     *
     * @param nivelRiesgo  null o "" para todos, o "BAJO", "MEDIO", "ALTO", "CRITICO"
     * @param query        texto de búsqueda (nombre, código o documento)
     */
    public void aplicarFiltro(String nivelRiesgo, String query) {
        filtroRiesgo = (nivelRiesgo != null) ? nivelRiesgo : "";
        filtroQuery = (query != null) ? query : "";

        List<Usuario> nuevos = new ArrayList<>();
        String q = filtroQuery.toLowerCase(java.util.Locale.ROOT).trim();

        for (Usuario u : estudiantes) {
            boolean coincideRiesgo = filtroRiesgo.isEmpty()
                    || filtroRiesgo.equals(u.getNivelRiesgo());

            boolean coincideTexto = q.isEmpty()
                    || (u.getNombre() != null && u.getNombre().toLowerCase(java.util.Locale.ROOT).contains(q))
                    || (u.getCodigo() != null && u.getCodigo().toLowerCase(java.util.Locale.ROOT).contains(q))
                    || (u.getDocumento() != null && u.getDocumento().contains(q));

            if (coincideRiesgo && coincideTexto) {
                nuevos.add(u);
            }
        }
        DiffUtil.DiffResult diff = ListDiff.calculate(filtrados, nuevos,
                (a, b) -> ListDiff.eq(a.getUid(), b.getUid()),
                (a, b) -> ListDiff.eq(a.getNombre(), b.getNombre())
                        && ListDiff.eq(a.getPrograma(), b.getPrograma())
                        && a.getSemestre() == b.getSemestre()
                        && a.getPromedio() == b.getPromedio()
                        && ListDiff.eq(a.getNivelRiesgo(), b.getNivelRiesgo())
                        && reportesActivos.contains(a.getUid())
                        == reportesActivos.contains(b.getUid()));
        filtrados.clear();
        filtrados.addAll(nuevos);
        diff.dispatchUpdatesTo(this);
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
        holder.txtPromedio.setText(String.format(java.util.Locale.getDefault(), "Prom: %.1f", prom));

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
            case "CRITICO":
                txt.setBackgroundResource(R.drawable.badge_risk_critical);
                break;
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