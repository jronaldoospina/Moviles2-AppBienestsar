package com.example.myapplication.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ReporteEmocionalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ReporteEmocionalEntity reporte);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ReporteEmocionalEntity> reportes);

    @Query("SELECT * FROM reportes_emocionales WHERE id = :id LIMIT 1")
    ReporteEmocionalEntity getById(String id);

    @Query("SELECT * FROM reportes_emocionales")
    List<ReporteEmocionalEntity> getAll();

    @Query("DELETE FROM reportes_emocionales")
    void deleteAll();
}