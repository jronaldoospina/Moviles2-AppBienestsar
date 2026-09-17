package com.example.myapplication.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado del parseo del reporte "Semáforo del Estudiante" (Academusoft 4.0).
 */
public class SemaforoPdf {

    public static class Materia {
        public int periodo;
        public String codigo;
        public String nombre;
        public String forma;        // null | NORMAL | REPETIDA
        public Integer creditos;
        public Double definitiva;   // null si aún no tiene nota ('-', 'A', 'P', ...)
        public String notaTexto;    // texto original de la nota cuando no es numérica
        public Double aprobacion;   // solo REPETIDA (nota de la primera vez)

        public boolean repetida() { return "REPETIDA".equals(forma); }
        public boolean sinNota() { return definitiva == null; }
    }

    public String documento;
    public String nombres;
    public String programa;
    public String pensum;
    public int semestre;
    public double promedioAcumulado;
    public final List<Materia> materias = new ArrayList<>();

    public int contarMaterias() { return materias.size(); }

    public int contarRepetidas() {
        int c = 0;
        for (Materia m : materias) {
            if (m.repetida()) c++;
        }
        return c;
    }

    public int contarSinNota() {
        int c = 0;
        for (Materia m : materias) {
            if (m.sinNota()) c++;
        }
        return c;
    }
}