package com.example.myapplication.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import androidx.core.content.ContextCompat;

import com.example.myapplication.R;

/**
 * Efecto shimmer para estados de carga (skeleton).
 *
 * En vez de mostrar un spinner, la pantalla dibuja bloques grises con un
 * degradado que los recorre, simulando la forma del contenido real. Se
 * percibe como "cargando" en lugar de "roto".
 *
 * Es un FrameLayout porque los bloques del skeleton son sus hijos: el
 * degradado se dibuja encima en dispatchDraw(), de modo que las formas
 * grises se transparentan y se aprecia el relieve deslizante.
 *
 * Uso:  android:id="@+id/shimmer_dashboard"
 *       shimmer.mostrar();  /  shimmer.ocultar();
 */
public class ShimmerView extends FrameLayout {

    private ValueAnimator animator;
    private final Paint shimmerPaint = new Paint();
    private final Rect rect = new Rect();
    private float progreso = 0f;
    private int colorShimmer = 0x66FFFFFF;

    public ShimmerView(Context context) {
        super(context);
        init();
    }

    public ShimmerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ShimmerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        // No fijamos LAYER_TYPE_HARDWARE a proposito: forzar una capa de
        // hardware duplica el buffer de la pantalla entera y en gama baja
        // baja el rendimiento justo cuando la app debe sentirse fluida.
        // El degradado lineal es barato de dibujar por GPU.
    }

    /** Muestra el shimmer y arranca la animacion. */
    public void mostrar() {
        setVisibility(VISIBLE);
        if (!isInEditMode()) {
            iniciarAnimacion();
        }
    }

    /** Oculta el shimmer y detiene la animacion (libera CPU). */
    public void ocultar() {
        detenerAnimacion();
        setVisibility(GONE);
    }

    private void iniciarAnimacion() {
        if (animator != null && animator.isRunning()) return;

        // El color se resuelve una sola vez, no en cada fotograma.
        colorShimmer = ContextCompat.getColor(getContext(), R.color.skeleton_shimmer);

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1200);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            progreso = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    private void detenerAnimacion() {
        if (animator != null) {
            animator.cancel();
            animator.removeAllUpdateListeners();
            animator = null;
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        // Primero los hijos: los bloques grises del skeleton.
        super.dispatchDraw(canvas);

        int anchoTotal = getWidth();
        int altoTotal = getHeight();
        if (anchoTotal == 0 || altoTotal == 0) return;

        // El degradado se dibuja despues, encima, como una banda de luz que
        // cruza la pantalla. Al ser semitransparente no tapa las formas.
        float ancho = anchoTotal * 0.6f;
        float inicio = -ancho + progreso * (anchoTotal + ancho);

        shimmerPaint.reset();
        shimmerPaint.setShader(new LinearGradient(
                inicio, 0, inicio + ancho, 0,
                new int[]{0x00000000, colorShimmer, 0x00000000},
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP
        ));

        rect.set(0, 0, anchoTotal, altoTotal);
        canvas.drawRect(rect, shimmerPaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // Importante: sin esto el animator sigue corriendo con la vista fuera
        detenerAnimacion();
    }
}