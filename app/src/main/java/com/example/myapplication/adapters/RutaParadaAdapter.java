package com.example.myapplication.adapters;

import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.RutaParada;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RutaParadaAdapter extends RecyclerView.Adapter<RutaParadaAdapter.VH> {

    public interface OnParadaClick {
        void onParada(RutaParada parada);
    }

    private final List<RutaParada> data = new ArrayList<>();
    private Double myLat = null;
    private Double myLng = null;
    private OnParadaClick listener;

    public void setListener(OnParadaClick l) { this.listener = l; }

    public void setData(List<RutaParada> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    public void setMyLocation(Double lat, Double lng) {
        this.myLat = lat;
        this.myLng = lng;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_parada, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        RutaParada p = data.get(position);
        h.txtOrden.setText(String.valueOf(p.getOrden()));
        h.txtNombre.setText(p.getNombre());
        h.txtRef.setText(p.getReferencia() != null ? p.getReferencia() : "");

        if (myLat != null && myLng != null) {
            float[] out = new float[1];
            Location.distanceBetween(myLat, myLng, p.getLat(), p.getLng(), out);
            float m = out[0];
            if (m < 1000) {
                h.txtDist.setText(String.format(Locale.US, "%.0f m", m));
            } else {
                h.txtDist.setText(String.format(Locale.US, "%.1f km", m / 1000f));
            }
        } else {
            h.txtDist.setText("");
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onParada(p);
        });
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView txtOrden, txtNombre, txtRef, txtDist;
        VH(@NonNull View v) {
            super(v);
            txtOrden = v.findViewById(R.id.txt_orden);
            txtNombre = v.findViewById(R.id.txt_nombre);
            txtRef = v.findViewById(R.id.txt_ref);
            txtDist = v.findViewById(R.id.txt_dist);
        }
    }
}
