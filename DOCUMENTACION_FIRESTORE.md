# Documentación de Reglas de Seguridad Firestore

**Proyecto:** Bienestar Universitario UPC Aguachica  
**Proyecto Firebase:** `bienestar-c9bfa`  
**Archivo:** `firestore.rules`

---

## 1. Resumen ejecutivo

Este documento describe las reglas de seguridad de Cloud Firestore para el proyecto "Bienestar Universitario UPC Aguachica". Las reglas definen **quién puede leer y escribir** cada colección, basándose en la autenticación de Firebase Auth y los roles de usuario almacenados en la colección `usuarios`.

**Estado:** ✅ Reglas desplegadas y activas en Firebase Production.

---

## 2. Modelo de roles

El sistema define los siguientes roles, almacenados en el campo `rol` del documento de cada usuario en la colección `usuarios`:

| Rol | Descripción | Permiso base |
|-----|-------------|--------------|
| `estudiante` | Estudiante de la universidad | Acceso a sus propios datos |
| `docente` | Docente de la universidad | Puede gestionar alumnos y citas asignadas |
| `psicologo` | Psicólogo de bienestar | Puede gestionar citas, reportes emocionales y alertas |
| `admin` | Administrador del sistema | Acceso completo a todo |

Los roles se resuelven mediante la función helper `esRol(rol)` que consulta el documento del usuario autenticado en la colección `usuarios`.

---

## 3. Reglas por colección

### 3.1 Usuarios (`usuarios`)

| Operación | Permitido a | Condición |
|-----------|-------------|-----------|
| Leer | Cualquier usuario autenticado | `isSignedIn()` |
| Crear | Admin, psicólogo o el propio usuario | `esAdmin() || esPsicologo() || request.auth.uid == userId` |
| Actualizar | Admin, psicólogo o el propio usuario | `esAdmin() || esPsicologo() || request.auth.uid == userId` |
| Eliminar | Solo admin | `esAdmin()` |

**Nota:** Un estudiante no puede crear ni editar el perfil de otro estudiante.

---

### 3.2 Notas (`notas`)

| Operación | Permitido a | Condición |
|-----------|-------------|-----------|
| Leer | Admin, psicólogo, docente o el estudiante dueño | `isSignedIn() && (esAdmin() || esPsicologo() || esDocente() || esEstudiante() || resource.data.estudianteId == uid())` |
| Crear | Estudiantes (suyas propias), admin, docente | `esEstudiante() && request.resource.data.estudianteId == uid() || esAdmin() || esDocente()` |
| Actualizar | Estudiantes (solo sus propias notas, sin cambiar `estudianteId`), admin, docente | `esEstudiante() && resource.data.estudianteId == uid() && request.resource.data.estudianteId == resource.data.estudianteId` |
| Eliminar | Solo admin | `esAdmin()` |

**Nota importante:** Un estudiante **no puede** cambiar el `estudianteId` al editar su propia nota, evitando que se apropie de notas de otros estudiantes.

---

### 3.3 Citas (`citas`)

| Operación | Permitido a | Condición |
|-----------|-------------|-----------|
| Leer | Admin, psicólogo, estudiante dueño | `isSignedIn() && (esAdmin() || esPsicologo() || resource.data.estudianteId == uid())` |
| Crear | Estudiantes (suyas propias), psicólogo, admin | `esEstudiante() && request.resource.data.estudianteId == uid() || esPsicologo() || esAdmin()` |
| Actualizar | Admin, psicólogo, estudiante dueño | `esAdmin() || esPsicologo() || resource.data.estudianteId == uid()` |
| Eliminar | Admin, psicólogo | `esAdmin() || esPsicologo()` |

---

### 3.4 Reportes emocionales (`reportes_emocionales`)

| Operación | Permitido a | Condición |
|-----------|-------------|-----------|
| Leer | Admin, psicólogo, estudiante dueño | `isSignedIn() && (esAdmin() || esPsicologo() || resource.data.estudianteId == uid())` |
| Crear/Actualizar | Estudiantes (suyos propios), psicólogo, admin | `esEstudiante() && request.resource.data.estudianteId == uid() || esPsicologo() || esAdmin()` |
| Eliminar | Admin, psicólogo | `esAdmin() || esPsicologo()` |

---

