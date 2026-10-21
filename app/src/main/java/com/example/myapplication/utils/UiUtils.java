package com.example.myapplication.utils;

import android.view.View;

import com.google.android.material.snackbar.Snackbar;

public final class UiUtils {

    private UiUtils() {
    }

    public static void showRetry(View root, String message, Runnable retry) {
        if (root == null) return;
        Snackbar.make(root, message, Snackbar.LENGTH_LONG)
                .setAction("Reintentar", v -> retry.run())
                .show();
    }
}
