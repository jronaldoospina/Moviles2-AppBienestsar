/*
 * Seed para Bienestar UPC.
 *
 * Uso:
 *   1) node -v  (Necesitas Node 16+)
 *   2) npm install        (dentro de la carpeta seed/)
 *   3) Descarga la "Nueva clave de cuenta de servicio" en Firebase:
 *        Project settings > Service accounts > Generate new private key
 *   4) node seed.js C:/ruta/a/serviceAccountKey.json
 *
*  Creará los usuarios en Firebase Auth y todas las colecciones en Firestore,
 *  y publicará el archivo ../firestore.rules en el proyecto (lo mismo que haría
 *  "firebase deploy --only firestore:rules", pero sin Firebase CLI).
 *  Idempotente: si un email ya existe en Auth, reutiliza su UID.
 */

const admin = require('firebase-admin');

const SERVICE_ACCOUNT = process.argv[2];
if (!SERVICE_ACCOUNT) {
  console.error('Falta la ruta del serviceAccountKey.json.\nUso: node seed.js C:/ruta/serviceAccountKey.json');
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.cert(SERVICE_ACCOUNT),
});

const CONTRASENA_INICIAL = 'bienestar123';
const db = admin.firestore();

// ============================================================
// USUARIOS EN AUTH (email -> tag)
// ============================================================
const USUARIOS = [
  { email: 'admin@bienestar.edu.co',   tag: 'ADMIN_UID' },
  { email: 'psi@bienestar.edu.co',     tag: 'PSI_UID' },
  { email: 'docente@bienestar.edu.co', tag: 'DOCE_UID' },
  { email: 'alu1@bienestar.edu.co',    tag: 'ALU1_UID' },
  { email: 'alu2@bienestar.edu.co',    tag: 'ALU2_UID' },
  { email: 'alu3@bienestar.edu.co',    tag: 'ALU3_UID' },
  { email: 'alu4@bienestar.edu.co',    tag: 'ALU4_UID' },
  { email: 'alu5@bienestar.edu.co',    tag: 'ALU5_UID' },
];

const uidMap = {};

async function crearOCargarUsuario(u) {
  try {
    const nuevo = await admin.auth().createUser({ email: u.email, password: CONTRASENA_INICIAL });
    uidMap[u.tag] = nuevo.uid;
    console.log(`  + Usuario en Auth (creado): ${u.email} -> ${nuevo.uid}`);
  } catch (e) {
    if (e && e.errorInfo && e.errorInfo.code === 'auth/email-already-exists') {
      const existente = await admin.auth().getUserByEmail(u.email);
      uidMap[u.tag] = existente.uid;
      console.log(`  = Ya existía en Auth: ${u.email} -> ${existente.uid}`);
    } else {
      throw e;
    }
  }
}

function reemplazar(valor) {
  return JSON.parse(JSON.stringify(valor), (key, v) =>
    typeof v === 'string' && uidMap[v] ? uidMap[v] : v
  );
}

async function setDoc(coleccion, docId, data) {
  await db.collection(coleccion).doc(docId).set(reemplazar(data));
  console.log(`  + ${coleccion}/${docId}`);
}

const NOMBRES = {
  ALU1_UID: 'Ana Torres',
  ALU2_UID: 'Luis Pérez',
  ALU3_UID: 'María López',
  ALU4_UID: 'Jorge Ramírez',
  ALU5_UID: 'Sofía Herrera',
};
const nomDe = t => NOMBRES[t] || 'Estudiante';

