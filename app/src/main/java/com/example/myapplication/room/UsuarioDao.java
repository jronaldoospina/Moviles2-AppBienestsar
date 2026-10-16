package com.example.myapplication.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface UsuarioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UsuarioEntity usuario);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<UsuarioEntity> usuarios);

    @Query("SELECT * FROM usuarios WHERE uid = :uid LIMIT 1")
    UsuarioEntity getById(String uid);

    @Query("SELECT * FROM usuarios")
    List<UsuarioEntity> getAll();

    @Query("DELETE FROM usuarios")
    void deleteAll();
}