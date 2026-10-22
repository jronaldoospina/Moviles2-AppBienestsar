package com.example.myapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.PacienteResumen;
import com.example.myapplication.utils.AnimoUtils;
import com.example.myapplication.utils.ListDiff;

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
        List<PacienteResumen> nuevas = lista != null ? new ArrayList<>(lista) : new ArrayList<>();
        DiffUtil.DiffResult diff = ListDiff.calculate(items, nuevas,
                (a, b) -> ListDiff.eq(a.getUid(), b.getUid()),
                (a, b) -> ListDiff.eq(a.getNombre(), b.getNombre())
                        && ListDiff.eq(a.getNivelRiesgo(), b.getNivelRiesgo())
                        && a.getAnimo() == b.getAnimo()
                        && a.getAlertasActivas() == b.getAlertasActivas());
        items.clear();
        items.addAll(nuevas);
        diff.dispatchUpdatesTo(this);
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

        android.content.Context ctx = holder.itemView.getContext();
        holder.txtNombre.setText(p.getNombre() != null ? p.getNombre()
                : ctx.getString(R.string.sin_nombre));
        holder.txtAnimo.setText(p.getAnimo() > 0 ? String.valueOf(p.getAnimo()) : "");
        holder.txtAnimo.setBackgroundResource(AnimoUtils.circle(p.getAnimo()));

        String alertas = ctx.getResources().getQuantityString(R.plurals.paciente_alertas,
                p.getAlertasActivas(), p.getAlertasActivas());
        String riesgo = (p.getNivelRiesgo() != null && !p.getNivelRiesgo().isEmpty())
                ? ctx.getString(R.string.paciente_riesgo, p.getNivelRiesgo())
                : ctx.getString(R.string.paciente_sin_riesgo);
        String animoLabel = p.getAnimo() > 0
                ? ctx.getString(R.string.paciente_animo, AnimoUtils.label(p.getAnimo()))
                : ctx.getString(R.string.paciente_sin_animo);
        holder.txtSub.setText(ctx.getString(R.string.paciente_sub, alertas, riesgo, animoLabel));

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