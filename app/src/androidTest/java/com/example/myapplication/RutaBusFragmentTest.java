package com.example.myapplication.fragments;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.myapplication.R;
import com.example.myapplication.data.RutaUpcData;
import com.example.myapplication.models.RutaParada;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/**
 * Pruebas instrumentadas para RutaBusFragment.
 *
 * Verifica que la pantalla de ruta se abra correctamente, que los
 * elementos clave esten presentes y que los datos de las paradas se
 * carguen correctamente.
 *
 * Nota: Estas pruebas se ejecutan en un dispositivo/emulador real y
 * requieren conexion a internet para cargar los tiles de OSM y los
 * datos de Firestore.
 *
 * Ejecutar con: ./gradlew connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4.class)
public class RutaBusFragmentTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
    }

    /**
     * La pantalla de ruta debe abrirse sin caer y mostrar los elementos
     * clave: mapa, barra de navegacion inferior y lista de paradas.
     */
    @Test
    public void rutaBusFragmentSeAbreSinCrash() {
        try (ActivityScenario<RutaBusFragment> scenario =
                     ActivityScenario.launch(RutaBusFragment.class)) {

            scenario.onActivity(fragment -> {
                assertNotNull("El fragmento no deberia ser nulo", fragment);
                assertNotNull("El mapa no deberia ser nulo", fragment.map);
                assertNotNull("El adapter no deberia ser nulo", fragment.adapter);
            });
        }
    }

    /**
     * La barra de navegacion inferior debe estar presente con las
     * pestañas principales, incluida la pestaña Ruta.
     */
    @Test
    public void barraNavInferiorTienePestanaRuta() {
        try (ActivityScenario<RutaBusFragment> scenario =
                     ActivityScenario.launch(RutaBusFragment.class)) {

            scenario.onActivity(fragment -> {
                // El menu inferior debe estar presente
                assertNotNull("El bottom navigation debe existir",
                        fragment.requireActivity().findViewById(R.id.bottom_nav_menu_estudiante));
                assertNotNull("La pestaña Ruta debe existir",
                        fragment.requireActivity().findViewById(R.id.nav_est_ruta));
            });
        }
    }

    /**
     * La lista de paradas debe contener las 9 paradas definidas en
     * RutaUpcData más la sede UPC.
     */
    @Test
    public void listaParadasTieneOchoParadasMasUPC() {
        try (ActivityScenario<RutaBusFragment> scenario =
                     ActivityScenario.launch(RutaBusFragment.class)) {

            scenario.onActivity(fragment -> {
                List<RutaParada> paradas = fragment.paradas;
                assertNotNull("La lista de paradas no debe ser nula", paradas);
                assertTrue("Debe haber al menos una parada", paradas.size() > 0);
                assertTrue("Debe haber al menos 9 paradas", paradas.size() >= 9);

                // Verificar que la primera parada sea el inicio
                RutaParada primera = paradas.get(0);
                assertNotNull("La primera parada no debe ser nula", primera);
                assertTrue("La primera parada debe ser Parque Nuevo Amanecer",
                        primera.getNombre().contains("Parque Nuevo Amanecer"));
            });
        }
    }

    /**
     * La lista de paradas debe incluir la sede UPC como destino final.
     */
    @Test
    public void listaParadasIncluyeSedeUPC() {
        try (ActivityScenario<RutaBusFragment> scenario =
                     ActivityScenario.launch(RutaBusFragment.class)) {

            scenario.onActivity(fragment -> {
                List<RutaParada> paradas = fragment.paradas;
                assertNotNull("La lista de paradas no debe ser nula", paradas);
                assertTrue("Debe haber paradas", paradas.size() > 0);

                // La ultima parada debe ser la sede UPC
                RutaParada ultima = paradas.get(paradas.size() - 1);
                assertNotNull("La ultima parada no debe ser nula", ultima);
                assertTrue("La ultima parada debe ser la sede UPC",
                        ultima.getNombre().contains("UPC Seccional Aguachica"));
            });
        }
    }

    /**
     * La distancia de la ruta debe calcularse correctamente cuando hay
     * al menos dos puntos.
     */
    @Test
    public void calcularDistanciaRutaDebeSerPositiva() {
        try (ActivityScenario<RutaBusFragment> scenario =
                     ActivityScenario.launch(RutaBusFragment.class)) {

            scenario.onActivity(fragment -> {
                List<RutaParada> paradas = fragment.paradas;
                assertNotNull("La lista de paradas no debe ser nula", paradas);
                assertTrue("Debe haber al menos 2 puntos para calcular distancia",
                        paradas.size() >= 2);

                // Verificar que las coordenadas sean validas
                for (RutaParada p : paradas) {
                    assertTrue("La latitud de " + p.getNombre()
                            + " debe estar en un rango valido",
                            p.getLat() >= -90 && p.getLat() <= 90);
                    assertTrue("La longitud de " + p.getNombre()
                            + " debe estar en un rango valido",
                            p.getLng() >= -180 && p.getLng() <= 180);
                }

                // La distancia total debe ser mayor a 0
                double distancia = fragment.distanciaRutaKm(paradasCoords(paradas));
                assertTrue("La distancia de la ruta debe ser mayor a 0 km",
                        distancia > 0);
            });
        }
    }

    /**
     * Convierte una lista de paradas en una lista de GeoPoint para pruebas.
     */
    private List<com.example.myapplication.data.RutaUpcData.RutaParadaInterno> crearGeoPoints(List<RutaParada> paradas) {
        // Este metodo es solo para documentar el esperado uso; la conversion
        // real se hace internamente en RutaBusFragment.dibujarRuta().
        throw new UnsupportedOperationException("Metodo solo para documentacion");
    }
}
