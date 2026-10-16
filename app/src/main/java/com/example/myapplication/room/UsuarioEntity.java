package com.example.myapplication.room;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "usuarios")
public class UsuarioEntity {

    @PrimaryKey
    @NonNull
    public String uid = "";

    public String email;
    public String usuario;
    public String nombre;
    public String rol;
    public String documento;
    public String telefono;
    public String fotoUrl;
    public long fechaRegistro;
    public long fechaCreacion;
    public long ultimaActualizacion;
    public boolean activo;
    public String nivelRiesgo;
    public double promedio;
    public String codigo;
    public String programa;
    public int semestre;
    public String lugar;

    public UsuarioEntity() {}
}