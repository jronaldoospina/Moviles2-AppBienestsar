package com.example.myapplication.utils;

import com.example.myapplication.R;

public class AnimoUtils {

    public static final String[] LABELS = {"Muy mal", "Mal", "Regular", "Bien", "Muy bien"};

    public static String label(int animo) {
        if (animo < 1 || animo > LABELS.length) return "Sin datos";
        return LABELS[animo - 1];
    }

    public static int circle(int animo) {
        switch (animo) {
            case 1: return R.drawable.mood_circle_1;
            case 2: return R.drawable.mood_circle_2;
            case 3: return R.drawable.mood_circle_3;
            case 4: return R.drawable.mood_circle_4;
            case 5: return R.drawable.mood_circle_5;
            default: return R.drawable.mood_circle_none;
        }
    }

    public static int color(int animo) {
        switch (animo) {
            case 1: return R.color.mood_1;
            case 2: return R.color.mood_2;
            case 3: return R.color.mood_3;
            case 4: return R.color.mood_4;
            case 5: return R.color.mood_5;
            default: return R.color.text_hint;
        }
    }
}