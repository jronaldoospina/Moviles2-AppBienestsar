package com.example.myapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.Cita;
import com.example.myapplication.utils.FechaUtils;

import java.util.ArrayList;
import java.util.List;

public class AgendaAdapter extends RecyclerView.Adapter<AgendaAdapter.ViewHolder> {

    public interface OnCitaClickListener {
        void onCitaClick(Cita cita);
    }

    private final List<Cita> citas = new ArrayList<>();
    private OnCitaClickListener clickListener;

    public void setOnCitaClickListener(@Nullable OnCitaClickListener listener) {
        this.clickListener = listener;
    }

    public void setCitas(List<Cita> lista) {
        citas.clear();
        if (lista != null) citas.addAll(lista);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_appointment, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Cita c = citas.get(position);

        holder.txtDate.setText(FechaUtils.fechaBonitaConAnio(c.getFecha()));

        String nombre = c.getEstudianteNombre() != null ? c.getEstudianteNombre() : "";
        String hora = c.getHora() != null ? c.getHora() : "";
        holder.txtTimePsicologo.setText(
                (hora.isEmpty() ? "" : hora + " · ") + (nombre.isEmpty() ? "" : nombre));

        holder.txtMotivo.setText(c.getMotivo() != null ? c.getMotivo() : "");
        if (c.getDuracionMinutos() > 0) {
            holder.txtMotivo.append(" · " + c.getDuracionMinutos() + " min");
        }

        String estado = c.getEstado() != null ? c.getEstado() : "PENDIENTE";
        pintarEstado(holder.txtStatus, estado);

        holder.btnCancel.setVisibility(View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onCitaClick(c);
        });
    }

    private void pintarEstado(TextView tv, String estado) {
        int bg;
        int fg;
        String texto;
        switch (estado) {
            case "CONFIRMADA":
                bg = R.drawable.badge_success;
                fg = R.color.success_text;
                texto = "Confirmada";
                break;
            case "ATENDIDA":
                bg = R.drawable.badge_info;
                fg = R.color.info_text;
                texto = "Atendida";
                break;
            case "CANCELADA":
                bg = R.drawable.badge_risk_high;
                fg = R.color.risk_high_text;
                texto = "Cancelada";
                break;
            case "PENDIENTE":
            default:
                bg = R.drawable.badge_risk_medium;
                fg = R.color.risk_medium_text;
                texto = "Pendiente";
                break;
        }
        tv.setBackgroundResource(bg);
        tv.setTextColor(tv.getContext().getColor(fg));
        tv.setText(texto);
    }

    @Override
    public int getItemCount() {
        return citas.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtDate, txtStatus, txtTimePsicologo, txtMotivo;
        com.google.android.material.button.MaterialButton btnCancel;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDate = itemView.findViewById(R.id.txt_date);
            txtStatus = itemView.findViewById(R.id.txt_status);
            txtTimePsicologo = itemView.findViewById(R.id.txt_time_psicologo);
            txtMotivo = itemView.findViewById(R.id.txt_motivo);
            btnCancel = itemView.findViewById(R.id.btn_cancel);
        }
    }
}