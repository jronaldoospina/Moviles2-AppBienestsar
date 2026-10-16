package com.example.myapplication.room;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "citas")
public class CitaEntity {

    @PrimaryKey
    @NonNull
    public String id = "";

    public String estudianteId;
    public String estudianteNombre;
    public String psicologoId;
    public String psicologoNombre;
    public String fecha;
    public String hora;
    public int duracionMinutos;
    public String motivo;
    public String estado;
    public String notasPsicologo;
    public long fechaCreacion;
    public long ultimaActualizacion;
    public String lugar;

    public CitaEntity() {}
}