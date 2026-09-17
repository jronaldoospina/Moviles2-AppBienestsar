package com.example.myapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.AuditoriaLog;
import com.example.myapplication.utils.FechaUtils;

import java.util.ArrayList;
import java.util.List;

public class AuditAdapter extends RecyclerView.Adapter<AuditAdapter.ViewHolder> {

    private final List<AuditoriaLog> logs = new ArrayList<>();

    public void setData(List<AuditoriaLog> nuevos) {
        logs.clear();
        if (nuevos != null) logs.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_audit, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AuditoriaLog log = logs.get(position);
        holder.txtTipo.setText(tipoLabel(log.getTipo()));
        holder.txtTipo.setBackgroundResource(tipoBadge(log.getTipo()));
        holder.txtTipo.setTextColor(holder.itemView.getContext()
                .getColor(tipoFgColor(log.getTipo())));
        holder.txtDetalle.setText(log.getDetalle() != null
                ? log.getDetalle() : "");
        String autor = log.getUsuarioNombre() != null ? log.getUsuarioNombre() : "Usuario";
        if (log.getRol() != null && !log.getRol().isEmpty()) {
            autor += " (" + log.getRol() + ")";
        }
        holder.txtAutor.setText("Por: " + autor);
        holder.txtFecha.setText(log.getFecha() > 0
                ? FechaUtils.fechaBonitaLong(log.getFecha()) : "");
    }

    private String tipoLabel(String tipo) {
        if (tipo == null) return "";
        switch (tipo) {
            case "LOGIN":             return "INICIO DE SESIÓN";
            case "USUARIO_CREADO":    return "USUARIO CREADO";
            case "USUARIO_ACTIVADO":  return "USUARIO ACTIVADO";
            case "USUARIO_DESACTIVADO": return "USUARIO DESACTIVADO";
            case "USUARIO_ROL_CAMBIADO": return "ROL CAMBIADO";
            case "ALERTA_CREADA":     return "ALERTA CREADA";
            case "ALERTA_ATENDIDA":   return "ALERTA ATENDIDA";
            case "ALERTA_ELIMINADA":  return "ALERTA ELIMINADA";
            case "TUTORIA_CREADA":    return "TUTORÍA CREADA";
            case "TUTORIA_ELIMINADA": return "TUTORÍA ELIMINADA";
            case "CURSO_CREADO":      return "CURSO CREADO";
            case "CURSO_ELIMINADO":   return "CURSO ELIMINADO";
            case "MATRICULA":         return "MATRÍCULA ACTUALIZADA";
            case "CITA_CANCELADA":    return "CITA CANCELADA";
            case "CITA_CREADA":       return "CITA CREADA";
            case "CITA_CONFIRMADA":   return "CITA CONFIRMADA";
            case "CITA_ATENDIDA":     return "CITA ATENDIDA";
            case "REPORTE_CREADO":    return "REPORTE CREADO";
            case "PLAN_CREADO":       return "PLAN CREADO";
            default:                  return tipo;
        }
    }

    private int tipoBadge(String tipo) {
        if (tipo == null) return R.drawable.badge_info;
        switch (tipo) {
            case "LOGIN":                 return R.drawable.badge_info;
            case "USUARIO_CREADO":
            case "USUARIO_ACTIVADO":
            case "USUARIO_ROL_CAMBIADO":  return R.drawable.badge_success;
            case "USUARIO_DESACTIVADO":   return R.drawable.badge_risk_medium;
            case "ALERTA_CREADA":
            case "ALERTA_ATENDIDA":
            case "ALERTA_ELIMINADA":
            case "TUTORIA_CREADA":
            case "TUTORIA_ELIMINADA":
            case "CURSO_CREADO":
            case "CURSO_ELIMINADO":
            case "MATRICULA":         return R.drawable.badge_risk_high;
            case "CITA_CANCELADA":
            case "CITA_CREADA":
            case "CITA_ATENDIDA":     return R.drawable.badge_risk_high;
            case "CITA_CONFIRMADA":   return R.drawable.badge_success;
            case "REPORTE_CREADO":
            case "PLAN_CREADO":           return R.drawable.badge_success;
            default:                      return R.drawable.badge_info;
        }
    }

    private int tipoFgColor(String tipo) {
        int badge = tipoBadge(tipo);
        if (badge == R.drawable.badge_success) return R.color.success_text;
        if (badge == R.drawable.badge_risk_medium) return R.color.risk_medium_text;
        if (badge == R.drawable.badge_risk_high) return R.color.risk_high_text;
        return R.color.info_text;
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTipo, txtDetalle, txtAutor, txtFecha;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTipo = itemView.findViewById(R.id.txt_audit_tipo);
            txtDetalle = itemView.findViewById(R.id.txt_audit_detalle);
            txtAutor = itemView.findViewById(R.id.txt_audit_autor);
            txtFecha = itemView.findViewById(R.id.txt_audit_fecha);
        }
    }
}