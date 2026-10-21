package com.example.myapplication.utils;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;
import java.util.function.BiPredicate;

public final class ListDiff {

    private ListDiff() {
    }

    public static <T> DiffUtil.DiffResult calculate(List<T> viejo, List<T> nuevo,
                                                    BiPredicate<T, T> mismosItems,
                                                    BiPredicate<T, T> mismoContenido) {
        return DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return viejo.size();
            }

            @Override
            public int getNewListSize() {
                return nuevo.size();
            }

            @Override
            public boolean areItemsTheSame(int posViejo, int posNuevo) {
                return mismosItems.test(viejo.get(posViejo), nuevo.get(posNuevo));
            }

            @Override
            public boolean areContentsTheSame(int posViejo, int posNuevo) {
                return mismoContenido.test(viejo.get(posViejo), nuevo.get(posNuevo));
            }
        });
    }

    public static boolean eq(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }
}
