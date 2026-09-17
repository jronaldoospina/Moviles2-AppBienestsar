package com.example.myapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.Nota;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CourseStudentAdapter extends RecyclerView.Adapter<CourseStudentAdapter.ViewHolder> {

    public interface OnReportarListener {
        void onReportar(StudentRow fila);
    }

    public static class StudentRow {
        public String estudianteId;
        public String nombre;
        public String codigo;
        public Nota nota; // puede ser null
        public List<Nota> notas; // lista de notas del estudiante

        public StudentRow(String estudianteId, String nombre, String codigo, Nota nota) {
            this.estudianteId = estudianteId;
            this.nombre = nombre;
            this.codigo = codigo;
            this.nota = nota;
        }
    }

    private final List<StudentRow> filas = new ArrayList<>();
    private OnReportarListener reportarListener;

    public void setOnReportarListener(OnReportarListener listener) {
        this.reportarListener = listener;
    }

    public List<StudentRow> getFilas() {
        return filas;
    }

    public void setData(List<StudentRow> nuevas, Map<String, Nota> notasIgnorado) {
        filas.clear();
        if (nuevas != null) filas.addAll(nuevas);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_teacher_student_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentRow fila = filas.get(position);
        holder.bind(fila, reportarListener);
    }

    @Override
    public int getItemCount() {
        return filas.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtIniciales, txtNombre, txtCodigo, txtNota, txtPromedio;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtIniciales = itemView.findViewById(R.id.txt_iniciales);
            txtNombre = itemView.findViewById(R.id.txt_nombre);
            txtCodigo = itemView.findViewById(R.id.txt_codigo);
            txtNota = itemView.findViewById(R.id.txt_nota);
            txtPromedio = itemView.findViewById(R.id.txt_promedio);
        }

        void bind(StudentRow fila, @Nullable OnReportarListener listener) {
            if (txtNombre != null) txtNombre.setText(fila.nombre);
            if (txtCodigo != null) txtCodigo.setText(fila.codigo != null ? fila.codigo : "-");
            if (txtIniciales != null) txtIniciales.setText(getIniciales(fila.nombre));

            // Calcular y mostrar promedio si la vista existe
            if (txtPromedio != null) {
                double promedio = calcularPromedio(fila.notas);
                txtPromedio.setText(String.format("Prom: %.1f", promedio));
            }

            if (txtNota != null) {
                if (fila.nota != null) {
                    txtNota.setText(String.format("%.1f", fila.nota.getValor()));
                    int color;
                    String nivel = fila.nota.getNivelRiesgo();
                    switch (nivel != null ? nivel : "") {
                        case "ALTO":  color = 0xFFD32F2F; break;
                        case "MEDIO": color = 0xFFF57F17; break;
                        default:      color = 0xFF388E3C; break;
                    }
                    txtNota.setTextColor(color);
                } else {
                    txtNota.setText("—");
                    txtNota.setTextColor(0xFF757575);
                }
            }

            View btnReportar = itemView.findViewById(R.id.btn_reportar);
            if (btnReportar != null) {
                btnReportar.setOnClickListener(
                        v -> { if (listener != null) listener.onReportar(fila); });
            }
        }

        private static String getIniciales(String nombre) {
            if (nombre == null || nombre.isEmpty()) return "?";
            String[] partes = nombre.trim().split("\\s+");
            if (partes.length >= 2) {
                return ("" + partes[0].charAt(0) + partes[1].charAt(0)).toUpperCase();
            }
            return ("" + partes[0].charAt(0)).toUpperCase();
        }

        /** Calcula el promedio a partir de una lista de notas */
        private static double calcularPromedio(List<Nota> notas) {
            if (notas == null || notas.isEmpty()) return 0.0;
            double suma = 0;
            int count = 0;
            for (Nota n : notas) {
                if (n != null && n.getValor() > 0) {
                    suma += n.getValor();
                    count++;
                }
            }
            return count > 0 ? suma / count : 0.0;
        }
    }
}