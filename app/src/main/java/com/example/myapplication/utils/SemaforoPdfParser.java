package com.example.myapplication.utils;

import com.example.myapplication.models.SemaforoPdf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parsea el texto extraído del reporte "Semáforo del Estudiante" (Academusoft 4.0).
 *
 * Estructura esperada:
 *   Identificación Tipo de Documento Nombres y Apellidos
 *   1002353355 OSPINA CAPATAZ JEISSON RONALDO
 *   ...
 *   Programa Pensum Ubicación Semestral Promedio Acumulado
 *   INGENIERIA DE SISTEMAS A-0402-IS-03 9 3.9
 *   Período 1
 *   Código Materia Forma Aprobación Créditos Def.
 *   EX009SA-CC CATEDRA UPECISTA NORMAL 1 5,0
 *   ISA001 CALCULO I REPETIDA 4 2,8 4,1
 *   ISA025 ANALISIS NUMERICO - 3
 */
public class SemaforoPdfParser {

    private static final Pattern P_PERIODO =
            Pattern.compile("^[Pp]er[ií]odo\\s+(\\d{1,2})");
    private static final Pattern P_PENSUM =
            Pattern.compile("\\b[A-Z]+-\\d{4}-[A-Z]{2}-\\d{2}\\b");
    private static final Pattern P_DOCUMENTO =
            Pattern.compile("^(\\d{6,})\\s+(.+)$");
    private static final Pattern P_CODIGO =
            Pattern.compile("[A-Za-z0-9]+(-[A-Za-z0-9]+)*");

    public static SemaforoPdf parse(String texto) throws IllegalArgumentException {
        if (texto == null || texto.trim().isEmpty()) {
            throw new IllegalArgumentException("El PDF no contiene texto.");
        }

        SemaforoPdf r = new SemaforoPdf();
        parsearInformacion(r, texto);

        int periodo = 0;
        boolean enMaterias = false;
        for (String linea : texto.split("\\r?\\n")) {
            String t = linea.trim().replaceAll("\\s+", " ");
            if (t.isEmpty()) continue;

            Matcher pm = P_PERIODO.matcher(t);
            if (pm.find()) {
                periodo = Integer.parseInt(pm.group(1));
                enMaterias = true;
                continue;
            }

            if (periodo == 0) continue;

            if (t.startsWith("Código") || t.contains("Materia Forma")) continue;
            if (t.startsWith("Cancelación") || t.startsWith("Lista de")
                    || t.startsWith("No se ha")) {
                enMaterias = false;
                continue;
            }

            if (enMaterias) {
                SemaforoPdf.Materia m = parsearMateria(t, periodo);
                if (m != null) r.materias.add(m);
            }
        }

        if (r.materias.isEmpty()) {
            throw new IllegalArgumentException(
                    "No se detectaron materias. ¿Es un Semáforo del Estudiante?");
        }
        if (r.documento == null || r.nombres == null) {
            throw new IllegalArgumentException(
                    "No se pudo leer la identificación del estudiante.");
        }
        return r;
    }

    private static void parsearInformacion(SemaforoPdf r, String texto) {
        for (String linea : texto.split("\\r?\\n")) {
            String t = linea.trim().replaceAll("\\s+", " ");
            if (t.isEmpty()) continue;

            if (r.documento == null) {
                Matcher dm = P_DOCUMENTO.matcher(t);
                if (dm.find()) {
                    r.documento = dm.group(1);
                    r.nombres = dm.group(2);
                }
            }

            if (r.pensum == null) {
                Matcher pm = P_PENSUM.matcher(t);
                if (pm.find()) {
                    r.pensum = pm.group();
                    String[] tok = t.split(" ");
                    List<String> pro = new ArrayList<>();
                    List<Double> nums = new ArrayList<>();
                    boolean despuesPensum = false;
                    for (String tk : tok) {
                        if (!despuesPensum) {
                            if (tk.equals(r.pensum)) {
                                despuesPensum = true;
                            } else {
                                pro.add(tk);
                            }
                        } else {
                            Double v = aNumero(tk);
                            if (v != null) nums.add(v);
                        }
                    }
                    r.programa = String.join(" ", pro);
                    if (!pro.isEmpty() && pro.get(pro.size() - 1).isEmpty()) {
                        r.programa = r.programa.trim();
                    }
                    if (nums.size() >= 2) {
                        r.semestre = nums.get(nums.size() - 2).intValue();
                        r.promedioAcumulado = nums.get(nums.size() - 1);
                    } else if (nums.size() == 1) {
                        r.promedioAcumulado = nums.get(0);
                    }
                }
            }
        }
    }

    private static SemaforoPdf.Materia parsearMateria(String t, int periodo) {
        String[] tok = t.split(" ");
        if (tok.length < 2) return null;
        if (!P_CODIGO.matcher(tok[0]).matches()) return null;

        int idxForma = -1;
        String forma = null;
        for (int i = 1; i < tok.length; i++) {
            if ("NORMAL".equalsIgnoreCase(tok[i]) || "REPETIDA".equalsIgnoreCase(tok[i])) {
                idxForma = i;
                forma = tok[i].toUpperCase();
                break;
            }
        }
        int idxDash = indexOf("-", tok, 1);

        SemaforoPdf.Materia m = new SemaforoPdf.Materia();
        m.periodo = periodo;
        m.codigo = tok[0];

        List<String> seg;
        if (idxForma >= 0) {
            m.forma = forma;
            m.nombre = unir(tok, 1, idxForma);
            seg = new ArrayList<>(Arrays.asList(tok).subList(idxForma + 1, tok.length));
        } else {
            int fin = idxDash >= 0 ? idxDash : tok.length;
            m.nombre = unir(tok, 1, fin);
            seg = new ArrayList<>(Arrays.asList(tok).subList(fin, tok.length));
            seg.remove("-");
        }
        if (m.nombre.isEmpty()) m.nombre = "Materia";

        List<Double> nums = new ArrayList<>();
        String marcador = null;
        for (String tk : seg) {
            if (tk.isEmpty() || tk.equals("-")) continue;
            Double v = aNumero(tk);
            if (v != null) {
                nums.add(v);
            } else if (marcador == null) {
                marcador = tk;
            }
        }

        if (idxForma >= 0) {
            if ("NORMAL".equals(m.forma)) {
                if (!nums.isEmpty()) m.creditos = nums.get(0).intValue();
                if (nums.size() >= 2) m.definitiva = nums.get(nums.size() - 1);
            } else { // REPETIDA
                if (!nums.isEmpty()) m.creditos = nums.get(0).intValue();
                if (nums.size() >= 3) {
                    m.aprobacion = nums.get(1);
                    m.definitiva = nums.get(2);
                } else if (nums.size() == 2) {
                    m.definitiva = nums.get(1);
                }
            }
        } else {
            if (!nums.isEmpty()) m.creditos = nums.get(0).intValue();
        }

        if (m.definitiva == null && marcador != null) {
            m.notaTexto = marcador;
        }
        return m;
    }

    private static Double aNumero(String s) {
        try {
            return Double.parseDouble(s.replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int indexOf(String valor, String[] tok, int desde) {
        for (int i = desde; i < tok.length; i++) {
            if (valor.equals(tok[i])) return i;
        }
        return -1;
    }

    private static String unir(String[] tok, int desde, int hasta) {
        StringBuilder sb = new StringBuilder();
        for (int i = desde; i < hasta; i++) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(tok[i]);
        }
        return sb.toString();
    }
}