package com.example.myapplication.utils;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

/**
 * Paginacion por cursor para Firestore.
 *
 * Evita cargar colecciones completas en memoria: cada pagina trae un limite
 * de documentos (por defecto 50) y el cursor del ultimo documento permite
 * pedir la siguiente.
 *
 * El orden debe ser estable (orderBy por un campo) para que el cursor no
 * salte registros entre paginas.
 */
public final class Paginacion {

    /** Cantidad de documentos por pagina */
    public static final int PAGINA_POR_DEFECTO = 50;

    private Paginacion() {}

    /** Resultado de una pagina, con el cursor para la siguiente. */
    public static class Pagina<T> {
        public final List<T> items;
        public final boolean haySiguiente;
        public final DocumentSnapshot ultimo;

        Pagina(List<T> items, boolean haySiguiente, DocumentSnapshot ultimo) {
            this.items = items;
            this.haySiguiente = haySiguiente;
            this.ultimo = ultimo;
        }
    }

    /** Convierte un DocumentSnapshot al modelo de la app. */
    public interface Mapper<T> {
        T map(DocumentSnapshot doc);
    }

    /**
     * Ejecuta una pagina de la consulta.
     *
     * @param query  consulta base ya filtrada
     * @param limite documentos por pagina
     * @param cursor ultimo documento de la pagina previa, o null para empezar
     * @param mapper conversor de documento a modelo
     */
    public static <T> Task<Pagina<T>> pagina(final Query query, final int limite,
                                             final DocumentSnapshot cursor,
                                             final Mapper<T> mapper) {
        Query q = query;
        if (limite > 0) {
            q = q.limit(limite);
        }
        if (cursor != null) {
            q = q.startAfter(cursor);
        }

        final int pedido = limite > 0 ? limite : Integer.MAX_VALUE;

        final TaskCompletionSource<Pagina<T>> fuente =
                new TaskCompletionSource<>();

        q.get().addOnCompleteListener(task -> {
            try {
                QuerySnapshot snap = task.getResult();
                List<T> items = new java.util.ArrayList<>();
                DocumentSnapshot ultimo = null;

                if (snap != null) {
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        T obj = mapper.map(doc);
                        if (obj != null) items.add(obj);
                        ultimo = doc;
                    }
                }

                boolean haySiguiente = snap != null && snap.size() >= pedido;
                fuente.setResult(new Pagina<>(items, haySiguiente, ultimo));
            } catch (Exception e) {
                fuente.setException(e);
            }
        });

        return fuente.getTask();
    }

    /** Atajo con el limite por defecto (50). */
    public static <T> Task<Pagina<T>> pagina(Query query, DocumentSnapshot cursor,
                                             Mapper<T> mapper) {
        return pagina(query, PAGINA_POR_DEFECTO, cursor, mapper);
    }
}