package com.example.myapplication.utils;

import android.os.Handler;
import android.os.Looper;

public final class Debouncer {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final long delayMs;
    private Runnable pending;

    public Debouncer(long delayMs) {
        this.delayMs = delayMs;
    }

    public void submit(Runnable action) {
        cancel();
        pending = () -> {
            pending = null;
            action.run();
        };
        handler.postDelayed(pending, delayMs);
    }

    public void cancel() {
        if (pending != null) {
            handler.removeCallbacks(pending);
            pending = null;
        }
    }
}
