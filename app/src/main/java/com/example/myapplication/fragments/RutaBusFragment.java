package com.example.myapplication.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapters.RutaParadaAdapter;
import com.example.myapplication.data.RutaUpcData;
import com.example.myapplication.models.RutaParada;
import com.example.myapplication.repositories.RutaBusRepository;
import com.example.myapplication.utils.GeoUtils;
import com.example.myapplication.utils.OsrmCaminata;
import com.example.myapplication.utils.OsrmRouter;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import android.annotation.SuppressLint;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Extra - Ruta Bus UPC Aguachica con OpenStreetMap (osmdroid).
 *
 * Fundamento: movilidad universitaria. El estudiante ve las 9 paradas,
 * la sede UPC y cuál parada le sirve según su ubicación.
 */
public class RutaBusFragment extends Fragment {

    /** Distancia maxima a pie para considerar que la parada le sirve (metros) */
    private static final double UMBRAL_CAMINATA = 700;

    private MapView map;
    private TextView txtCercana;
    private TextView txtResumenCompacto;
    private View cardCercana;
    private RutaParadaAdapter adapter;
    private List<RutaParada> paradas = new ArrayList<>();
    /** Marcadores de parada en el mismo orden que la lista, para poder ajustarlos. */
    private final List<Marker> marcadoresParada = new ArrayList<>();

    private FusedLocationProviderClient fused;
    private Double myLat = null;
    private Double myLng = null;
    private final RutaBusRepository rutaRepo = new RutaBusRepository();
    private RutaParada ultimaCercana = null;
    private float ultimaDistM = -1;
    private List<GeoPoint> rutaReal = null;
    private boolean mostrarDistanciaRuta = false;
    private Polyline polylineBorde = null;
    private Polyline polylineRuta = null;
    private Polyline polylineCaminata = null;

    /** Horarios en pantalla; se leen de Firestore y se muestran en el menu. */
    private String horarioRecogida = "Recogida: 05:00 A.M. / 01:00 P.M.";
    private String horarioSalida = "Salida UPC: 12:00 M. / 06:00 P.M.";

