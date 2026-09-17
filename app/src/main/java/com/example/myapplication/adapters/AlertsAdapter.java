package com.example.myapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.Alerta;
import com.example.myapplication.utils.FechaUtils;

import java.util.ArrayList;
import java.util.List;

public class AlertsAdapter extends RecyclerView.Adapter<AlertsAdapter.ViewHolder> {

    public interface OnAlertaClickListener {
        void onAlertaClick(Alerta alerta);
    }

    private final List<Alerta> alertas = new ArrayList<>();
    private OnAlertaClickListener clickListener;

    public void setOnAlertaClickListener(@Nullable OnAlertaClickListener listener) {
        this.clickListener = listener;
    }

    public void setData(List<Alerta> nuevas) {
        alertas.clear();
        if (nuevas != null) alertas.addAll(nuevas);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alerta, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Alerta a = alertas.get(position);
        holder.txtEstudiante.setText(a.getEstudianteNombre() != null
                ? a.getEstudianteNombre() : "Estudiante");
        holder.txtCurso.setText(a.getCursoNombre() != null ? a.getCursoNombre() : "");
        holder.txtMotivo.setText(motivoLabel(a.getMotivo()));
        holder.txtDesc.setText(a.getDescripcion() != null ? a.getDescripcion() : "");
        holder.txtFecha.setText(a.getFecha() > 0
                ? FechaUtils.fechaBonitaLong(a.getFecha()) : "");
        holder.txtDocente.setText("Reportado por: " +
                (a.getDocenteNombre() != null ? a.getDocenteNombre() : "Docente"));

        holder.txtEstado.setText(a.getEstado() != null ? a.getEstado() : "PENDIENTE");
        String estado = a.getEstado() != null ? a.getEstado() : "PENDIENTE";
        boolean atendida = estado.equals("ATENDIDA");
        boolean revisada = estado.equals("REVISADA");
        holder.txtEstado.setBackgroundResource(atendida
                ? R.drawable.badge_success
                : revisada
                ? R.drawable.badge_info
                : R.drawable.badge_risk_high);
        holder.txtEstado.setTextColor(holder.itemView.getContext().getColor(atendida
                ? R.color.success_text
                : revisada ? R.color.info_text : R.color.risk_high_text));

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onAlertaClick(a);
        });
    }

    private String motivoLabel(String motivo) {
        if (motivo == null) return "";
        switch (motivo) {
            case "INASISTENCIA":   return "Inasistencia";
            case "COMPORTAMIENTO": return "Comportamiento";
            case "OTRO":           return "Otro";
            default:               return "Bajo rendimiento";
        }
    }

    @Override
    public int getItemCount() {
        return alertas.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtEstudiante, txtCurso, txtMotivo, txtDesc, txtFecha, txtDocente, txtEstado;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtEstudiante = itemView.findViewById(R.id.txt_alerta_estudiante);
            txtCurso = itemView.findViewById(R.id.txt_alerta_curso);
            txtMotivo = itemView.findViewById(R.id.txt_alerta_motivo);
            txtDesc = itemView.findViewById(R.id.txt_alerta_desc);
            txtFecha = itemView.findViewById(R.id.txt_alerta_fecha);
            txtDocente = itemView.findViewById(R.id.txt_alerta_docente);
            txtEstado = itemView.findViewById(R.id.txt_alerta_estado);
        }
    }
}