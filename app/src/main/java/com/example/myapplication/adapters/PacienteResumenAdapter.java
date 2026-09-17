package com.example.myapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.PacienteResumen;
import com.example.myapplication.utils.AnimoUtils;

import java.util.ArrayList;
import java.util.List;

public class PacienteResumenAdapter extends RecyclerView.Adapter<PacienteResumenAdapter.ViewHolder> {

    public interface OnPacienteClickListener {
        void onPacienteClick(PacienteResumen paciente);
    }

    private final List<PacienteResumen> items = new ArrayList<>();
    private final OnPacienteClickListener listener;

    public PacienteResumenAdapter(OnPacienteClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<PacienteResumen> lista) {
        items.clear();
        if (lista != null) items.addAll(lista);
        notifyDataSetChanged();
    }

    public void updateAnimo(String uid, int animo) {
        for (int i = 0; i < items.size(); i++) {
            PacienteResumen p = items.get(i);
            if (p.getUid() != null && p.getUid().equals(uid)) {
                p.setAnimo(animo);
                notifyItemChanged(i);
                return;
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_paciente_resumen, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PacienteResumen p = items.get(position);

        holder.txtNombre.setText(p.getNombre() != null ? p.getNombre() : "Sin nombre");
        holder.txtAnimo.setText(p.getAnimo() > 0 ? String.valueOf(p.getAnimo()) : "");
        holder.txtAnimo.setBackgroundResource(AnimoUtils.circle(p.getAnimo()));

        String alertas = p.getAlertasActivas() == 1
                ? "1 alerta activa" : p.getAlertasActivas() + " alertas activas";
        String riesgo = (p.getNivelRiesgo() != null && !p.getNivelRiesgo().isEmpty())
                ? "Riesgo " + p.getNivelRiesgo() : "Sin riesgo";
        String animoLabel = p.getAnimo() > 0
                ? " · Ánimo: " + AnimoUtils.label(p.getAnimo()) : " · Sin estado registrado";
        holder.txtSub.setText(alertas + " · " + riesgo + animoLabel);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPacienteClick(p);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtAnimo, txtNombre, txtSub;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtAnimo = itemView.findViewById(R.id.txt_pac_animo);
            txtNombre = itemView.findViewById(R.id.txt_pac_nombre);
            txtSub = itemView.findViewById(R.id.txt_pac_sub);
        }
    }
}