package com.example.myapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.Alerta;

import java.util.List;

public class AlertaAdapter extends RecyclerView.Adapter<AlertaAdapter.ViewHolder> {

    private final List<Alerta> alertas;

    public AlertaAdapter(List<Alerta> alertas) {
        this.alertas = alertas;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alerta_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Alerta alerta = alertas.get(position);

        holder.txtTitulo.setText(alerta.getTitulo());
        holder.txtFecha.setText(alerta.getFechaStr());
        holder.txtEstudiante.setText(alerta.getEstudianteNombre());
        holder.txtMotivo.setText(alerta.getMotivoLabel());
        holder.txtDescripcion.setText(alerta.getDescripcion() != null
                ? alerta.getDescripcion() : "-");
    }

    @Override
    public int getItemCount() {
        return alertas.size();
    }

    public void updateData(List<Alerta> nuevasAlertas) {
        alertas.clear();
        if (nuevasAlertas != null) {
            alertas.addAll(nuevasAlertas);
        }
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView txtTitulo;
        public TextView txtFecha;
        public TextView txtEstudiante;
        public TextView txtMotivo;
        public TextView txtDescripcion;

        public ViewHolder(View view) {
            super(view);
            txtTitulo = view.findViewById(R.id.txtTitulo);
            txtFecha = view.findViewById(R.id.txtFecha);
            txtEstudiante = view.findViewById(R.id.txtEstudiante);
            txtMotivo = view.findViewById(R.id.txtMotivo);
            txtDescripcion = view.findViewById(R.id.txtDescripcion);
        }
    }
}