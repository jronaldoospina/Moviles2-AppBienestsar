#!/usr/bin/env python3
import os

BASE = r"C:\Users\ronal\AndroidStudioProjects\MyApplication4\app\src\main\java\com\example\myapplication"

# 1. TutoriasFragment.java: agregar spnLugar a guardarTutoria
tutorias_path = os.path.join(BASE, "fragments/TutoriasFragment.java")
with open(tutorias_path, "r", encoding="utf-8") as f:
    content = f.read()

# Reemplazar la llamada al guardarTutoria en lines 182-184
old_call = """dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(x -> guardarTutoria(
                        spnCurso, editMateria, chipDia, editInicio, editFin, editAula, dialog));"""

new_call = """dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(x -> guardarTutoria(
                        spnCurso, editMateria, chipDia, editInicio, editFin, editAula, spnLugar, dialog));"""

content = content.replace(old_call, new_call)

# Reemplazar la firma de guardarTutoria
old_sig = """private void guardarTutoria(AutoCompleteTextView spnCurso, TextInputEditText editMateria,
                                ChipGroup chipDia, TextInputEditText editInicio,
                                TextInputEditText editFin, TextInputEditText editAula,
                                androidx.appcompat.app.AlertDialog dialog) {"""

new_sig = """private void guardarTutoria(AutoCompleteTextView spnCurso, TextInputEditText editMateria,
                                ChipGroup chipDia, TextInputEditText editInicio,
                                TextInputEditText editFin, TextInputEditText editAula,
                                AutoCompleteTextView spnLugar,
                                androidx.appcompat.app.AlertDialog dialog) {"""

content = content.replace(old_sig, new_sig)

# Ahora también actualizar el cuerpo donde usa editAula: poner como lugar si vacío
old_body = """String aula = editAula.getText() != null ? editAula.getText().toString().trim() : "";
        String seleccion = spnCurso.getText() != null ? spnCurso.getText().toString().trim() : "";
        
        Chip chipDiaSel = chipDia.findViewById(chipDia.getCheckedChipId());
        String dia = chipDiaSel != null ? String.valueOf(chipDiaSel.getTag()) : "";
        
        if (materia.isEmpty() || seleccion.isEmpty() || dia.isEmpty()
                || inicio.isEmpty() || fin.isEmpty()) {"""

new_body = """String aula = editAula.getText() != null ? editAula.getText().toString().trim() : "";
        String lugar = spnLugar != null && spnLugar.getText() != null ? spnLugar.getText().toString().trim() : "";
        String seleccion = spnCurso.getText() != null ? spnCurso.getText().toString().trim() : "";
        
        Chip chipDiaSel = chipDia.findViewById(chipDia.getCheckedChipId());
        String dia = chipDiaSel != null ? String.valueOf(chipDiaSel.getTag()) : "";
        
        if (materia.isEmpty() || seleccion.isEmpty() || dia.isEmpty()
                || inicio.isEmpty() || fin.isEmpty || lugar.isEmpty()) {"""

content = content.replace(old_body, new_body)

with open(tutorias_path, "w", encoding="utf-8") as f:
    f.write(content)

print("TutoriasFragment.java actualizado")