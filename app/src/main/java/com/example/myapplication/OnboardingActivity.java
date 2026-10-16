package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class OnboardingActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "onboarding_prefs";
    private static final String KEY_FIRST_TIME = "first_time";

    /** True si el usuario aun no ha visto el onboarding. Lo consulta el Splash. */
    public static boolean isPrimerArranque(Context contexto) {
        return contexto.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_FIRST_TIME, true);
    }

    /** Marca el onboarding como visto. */
    public static void marcarCompletado(Context contexto) {
        contexto.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_FIRST_TIME, false).apply();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_onboarding);

        // Aplicar insets del sistema para borde a borde (modo oscuro compatible)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button btnEmpezar = findViewById(R.id.btn_empezar);

        // Si no es la primera vez, avanzar directo a la app
        if (!isPrimerArranque(this)) {
            irAInicio();
            return;
        }

        btnEmpezar.setOnClickListener(v -> {
            marcarCompletado(this);
            irAInicio();
        });
    }

    /**
     * Continua el arranque. Sin sesion debe ir al login, no a MainActivity:
     * MainActivity exige sesion y, si no la hay, hace finish() dejando al
     * usuario en el launcher sin forma de iniciar sesion.
     */
    private void irAInicio() {
        boolean haySesion = new com.example.myapplication.services.SessionManager(this).isLoggedIn();
        Intent intent = new Intent(this, haySesion ? MainActivity.class : LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}