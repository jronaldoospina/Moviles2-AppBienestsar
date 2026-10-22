package com.example.myapplication;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.content.Context;
import android.content.Intent;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Pruebas instrumentadas de la pantalla Ruta Bus.
 *
 * Abren la app con una sesion de ESTUDIANTE y navegan a la pestaña Ruta,
 * verificando que el mapa, la lista de paradas y las acciones principales
 * esten presentes. Se otorgan los permisos de ubicacion y notificaciones
 * para que los dialogos del sistema no bloqueen la interaccion.
 *
 * Ejecutar con: ./gradlew connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4.class)
public class RutaBusFragmentTest {

    @Rule
    public GrantPermissionRule permisos = GrantPermissionRule.grant(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS);

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("BienestarUPCSession", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("isLoggedIn", true)
                .putString("userId", "test_uid")
                .putString("userName", "Prueba Estudiante")
                .putString("userEmail", "estudiante@upc.edu.co")
                .putString("userRole", "ESTUDIANTE")
                .apply();
    }

    @After
    public void tearDown() {
        context.getSharedPreferences("BienestarUPCSession", Context.MODE_PRIVATE)
                .edit().clear().apply();
    }

    /** Abre MainActivity con sesion de estudiante y navega a la pestaña Ruta. */
    private ActivityScenario<MainActivity> abrirRuta() {
        Intent intent = new Intent(context, MainActivity.class);
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent);
        onView(withId(R.id.nav_est_ruta)).perform(click());
        return scenario;
    }

    /** La ruta debe abrirse sin crash y mostrar mapa y lista de paradas. */
    @Test
    public void rutaSeAbreYMapaVisible() {
        try (ActivityScenario<MainActivity> ignored = abrirRuta()) {
            onView(withId(R.id.map_ruta)).check(matches(isDisplayed()));
            onView(withId(R.id.recycler_paradas)).check(matches(isDisplayed()));
        }
    }

    /** La lista debe contener al menos las 9 paradas oficiales. */
    @Test
    public void listaDeParadasTieneAlMenosNueveElementos() {
        try (ActivityScenario<MainActivity> scenario = abrirRuta()) {
            scenario.onActivity(activity -> {
                RecyclerView recycler = activity.findViewById(R.id.recycler_paradas);
                assertNotNull("El RecyclerView de paradas debe existir", recycler);
                assertNotNull("El adaptador de paradas debe estar asignado",
                        recycler.getAdapter());
                int total = recycler.getAdapter().getItemCount();
                assertTrue("Deben mostrarse al menos 9 paradas, hay " + total,
                        total >= 9);
            });
        }
    }

    /** La primera parada del recorrido debe ser Parque Nuevo Amanecer. */
    @Test
    public void primeraParadaEsParqueNuevoAmanecer() {
        try (ActivityScenario<MainActivity> ignored = abrirRuta()) {
            onView(withText("Parque Nuevo Amanecer")).check(matches(isDisplayed()));
        }
    }

    /** Las acciones principales de la ruta deben estar visibles. */
    @Test
    public void accionesDeLaRutaEstanDisponibles() {
        try (ActivityScenario<MainActivity> ignored = abrirRuta()) {
            onView(withId(R.id.btn_menu_ruta)).check(matches(isDisplayed()));
            onView(withId(R.id.btn_ver_ruta)).check(matches(isDisplayed()));
            onView(withId(R.id.btn_ver_upc)).check(matches(isDisplayed()));
            onView(withId(R.id.btn_compartir)).check(matches(isDisplayed()));
            onView(withId(R.id.fab_mi_ubicacion)).check(matches(isDisplayed()));
        }
    }
}
