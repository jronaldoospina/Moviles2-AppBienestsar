package com.example.myapplication.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CitaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CitaEntity cita);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CitaEntity> citas);

    @Query("SELECT * FROM citas WHERE id = :id LIMIT 1")
    CitaEntity getById(String id);

    @Query("SELECT * FROM citas")
    List<CitaEntity> getAll();

    @Query("DELETE FROM citas")
    void deleteAll();
}