// ============================================================
// DOCUMENTOS DE USUARIOS
// ============================================================
const USUARIOS_DOC = [
  { tag: 'ADMIN_UID', nombre: 'Tatiana Reyes', documento: '1020000001', telefono: '3001112233', rol: 'ADMIN' },
  { tag: 'PSI_UID', nombre: 'Luisa Martínez', documento: '1020000002', telefono: '3112223344', rol: 'PSICOLOGO', especialidad: 'Clínica', estudiantesAsignados: [] },
  { tag: 'DOCE_UID', nombre: 'Carlos Gómez', documento: '1020000003', telefono: '3223334455', rol: 'DOCENTE', cursos: ['curso-1', 'curso-2', 'curso-3'] },
  { tag: 'ALU1_UID', nombre: 'Ana Torres', documento: '1020000004', telefono: '3334445566', rol: 'ESTUDIANTE', codigo: '202451001', programa: 'Ing. de Sistemas', semestre: 4, promedio: 3.5, nivelRiesgo: 'ALTO' },
  { tag: 'ALU2_UID', nombre: 'Luis Pérez', documento: '1020000005', telefono: '3445556677', rol: 'ESTUDIANTE', codigo: '202452002', programa: 'Ing. de Sistemas', semestre: 4, promedio: 3.8, nivelRiesgo: 'MEDIO' },
  { tag: 'ALU3_UID', nombre: 'María López', documento: '1020000006', telefono: '3556667788', rol: 'ESTUDIANTE', codigo: '202453003', programa: 'Psicología', semestre: 3, promedio: 4.2, nivelRiesgo: 'BAJO' },
  { tag: 'ALU4_UID', nombre: 'Jorge Ramírez', documento: '1020000007', telefono: '3667778899', rol: 'ESTUDIANTE', codigo: '202454004', programa: 'Medicina', semestre: 2, promedio: 2.6, nivelRiesgo: 'CRITICO' },
  { tag: 'ALU5_UID', nombre: 'Sofía Herrera', documento: '1020000008', telefono: '3778889900', rol: 'ESTUDIANTE', codigo: '202455005', programa: 'Derecho', semestre: 5, promedio: 4.0, nivelRiesgo: 'BAJO' },
];

async function seedUsuarios() {
  console.log('\n== Documentos en usuarios ==');
  for (const u of USUARIOS_DOC) {
    const email = u.email || (u.tag.toLowerCase().replace('_uid', '') + '@bienestar.edu.co');
    const base = {
      uid: u.tag,
      email,
      usuario: email.split('@')[0],
      documento: u.documento,
      nombre: u.nombre,
      rol: u.rol,
      activo: true,
      telefono: u.telefono,
      requiereCambioContrasena: false,
      ultimoCambioContrasena: 1767225600000,
      fechaRegistro: 1767225600000,
      fechaCreacion: 1767225600000,
      ultimaActualizacion: 1767225600000,
    };
    // Se agregan los campos extras (especialidad, codigo, promedio, etc.)
    const data = { ...base, ...u };
    await db.collection('usuarios').doc(uidMap[u.tag]).set(reemplazar(data));
    console.log(`  + usuarios/${uidMap[u.tag]}`);
  }
}

