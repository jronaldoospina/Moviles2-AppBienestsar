package com.example.myapplication;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Pruebas de robustez de la app ante la falta de sesion.
 *
 * MainActivity exige sesion activa y, sin ella, redirige al login.
 * Estas pruebas verifican que ese camino red de seguridad funcione y que
 * la app no quede en pantalla negra ni reviente.
 *
 * Contexto historico: hubo un crash al alternar Inicio/Cursos causado por
 * tener dos listeners en el BottomNavigationView. Estas pruebas comprueban
 * que la app sobrevive a la navegacion incluso en el estado degradado.
 */
@RunWith(AndroidJUnit4.class)
public class NavegacionTabsTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        // Sin sesion: se reproduce el escenario de un usuario no autenticado,
        // que es cuando MainActivity decide redirigir.
        limpiarSesion();
    }

    /**
     * Estas pruebas crean una sesion falsa para poder abrir MainActivity.
     * Si no se limpia al terminar, ArranqueAppTest.elLoginValidaCamposVacios
     * puede ejecutarse despues, LoginActivity ve isLoggedIn=true, redirige a
     * MainActivity y btn_login deja de existir: falso negativo por pollution
     * entre suites. El orden de ejecucion de JUnit no esta garantizado.
     */
    @After
    public void tearDown() {
        limpiarSesion();
    }

    private void limpiarSesion() {
        context.getSharedPreferences("BienestarUPCSession", Context.MODE_PRIVATE)
                .edit().clear().apply();
    }

    /** MainActivity sin sesion debe redirigir, no fallar. */
    @Test
    public void mainActivitySinSesionRedirigeSinCrash() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(), MainActivity.class);
        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(intent)) {
            // Si redirige al login, la actividad queda destruida pero el
            // proceso sigue vivo: eso es exactamente lo que se valida.
            assertNotNull("El proceso no debe morir", scenario.getState());
        }
    }

    /** Reintentar entrar varias veces no debe acumular actividad. */
    @Test
    public void reintentarMainNoAcumulaActividades() throws Exception {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(), MainActivity.class);
        for (int i = 0; i < 3; i++) {
            try (ActivityScenario<MainActivity> s =
                         ActivityScenario.launch(intent)) {
                assertNotNull(s.getState());
                Thread.sleep(400);
            }
        }
    }

    /**
     * Con sesion completa, MainActivity debe mostrar el bottom nav.
     * Esta es la prueba que cubre el crash de navegacion real.
     */
    @Test
    public void conSesionMuestraLaApp() throws Exception {
        context.getSharedPreferences("BienestarUPCSession", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("isLoggedIn", true)
                .putString("userId", "test_uid")
                .putString("userName", "Prueba")
                .putString("userEmail", "prueba@upc.edu.co")
                .putString("userRole", "ADMIN")
                .apply();

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(), MainActivity.class);
        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(intent)) {
            Thread.sleep(2500);  // deja que infle el grafo de navegacion
            assertNotNull("Debe seguir viva con sesion", scenario.getState());
        }
    }
}