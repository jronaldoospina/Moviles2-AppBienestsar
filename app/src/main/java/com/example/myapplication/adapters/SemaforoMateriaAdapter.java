package com.example.myapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.SemaforoPdf;
import com.example.myapplication.utils.SemaforoUtils;

import java.util.List;
import java.util.Locale;

public class SemaforoMateriaAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_PERIODO = 0;
    public static final int TYPE_MATERIA = 1;

    private final List<Object> items;

    public SemaforoMateriaAdapter(List<Object> items) {
        this.items = items;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof Integer ? TYPE_PERIODO : TYPE_MATERIA;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_PERIODO) {
            return new PeriodoHolder(inf.inflate(R.layout.item_semaforo_periodo, parent, false));
        }
        return new MateriaHolder(inf.inflate(R.layout.item_semaforo_materia, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof PeriodoHolder) {
            ((PeriodoHolder) holder).titulo.setText("PERÍODO " + items.get(position));
        } else {
            MateriaHolder mh = (MateriaHolder) holder;
            SemaforoPdf.Materia m = (SemaforoPdf.Materia) items.get(position);

            mh.codigo.setText(m.codigo);

            String nombre = m.nombre != null ? m.nombre : "";
            if (m.repetida()) nombre = nombre + "  [REPETIDA]";
            mh.nombre.setText(nombre);

            if (m.definitiva != null) {
                mh.nota.setText(String.format(Locale.US, "%.1f", m.definitiva));
                mh.nota.setTextColor(SemaforoUtils.colorFor(
                        holder.itemView.getContext(), m.definitiva));
            } else {
                String nota = (m.notaTexto != null && !m.notaTexto.isEmpty())
                        ? m.notaTexto : "S/N";
                mh.nota.setText(nota);
                mh.nota.setTextColor(holder.itemView.getContext()
                        .getColor(R.color.text_secondary));
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class PeriodoHolder extends RecyclerView.ViewHolder {
        final TextView titulo;
        PeriodoHolder(View v) {
            super(v);
            titulo = v.findViewById(R.id.txt_periodo);
        }
    }

    static class MateriaHolder extends RecyclerView.ViewHolder {
        final TextView codigo, nombre, nota;
        MateriaHolder(View v) {
            super(v);
            codigo = v.findViewById(R.id.txt_sf_codigo);
            nombre = v.findViewById(R.id.txt_sf_nombre);
            nota = v.findViewById(R.id.txt_sf_nota);
        }
    }
}