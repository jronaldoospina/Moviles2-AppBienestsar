package com.example.myapplication.adapters;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.Curso;
import com.example.myapplication.models.Nota;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MassGradeAdapter extends RecyclerView.Adapter<MassGradeAdapter.ViewHolder> {

    private final List<String> estudianteIds;
    private final List<String> estudianteNombres;
    private final List<String> estudianteCodigos;
    private final Curso curso;
    private final OnNotaChangedListener listener;
    private final Map<String, Double> notasTemp = new HashMap<>();

    public interface OnNotaChangedListener {
        void onAllNotesReady(Map<String, Double> notas);
        void onNoteInvalid(String codigo, String error);
        default void onNoteValid(String codigo, double valor) {}
    }

    public MassGradeAdapter(List<String> estudianteIds, List<String> estudianteNombres,
                          List<String> estudianteCodigos, Curso curso, OnNotaChangedListener listener) {
        this.estudianteIds = estudianteIds;
        this.estudianteNombres = estudianteNombres;
        this.estudianteCodigos = estudianteCodigos;
        this.curso = curso;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_estudiante_nota, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String estId = estudianteIds.get(position);
        String nombre = estudianteNombres.get(position);
        String codigo = estudianteCodigos.get(position);

        holder.txtNombre.setText(nombre);
        
        // Remover listener y watcher anteriores para evitar reciclaje incorrecto
        holder.editNota.setOnFocusChangeListener(null);
        if (holder.textWatcher != null) {
            holder.editNota.removeTextChangedListener(holder.textWatcher);
        }

        // Cargar nota existente si existe
        Double notaExistente = notasTemp.get(estId);

        holder.editNota.setTag(estId);
        if (notaExistente != null) {
            holder.editNota.setText(String.format(Locale.US, "%.1f", notaExistente));
        } else {
            holder.editNota.setText("");
        }
        
        holder.editNota.setEnabled(true);
        
        holder.textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String texto = s.toString().trim().replace(",", ".");
                if (!texto.isEmpty()) {
                    try {
                        double valor = Double.parseDouble(texto);
                        if (valor >= 0.0 && valor <= 5.0) {
                            notasTemp.put(estId, valor);
                            listener.onNoteValid(codigo, valor);
                        }
                    } catch (NumberFormatException e) {
                        // Ignorar
                    }
                } else {
                    notasTemp.remove(estId);
                }
            }
        };
        holder.editNota.addTextChangedListener(holder.textWatcher);

        holder.editNota.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String texto = holder.editNota.getText().toString().trim().replace(",", ".");
                if (!texto.isEmpty()) {
                    try {
                        double valor = Double.parseDouble(texto);
                        if (valor < 0.0 || valor > 5.0) {
                            Toast.makeText(v.getContext(), 
                                "Nota debe estar entre 0.0 y 5.0 para " + codigo, 
                                Toast.LENGTH_SHORT).show();
                            holder.editNota.setText("");
                            notasTemp.remove(estId);
                        }
                    } catch (NumberFormatException e) {
                        holder.editNota.setText("");
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return estudianteIds.size();
    }

    public Map<String, Double> getNotasTemp() {
        return notasTemp;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView txtNombre;
        public TextInputEditText editNota;
        public TextWatcher textWatcher;

        public ViewHolder(View view) {
            super(view);
            txtNombre = view.findViewById(R.id.txt_estudiante_nombre_fila);
            editNota = view.findViewById(R.id.edit_nota_estudiante);
        }
    }
}