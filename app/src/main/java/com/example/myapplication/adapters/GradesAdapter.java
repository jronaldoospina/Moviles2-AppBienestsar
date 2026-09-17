package com.example.myapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.Curso;
import com.example.myapplication.models.Nota;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GradesAdapter extends RecyclerView.Adapter<GradesAdapter.ViewHolder> {

    private final List<String> estudianteIds;
    private final List<String> estudianteNombres;
    private final List<String> estudianteCodigos;
    private final Curso cursoActual;
    private final Map<String, Nota> notasExistentes;
    private final Map<String, Double> notasTemp = new HashMap<>();
    private final OnNotaCambiante listener;

    public interface OnNotaCambiante {
        void onNotaCambiada(String estudianteId, double nuevoValor);
    }

    public GradesAdapter(List<String> estudianteIds, Map<String, Nota> notasExistentes) {
        this(estudianteIds, new ArrayList<>(), new ArrayList<>(), null, notasExistentes, null);
    }

    public GradesAdapter(List<String> estudianteIds, List<String> estudianteNombres,
                         List<String> estudianteCodigos, Curso cursoActual,
                         Map<String, Nota> notasExistentes) {
        this(estudianteIds, estudianteNombres, estudianteCodigos, cursoActual, notasExistentes, null);
    }

    public GradesAdapter(List<String> estudianteIds, List<String> estudianteNombres,
                         List<String> estudianteCodigos, Curso cursoActual,
                         Map<String, Nota> notasExistentes, OnNotaCambiante listener) {
        this.estudianteIds = estudianteIds != null ? estudianteIds : new ArrayList<>();
        this.estudianteNombres = estudianteNombres != null ? estudianteNombres : new ArrayList<>();
        this.estudianteCodigos = estudianteCodigos != null ? estudianteCodigos : new ArrayList<>();
        this.cursoActual = cursoActual;
        this.notasExistentes = notasExistentes != null ? notasExistentes : new HashMap<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_grade_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String estId = estudianteIds.get(position);
        String nombre = estudianteNombres.get(position);
        String codigo = estudianteCodigos.get(position);

        holder.txtNombre.setText(nombre);
        holder.txtCodigo.setText("Código: " + (codigo != null ? codigo : "-"));

        // Cargar nota existente si existe
        Nota notaExistente = null;
        if (notasExistentes != null && !notasExistentes.isEmpty()) {
            notaExistente = notasExistentes.get(estId);
        }

        if (notaExistente != null && notaExistente.getValor() > 0) {
            holder.txtValor.setText(String.format("%.1f", notaExistente.getValor()));
            // Cargar nota temporal si fue modificada
            if (notasTemp.containsKey(estId)) {
                holder.txtValor.setText(String.format("%.1f", notasTemp.get(estId)));
            }
        } else {
            holder.txtValor.setText("--");
        }

        // Configurar campo de edición
        holder.etNota.setTag(estId);
        holder.etNota.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && !notasTemp.containsKey(estId)) {
                // Obtener valor actual de la vista
                String texto = holder.etNota.getText().toString().trim();
                if (!texto.isEmpty()) {
                    try {
                        double valor = Double.parseDouble(texto);
                        notasTemp.put(estId, valor);
                        if (listener != null) {
                            listener.onNotaCambiada(estId, valor);
                        }
                    } catch (NumberFormatException e) {
                        // Ignorar conversión inválida
                    }
                }
            }
        });

        // Si el campo ya tiene texto (valor temporal), configurar listener de cambio de texto
        if (notasTemp.containsKey(estId)) {
            holder.etNota.setText(String.format("%.1f", notasTemp.get(estId)));
        }

        holder.txtValor.setOnClickListener(v -> {
            // Focar el editText para editar
            holder.etNota.setText(holder.txtValor.getText());
            holder.etNota.setFocusable(true);
            holder.etNota.setFocusableInTouchMode(true);
            holder.etNota.requestFocus();
        });
    }

    @Override
    public int getItemCount() {
        return estudianteIds.size();
    }

    public Map<String, Double> getNotas() {
        // Retornar el mapa de notas temporales (solo las que fueron modificadas)
        return new HashMap<>(notasTemp);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView txtNombre;
        public TextView txtCodigo;
        public TextView txtValor;
        public EditText etNota;

        public ViewHolder(View view) {
            super(view);
            txtNombre = view.findViewById(R.id.txt_nombre);
            txtCodigo = view.findViewById(R.id.txt_codigo);
            txtValor = view.findViewById(R.id.txt_valor);
            etNota = view.findViewById(R.id.et_nota);
        }
    }
}