package com.example.myapplication.data;

import java.util.Arrays;
import java.util.List;

/**
 * Lugares fijos UPC Aguachica para tutorías y citas.
 * Evita texto libre ("aula 302") y conecta con la Ruta Bus.
 */
public class LugaresUpcData {
    public static List<String> getLugares() {
        return Arrays.asList(
                "Oficina Bienestar",
                "Consultorio Psicología 1",
                "Consultorio Psicología 2",
                "Biblioteca",
                "Bloque A",
                "Bloque B",
                "Sede Principal - Cra 40",
                "Virtual"
        );
    }
}