async function seedDatos() {
  console.log('\n== Cursos ==');
  const cursos = [
    {
      id: 'curso-1', nombre: 'Cálculo I', codigo: 'IS-101', programa: 'Ing. de Sistemas',
      semestre: 3, creditos: 4, periodo: '2026-1',
      estudiantesIds: ['ALU1_UID', 'ALU2_UID', 'ALU3_UID', 'ALU4_UID', 'ALU5_UID'],
    },
    {
      id: 'curso-2', nombre: 'Fundamentos de Programación', codigo: 'IS-102', programa: 'Ing. de Sistemas',
      semestre: 2, creditos: 3, periodo: '2026-1',
      estudiantesIds: ['ALU1_UID', 'ALU2_UID', 'ALU3_UID', 'ALU4_UID', 'ALU5_UID'],
    },
    {
      id: 'curso-3', nombre: 'Cálculo Vectorial', codigo: 'IS-201', programa: 'Ing. de Sistemas',
      semestre: 4, creditos: 4, periodo: '2026-1',
      estudiantesIds: ['ALU1_UID', 'ALU2_UID', 'ALU3_UID'],
    },
  ];
  for (const c of cursos) {
    await setDoc('cursos', c.id, {
      nombre: c.nombre, codigo: c.codigo, programa: c.programa,
      semestre: c.semestre, creditos: c.creditos, periodo: c.periodo,
      docenteId: 'DOCE_UID', docenteNombre: 'Carlos Gómez',
      estudiantesIds: c.estudiantesIds,
      cortes: [
        { id: 'corte1', nombre: 'Corte 1', porcentaje: 0.33, activo: true, cerrado: false },
        { id: 'corte2', nombre: 'Corte 2', porcentaje: 0.33, activo: false, cerrado: false },
        { id: 'corte3', nombre: 'Corte 3', porcentaje: 0.34, activo: false, cerrado: false },
      ],
      fechaCreacion: 1767225600000, ultimaActualizacion: 1767225600000,
    });
  }

  console.log('\n== Notas ==');
  const notas = [
    { id: 'nota-1', cursoId: 'curso-1', student: 'ALU1_UID', valor: 3.2, nivelRiesgo: 'ALTO' },
    { id: 'nota-2', cursoId: 'curso-1', student: 'ALU4_UID', valor: 2.4, nivelRiesgo: 'CRITICO' },
    { id: 'nota-3', cursoId: 'curso-1', student: 'ALU3_UID', valor: 4.5, nivelRiesgo: 'BAJO' },
    { id: 'nota-4', cursoId: 'curso-2', student: 'ALU2_UID', valor: 4.0, nivelRiesgo: 'MEDIO' },
    { id: 'nota-5', cursoId: 'curso-2', student: 'ALU3_UID', valor: 3.6, nivelRiesgo: 'BAJO' },
    { id: 'nota-6', cursoId: 'curso-2', student: 'ALU4_UID', valor: 2.9, nivelRiesgo: 'ALTO' },
  ];
  for (const n of notas) {
    await setDoc('notas', n.id, {
      estudianteId: n.student, cursoId: n.cursoId, corteId: 'corte1',
      valor: n.valor, observacion: '', registradoPor: 'DOCE_UID',
      fechaRegistro: 1767225600000, ultimaActualizacion: 1767225600000,
      nivelRiesgo: n.nivelRiesgo,
    });
  }

  console.log('\n== Citas ==');
  const citas = [
    { id: 'cita-1', estudianteId: 'ALU1_UID', fecha: '2026-09-16', hora: '10:00', duracionMinutos: 45, motivo: 'Ansiedad por parciales', estado: 'CONFIRMADA' },
    { id: 'cita-2', estudianteId: 'ALU1_UID', fecha: '2026-09-20', hora: '14:30', duracionMinutos: 30, motivo: 'Seguimiento', estado: 'PENDIENTE' },
    { id: 'cita-3', estudianteId: 'ALU2_UID', fecha: '2026-09-05', hora: '09:00', duracionMinutos: 45, motivo: 'Primera consulta', estado: 'ATENDIDA' },
  ];
  for (const c of citas) {
    await setDoc('citas', c.id, {
      estudianteId: c.estudianteId, estudianteNombre: nomDe(c.estudianteId),
      psicologoId: 'PSI_UID', psicologoNombre: 'Luisa Martínez',
      fecha: c.fecha, hora: c.hora, duracionMinutos: c.duracionMinutos,
      motivo: c.motivo, estado: c.estado,
      fechaCreacion: 1767225600000, ultimaActualizacion: 1767225600000,
    });
  }

  console.log('\n== Reportes ==');
  await setDoc('reportes', 'reporte-1', {
    estudianteId: 'ALU1_UID', estudianteNombre: 'Ana Torres',
    psicologoAsignado: 'PSI_UID', psicologoNombre: 'Luisa Martínez',
    titulo: 'Acompañamiento académico', tipo: 'SEGUIMIENTO',
    descripcion: 'Sesiones de manejo de ansiedad', nivelRiesgo: 'ALTO',
    estado: 'EN_SEGUIMIENTO', fecha: 1778400000000, ultimaActualizacion: 1778662800000,
    planCumplido: false,
    avances: [{ texto: 'Asistió a la primera sesión', fecha: 1778662800000 }],
  });
  await setDoc('reportes', 'reporte-2', {
    estudianteId: 'ALU2_UID', estudianteNombre: 'Luis Pérez',
    psicologoAsignado: 'PSI_UID', psicologoNombre: 'Luisa Martínez',
    titulo: 'Orientación vocacional', tipo: 'ORIENTACION',
    descripcion: 'Plan culminado', nivelRiesgo: 'MEDIO',
    estado: 'ATENDIDO', fecha: 1776463200000, ultimaActualizacion: 1777575600000,
    planCumplido: true,
    avances: [],
  });

  console.log('\n== Reportes emocionales ==');
  const emoc = [
    { id: 'emp-1', estudianteId: 'ALU1_UID', animo: 2, descripcion: 'Semana difícil', fechaDia: '2026-09-10', fecha: 1778662800000 },
    { id: 'emp-2', estudianteId: 'ALU1_UID', animo: 3, descripcion: 'Un poco mejor', fechaDia: '2026-08-28', fecha: 1777575600000 },
    { id: 'emp-3', estudianteId: 'ALU1_UID', animo: 4, descripcion: 'Con calma', fechaDia: '2026-08-15', fecha: 1776463200000 },
    { id: 'emp-4', estudianteId: 'ALU4_UID', animo: 1, descripcion: 'Muy estresado', fechaDia: '2026-09-08', fecha: 1778400000000 },
    { id: 'emp-5', estudianteId: 'ALU4_UID', animo: 2, descripcion: 'Decae', fechaDia: '2026-08-20', fecha: 1777071600000 },
    { id: 'emp-6', estudianteId: 'ALU3_UID', animo: 4, descripcion: 'Okay', fechaDia: '2026-09-11', fecha: 1778749200000 },
  ];
  for (const e of emoc) {
    await setDoc('reportes_emocionales', e.id, {
      estudianteId: e.estudianteId, animo: e.animo, descripcion: e.descripcion,
      fechaDia: e.fechaDia, fecha: e.fecha,
    });
  }

  console.log('\n== Alertas ==');
  const alertas = [
    { id: 'alerta-1', estudianteId: 'ALU1_UID', estudianteNombre: 'Ana Torres', motivo: 'BAJO_RENDIMIENTO', descripcion: 'Nota 3.2 en corte 1', estado: 'PENDIENTE' },
    { id: 'alerta-2', estudianteId: 'ALU4_UID', estudianteNombre: 'Jorge Ramírez', motivo: 'INASISTENCIA', descripcion: '3 faltas consecutivas', estado: 'PENDIENTE' },
  ];
  for (const a of alertas) {
    await setDoc('alertas', a.id, {
      docenteId: 'DOCE_UID', docenteNombre: 'Carlos Gómez',
      estudianteId: a.estudianteId, estudianteNombre: a.estudianteNombre,
      cursoId: 'curso-1', cursoNombre: 'Cálculo I',
      motivo: a.motivo, descripcion: a.descripcion,
      estado: a.estado, fecha: 1777575600000,
    });
  }

  console.log('\n== Tutorías ==');
  await setDoc('tutorias', 'tutoria-1', {
    docenteId: 'DOCE_UID', docenteNombre: 'Carlos Gómez',
    cursoId: 'curso-1', cursoNombre: 'Cálculo I', codigoCurso: 'IS-101',
    materia: 'Cálculo I', diaSemana: 'MIÉRCOLES', horaInicio: '14:00',
    horaFin: '15:00', aula: 'A-204', activa: true,
    fechaCreacion: 1767225600000, ultimaActualizacion: 1767225600000,
  });
}

