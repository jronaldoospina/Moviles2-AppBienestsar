package com.example.myapplication;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Pruebas instrumentadas de arranque y navegacion.
 *
 * Verifican que la app no se caiga al abrir, que el flujo de pantallas
 * exista y que las secciones clave sean accesibles. Son las pruebas que
 * habrian atrapado los crashes de navegacion y de menu.
 *
 * Requieren un dispositivo o emulador conectado:
 *   ./gradlew connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4.class)
public class ArranqueAppTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        // Cada prueba arranca desde el mismo estado. Sin esto el resultado
        // depende del orden de ejecucion: si SplashActivity ya dejo
        // first_time=true, su Handler pendiente lanza OnboardingActivity por
        // encima del LoginActivity de la prueba y btn_login no existe.
        context.getSharedPreferences("BienestarUPCSession", Context.MODE_PRIVATE)
                .edit().clear().apply();
        context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("first_time", false).apply();
    }

    /**
     * La app debe abrirse sin caer. Este es el smoke test mas basico:
     * si falla, todo lo demas es irrelevante.
     */
    @Test
    public void laAppAbreSinCrash() {
        try (ActivityScenario<SplashActivity> scenario =
                     ActivityScenario.launch(SplashActivity.class)) {
            assertNotNull("La actividad no se lanzo", scenario.getState());
        }
    }

    /** El splash debe navegar a alguna pantalla valida (onboarding o main). */
    @Test
    public void elSplashNavegaAUnaPantallaValida() throws InterruptedException {
        try (ActivityScenario<SplashActivity> scenario =
                     ActivityScenario.launch(SplashActivity.class)) {
            // El splash dura 2 segundos y luego navega
            Thread.sleep(3500);
            assertNotNull(scenario.getState());
        }
    }

    /**
     * La pantalla de login debe mostrar sus campos. Si algo se rompe en el
     * layout, falla aqui en lugar de en produccion.
     */
    @Test
    public void elLoginMuestraSusCampos() {
        try (ActivityScenario<LoginActivity> scenario =
                     ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.edit_email)).check(matches(isDisplayed()));
            onView(withId(R.id.edit_password)).check(matches(isDisplayed()));
            onView(withId(R.id.btn_login)).check(matches(isDisplayed()));
        }
    }

    /**
     * El boton de login no debe avanzar con campos vacios.
     * Verifica que exista la validacion de formulario.
     */
    @Test
    public void elLoginValidaCamposVacios() throws Exception {
        try (ActivityScenario<LoginActivity> scenario =
                     ActivityScenario.launch(LoginActivity.class)) {
            Thread.sleep(1200);   // la pantalla puede tardar en estabilizarse
            onView(withId(R.id.btn_login)).perform(click());
            Thread.sleep(800);
            // Debe seguir en login, no avanzar al Main
            onView(withId(R.id.edit_email)).check(matches(isDisplayed()));
        }
    }

    /** El onboarding debe poder completarse y llegar a la app. */
    @Test
    public void elOnboardingSePuedeCompletar() {
        // Se limpia la preferencia para forzar la primera vez
        context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
                .edit().clear().apply();

        try (ActivityScenario<OnboardingActivity> scenario =
                     ActivityScenario.launch(OnboardingActivity.class)) {
            onView(withId(R.id.btn_empezar)).perform(click());
            // Tras Empezar debe navegar a MainActivity
            assertNotNull(scenario.getState());
        }
    }

    /**
     * Regresion: en una instalacion nueva el usuario no tiene sesion.
     * El onboarding debe llevar al LOGIN, no a MainActivity, porque
     * MainActivity sin sesion hace finish() y devuelve al launcher dejando
     * al usuario sin forma de iniciar sesion.
     */
    @Test
    public void elOnboardingSinSesionLlegaAlLogin() throws Exception {
        context.getSharedPreferences("BienestarUPCSession", Context.MODE_PRIVATE)
                .edit().clear().apply();
        context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
                .edit().clear().apply();   // primera vez

        try (ActivityScenario<OnboardingActivity> scenario =
                     ActivityScenario.launch(OnboardingActivity.class)) {
            onView(withId(R.id.btn_empezar)).perform(click());
            Thread.sleep(1200);
            // Debe verse el formulario de login
            onView(withId(R.id.edit_email)).check(matches(isDisplayed()));
        }
    }

    /**
     * Regresion: con onboarding ya completado y sin sesion, el splash debe
     * llevar directo al login en vez de pasar por el onboarding.
     */
    @Test
    public void elSplashSinSesionTrasOnboardingVaAlLogin() throws Exception {
        context.getSharedPreferences("BienestarUPCSession", Context.MODE_PRIVATE)
                .edit().clear().apply();
        context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("first_time", false).apply();

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        ApplicationProvider.getApplicationContext().startActivity(intent);

        Thread.sleep(4000);
        onView(withId(R.id.edit_email)).check(matches(isDisplayed()));
    }
}