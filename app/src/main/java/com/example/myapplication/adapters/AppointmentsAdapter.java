package com.example.myapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.Cita;
import com.example.myapplication.utils.FechaUtils;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class AppointmentsAdapter extends RecyclerView.Adapter<AppointmentsAdapter.CitaViewHolder> {

    public interface OnAppointmentListener {
        void onCancelClick(Cita cita);
    }

    private final List<Cita> citas = new ArrayList<>();
    private OnAppointmentListener listener;

    public void setListener(OnAppointmentListener listener) {
        this.listener = listener;
    }

    public void setCitas(List<Cita> lista) {
        citas.clear();
        if (lista != null) citas.addAll(lista);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CitaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_appointment, parent, false);
        return new CitaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CitaViewHolder holder, int position) {
        Cita c = citas.get(position);

        holder.txtDate.setText(FechaUtils.fechaBonitaConAnio(c.getFecha()));

        String hora = c.getHora() != null ? c.getHora() : "";
        holder.txtTimePsicologo.setText(hora.isEmpty() ? "--:--" : hora);

        String psi = c.getPsicologoNombre() != null && !c.getPsicologoNombre().isEmpty()
                ? c.getPsicologoNombre() : "";
        if (psi.isEmpty()) {
            holder.layoutDoctor.setVisibility(View.GONE);
        } else {
            holder.layoutDoctor.setVisibility(View.VISIBLE);
            holder.txtPsicologoName.setText(psi);
        }

        holder.txtMotivo.setText(c.getMotivo() != null ? c.getMotivo() : "");

        String estado = c.getEstado() != null ? c.getEstado() : "PENDIENTE";
        pintarEstado(holder.txtStatus, estado);

        boolean cancelable = "PENDIENTE".equals(estado) || "CONFIRMADA".equals(estado);
        holder.btnCancel.setVisibility(cancelable ? View.VISIBLE : View.GONE);
        holder.btnCancel.setOnClickListener(v -> {
            if (listener != null) listener.onCancelClick(c);
        });
    }

    @Override
    public int getItemCount() {
        return citas.size();
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

    static class CitaViewHolder extends RecyclerView.ViewHolder {
        TextView txtDate, txtStatus, txtTimePsicologo, txtPsicologoName, txtMotivo;
        View layoutDoctor;
        MaterialButton btnCancel;

        CitaViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDate = itemView.findViewById(R.id.txt_date);
            txtStatus = itemView.findViewById(R.id.txt_status);
            txtTimePsicologo = itemView.findViewById(R.id.txt_time_psicologo);
            txtPsicologoName = itemView.findViewById(R.id.txt_psicologo_name);
            layoutDoctor = itemView.findViewById(R.id.layout_doctor);
            txtMotivo = itemView.findViewById(R.id.txt_motivo);
            btnCancel = itemView.findViewById(R.id.btn_cancel);
        }
    }
}