package com.example.myapplication.room;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "reportes_emocionales")
public class ReporteEmocionalEntity {

    @PrimaryKey
    @NonNull
    public String id = "";

    public String estudianteId;
    public int animo;
    public String descripcion;
    public String fechaDia;
    public long fecha;
    public long fechaRegistro;

    public ReporteEmocionalEntity() {}
}