### 3.5 Reportes y Planes de Seguimiento (`reportes` y `planes`)

| Operación | Permitido a | Condición |
|-----------|-------------|-----------|
| Leer | Admin, psicólogo, estudiante dueño | `isSignedIn() && (esAdmin() || esPsicologo() || resource.data.estudianteId == uid())` |
| Escribir (crear/actualizar) | Estudiantes (suyos propios), admin, psicólogo | `esEstudiante() && request.resource.data.estudianteId == uid() || esAdmin() || esPsicologo()` |

**Nota:** Un estudiante no puede crear ni modificar reportes de otros estudiantes.

---

### 3.6 Tutorías (`tutorias`)

| Operación | Permitido a | Condición |
|-----------|-------------|-----------|
| Leer | Cualquier usuario autenticado | `isSignedIn()` |
| Crear/Actualizar | Admin, psicólogo, docente (solo sus propias tutorías) | `esAdmin() || esPsicologo() || (esDocente() && request.resource.data.docenteId == uid())` |
| Eliminar | Admin, psicólogo | `esAdmin() || esPsicologo()` |

**Nota:** Un docente solo puede crear tutorías donde él sea el `docenteId`.

---

### 3.7 Alertas (`alertas`)

| Operación | Permitido a | Condición |
|-----------|-------------|-----------|
| Leer | Admin, psicólogo, docente dueño, estudiante dueño | `isSignedIn() && (esAdmin() || esPsicologo() || resource.data.docenteId == uid() || resource.data.estudianteId == uid())` |
| Crear | Admin, psicólogo, docente (solo sus propias alertas) | `esAdmin() || esPsicologo() || (esDocente() && request.resource.data.docenteId == uid())` |
| Actualizar/Eliminar | Admin, psicólogo | `esAdmin() || esPsicologo()` |

---

### 3.8 Conversaciones (`conversaciones` - Chat)

| Operación | Permitido a | Condición |
|-----------|-------------|-----------|
| Leer/Actualizar/Eliminar | Cualquier usuario autenticado que participe en la conversación | `isSignedIn() && participa()` donde `participa()` verifica que `uid()` esté en `resource.data.participantes` |
| Crear | Cualquier usuario autenticado que sea parte de la conversación | `request.resource.data.participantes.hasAny([uid()])` |

---

### 3.9 Mensajes (`mensajes` - Chat)

| Operación | Permitido a | Condición |
|-----------|-------------|-----------|
| Leer/Actualizar | Cualquier usuario autenticado que participe en la conversación | `isSignedIn() && resource.data.conversacionId is string && convParticipa(resource.data.conversacionId)` |
| Crear | Cualquier usuario autenticado que participe en la conversación y sea el remitente | `request.resource.data.conversacionId is string && request.resource.data.remitenteId == uid() && convParticipa(request.resource.data.conversacionId)` |
| Eliminar | **Bloqueado** | `allow delete: if false` |

---

### 3.10 Auditoría (`auditoria`)

| Operación | Permitido a | Condición |
|-----------|-------------|-----------|
| Leer | Solo admin | `isSignedIn() && esAdmin()` |
| Crear | Cualquier usuario autenticado (solo sus propios logs) | `isSignedIn() && request.resource.data.usuarioId == uid()` |
| Actualizar/Eliminar | Solo admin | `isSignedIn() && esAdmin()` |

**Nota:** Un usuario no puede crear logs de auditoría en nombre de otro usuario.

---

### 3.11 Notificaciones (`notificaciones`)

| Operación | Permitido a | Condición |
|-----------|-------------|-----------|
| Leer | Solo el receptor | `isSignedIn() && resource.data.receptorId == uid()` |
| Crear | Admin, psicólogo, docente, estudiante (solo para sí mismo) | `esAdmin() || esPsicologo() || esDocente() || (esEstudiante() && request.resource.data.receptorId == uid())` |
| Actualizar | Solo el receptor | `isSignedIn() && resource.data.receptorId == uid()` |
| Eliminar | Admin o el receptor | `isSignedIn() && (esAdmin() || resource.data.receptorId == uid())` |

---

### 3.12 Rutas del Bus (`rutas`)

