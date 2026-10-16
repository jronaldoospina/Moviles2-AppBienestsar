package com.example.myapplication.repositories;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Ruta bus: intenta leer config remota, si no existe usa datos locales.
 * Colección: rutas_bus / doc: ruta_principal
 * Campos: horarioRecogida, horarioSalida, paradas: [{orden, nombre, referencia, lat, lng}]
 */
public class RutaBusRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public Task<DocumentSnapshot> getRutaPrincipal() {
        return db.collection("rutas_bus").document("ruta_principal").get();
    }
}
