package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.myapplication.databinding.ActivityMainBinding;
import com.example.myapplication.models.Conversacion;
import com.example.myapplication.models.Notificacion;
import com.example.myapplication.repositories.ChatRepository;
import com.example.myapplication.repositories.NotificacionRepository;
import com.example.myapplication.services.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_NAV_DESTINO = "extra_nav_destino";

    private ActivityMainBinding binding;
    private SessionManager sessionManager;
    private NavController navController;
    private NotificacionRepository notificacionRepository;
    private ListenerRegistration notificacionRegistration;
    private final ChatRepository chatRepository = new ChatRepository();
    private ListenerRegistration chatRegistration;
    private int noLeidasNotif = 0;
    private int noLeidasChat = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        // 1. Validar que haya sesión
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Sesión no válida", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2. Validar que tenga rol
        String rol = sessionManager.getUserRole();
        if (rol == null || rol.isEmpty()) {
            Toast.makeText(this, "Usuario sin rol asignado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Se omite el título personalizado para evitar la repetición de "hola, usuario"
// en la barra superior; el título provendrá del XML de la actividad

        // 4. Configurar NavController (una sola vez)
        configurarNavController();

        // 5. Configurar menú + grafo según rol
        setupBottomNavByRole(rol);

        // 6. Botón de notificaciones + contador de no leídas en vivo
        binding.btnNotifications.setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));
        configurarBadgeNotificaciones();
        configurarBadgeChat();

        // 7. Deep-link desde notificaciones (cold start)
        procesarDeepLink(getIntent());

        // 8. Permiso para recordatorios locales (Android 13+)
        pedirPermisoNotificaciones();
    }

    private void pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2001);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        procesarDeepLink(intent);
    }

    /**
     * Navega a la sección indicada por una notificación (según rol).
     */
    private void procesarDeepLink(Intent intent) {
        if (intent == null || navController == null) return;
        String destino = intent.getStringExtra(EXTRA_NAV_DESTINO);
        if (destino == null) return;

        int navId = destinoResolvido(destino);
        if (navId != 0) {
            try {
                navController.navigate(navId);
            } catch (Exception ignored) {
                // destino fuera del grafo actual
            }
        }
    }

    private int destinoResolvido(String destino) {
        String rol = sessionManager.getUserRole();
        if (rol == null) return 0;

        if ("citas".equals(destino)) {
            if (SessionManager.ROLE_ESTUDIANTE.equals(rol)) return R.id.nav_est_appointments;
            if (SessionManager.ROLE_PSICOLOGO.equals(rol)) return R.id.nav_psi_agenda;
            return 0;
        }
        if ("cursos".equals(destino)) {
            if (SessionManager.ROLE_ESTUDIANTE.equals(rol)) return R.id.nav_est_courses;
            if (SessionManager.ROLE_DOCENTE.equals(rol)) return R.id.nav_doc_courses;
            return 0;
        }
        if ("bienestar".equals(destino)) {
            if (SessionManager.ROLE_ESTUDIANTE.equals(rol)) return R.id.nav_est_bienestar;
            return 0;
        }
        if ("alertas".equals(destino)) {
            if (SessionManager.ROLE_PSICOLOGO.equals(rol)) return R.id.nav_psi_alerts;
            if (SessionManager.ROLE_DOCENTE.equals(rol)) return R.id.nav_doc_alerts;
            return 0;
        }
        if ("tutorias".equals(destino)) {
            if (SessionManager.ROLE_DOCENTE.equals(rol)) return R.id.nav_doc_tutorias;
            return 0;
        }
        return 0;
    }

    private void configurarBadgeNotificaciones() {
        String uid = sessionManager.getUserId();
        if (uid == null) return;

        notificacionRepository = new NotificacionRepository();
        // Cancelar registro previo si existe
        if (notificacionRegistration != null) {
            notificacionRegistration.remove();
            notificacionRegistration = null;
        }
        notificacionRegistration = notificacionRepository.escuchar(uid, (snapshot, error) -> {
            if (error != null || snapshot == null) return;

            noLeidasNotif = 0;
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Notificacion n = doc.toObject(Notificacion.class);
                if (n != null && !n.isLeida()) {
                    noLeidasNotif++;
                }
            }
            actualizarBadge();
        });
    }

    private void configurarBadgeChat() {
        String uid = sessionManager.getUserId();
        if (uid == null) return;

        chatRegistration = chatRepository.escucharConversaciones(uid, (snapshot, error) -> {
            if (error != null || snapshot == null) return;

            noLeidasChat = 0;
            if (snapshot.getDocuments() != null) {
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Conversacion c = doc.toObject(Conversacion.class);
                    if (c == null || c.getPendientes() == null) continue;
                    Long v = c.getPendientes().get(uid);
                    if (v != null) noLeidasChat += v;
                }
            }
            actualizarBadge();
        });
    }

    private void actualizarBadge() {
        int total = noLeidasNotif + noLeidasChat;
        TextView badge = binding.badgeNotifications;
        if (total > 0) {
            badge.setText(total > 9 ? "9+" : String.valueOf(total));
            badge.setVisibility(View.VISIBLE);
        } else {
            badge.setVisibility(View.GONE);
        }
    }

    /**
     * Obtiene el NavController del NavHostFragment.
     */
    private void configurarNavController() {
        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        } else {
            Toast.makeText(this, "Error: NavHost no encontrado", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Infla el menú bottom nav y carga el grafo de navegación correspondiente al rol.
     */
    private void setupBottomNavByRole(String rol) {
        BottomNavigationView bottomNav = binding.bottomNavigation;

        int menuRes;
        int navGraphRes;

        switch (rol) {
            case SessionManager.ROLE_ADMIN:
                menuRes = R.menu.bottom_nav_menu_admin;
                navGraphRes = R.navigation.nav_graph_admin;
                break;

            case SessionManager.ROLE_PSICOLOGO:
                menuRes = R.menu.bottom_nav_menu_psicologo;
                navGraphRes = R.navigation.nav_graph_psicologo;
                break;

            case SessionManager.ROLE_DOCENTE:
                menuRes = R.menu.bottom_nav_menu_docente;
                navGraphRes = R.navigation.nav_graph_docente;
                break;

            case SessionManager.ROLE_ESTUDIANTE:
                menuRes = R.menu.bottom_nav_menu_estudiante;
                navGraphRes = R.navigation.nav_graph_estudiante;
                break;

            default:
                // No debería pasar, pero si pasa cerramos por seguridad
                Toast.makeText(this, "Rol no reconocido: " + rol, Toast.LENGTH_SHORT).show();
                finish();
                return;
        }

        // Limpiar menú previo (por si acaso) e inflar el nuevo
        bottomNav.getMenu().clear();
        bottomNav.inflateMenu(menuRes);

        // Cargar el grafo y conectar bottom nav con NavController
        if (navController != null) {
            navController.setGraph(navGraphRes);
            NavigationUI.setupWithNavController(bottomNav, navController);

            bottomNav.setOnItemSelectedListener(item -> {
                try {
                    int startDestId = navController.getGraph().getStartDestinationId();
                    NavOptions navOptions = new NavOptions.Builder()
                            .setLaunchSingleTop(true)
                            .setPopUpTo(startDestId, false)
                            .build();
                    navController.navigate(item.getItemId(), null, navOptions);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            });

            bottomNav.setOnItemReselectedListener(item -> {
                // Prevenir recargas innecesarias o crasheos al re-seleccionar la misma pestaña (ej. Inicio)
            });
        }
    }

    @Override
    protected void onDestroy() {
        if (notificacionRegistration != null) {
            notificacionRegistration.remove();
            notificacionRegistration = null;
        }
        if (chatRegistration != null) {
            chatRegistration.remove();
            chatRegistration = null;
        }
        super.onDestroy();
    }
}