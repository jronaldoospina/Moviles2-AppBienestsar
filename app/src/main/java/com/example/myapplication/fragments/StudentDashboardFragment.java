package com.example.myapplication.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.myapplication.R;
import com.example.myapplication.models.Cita;
import com.example.myapplication.services.SessionManager;
import com.example.myapplication.utils.FechaUtils;
import com.example.myapplication.utils.SemaforoUtils;
import com.example.myapplication.viewmodels.StudentDashboardViewModel;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

public class StudentDashboardFragment extends Fragment {

    private TextView txtDate;
    private TextView txtPromedio, txtNivel, txtRecomendacion;
    private TextView txtMaterias, txtRiesgo, txtAprobadas;
    private TextView txtProximaCita, txtEmpty;
    private View semaforoCircle;
    private CircularProgressIndicator progressLoading;

    private SessionManager sessionManager;
    private StudentDashboardViewModel viewModel;
    private boolean primeraResume = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        txtDate = view.findViewById(R.id.text_date);
        txtPromedio = view.findViewById(R.id.txt_promedio);
        txtNivel = view.findViewById(R.id.txt_nivel);
        txtRecomendacion = view.findViewById(R.id.txt_recomendacion);
        txtMaterias = view.findViewById(R.id.txt_materias);
        txtRiesgo = view.findViewById(R.id.txt_riesgo);
        txtAprobadas = view.findViewById(R.id.txt_aprobadas);
        txtProximaCita = view.findViewById(R.id.txt_proxima_cita);
        txtEmpty = view.findViewById(R.id.txt_empty);
        semaforoCircle = view.findViewById(R.id.semaforo_circle);
        progressLoading = view.findViewById(R.id.progress_loading);

        sessionManager = new SessionManager(requireContext());
        viewModel = new ViewModelProvider(this).get(StudentDashboardViewModel.class);

        setupHeader();
        setupQuickAccess(view);
        observarViewModel();
    }

    private void setupHeader() {
        if (txtDate != null) {
            LocalDate hoy = LocalDate.now();
            String fecha = hoy.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("es", "CO"))
                    + ", " + hoy.getDayOfMonth() + " de "
                    + hoy.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "CO"))
                    + " de " + hoy.getYear();
            String fechaCapitalizada = fecha.substring(0, 1).toUpperCase() + fecha.substring(1);
            txtDate.setText(fechaCapitalizada);
        }
    }

    private void setupQuickAccess(View view) {
        View quickProgress = view.findViewById(R.id.quick_progress);
        if (quickProgress != null) quickProgress.setOnClickListener(v -> navigateTo(v, R.id.nav_est_progress));

        View quickAppointments = view.findViewById(R.id.quick_appointments);
        if (quickAppointments != null) quickAppointments.setOnClickListener(v -> navigateTo(v, R.id.nav_est_appointments));

        View quickEmotion = view.findViewById(R.id.quick_emotion);
        if (quickEmotion != null) quickEmotion.setOnClickListener(v -> navigateTo(v, R.id.nav_est_emotion));

        View quickBienestar = view.findViewById(R.id.quick_bienestar);
        if (quickBienestar != null) quickBienestar.setOnClickListener(v -> navigateTo(v, R.id.nav_est_bienestar));

        View quickCourses = view.findViewById(R.id.quick_courses);
        if (quickCourses != null) quickCourses.setOnClickListener(v -> navigateTo(v, R.id.nav_est_courses));
    }

    private void navigateTo(View v, int destinationId) {
        try {
            NavController navController = Navigation.findNavController(v);
            if (navController.getCurrentDestination() != null) {
                int currentId = navController.getCurrentDestination().getId();
                if (currentId == R.id.nav_est_home) {
                    navController.navigate(destinationId);
                }
            }
        } catch (IllegalArgumentException e) {
            // Ignorar clics múltiples/rápidos
        }
    }

    private void observarViewModel() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), cargando ->
                progressLoading.setVisibility(
                        Boolean.TRUE.equals(cargando) ? View.VISIBLE : View.GONE));

        viewModel.getResumen().observe(getViewLifecycleOwner(), resumen -> {
            if (resumen == null) return;
            txtEmpty.setVisibility(resumen.sinDatos ? View.VISIBLE : View.GONE);
            if (resumen.sinDatos) {
                txtPromedio.setText("--");
                txtMaterias.setText("0");
                txtRiesgo.setText("0");
                txtAprobadas.setText("0");
                txtNivel.setText(R.string.estudiante_sin_datos_academicos);
                txtRecomendacion.setText("");
                return;
            }

            txtPromedio.setText(SemaforoUtils.format(resumen.promedio));
            txtMaterias.setText(String.valueOf(resumen.materias));
            txtRiesgo.setText(String.valueOf(resumen.enRiesgo));
            txtAprobadas.setText(String.valueOf(resumen.aprobadas));

            semaforoCircle.setBackgroundResource(SemaforoUtils.drawableFor(resumen.promedio));
            int color = SemaforoUtils.colorFor(requireContext(), resumen.promedio);
            txtNivel.setText(getString(R.string.nivel_riesgo,
                    SemaforoUtils.nivelFor(resumen.promedio)));
            txtNivel.setTextColor(color);
            txtRecomendacion.setTextColor(color);
            txtRecomendacion.setText(recomendacionPara(resumen.promedio, resumen.enRiesgo));
        });

        viewModel.getProximaCita().observe(getViewLifecycleOwner(), cita ->
                txtProximaCita.setText(textoCita(cita)));

        viewModel.getError().observe(getViewLifecycleOwner(), mensaje -> {
            if (mensaje == null) return;
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show();
            viewModel.clearError();
        });
    }

    private String textoCita(@Nullable Cita proxima) {
        if (proxima == null) return "No tienes citas próximas";
        return FechaUtils.fechaBonita(proxima.getFecha()) + " · " + proxima.getHora()
                + (proxima.getPsicologoNombre() != null
                ? "\nCon: " + proxima.getPsicologoNombre() : "");
    }

    private String recomendacionPara(double promedio, int enRiesgo) {
        if (promedio >= 4.0) {
            return "¡Excelente! Tu rendimiento está muy bien. Sigue así.";
        }
        if (promedio >= 3.0) {
            if (enRiesgo > 0) {
                return "Tienes materias por debajo de tu promedio. Revisa tu progreso y habla con tus docentes.";
            }
            return "Buen rendimiento. Puedes mejorar un poco más tus materias.";
        }
        return "Tu rendimiento requiere atención. Agenda una cita con Bienestar Universitario.";
    }

    @Override
    public void onResume() {
        super.onResume();
        if (primeraResume) {
            primeraResume = false;
            viewModel.cargar(sessionManager.getUserId());
        } else {
            viewModel.recargar();
        }
    }
}
