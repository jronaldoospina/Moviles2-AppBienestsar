/**
 * Corrige en Firestore la parada 5 ("Cra 14 con Calle 7").
 *
 * Motivo: sus coordenadas (8.3091, -73.6042) caian DENTRO del poligono del
 * Hospital Regional de Aguachica, a 15 m de su entrada. Para llegar a ella,
 * OSRM subia por el camino interno del hospital (274 m de via sin nombre).
 * Se mueve a Calle 5, la calle publica que bordea el hospital por el norte,
 * de modo que la ruta lo rodea sin entrar a sus instalaciones.
 *
 * Uso:  node seed/corregir_parada5.js
 */
const admin = require('firebase-admin');

const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
const db = admin.firestore();

const RUTA = 'rutas_bus/ruta_principal';

// Calle 5, carril que va hacia la UPC (el bus pasa de largo por el otro).
// Calle 5 es una via dividida: la franja 8.30962-8.30972 es el carril
// correcto. Con 8.30980 (el otro carril) OSRM hacia un rodeo de 304 m
// en forma de "diente", porque tenia que volver por la via opuesta.
const LATITUD = 8.30967;
const LONGITUD = -73.60460;

(async () => {
  const ref = db.doc(RUTA);
  const doc = await ref.get();
  if (!doc.exists) {
    console.error('No existe el documento ' + RUTA);
    process.exit(1);
  }

  const data = doc.data();
  const paradas = data.paradas || [];
  const p5 = paradas.find(p => p.orden === 5);

  if (!p5) {
    console.error('No se encontro la parada con orden 5');
    process.exit(1);
  }

  console.log('Antes  : parada 5 "' + p5.nombre + '" -> ' + p5.lat + ', ' + p5.lng);
  p5.lat = LATITUD;
  p5.lng = LONGITUD;
  data.ultimaActualizacion = Date.now();

  await ref.set(data, { merge: true });

  const check = await ref.get();
  const nueva = check.data().paradas.find(p => p.orden === 5);
  console.log('Despues: parada 5 "' + nueva.nombre + '" -> ' + nueva.lat + ', ' + nueva.lng);
  console.log('OK');
  process.exit(0);
})().catch(e => {
  console.error('Error: ' + e.message);
  process.exit(1);
});