| Operación | Permitido a | Condición |
|-----------|-------------|-----------|
| Leer | Admin, psicólogo, docente, estudiante dueño | `isSignedIn() && (esAdmin() || esPsicologo() || esDocente() || resource.data.estudianteId == uid())` |
| Crear | Estudiantes (suyas propias), admin, docente | `esEstudiante() && request.resource.data.estudianteId == uid() || esAdmin() || esDocente()` |
| Actualizar | Admin, docente, estudiante (suyas propias) | `esAdmin() || esDocente() || (esEstudiante() && request.resource.data.estudianteId == uid())` |
| Eliminar | Solo admin | `isSignedIn() && esAdmin()` |

---

## 4. Reglas globales y de seguridad

### Reglas generales aplicables a todas las colecciones:
- **Autenticación requerida:** Casi todas las operaciones requieren `isSignedIn()` (usuario autenticado mediante Firebase Auth).
- **Validación de ownership:** Las reglas utilizan `resource.data.estudianteId == uid()` para evitar que un estudiante acceda a datos de otro estudiante.
- **Validación de `request.resource.data`:** En operaciones de escritura, se valida el documento que se está creando/actualizando (`request.resource.data`), no solo el existente (`resource.data`), evitando que un usuario cambie campos críticos como `estudianteId` o `usuarioId`.

### Reglas de seguridad adicionales aplicadas:

| Problema corregido | Solución aplicada |
|--------------------|-------------------|
| Estudiantes creando notas de otros estudiantes | `request.resource.data.estudianteId == uid()` en `create` |
| Estudiantes cambiando el `estudianteId` al editar | `resource.data.estudianteId == uid() && request.resource.data.estudianteId == resource.data.estudianteId` |
| Estudiantes creando citas para otros estudiantes | `request.resource.data.estudianteId == uid()` en `create` |
| Estudiantes creando reportes emocionales para otros | `request.resource.data.estudianteId == uid()` en `create` y `update` |
| Docentes creando tutorías para estudiantes no suyos | `request.resource.data.docenteId == uid()` en `create` y `update` |
| Docentes creando alertas para estudiantes no suyos | `request.resource.data.docenteId == uid()` en `create` |
| Estudiantes recibiendo notificaciones a nombre de otros | `request.resource.data.receptorId == uid()` en `create` |
| Estudiantes creando logs de auditoría para otros usuarios | `request.resource.data.usuarioId == uid()` en `create` |

---

## 5. Cómo desplegar las reglas

Las reglas se despliegan usando Firebase CLI:

```bash
# Instalar Firebase CLI (si no está instalado)
npm install -g firebase-tools

# Iniciar sesión
firebase login

# Seleccionar el proyecto
firebase use bienestar-c9bfa

# Desplegar reglas de Firestore
firebase deploy --only firestore:rules
```

**Resultado esperado:**
```
✔  firestore: rules file firestore.rules compiled successfully
✔  firestore: released rules firestore.rules to cloud.firestore
```

---

## 6. Consideraciones adicionales

### Campos sensibles que requieren validación de `uid`:
- `estudianteId` en colecciones `notas`, `citas`, `reportes_emocionales`, `reportes`, `planes`, `rutas`
- `docenteId` en colecciones `tutorías`, `alertas`
- `receptorId` en colección `notificaciones`
- `usuarioId` en colección `auditoria`

### Pruebas recomendadas:
1. Crear un estudiante y verificar que no puede leer/escribir notas de otro estudiante.
2. Intentar crear una cita con `estudianteId` de otro estudiante y verificar que se rechaza.
3. Intentar crear una tutoría con `docenteId` de otro docente y verificar que se rechaza.
4. Verificar que un estudiante no puede crear notificaciones para otro estudiante.
5. Verificar que un estudiante no puede crear un log de auditoría con `usuarioId` diferente al suyo.

---

## 7. Historial de cambios

| Versión | Fecha | Cambios |
|---------|-------|---------|
| 1.0 | Versión inicial | Reglas base con roles de usuario |
| 2.0 | Corrección de seguridad | Separación de `estudianteId`, `docenteId`, `receptorId`, `usuarioId` por operación |
| 2.1 | Adición de `rutas` | Reglas para la colección de rutas del bus |

---

**Autor:** Proyecto Bienestar Universitario UPC Aguachica  
**Contacto:** bienestar@unicesar.edu.co
