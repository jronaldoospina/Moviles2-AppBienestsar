package com.example.myapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.Conversacion;
import com.example.myapplication.utils.FechaUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConversationsAdapter extends RecyclerView.Adapter<ConversationsAdapter.VH> {

    public interface OnConversationClickListener {
        void onConversationClick(Conversacion conversacion);
    }

    private final List<Conversacion> items = new ArrayList<>();
    private final OnConversationClickListener listener;
    private final String miUid;
    private final Locale es = new Locale("es", "CO");

    public ConversationsAdapter(String miUid, OnConversationClickListener listener) {
        this.miUid = miUid;
        this.listener = listener;
    }

    public void setConversaciones(List<Conversacion> conversaciones) {
        items.clear();
        if (conversaciones != null) {
            items.addAll(conversaciones);
        }
        notifyDataSetChanged();
    }

    private String nombreOtro(Conversacion c) {
        if (miUid != null && miUid.equals(c.getPsicologoId())) {
            return c.getEstudianteNombre() != null ? c.getEstudianteNombre() : "Estudiante";
        }
        return c.getPsicologoNombre() != null ? c.getPsicologoNombre() : "Psicólogo";
    }

    private String iniciales(String nombre) {
        if (nombre == null || nombre.isEmpty()) return "??";
        String[] partes = nombre.trim().split(" ");
        if (partes.length == 1) {
            return partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        }
        return (partes[0].charAt(0) + "" + partes[partes.length - 1].charAt(0)).toUpperCase();
    }

    private String fechaLabel(long ms) {
        java.time.LocalDate hoy = java.time.LocalDate.now(ZoneId.systemDefault());
        java.time.LocalDate fecha = new java.util.Date(ms).toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        if (hoy.equals(fecha)) return FechaUtils.horaCorta(ms);
        if (hoy.minusDays(1).equals(fecha)) return "Ayer";
        return fecha.getDayOfMonth() + " "
                + fecha.getMonth().getDisplayName(TextStyle.SHORT, es);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Conversacion c = items.get(position);
        String nombre = nombreOtro(c);
        holder.txtNombre.setText(nombre);
        holder.txtIniciales.setText(iniciales(nombre));
        holder.txtUltimo.setText(c.getUltimoMensaje() != null ? c.getUltimoMensaje() : "");
        holder.txtFecha.setText(fechaLabel(c.getUltimaActualizacion()));

        long noLeidos = 0;
        if (miUid != null && c.getPendientes() != null) {
            Long v = c.getPendientes().get(miUid);
            if (v != null) noLeidos = v;
        }
        if (noLeidos > 0) {
            holder.txtBadge.setVisibility(View.VISIBLE);
            holder.txtBadge.setText(noLeidos > 99 ? "99+" : String.valueOf(noLeidos));
            holder.txtNombre.setTypeface(holder.txtNombre.getTypeface(), android.graphics.Typeface.BOLD);
            holder.txtUltimo.setTextColor(holder.txtUltimo.getContext()
                    .getColor(R.color.text_primary));
        } else {
            holder.txtBadge.setVisibility(View.GONE);
            holder.txtUltimo.setTextColor(holder.txtUltimo.getContext()
                    .getColor(R.color.text_secondary));
        }

        holder.itemView.setOnClickListener(v -> listener.onConversationClick(c));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView txtIniciales, txtNombre, txtUltimo, txtFecha, txtBadge;

        VH(@NonNull View itemView) {
            super(itemView);
            txtIniciales = itemView.findViewById(R.id.txt_conv_iniciales);
            txtNombre = itemView.findViewById(R.id.txt_conv_nombre);
            txtUltimo = itemView.findViewById(R.id.txt_conv_ultimo);
            txtFecha = itemView.findViewById(R.id.txt_conv_fecha);
            txtBadge = itemView.findViewById(R.id.txt_conv_badge);
        }
    }
}