    private final ActivityResultLauncher<String[]> permLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {
                        Boolean fine = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                        Boolean coarse = result.get(Manifest.permission.ACCESS_COARSE_LOCATION);
                        if (Boolean.TRUE.equals(fine) || Boolean.TRUE.equals(coarse)) {
                            localizar();
                        } else {
                            if (txtCercana != null) {
                                setMensajeCercano(
                                        "Sin permiso de ubicacion: no podemos indicarte "
                                                + "cuál parada te queda más cerca.\n"
                                                + "Puedes ver la ruta completa igual.");
                            }
                            Toast.makeText(requireContext(),
                                    "Permiso denegado: activa la ubicacion en ajustes",
                                    Toast.LENGTH_LONG).show();
                        }
                    });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ruta_bus, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // User-Agent válido (OSM bloquea 403 si es genérico/vacío)
        Configuration.getInstance().setUserAgentValue("BienestarUPC-Aguachica/1.0");
        Configuration.getInstance().setTileDownloadThreads((short) 4);
        Configuration.getInstance().setTileFileSystemCacheMaxBytes(100L * 1024 * 1024);

        map = view.findViewById(R.id.map_ruta);
        txtCercana = view.findViewById(R.id.txt_parada_cercana);
        txtResumenCompacto = view.findViewById(R.id.txt_resumen_compacto);
        cardCercana = view.findViewById(R.id.card_cercana);
        RecyclerView recycler = view.findViewById(R.id.recycler_paradas);

        // OSM Humanitario (HOT - tile.openstreetmap.fr): datos OSM, sin API key
        // y sin el bloqueo 403 de tile.openstreetmap.org ni el "api key required" de CARTO.
        try {
            XYTileSource osmHot = new XYTileSource(
                    "OSM-HOT",
                    0, 19, 256, ".png",
                    new String[]{
                            "https://a.tile.openstreetmap.fr/hot/",
                            "https://b.tile.openstreetmap.fr/hot/",
                            "https://c.tile.openstreetmap.fr/hot/"},
                    "© OpenStreetMap contributors, Tiles style by HOT");
            map.setTileSource(osmHot);
            map.setMultiTouchControls(true);
            map.getController().setZoom(14.0);
            map.getController().setCenter(
                    new GeoPoint(RutaUpcData.CENTER_LAT, RutaUpcData.CENTER_LNG));
        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    "No se pudo iniciar el mapa: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }

        paradas = RutaUpcData.getParadas();
        dibujarRuta();

        adapter = new RutaParadaAdapter();
        adapter.setData(paradas);
        adapter.setListener(p -> {
            guardarUltimaParada(p);
            centrarEnParada(p);
        });
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        mostrarUltimaParada();
        cargarRutaFirestore(view);
        preguntarPermisoSiHaceFalta();

        // Menu hamburguesa: el detalle de la ruta vive en una hoja inferior
        // para no tapar el mapa
        view.findViewById(R.id.btn_menu_ruta).setOnClickListener(v -> abrirInfoRuta());

        fused = LocationServices.getFusedLocationProviderClient(requireContext());

        // Boton flotante:asking icono de ubicacion en el mapa
        view.findViewById(R.id.fab_mi_ubicacion).setOnClickListener(v -> {
            pedirPermisoYLocalizar();
            if (myLat != null && myLng != null) {
                try {
                    map.getController().setZoom(17.0);
                    map.getController().animateTo(new GeoPoint(myLat, myLng));
                } catch (Exception ignored) { }
            }
        });

        view.findViewById(R.id.btn_ver_upc).setOnClickListener(v -> {
            try {
                map.getController().setZoom(18.5);
                map.getController().animateTo(
                        new GeoPoint(RutaUpcData.UPC_LAT, RutaUpcData.UPC_LNG));
            } catch (Exception ex) {
                Toast.makeText(requireContext(),
                        "No se pudo centrar el mapa", Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.btn_compartir).setOnClickListener(v -> compartirRuta());

        // Boton "Ver ruta": encuadra toda la ruta real en pantalla
        view.findViewById(R.id.btn_ver_ruta).setOnClickListener(v -> {
            List<GeoPoint> puntos = rutaReal;
            if (puntos == null || puntos.isEmpty()) {
                Toast.makeText(requireContext(),
                        "Ruta sobre la calle no disponible (sin internet)",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                double latN = Double.NEGATIVE_INFINITY, latS = Double.POSITIVE_INFINITY;
                double lonE = Double.NEGATIVE_INFINITY, lonW = Double.POSITIVE_INFINITY;
                for (GeoPoint g : puntos) {
                    latN = Math.max(latN, g.getLatitude());
                    latS = Math.min(latS, g.getLatitude());
                    lonE = Math.max(lonE, g.getLongitude());
                    lonW = Math.min(lonW, g.getLongitude());
                }
                if (latN <= latS || lonE <= lonW) return;
                map.zoomToBoundingBox(
                        new org.osmdroid.util.BoundingBox(latN, lonE, latS, lonW), true);
                // El detalle va al resumen compacto de la barra, no a una
                // tarjeta sobre el mapa.
                mostrarDistanciaRuta = true;
                actualizarResumen();
            } catch (Exception ex) {
                Toast.makeText(requireContext(),
                        "No se pudo mostrar la ruta completa",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarRutaFirestore(View view) {
        rutaRepo.getRutaPrincipal()
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || doc == null || !doc.exists()) return;
                    try {
                        String hRec = doc.getString("horarioRecogida");
                        String hSal = doc.getString("horarioSalida");
                        if (hRec != null && hRec.trim().length() > 0) {
                            horarioRecogida = hRec.trim();
                        }
                        if (hSal != null && hSal.trim().length() > 0) {
                            horarioSalida = hSal.trim();
                        }
                        Object arr = doc.get("paradas");
                        if (arr instanceof java.util.List) {
                            java.util.List<?> raw = (java.util.List<?>) arr;
                            List<RutaParada> remotas = new ArrayList<>();
                            for (Object o : raw) {
                                if (o instanceof java.util.Map) {
                                    java.util.Map<?, ?> m = (java.util.Map<?, ?>) o;
                                    Number orden = (Number) m.get("orden");
                                    String nombre = (String) m.get("nombre");
                                    String ref = (String) m.get("referencia");
                                    Number lat = (Number) m.get("lat");
                                    Number lng = (Number) m.get("lng");
                                    if (nombre != null && lat != null && lng != null) {
                                        remotas.add(new RutaParada(
                                                orden != null ? orden.intValue() : remotas.size() + 1,
                                                nombre, ref != null ? ref : "",
                                                lat.doubleValue(), lng.doubleValue()));
                                    }
                                }
                            }
                            if (!remotas.isEmpty()) {
                                remotas.sort((a, b) -> Integer.compare(a.getOrden(), b.getOrden()));
                                paradas = remotas;
                                adapter.setData(paradas);
                                dibujarRuta();
                            }
                        }
                    } catch (Exception ignored) { }
                });
    }

    /**
     * Abre el detalle de la ruta en una hoja inferior: horarios, resumen del
     * recorrido y listado de paradas. Se mantiene fuera del mapa para que la
     * vista del recorrido no pierda espacio.
     */
    @SuppressLint("InflateParams")
    private void abrirInfoRuta() {
        if (!isAdded()) return;
        View sheet = LayoutInflater.from(requireContext())
                .inflate(R.layout.sheet_ruta_info, null);

        TextView rec = sheet.findViewById(R.id.txt_horario_recogida_menu);
        TextView sal = sheet.findViewById(R.id.txt_horario_salida_menu);
        TextView lista = sheet.findViewById(R.id.txt_paradas_ruta_menu);
        TextView resumen = sheet.findViewById(R.id.txt_resumen_ruta);
        TextView fuente = sheet.findViewById(R.id.txt_fuente_menu);

        rec.setText(horarioRecogida);
        sal.setText(horarioSalida);
        resumen.setText(resumenRuta());
        fuente.setText(rutaReal != null ? "OSM + OSRM" : "OSM");

        StringBuilder sb = new StringBuilder();
        for (RutaParada p : paradas) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(p.getOrden()).append(". ").append(p.getNombre());
            if (p.getReferencia() != null && !p.getReferencia().isEmpty()) {
                sb.append(" · ").append(p.getReferencia());
            }
        }
        sb.append('\n').append("10. Sede UPC Aguachica");
        lista.setText(sb.toString());

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(sheet);
        dialog.show();
    }

    private void guardarUltimaParada(RutaParada p) {
        if (getContext() == null) return;
        getContext().getSharedPreferences("ruta_bus", android.content.Context.MODE_PRIVATE)
                .edit().putInt("ultima_orden", p.getOrden())
                .putString("ultima_nombre", p.getOrden() + ". " + p.getNombre())
                .apply();
    }

    private void mostrarUltimaParada() {
        if (getContext() == null) return;
        String ultima = getContext().getSharedPreferences("ruta_bus",
                android.content.Context.MODE_PRIVATE).getString("ultima_nombre", null);
        if (ultima != null && txtCercana != null
                && txtCercana.getText().toString().contains("Pulsa")) {
            setMensajeCercano("Tu última parada: " + ultima
                    + ". Pulsa el botón de ubicación para actualizar.");
        }
    }

    private void compartirRuta() {
        String texto = "Ruta Bus UPC Aguachica\n"
                + RutaUpcData.HORARIO_RECOGIDA + "\n"
                + RutaUpcData.HORARIO_SALIDA + "\n";
        if (ultimaCercana != null) {
            String d = ultimaDistM < 1000
                    ? String.format(new Locale("es", "CO"), "%.0f m", ultimaDistM)
                    : String.format(new Locale("es", "CO"), "%.1f km", ultimaDistM / 1000f);
            texto += "Me sirve: " + ultimaCercana.getOrden() + ". "
                    + ultimaCercana.getNombre() + " (" + d + ")\n";
        }
        texto += "https://www.openstreetmap.org/#map=14/"
                + RutaUpcData.CENTER_LAT + "/" + RutaUpcData.CENTER_LNG;

        try {
            android.content.Intent i = new android.content.Intent(
                    android.content.Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(android.content.Intent.EXTRA_TEXT, texto);

            // Verificar que exista alguna app able de recibir el intent
            if (i.resolveActivity(requireContext().getPackageManager()) == null) {
                Toast.makeText(requireContext(),
                        "No hay apps para compartir. Copia este dato:\n" + texto,
                        Toast.LENGTH_LONG).show();
                return;
            }
            startActivity(android.content.Intent.createChooser(i, "Compartir ruta"));
        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    "No se pudo compartir en este dispositivo",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void dibujarRuta() {
        if (map == null) return;
        map.getOverlays().clear();
        polylineBorde = null;
        polylineRuta = null;
        polylineCaminata = null;

        List<GeoPoint> paradasCoords = new ArrayList<>();
        for (RutaParada p : paradas) {
            paradasCoords.add(new GeoPoint(p.getLat(), p.getLng()));
        }
        paradasCoords.add(new GeoPoint(RutaUpcData.UPC_LAT, RutaUpcData.UPC_LNG));

        dibujarMarcadores(paradasCoords);

        // Pedir la geometria real de la calle a OSRM (OpenStreetMap)
        OsrmRouter.calcularRuta(paradasCoords, resultado -> {
            if (!isAdded() || map == null || getView() == null) return;

            if (resultado == null || resultado.geometria == null
                    || resultado.geometria.isEmpty()) {
                // Sin internet o servicio caido: fallback en linea recta.
                // Los marcadores se apoyan en esa misma linea.
                rutaReal = paradasCoords;
                ajustarMarcadores(GeoUtils.ajustarParadas(paradasCoords, paradasCoords));
                pintarPolyline(paradasCoords);
                actualizarResumen();
                map.invalidate();
                return;
            }

            rutaReal = resultado.geometria;
            // OSRM ya devuelve cada parada fijada sobre la calzada, asi que el
            // marcador cae exactamente en la linea dibujada.
            List<GeoPoint> ajustadas = resultado.paradasAjustadas != null
                    && resultado.paradasAjustadas.size() == paradasCoords.size()
                    ? resultado.paradasAjustadas
                    : GeoUtils.ajustarParadas(paradasCoords, resultado.geometria);

            ajustarMarcadores(ajustadas);
            pintarPolyline(resultado.geometria);
            actualizarResumen();
            map.invalidate();
        });
    }

    /** Reubica los marcadores sobre los puntos ya ajustados a la calzada. */
    private void ajustarMarcadores(List<GeoPoint> ajustadas) {
        if (ajustadas == null) return;
        int n = Math.min(marcadoresParada.size(), ajustadas.size());
        for (int i = 0; i < n; i++) {
            marcadoresParada.get(i).setPosition(ajustadas.get(i));
        }
    }

    private void actualizarResumen() {
        if (txtResumenCompacto == null) return;
        txtResumenCompacto.setText(resumenRuta());
    }

    /** Linea de resumen compartida por la barra compacta y la hoja de detalle. */
    private String resumenRuta() {
        String km = rutaReal != null && rutaReal.size() > 1
                ? String.format(new Locale("es", "CO"), "%.1f km",
                        distanciaRutaKm(rutaReal))
                : null;
        if (km == null) {
            return paradas.size() + " paradas · OpenStreetMap";
        }
        return paradas.size() + " paradas · " + km
                + (mostrarDistanciaRuta ? " sobre la calle" : "");
    }

    /** Dibuja la polyline con borde blanco, reemplazando la anterior si existe */
    private void pintarPolyline(List<GeoPoint> puntos) {
        if (map == null || puntos == null || puntos.isEmpty()) return;

        // Quitar las polylines previas para no superponer lineas
        if (polylineBorde != null) map.getOverlays().remove(polylineBorde);
        if (polylineRuta != null) map.getOverlays().remove(polylineRuta);

        // Borde exterior (blanco)
        Polyline borde = new Polyline();
        borde.setPoints(puntos);
        borde.setColor(0xFFFFFFFF);
        borde.setWidth(13f);
        map.getOverlays().add(borde);
        polylineBorde = borde;

        // Linea interior (verde esmeralda)
        Polyline linea = new Polyline();
        linea.setPoints(puntos);
        linea.setColor(0xFF1B7A43);
        linea.setWidth(8f);
        map.getOverlays().add(linea);
        polylineRuta = linea;

        map.invalidate();
    }

    /** Longitud total de la ruta en km */
    private double distanciaRutaKm(List<GeoPoint> pts) {
        double metros = 0;
        for (int i = 1; i < pts.size(); i++) {
            float[] out = new float[1];
            android.location.Location.distanceBetween(
                    pts.get(i - 1).getLatitude(), pts.get(i - 1).getLongitude(),
                    pts.get(i).getLatitude(), pts.get(i).getLongitude(), out);
            metros += out[0];
        }
        return metros / 1000.0;
    }

    private void dibujarMarcadores(List<GeoPoint> coords) {
        marcadoresParada.clear();

        // Markers de paradas, en el mismo orden que la lista de coordenadas
        for (int i = 0; i < paradas.size() && i < coords.size(); i++) {
            RutaParada p = paradas.get(i);
            Marker m = new Marker(map);
            m.setPosition(coords.get(i));
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            m.setTitle(p.getOrden() + ". " + p.getNombre());
            m.setSnippet(p.getReferencia());
            final Marker marcador = m;
            m.setOnMarkerClickListener((marker, mapView) -> {
                guardarUltimaParada(p);
                mapView.getController().setZoom(18.5);
                mapView.getController().animateTo(marcador.getPosition());
                return true;
            });
            map.getOverlays().add(m);
            marcadoresParada.add(m);
        }

        // Marker UPC (destino), en la ultima coordenada
        if (coords.size() > paradas.size()) {
            Marker upc = new Marker(map);
            upc.setPosition(coords.get(coords.size() - 1));
            upc.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            upc.setTitle("UPC Seccional Aguachica");
            upc.setSnippet("Cra 40 Vía al mar - Destino");
            upc.setOnMarkerClickListener((marker, mapView) -> {
                mapView.getController().setZoom(18.5);
                mapView.getController().animateTo(marker.getPosition());
                return true;
            });
            map.getOverlays().add(upc);
        }

        map.invalidate();
    }

    private void centrarEnParada(RutaParada p) {
        map.getController().setZoom(18.5);
        map.getController().animateTo(new GeoPoint(p.getLat(), p.getLng()));
        Toast.makeText(requireContext(),
                p.getOrden() + ". " + p.getNombre(), Toast.LENGTH_SHORT).show();
    }

    /**
 * Al abrir la pantalla se ofrece el permiso de ubicacion de forma proactiva,
 * explicando antes para que el estudiante entienda para que se usa.
 */
private void preguntarPermisoSiHaceFalta() {
    boolean fine = ContextCompat.checkSelfPermission(requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    boolean coarse = ContextCompat.checkSelfPermission(requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    if (fine || coarse) {
        localizar();
        return;
    }

    new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Ubicacion activada")
            .setMessage("Para saber cuál parada del bus te queda más cerca y cuánto "
                    + "debes caminar, necesitamos acceso a tu ubicacion.\n\n"
                    + "Solo se usa en esta pantalla y no se envía a ningún servidor.")
            .setPositiveButton("Activar", (d, w) -> permLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}))
            .setNegativeButton("Ahora no", null)
            .show();
}

private void pedirPermisoYLocalizar() {
    boolean fine = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (fine || coarse) {
            localizar();
        } else {
            permLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION});
        }
    }

    private void localizar() {
        if (fused == null) {
            try {
                fused = LocationServices.getFusedLocationProviderClient(requireContext());
            } catch (Exception e) {
                Toast.makeText(requireContext(), "GPS no disponible",
                        Toast.LENGTH_SHORT).show();
                return;
            }
        }
        try {
            fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .addOnSuccessListener(loc -> {
                        if (loc != null) {
                            onUbicacion(loc.getLatitude(), loc.getLongitude());
                        } else {
                            fused.getLastLocation().addOnSuccessListener(last -> {
                                if (last != null) {
                                    onUbicacion(last.getLatitude(), last.getLongitude());
                                } else {
                                    Toast.makeText(requireContext(),
                                            "No se pudo obtener tu ubicación",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(requireContext(),
                            "Error ubicación: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } catch (SecurityException e) {
            Toast.makeText(requireContext(), "Sin permiso de ubicación",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void onUbicacion(double lat, double lng) {
        if (map == null || adapter == null || txtCercana == null) return;
        myLat = lat;
        myLng = lng;
        adapter.setMyLocation(lat, lng);

        // Mi marcador
        Marker me = new Marker(map);
        me.setPosition(new GeoPoint(lat, lng));
        me.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        me.setTitle("Tú estás aquí");
        me.setOnMarkerClickListener((marker, mapView) -> {
            mapView.getController().setZoom(18.0);
            mapView.getController().animateTo(marker.getPosition());
            return true;
        });
        map.getOverlays().add(me);
        map.getController().setZoom(17.0);
        map.getController().animateTo(new GeoPoint(lat, lng));
        map.invalidate();

        calcularParadaCercana(lat, lng);
    }

    /**
     * Analiza las 9 paradas y muestra cuales le sirven al estudiante.
     * Primero por distancia aerea (rapida) y luego afina la parada
     * ganadora con la distancia real caminando sobre la calle.
     */
    private void calcularParadaCercana(double lat, double lng) {
        if (paradas.isEmpty()) return;

        // Ordenar por distancia en linea recta (rapido, sin red)
        List<RutaParada> ordenadas = new ArrayList<>(paradas);
        Map<RutaParada, Float> distAerea = new HashMap<>();
        for (RutaParada p : ordenadas) {
            float[] out = new float[1];
            Location.distanceBetween(lat, lng, p.getLat(), p.getLng(), out);
            distAerea.put(p, out[0]);
        }
        Collections.sort(ordenadas,
                Comparator.comparingDouble(p -> distAerea.get(p)));

        final List<RutaParada> top = ordenadas.size() >= 2
                ? new ArrayList<>(ordenadas.subList(0, 2))
                : new ArrayList<>(ordenadas);

        // Afinar la primera con la ruta real caminando
        final RutaParada candidata = top.get(0);
        OsrmCaminata.rutaCaminando(
                new GeoPoint(lat, lng),
                new GeoPoint(candidata.getLat(), candidata.getLng()),
                res -> {
                    if (!isAdded() || txtCercana == null) return;

                    // Distancia real caminando; si falla, la aerea
                    double dist = res.metros > 0
                            ? res.metros
                            : distAerea.get(candidata);

                    // Dibujar la ruta por las calles hasta el paradero
                    dibujarRutaCaminata(res.puntos, candidata);

                    mostrarResultado(candidata, dist, top, distAerea, lat, lng);
                });
    }

    /** Presenta el resultado: si le sirve, cuanto camina y a que hora pasa el bus */
    private void mostrarResultado(RutaParada mejor, double distMetros,
                                  List<RutaParada> alternativas,
                                  Map<RutaParada, Float> distAerea,
                                  double lat, double lng) {
        if (!isAdded() || txtCercana == null) return;

        ultimaCercana = mejor;
        ultimaDistM = (float) distMetros;
        mostrarDistanciaRuta = false;

        StringBuilder sb = new StringBuilder();
        String distTxt = formatearDistancia(distMetros);

        if (distMetros <= UMBRAL_CAMINATA) {
            // Le sirve: debe caminar hasta la parada
            sb.append("Te sirve. Parada ").append(mejor.getOrden())
              .append(" · ").append(mejor.getNombre()).append("\n")
              .append("Camina ").append(distTxt).append(" por las calles (ruta en azul) ")
              .append("· el bus pasa a las ")
              .append(mejor.getOrden() == 1 ? "05:00 AM y 01:00 PM" : "01:00 PM")
              .append(".");
        } else {
            // No le sirve bien: la más cercana está lejos
            sb.append("La ruta no te cubre bien.\n")
              .append("Parada más cercana: ").append(mejor.getOrden()).append(". ")
              .append(mejor.getNombre()).append(", a ").append(distTxt)
              .append(" caminando (ruta en azul).");
            if (alternativas.size() > 1) {
                RutaParada segunda = alternativas.get(1);
                sb.append("\nAlternativa ").append(segunda.getOrden()).append(". ")
                  .append(segunda.getNombre()).append(" a ")
                  .append(formatearDistancia(distAerea.get(segunda))).append(".");
            }
        }

        // Distancia hasta la sede, para saber cuanto falta en el recorrido
        float[] out = new float[1];
        Location.distanceBetween(lat, lng, RutaUpcData.UPC_LAT, RutaUpcData.UPC_LNG, out);
        sb.append("\nSede UPC a ").append(formatearDistancia(out[0])).append(" de ti.");

        setMensajeCercano(sb.toString());
        guardarUltimaParada(mejor);
    }
    /** Dibuja como caminar por la calle hasta el paradero, en azul punteado */
    private void dibujarRutaCaminata(List<GeoPoint> puntos, RutaParada parada) {
        if (map == null || parada == null) return;

        if (polylineCaminata != null) {
            map.getOverlays().remove(polylineCaminata);
            polylineCaminata = null;
        }

        List<GeoPoint> aDibujar = (puntos != null && puntos.size() > 1)
                ? puntos
                : null;

        if (aDibujar != null) {
            Polyline caminata = new Polyline(map);
            caminata.setPoints(aDibujar);
            caminata.setColor(0xFF1D4ED8);
            caminata.setWidth(7f);
            map.getOverlays().add(0, caminata);
            polylineCaminata = caminata;
        }
        map.invalidate();
    }

    /**
     * Muestra un mensaje en la tarjeta de parada cercana. Se centraliza aqui
     * porque la tarjeta arranca oculta y solo debe aparecer cuando hay algo
     * que decir, para no tapar el mapa sin motivo.
     */
    private void setMensajeCercano(CharSequence texto) {
        if (txtCercana == null) return;
        txtCercana.setText(texto);
        if (cardCercana != null) cardCercana.setVisibility(View.VISIBLE);
    }

    private String formatearDistancia(double metros) {
        if (metros < 1000) {
            return String.format(new Locale("es", "CO"), "%.0f m", metros);
        }
        return String.format(new Locale("es", "CO"), "%.1f km", metros / 1000.0);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null) {
            map.onResume();
            // Forzar el dibujado de los tiles al volver a la pantalla
            map.invalidate();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (map != null) map.onDetach();
        map = null;
    }
}
