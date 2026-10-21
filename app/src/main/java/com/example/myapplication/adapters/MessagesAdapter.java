package com.example.myapplication.adapters;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.Mensaje;
import com.example.myapplication.utils.FechaUtils;
import com.example.myapplication.utils.ListDiff;

import java.util.ArrayList;
import java.util.List;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.VH> {

    private final List<Mensaje> items = new ArrayList<>();
    private final String miUid;

    public MessagesAdapter(String miUid) {
        this.miUid = miUid;
    }

    public void setMensajes(List<Mensaje> mensajes) {
        List<Mensaje> nuevos = mensajes != null ? new ArrayList<>(mensajes) : new ArrayList<>();
        DiffUtil.DiffResult diff = ListDiff.calculate(items, nuevos,
                (a, b) -> ListDiff.eq(a.getId(), b.getId()),
                (a, b) -> ListDiff.eq(a.getTexto(), b.getTexto())
                        && ListDiff.eq(a.getRemitenteId(), b.getRemitenteId())
                        && a.isLeido() == b.isLeido()
                        && a.getFecha() == b.getFecha());
        items.clear();
        items.addAll(nuevos);
        diff.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Mensaje m = items.get(position);
        boolean mio = miUid != null && miUid.equals(m.getRemitenteId());

        holder.root.setGravity(mio ? Gravity.END : Gravity.START);

        holder.txtBubble.setText(m.getTexto() != null ? m.getTexto() : "");
        holder.txtHora.setText(FechaUtils.horaCorta(m.getFecha()));

        if (mio) {
            holder.txtBubble.setBackgroundResource(R.drawable.badge_circle_green_light);
            holder.txtBubble.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary));
            holder.txtHora.setGravity(Gravity.END);
        } else {
            holder.txtBubble.setBackgroundResource(R.color.background_gray);
            holder.txtBubble.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary));
            holder.txtHora.setGravity(Gravity.START);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        LinearLayout root;
        TextView txtBubble, txtHora;

        VH(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.layoutMensaje);
            txtBubble = itemView.findViewById(R.id.txtMensaje);
            txtHora = itemView.findViewById(R.id.txtFecha);
        }
    }
}