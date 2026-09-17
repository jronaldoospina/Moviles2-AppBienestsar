package com.example.myapplication.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.example.myapplication.models.SemaforoPdf;

import org.junit.Test;

public class SemaforoPdfParserTest {

    private static final String MUESTRA =
            "Fecha del Reporte 09-09-2026 20:04:50 Académico - Academusoft 4.0\n" +
            "Semáforo del Estudiante\n" +
            "Información\n" +
            "Identificación Tipo de Documento Nombres y Apellidos\n" +
            "1002353355 OSPINA CAPATAZ JEISSON RONALDO\n" +
            "Categoría Situación Jornada\n" +
            "ANTIGUO ACTIVO JORNADA UNICA\n" +
            "Programa Pensum Ubicación Semestral Promedio Acumulado\n" +
            "INGENIERIA DE SISTEMAS A-0402-IS-03 9 3.9\n" +
            "Período 1\n" +
            "Código Materia Forma Aprobación Créditos Def.\n" +
            "EX009SA-CC CATEDRA UPECISTA NORMAL 1 5,0\n" +
            "EX022SA LENGUA EXTRANJERA I (GRAMATICA) NORMAL 0 A\n" +
            "ISA001 CALCULO I REPETIDA 4 2,8 4,1\n" +
            "ISA002 LOGICA MATEMATICA NORMAL 2 3,4\n" +
            "ISA003 EXPRESION GRAFICA NORMAL 2 4,8\n" +
            "ISA004 FUNDAMENTOS DE PROGRAMACION NORMAL 3 3,6\n" +
            "ISA005 INTRODUCCION A LA INGENIERIA DE SISTEMAS NORMAL 2 3,8\n" +
            "ISA006 COMUNICACION ORAL Y ESCRITA I NORMAL 3 3,4\n" +
            "Período 2\n" +
            "Código Materia Forma Aprobación Créditos Def.\n" +
            "EX005SA-CC ACTIVIDAD CULTURAL Y DEPORTIVA I NORMAL 1 5,0\n" +
            "EX023SA LENGUA EXTRAJERA II (LECTURA) NORMAL 0 A\n" +
            "ISA007 CALCULO II REPETIDA 3 2,8 3,2\n" +
            "ISA009 ALGEBRA LINEAL NORMAL 2 4,0\n" +
            "ISA010 PROGRAMACION I NORMAL 3 3,7\n" +
            "ISA011 COMUNICACION ORAL Y ESCRITA II NORMAL 3 3,9\n" +
            "ISA012 DESARROLLO HUMANO NORMAL 2 4,0\n" +
            "IS0044SA FISICA I NORMAL 3 3,0\n" +
            "Período 3\n" +
            "Código Materia Forma Aprobación Créditos Def.\n" +
            "EX006SA-CC ACTIVIDAD CULTURAL Y DEPORTIVA II REPETIDA 1 2,9 5,0\n" +
            "EX024SA LENGUA EXTRANJERA III (ESCRITURA) NORMAL 0 A\n" +
            "ISA013 CALCULO III NORMAL 3 3,0\n" +
            "ISA015 PROGRAMACION II NORMAL 3 3,2\n" +
            "ISA017 DISEÑO GRAFICO NORMAL 2 4,8\n" +
            "ISA018 SEMILLERO DE INVESTIGACION NORMAL 1 3,4\n" +
            "ISA019 ELECTIVA COMPLEMENTARIA NORMAL 2 4,8\n" +
            "IS0014SA PENSAMIENTO SISTEMICO NORMAL 2 3,9\n" +
            "IS0045SA FISICA II NORMAL 3 3,0\n" +
            "Período 5\n" +
            "Código Materia Forma Aprobación Créditos Def.\n" +
            "EX021SA-CC COMERCIO INTERNACIONAL NORMAL 2 5,0\n" +
            "ISA025 ANALISIS NUMERICO - 3\n" +
            "ISA026 PROGRAMACION IV NORMAL 3 4,4\n" +
            "Cancelación de Semestre\n" +
            "No se ha encontrado información para mostrar.\n" +
            "Lista de Materias Canceladas\n" +
            "No se ha encontrado información para mostrar.\n" +
            "Lista de Unidades\n" +
            "No se ha encontrado información para mostrar.\n";

    @Test
    public void parseaInformacion() throws Exception {
        SemaforoPdf r = SemaforoPdfParser.parse(MUESTRA);

        assertEquals("1002353355", r.documento);
        assertEquals("OSPINA CAPATAZ JEISSON RONALDO", r.nombres);
        assertEquals("INGENIERIA DE SISTEMAS", r.programa);
        assertEquals("A-0402-IS-03", r.pensum);
        assertEquals(9, r.semestre);
        assertEquals(3.9, r.promedioAcumulado, 0.001);
    }

    @Test
    public void parseaMaterias() throws Exception {
        SemaforoPdf r = SemaforoPdfParser.parse(MUESTRA);

        // Total: 8 (P1) + 8 (P2) + 9 (P3) + 3 (P5)
        assertEquals(28, r.materias.size());
        assertEquals(3, r.contarRepetidas());
        assertEquals(4, r.contarSinNota()); // 3 Lenguas Extranjeras (A) + ISA025 (-)

        SemaforoPdf.Materia cal1 = buscar(r, "ISA001");
        assertNotNull(cal1);
        assertEquals(1, cal1.periodo);
        assertEquals("REPETIDA", cal1.forma);
        assertEquals("CALCULO I", cal1.nombre);
        assertEquals(Integer.valueOf(4), cal1.creditos);
        assertEquals(2.8, cal1.aprobacion, 0.001);
        assertEquals(4.1, cal1.definitiva, 0.001);

        SemaforoPdf.Materia normal = buscar(r, "ISA002");
        assertNotNull(normal);
        assertEquals("NORMAL", normal.forma);
        assertEquals(Integer.valueOf(2), normal.creditos);
        assertEquals(3.4, normal.definitiva, 0.001);

        SemaforoPdf.Materia sinNota = buscar(r, "ISA025");
        assertNotNull(sinNota);
        assertEquals("ANALISIS NUMERICO", sinNota.nombre);
        assertEquals(Integer.valueOf(3), sinNota.creditos);
        assertNull(sinNota.definitiva);
        assertEquals(5, sinNota.periodo);

        SemaforoPdf.Materia extranjera = buscar(r, "EX022SA");
        assertNotNull(extranjera);
        assertEquals("NORMAL", extranjera.forma);
        assertEquals(Integer.valueOf(0), extranjera.creditos);
        assertNull(extranjera.definitiva);
        assertEquals("A", extranjera.notaTexto);
    }

    private SemaforoPdf.Materia buscar(SemaforoPdf r, String codigo) {
        for (SemaforoPdf.Materia m : r.materias) {
            if (codigo.equals(m.codigo)) return m;
        }
        return null;
    }
}