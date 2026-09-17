package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.services.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000; // 2 segundos

    private ImageView logo;
    private TextView appName;
    private TextView tagline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.core.splashscreen.SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Vincular vistas
        logo = findViewById(R.id.logo_splash);
        appName = findViewById(R.id.text_app_name);
        tagline = findViewById(R.id.text_tagline);

        // Animaciones de entrada
        startAnimations();

        // Después del tiempo del splash, decidir a dónde ir
        new Handler(Looper.getMainLooper()).postDelayed(this::decideNextScreen, SPLASH_DURATION);
    }

    /**
     * Aplica animaciones de entrada al logo y textos
     */
    private void startAnimations() {
        // Animación del logo (fade in + escala)
        logo.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));

        // Animación del nombre (fade in con delay)
        appName.setAlpha(0f);
        appName.animate()
                .alpha(1f)
                .setStartDelay(400)
                .setDuration(800)
                .start();

        // Animación del tagline (fade in con más delay)
        tagline.setAlpha(0f);
        tagline.animate()
                .alpha(1f)
                .setStartDelay(800)
                .setDuration(800)
                .start();
    }

    /**
     * Decide la siguiente pantalla basándose en la sesión del usuario
     */
    private void decideNextScreen() {
        SessionManager sessionManager = new SessionManager(this);

        Intent intent;
        if (sessionManager.isLoggedIn()) {
            // Usuario ya logueado → ir directo al Main
            intent = new Intent(SplashActivity.this, MainActivity.class);
        } else {
            // No logueado → ir al Login
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }

        startActivity(intent);
        finish(); // Cerrar splash para que no vuelva al presionar "atrás"
    }
}