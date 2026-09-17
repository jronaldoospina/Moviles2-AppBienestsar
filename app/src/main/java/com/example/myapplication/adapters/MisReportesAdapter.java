package com.example.myapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.Reporte;
import com.example.myapplication.utils.FechaUtils;

import java.util.ArrayList;
import java.util.List;

public class MisReportesAdapter extends RecyclerView.Adapter<MisReportesAdapter.ViewHolder> {

    public interface OnReporteClickListener {
        void onReporteClick(Reporte reporte);
    }

    private final List<Reporte> reportes = new ArrayList<>();
    private OnReporteClickListener clickListener;

    public void setOnReporteClickListener(@Nullable OnReporteClickListener listener) {
        this.clickListener = listener;
    }

    public void setData(List<Reporte> nuevos) {
        reportes.clear();
        if (nuevos != null) reportes.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reporte_estudiante, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Reporte r = reportes.get(position);

        boolean esPlan = "PLAN".equals(r.getTipo());
        holder.txtTipo.setText(esPlan ? "PLAN DE ACCIÓN" : "REPORTE");
        holder.txtTipo.setBackgroundResource(esPlan
                ? R.drawable.badge_success : R.drawable.badge_info);
        holder.txtTipo.setTextColor(holder.itemView.getContext()
                .getColor(esPlan ? R.color.success_text : R.color.info_text));

        String estado = r.getEstado() != null ? r.getEstado() : "ABIERTO";
        holder.txtEstado.setText(estadoLabel(estado));
        holder.txtEstado.setBackgroundResource(estadoBadge(estado));
        holder.txtEstado.setTextColor(holder.itemView.getContext()
                .getColor(estadoFgColor(estado)));

        holder.txtTitulo.setText(r.getTitulo() != null ? r.getTitulo() : "Sin título");
        holder.txtDesc.setText(r.getDescripcion() != null
                ? r.getDescripcion() : "Sin descripción");
        holder.txtFecha.setText(r.getFecha() > 0
                ? FechaUtils.fechaBonitaLong(r.getFecha()) : "");

        holder.txtPsicologo.setText("Por: " + (r.getPsicologoNombre() != null
                ? r.getPsicologoNombre() : "Bienestar Univ."));

        String nivel = r.getNivelRiesgo() != null ? r.getNivelRiesgo() : "";
        if ("ALTO".equals(nivel)) {
            holder.txtRiesgo.setText("Riesgo: Alto");
            holder.txtRiesgo.setTextColor(holder.itemView.getContext()
                    .getColor(R.color.risk_high_text));
        } else if ("MEDIO".equals(nivel)) {
            holder.txtRiesgo.setText("Riesgo: Medio");
            holder.txtRiesgo.setTextColor(holder.itemView.getContext()
                    .getColor(R.color.risk_medium_text));
        } else if ("BAJO".equals(nivel)) {
            holder.txtRiesgo.setText("Riesgo: Bajo");
            holder.txtRiesgo.setTextColor(holder.itemView.getContext()
                    .getColor(R.color.risk_low_text));
        } else {
            holder.txtRiesgo.setText("");
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onReporteClick(r);
        });
    }

    private String estadoLabel(String estado) {
        switch (estado) {
            case "ABIERTO":        return "ABIERTO";
            case "EN_SEGUIMIENTO": return "EN SEGUIMIENTO";
            case "ATENDIDO":       return "ATENDIDO";
            default:               return estado;
        }
    }

    private int estadoBadge(String estado) {
        switch (estado) {
            case "ABIERTO":        return R.drawable.badge_risk_high;
            case "EN_SEGUIMIENTO": return R.drawable.badge_info;
            case "ATENDIDO":       return R.drawable.badge_success;
            default:               return R.drawable.badge_info;
        }
    }

    private int estadoFgColor(String estado) {
        switch (estado) {
            case "ABIERTO":        return R.color.risk_high_text;
            case "EN_SEGUIMIENTO": return R.color.info_text;
            case "ATENDIDO":       return R.color.success_text;
            default:               return R.color.info_text;
        }
    }

    @Override
    public int getItemCount() {
        return reportes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTipo, txtEstado, txtFecha, txtTitulo, txtDesc, txtPsicologo, txtRiesgo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTipo = itemView.findViewById(R.id.txt_reporte_tipo);
            txtEstado = itemView.findViewById(R.id.txt_reporte_estado);
            txtFecha = itemView.findViewById(R.id.txt_reporte_fecha);
            txtTitulo = itemView.findViewById(R.id.txt_reporte_titulo);
            txtDesc = itemView.findViewById(R.id.txt_reporte_desc);
            txtPsicologo = itemView.findViewById(R.id.txt_reporte_psicologo);
            txtRiesgo = itemView.findViewById(R.id.txt_reporte_riesgo);
        }
    }
}