// ============================================================
// DESPLIEGUE DE REGLAS DE FIRESTORE
// ============================================================
async function obtenerToken() {
  const { GoogleAuth } = require('google-auth-library');
  const auth = new GoogleAuth({
    keyFile: SERVICE_ACCOUNT,
    // Nota: con el scope 'firebase.rules' el endpoint devuelve solo un
    // id_token (sin access_token) y la librería falla; con 'cloud-platform'
    // devuelve un access_token válido para la Rules REST API.
    scopes: ['https://www.googleapis.com/auth/cloud-platform'],
  });
  return auth.getAccessToken();
}

async function leerReglas() {
  const fs = require('fs');
  const path = require('path');
  return fs.readFileSync(path.join(__dirname, '..', 'firestore.rules'), 'utf8');
}

async function desplegarReglas() {
  console.log('\n== Paso 0: Reglas de Firestore ==');
  const fs = require('fs');
  const rules = await leerReglas();
  const projectId = JSON.parse(fs.readFileSync(SERVICE_ACCOUNT, 'utf8')).project_id;
  const token = await obtenerToken();
  if (!token) throw new Error('No se pudo obtener un token de acceso (revisa la service account).');
  const headers = {
    'Content-Type': 'application/json',
    Authorization: 'Bearer ' + token,
  };
  const base = 'https://firebaserules.googleapis.com/v1';

  // 1) Crear el ruleset con el contenido del archivo
  const rsRes = await fetch(`${base}/projects/${encodeURIComponent(projectId)}/rulesets`, {
    method: 'POST',
    headers,
    body: JSON.stringify({
      source: { files: [{ name: 'firestore.rules', content: rules }] },
    }),
  });
  const rsBody = await rsRes.json();
  if (!rsRes.ok) {
    throw new Error('Error creando ruleset: ' + JSON.stringify(rsBody));
  }
  const rulesetName = rsBody.name;
  console.log('  + Ruleset creado: ' + rulesetName);

  // 2) Apuntar el release cloud.firestore al ruleset (los releases no se borran,
  //    así que el primer deploy debe crearlo; los siguientes lo actualizan)
  const releaseName = `projects/${encodeURIComponent(projectId)}/releases/cloud.firestore`;
  const releaseUrl = `${base}/${releaseName}`;
  const patch = await fetch(releaseUrl, {
    method: 'PATCH',
    headers,
    body: JSON.stringify({
      release: { name: releaseName, rulesetName },
      updateMask: 'ruleset_name',
    }),
  });
  const patchBody = await patch.json();
  if (!patch.ok && patch.status !== 404) {
    throw new Error('Error actualizando release: ' + JSON.stringify(patchBody));
  }
  if (patch.status === 404) {
    const create = await fetch(`${base}/projects/${encodeURIComponent(projectId)}/releases`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ release: { name: releaseName, rulesetName } }),
    });
    const createBody = await create.json();
    if (!create.ok) {
      throw new Error('Error creando release: ' + JSON.stringify(createBody));
    }
    console.log('  + Release cloud.firestore creado');
  } else {
    console.log('  + Release cloud.firestore actualizado');
  }
}

async function main() {
  console.log('Seed de Bienestar UPC\n=====================');

  await desplegarReglas();

  console.log('\n== Paso 1: Usuarios en Firebase Auth ==');
  for (const u of USUARIOS) {
    await crearOCargarUsuario(u);
  }

  await seedUsuarios();
  await seedDatos();

  console.log('\n¡Listo! Credenciales de prueba (todas con ' + CONTRASENA_INICIAL + '):');
  for (const u of USUARIOS) {
    console.log('  ' + u.email + '  /  ' + CONTRASENA_INICIAL);
  }
  console.log('\nReglas Firestore publicadas desde firestore.rules.');
  console.log('Nota: conversaciones, mensajes y notificaciones se generan solos desde la app.');
}

main().catch(e => {
  console.error('\nHubo un error:', e);
  process.exit(1);
});