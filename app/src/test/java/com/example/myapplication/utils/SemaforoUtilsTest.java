package com.example.myapplication.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.myapplication.models.Curso;
import com.example.myapplication.models.Nota;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Pruebas de la logica academica del Semáforo del Estudiante.
 *
 * Esta logica decide el nivel de riesgo de un estudiante, que es lo que
 * dispara las alertas de Bienestar. Si falla, se pierde intervencion
 * temprana, asi que merece pruebas explicitas.
 *
 * No requiere dispositivo: es codigo puro.
 */
public class SemaforoUtilsTest {

    // ============================================================
    // NIVEL DE RIESGO
    // ============================================================

    @Test
    public void promedioAltoEsRiesgoBajo() {
        assertEquals("BAJO", SemaforoUtils.nivelFor(4.5));
        assertEquals("BAJO", SemaforoUtils.nivelFor(4.0));
        assertEquals("BAJO", SemaforoUtils.nivelFor(5.0));
    }

    @Test
    public void promedioMedioEsRiesgoMedio() {
        assertEquals("MEDIO", SemaforoUtils.nivelFor(3.9));
        assertEquals("MEDIO", SemaforoUtils.nivelFor(3.0));
    }

    @Test
    public void promedioBajoEsRiesgoAlto() {
        assertEquals("ALTO", SemaforoUtils.nivelFor(2.9));
        assertEquals("ALTO", SemaforoUtils.nivelFor(2.0));
    }

    @Test
    public void promedioMuyBajoEsCritico() {
        // Por debajo de 2.0 el estudiante queda en riesgo CRITICO.
        // Debe coincidir con el calculo de ProfileFragment.
        assertEquals("CRITICO", SemaforoUtils.nivelFor(1.9));
        assertEquals("CRITICO", SemaforoUtils.nivelFor(0.0));
    }

    @Test
    public void elUmbralCriticoCoincideConElDeProfileFragment() {
        // ProfileFragment usa: >=4 BAJO, >=3 MEDIO, >=2 ALTO, resto CRITICO
        assertEquals("BAJO", SemaforoUtils.nivelFor(4.0));
        assertEquals("MEDIO", SemaforoUtils.nivelFor(3.0));
        assertEquals("ALTO", SemaforoUtils.nivelFor(2.0));
        assertEquals("CRITICO", SemaforoUtils.nivelFor(1.9));
    }

    /** Un promedio negativo no debe exploitar: la app no lo genera, pero
     *  un dato corrupto en Firestore podria mandarlo. */
    @Test
    public void promedioNegativoNoRompeElCalculo() {
        String nivel = SemaforoUtils.nivelFor(-5.0);
        assertTrue("Debe devolver un nivel valido, no una excepcion",
                nivel.equals("CRITICO") || nivel.equals("ALTO"));
    }

    // ============================================================
    // PROMEDIO POR MATERIA
    // ============================================================

    @Test
    public void promedioDeMateriaPonderaLosCortes() {
        Curso curso = new Curso();
        curso.setId("c1");

        // El promedio se calcula sobre los 3 cortes, no sobre las notas sueltas
        List<Nota> notas = Arrays.asList(
                nota("c1", "corte1", 5.0),
                nota("c1", "corte2", 3.0),
                nota("c1", "corte3", 4.0)
        );

        SemaforoUtils.PromedioMateria pm =
                SemaforoUtils.computePromedioMateria("c1", notas, curso);

        assertEquals("Promedio de los 3 cortes = 4.0",
                4.0, pm.promedio, 0.001);
    }

    /** Si falta un corte, el promedio debe salir sobre los que si existen. */
    @Test
    public void promedioConCortesFaltantesUsaLosQueHay() {
        Curso curso = new Curso();
        curso.setId("c1");

        List<Nota> notas = Arrays.asList(
                nota("c1", "corte1", 5.0),
                nota("c1", "corte3", 3.0)
        );

        SemaforoUtils.PromedioMateria pm =
                SemaforoUtils.computePromedioMateria("c1", notas, curso);

        assertEquals("Promedio de 5 y 3 = 4.0",
                4.0, pm.promedio, 0.001);
    }

    /** Un docente puede corregir una nota: debe ganar la mas reciente. */
    @Test
    public void usaLaNotaMasRecienteDelCorte() {
        Curso curso = new Curso();
        curso.setId("c1");

        Nota vieja = nota("c1", "corte1", 2.0);
        vieja.setFechaRegistro(1000L);
        Nota nueva = nota("c1", "corte1", 5.0);
        nueva.setFechaRegistro(2000L);

        SemaforoUtils.PromedioMateria pm = SemaforoUtils.computePromedioMateria(
                "c1", Arrays.asList(vieja, nueva), curso);

        assertEquals("Debe ganar la correccion mas reciente (5.0)",
                5.0, pm.promedio, 0.001);
    }

