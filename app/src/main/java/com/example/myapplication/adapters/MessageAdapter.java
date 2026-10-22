package com.example.myapplication.adapters;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.Message;
import com.example.myapplication.utils.ListDiff;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private final List<Message> messages = new ArrayList<>();
    private final String userCurrentId; // ID del usuario actual (para saber si es emisor o receptor)

    public MessageAdapter(List<Message> messages, String userCurrentId) {
        if (messages != null) this.messages.addAll(messages);
        this.userCurrentId = userCurrentId;
    }

    public void updateData(List<Message> newMessages) {
        List<Message> nuevos = newMessages != null ? new ArrayList<>(newMessages) : new ArrayList<>();
        DiffUtil.DiffResult diff = ListDiff.calculate(messages, nuevos,
                (a, b) -> ListDiff.eq(a.getId(), b.getId()),
                (a, b) -> ListDiff.eq(a.getMensaje(), b.getMensaje())
                        && ListDiff.eq(a.getRemitenteId(), b.getRemitenteId())
                        && ListDiff.eq(a.getNombreRemitente(), b.getNombreRemitente())
                        && a.isLeido() == b.isLeido()
                        && a.getFecha() == b.getFecha());
        messages.clear();
        messages.addAll(nuevos);
        diff.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message message = messages.get(position);

        // Formatear fecha
        String fechaFormateada = formatearFecha(message.getFecha());

        // Determinar si soy el emisor o receptor
        boolean soyEmisor = message.getRemitenteId() != null && message.getRemitenteId().equals(userCurrentId);

        // Configurar vistas según quién envió
        if (soyEmisor) {
            // Mi mensaje a la derecha
            ((LinearLayout) holder.layoutMensaje).setGravity(Gravity.END);
            holder.txtMensaje.setBackgroundResource(R.drawable.badge_circle_green_light);
            holder.txtNombre.setText(R.string.msg_yo);
        } else {
            // Mensaje de otro persona a la izquierda
            ((LinearLayout) holder.layoutMensaje).setGravity(Gravity.START);
            holder.txtNombre.setText(message.getNombreRemitente() != null
                    ? message.getNombreRemitente() : holder.itemView.getContext().getString(R.string.msg_desconocido));
            holder.txtMensaje.setBackgroundResource(android.R.color.darker_gray);
        }

        holder.txtMensaje.setText(message.getMensaje());
        holder.txtFecha.setText(fechaFormateada);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView txtMensaje;
        public TextView txtFecha;
        public TextView txtNombre;
        public View layoutMensaje;

        public ViewHolder(View view) {
            super(view);
            txtMensaje = view.findViewById(R.id.txtMensaje);
            txtFecha = view.findViewById(R.id.txtFecha);
            txtNombre = view.findViewById(R.id.txtNombre);
            layoutMensaje = view.findViewById(R.id.layoutMensaje);
        }
    }

    /** Formatea la fecha a un formato legible */
    private String formatearFecha(long timestamp) {
        Date date = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", new Locale("es", "ES"));
        return sdf.format(date);
    }
}
