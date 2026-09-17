package com.example.myapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.Tutoria;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TutoriaAdapter extends RecyclerView.Adapter<TutoriaAdapter.ViewHolder> {

    public interface OnTutoriaClickListener {
        void onTutoriaClick(Tutoria tutoria);
    }

    private final List<Tutoria> tutorias = new ArrayList<>();
    private OnTutoriaClickListener clickListener;

    public void setOnTutoriaClickListener(@Nullable OnTutoriaClickListener listener) {
        this.clickListener = listener;
    }

    public void setData(List<Tutoria> nuevas) {
        tutorias.clear();
        if (nuevas != null) {
            tutorias.addAll(nuevas);
            Collections.sort(tutorias, (a, b) -> Long.compare(b.getFechaCreacion(), a.getFechaCreacion()));
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tutoria, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Tutoria t = tutorias.get(position);
        holder.txtMateria.setText(t.getMateria() != null ? t.getMateria() : "Tutoría");
        holder.txtCurso.setText(((t.getCodigoCurso() != null ? t.getCodigoCurso() + " · " : "")
                + (t.getCursoNombre() != null ? t.getCursoNombre() : "")));
        holder.txtHorario.setText(diaLabel(t.getDiaSemana()) + " · "
                + (t.getHoraInicio() != null ? t.getHoraInicio() : "--")
                + " - " + (t.getHoraFin() != null ? t.getHoraFin() : "--"));
        holder.txtAula.setText((t.getAula() != null && !t.getAula().isEmpty())
                ? "Aula: " + t.getAula() : "Aula por confirmar");

        boolean activa = t.isActiva();
        holder.txtEstado.setText(activa ? "ACTIVA" : "INACTIVA");
        holder.txtEstado.setBackgroundResource(activa
                ? R.drawable.badge_success : R.drawable.badge_info);
        holder.txtEstado.setTextColor(holder.itemView.getContext().getColor(activa
                ? R.color.success_text : R.color.info_text));

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onTutoriaClick(t);
        });
    }

    private String diaLabel(String dia) {
        if (dia == null) return "";
        switch (dia) {
            case "MARTES": return "Martes";
            case "MIERCOLES": return "Miércoles";
            case "JUEVES": return "Jueves";
            case "VIERNES": return "Viernes";
            case "SABADO": return "Sábado";
            case "DOMINGO": return "Domingo";
            default: return "Lunes";
        }
    }

    @Override
    public int getItemCount() {
        return tutorias.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtMateria, txtCurso, txtHorario, txtAula, txtEstado;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtMateria = itemView.findViewById(R.id.txt_tutoria_materia);
            txtCurso = itemView.findViewById(R.id.txt_tutoria_curso);
            txtHorario = itemView.findViewById(R.id.txt_tutoria_horario);
            txtAula = itemView.findViewById(R.id.txt_tutoria_aula);
            txtEstado = itemView.findViewById(R.id.txt_tutoria_estado);
        }
    }
}