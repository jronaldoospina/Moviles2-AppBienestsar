package com.example.myapplication.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.Usuario;
import com.example.myapplication.services.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(Usuario usuario);
    }

    public interface OnOptionsClickListener {
        void onOptionsClick(Usuario usuario, View anchor);
    }

    private final List<Usuario> usuarios = new ArrayList<>();
    private final List<Usuario> usuariosFiltrados = new ArrayList<>();
    private final Context context;
    private final OnUserClickListener userClickListener;
    private final OnOptionsClickListener optionsClickListener;

    public UsersAdapter(Context context,
                        OnUserClickListener userClickListener,
                        OnOptionsClickListener optionsClickListener) {
        this.context = context;
        this.userClickListener = userClickListener;
        this.optionsClickListener = optionsClickListener;
    }

    public void setUsuarios(List<Usuario> lista) {
        usuarios.clear();
        usuarios.addAll(lista);
        aplicarFiltro(null, null);
    }

    /**
     * Filtra por rol y/o texto.
     * @param rol    null o "" para todos
     * @param query  texto de búsqueda (null o "" para no filtrar)
     */
    public void aplicarFiltro(String rol, String query) {
        usuariosFiltrados.clear();
        String q = (query != null) ? query.toLowerCase().trim() : "";

        for (Usuario u : usuarios) {
            boolean coincideRol = (rol == null || rol.isEmpty() || rol.equals(u.getRol()));
            boolean coincideTexto = q.isEmpty()
                    || (u.getNombre() != null && u.getNombre().toLowerCase().contains(q))
                    || (u.getEmail() != null && u.getEmail().toLowerCase().contains(q))
                    || (u.getUsuario() != null && u.getUsuario().toLowerCase().contains(q))
                    || (u.getDocumento() != null && u.getDocumento().contains(q));

            if (coincideRol && coincideTexto) {
                usuariosFiltrados.add(u);
            }
        }
        notifyDataSetChanged();
    }

    public int getTotalFiltrado() {
        return usuariosFiltrados.size();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_card, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        Usuario u = usuariosFiltrados.get(position);

        holder.txtNombre.setText(u.getNombre() != null ? u.getNombre() : "Sin nombre");
        holder.txtEmail.setText(u.getEmail() != null ? u.getEmail() : "");
        holder.txtIniciales.setText(u.getIniciales());

        // Rol con color según tipo
        holder.txtRol.setText(u.getRol() != null ? u.getRol() : "SIN ROL");
        aplicarColorRol(holder.txtRol, u.getRol());

        // Estado activo/inactivo
        boolean activo = u.isActivo();
        holder.txtEstado.setText(activo ? "ACTIVO" : "INACTIVO");
        holder.txtEstado.setBackgroundResource(activo
                ? R.drawable.badge_success
                : R.drawable.badge_risk_high);
        holder.txtEstado.setTextColor(holder.itemView.getContext().getColor(activo
                ? R.color.success_text : R.color.risk_high_text));

        // Click en la tarjeta
        holder.itemView.setOnClickListener(v -> {
            if (userClickListener != null) userClickListener.onUserClick(u);
        });

        // Click en opciones
        holder.btnOptions.setOnClickListener(v -> {
            if (optionsClickListener != null) optionsClickListener.onOptionsClick(u, v);
        });
    }

    private void aplicarColorRol(TextView txt, String rol) {
        if (rol == null) return;
        int fg;
        switch (rol) {
            case SessionManager.ROLE_ADMIN:
                txt.setBackgroundResource(R.drawable.badge_risk_high); // rojo
                fg = R.color.risk_high_text;
                break;
            case SessionManager.ROLE_PSICOLOGO:
                txt.setBackgroundResource(R.drawable.badge_info); // azul
                fg = R.color.info_text;
                break;
            case SessionManager.ROLE_DOCENTE:
                txt.setBackgroundResource(R.drawable.badge_risk_medium); // amarillo
                fg = R.color.risk_medium_text;
                break;
            case SessionManager.ROLE_ESTUDIANTE:
                txt.setBackgroundResource(R.drawable.badge_success); // verde
                fg = R.color.success_text;
                break;
            default:
                fg = R.color.info_text;
                break;
        }
        txt.setTextColor(txt.getContext().getColor(fg));
    }

    @Override
    public int getItemCount() {
        return usuariosFiltrados.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView txtIniciales, txtNombre, txtEmail, txtRol, txtEstado;
        ImageView btnOptions;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            txtIniciales = itemView.findViewById(R.id.txt_iniciales);
            txtNombre = itemView.findViewById(R.id.txt_nombre);
            txtEmail = itemView.findViewById(R.id.txt_email);
            txtRol = itemView.findViewById(R.id.txt_rol);
            txtEstado = itemView.findViewById(R.id.txt_estado);
            btnOptions = itemView.findViewById(R.id.btn_options);
        }
    }
}