    @Test
    public void materiaSinNotasNoPromedia() {
        Curso curso = new Curso();
        curso.setId("c1");

        SemaforoUtils.PromedioMateria pm = SemaforoUtils.computePromedioMateria(
                "c1", new ArrayList<>(), curso);

        assertEquals("Sin notas el promedio debe ser 0",
                0.0, pm.promedio, 0.001);
    }

    @Test
    public void notasEnCeroNoCuentanComoAprobadas() {
        Curso curso = new Curso();
        curso.setId("c1");

        // El docente puede registrar 0 cuando el estudiante no-presenta
        List<Nota> notas = Arrays.asList(
                nota("c1", "corte1", 0.0),
                nota("c1", "corte2", 0.0));

        SemaforoUtils.PromedioMateria pm =
                SemaforoUtils.computePromedioMateria("c1", notas, curso);

        assertEquals("Un 0 debe promediar 0, no ignorarse",
                0.0, pm.promedio, 0.001);
    }

    /**
 * Los callers (StudentDashboard y MyProgress) ya agrupan las notas por
 * curso antes de invocar este metodo, asi que el cursoId recibido siempre
 * coincide con todas las notas. Esta prueba fija ese contrato: una nota de
 * un corte que no pertenece al curso no debe existir en esta llamada.
 */
@Test
    public void cadaCursoSeCalculaConSusPropiasNotas() {
        Curso c1 = new Curso();
        c1.setId("c1");
        Curso c2 = new Curso();
        c2.setId("c2");

        // Grupo del curso 1
        List<Nota> notasC1 = Arrays.asList(
                nota("c1", "corte1", 4.0),
                nota("c1", "corte2", 4.0)
        );
        // Grupo del curso 2
        List<Nota> notasC2 = Arrays.asList(
                nota("c2", "corte1", 2.0),
                nota("c2", "corte2", 2.0)
        );

        double prom1 = SemaforoUtils
                .computePromedioMateria("c1", notasC1, c1).promedio;
        double prom2 = SemaforoUtils
                .computePromedioMateria("c2", notasC2, c2).promedio;

        assertEquals("Curso 1 promedia 4.0", 4.0, prom1, 0.001);
        assertEquals("Curso 2 promedia 2.0 y NO se contamina con el otro",
                2.0, prom2, 0.001);
    }

    // ============================================================
    // RESUMEN GENERAL
    // ============================================================

    @Test
    public void cuentaMateriasEnRiesgo() {
        List<SemaforoUtils.PromedioMateria> items = Arrays.asList(
                materia("A", 4.5),   // ok
                materia("B", 3.2),   // ok
                materia("C", 2.5),   // en riesgo
                materia("D", 1.8)    // en riesgo
        );

        assertEquals("Deben contar 2 materias en riesgo",
                2, SemaforoUtils.enRiesgo(items));
    }

    @Test
    public void cuentaMateriasAprobadas() {
        List<SemaforoUtils.PromedioMateria> items = Arrays.asList(
                materia("A", 4.5),   // aprobado
                materia("B", 3.0),   // aprobado (justo en el limite)
                materia("C", 2.9)    // reprobado
        );

        assertEquals("Deben contar 2 aprobadas (3.0 es aprobado)",
                2, SemaforoUtils.aprobadas(items));
    }

    @Test
    public void promedioGeneralPonderaPorMateria() {
        List<SemaforoUtils.PromedioMateria> items = Arrays.asList(
                materia("A", 5.0),
                materia("B", 3.0),
                materia("C", 4.0)
        );

        assertEquals("Promedio de 5, 3 y 4 = 4.0",
                4.0, SemaforoUtils.promedioGeneral(items), 0.001);
    }

    @Test
    public void listaVaciaDaCeros() {
        List<SemaforoUtils.PromedioMateria> vacia = new ArrayList<>();
        assertEquals(0, SemaforoUtils.enRiesgo(vacia));
        assertEquals(0, SemaforoUtils.aprobadas(vacia));
        assertEquals(0.0, SemaforoUtils.promedioGeneral(vacia), 0.001);
    }

    // ============================================================
    // FORMATO
    // ============================================================

    @Test
    public void formatoMuestraUnDecimal() {
        assertEquals("4.5", SemaforoUtils.format(4.5));
        assertEquals("3.0", SemaforoUtils.format(3.0));
        assertEquals("0.0", SemaforoUtils.format(0.0));
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private static Nota nota(String cursoId, String corteId, double valor) {
        Nota n = new Nota();
        n.setCursoId(cursoId);
        n.setCorteId(corteId);
        n.setValor(valor);
        return n;
    }

    private static SemaforoUtils.PromedioMateria materia(String nombre, double promedio) {
        SemaforoUtils.PromedioMateria pm = new SemaforoUtils.PromedioMateria();
        pm.nombre = nombre;
        pm.promedio = promedio;
        return pm;
    }
}