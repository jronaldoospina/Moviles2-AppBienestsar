package com.example.myapplication.adapters;

import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.Notificacion;
import com.example.myapplication.utils.FechaUtils;
import com.example.myapplication.utils.ListDiff;
import com.google.android.material.card.MaterialCardView;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.VH> {

    public interface OnNotificationClickListener {
        void onClick(Notificacion notificacion);
    }

    private final List<Notificacion> items = new ArrayList<>();
    private final OnNotificationClickListener listener;
    private final Locale es = new Locale("es", "CO");

    public NotificationsAdapter(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    public void setNotificaciones(List<Notificacion> notificaciones) {
        List<Notificacion> nuevas = notificaciones != null
                ? new ArrayList<>(notificaciones)
                : new ArrayList<>();
        DiffUtil.DiffResult diff = ListDiff.calculate(items, nuevas,
                (a, b) -> ListDiff.eq(a.getId(), b.getId()),
                (a, b) -> ListDiff.eq(a.getTitulo(), b.getTitulo())
                        && ListDiff.eq(a.getCuerpo(), b.getCuerpo())
                        && ListDiff.eq(a.getTipo(), b.getTipo())
                        && a.isLeida() == b.isLeida()
                        && a.getFecha() == b.getFecha());
        items.clear();
        items.addAll(nuevas);
        diff.dispatchUpdatesTo(this);
    }

    private String fechaLabel(long ms) {
        LocalDate hoy = LocalDate.now(ZoneId.systemDefault());
        LocalDate fecha = new java.util.Date(ms).toInstant()
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
                .inflate(R.layout.item_notification, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Notificacion n = items.get(position);

        holder.txtTitulo.setText(n.getTitulo() != null ? n.getTitulo() : "");
        holder.txtCuerpo.setText(n.getCuerpo() != null ? n.getCuerpo() : "");
        holder.txtFecha.setText(fechaLabel(n.getFecha()));

        holder.imgIcon.setImageResource(iconoPara(n.getTipo()));
        holder.imgIcon.setColorFilter(holder.itemView.getContext()
                .getColor(R.color.green_primary_dark), PorterDuff.Mode.SRC_IN);

        boolean noLeida = !n.isLeida();
        holder.dot.setVisibility(noLeida ? View.VISIBLE : View.INVISIBLE);
        holder.card.setCardBackgroundColor(
                noLeida
                        ? holder.itemView.getContext().getColor(R.color.green_light)
                        : holder.itemView.getContext().getColor(R.color.white_card));

        holder.itemView.setOnClickListener(v -> listener.onClick(n));
    }

    private int iconoPara(String tipo) {
        if (tipo == null) return R.drawable.ic_notifications;
        switch (tipo) {
            case "CITA":     return R.drawable.ic_calendar;
            case "CURSO":    return R.drawable.ic_book;
            case "REPORTE":  return R.drawable.ic_info;
            case "PLAN":     return R.drawable.ic_badge;
            case "ALERTA":   return R.drawable.ic_warning;
            case "TUTORIA":  return R.drawable.ic_assignment;
            case "MENSAJE":  return R.drawable.ic_chat;
            default:         return R.drawable.ic_notifications;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        MaterialCardView card;
        ImageView imgIcon;
        View dot;
        TextView txtTitulo, txtCuerpo, txtFecha;

        VH(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            imgIcon = itemView.findViewById(R.id.img_notif_icon);
            dot = itemView.findViewById(R.id.dot_notif);
            txtTitulo = itemView.findViewById(R.id.txt_notif_titulo);
            txtCuerpo = itemView.findViewById(R.id.txt_notif_cuerpo);
            txtFecha = itemView.findViewById(R.id.txt_notif_fecha);
        }
    }
}