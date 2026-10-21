package com.example.myapplication.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.models.Mensaje;
import com.example.myapplication.models.Notificacion;
import com.example.myapplication.repositories.ChatRepository;
import com.example.myapplication.repositories.NotificacionRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class ChatDetailViewModel extends ViewModel {

    private final ChatRepository chatRepository = new ChatRepository();
    private final NotificacionRepository notificacionRepository = new NotificacionRepository();

    private final MutableLiveData<List<Mensaje>> mensajes = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> aviso = new MutableLiveData<>();
    private final MutableLiveData<Long> enviado = new MutableLiveData<>();

    private ListenerRegistration registration;
    private String conversacionId;
    private String miUid;
    private String otroId;
    private String miNombre;
    private boolean pendienteLimpiado;

    public LiveData<List<Mensaje>> getMensajes() {
        return mensajes;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<String> getAviso() {
        return aviso;
    }

    public LiveData<Long> getEnviado() {
        return enviado;
    }

    public void empezarEscucha(String conversacionId, String miUid, String otroId, String miNombre) {
        if (conversacionId == null) return;
        if (conversacionId.equals(this.conversacionId) && registration != null) return;

        detenerEscucha();
        this.conversacionId = conversacionId;
        this.miUid = miUid;
        this.otroId = otroId;
        this.miNombre = miNombre;
        pendienteLimpiado = false;
        loading.setValue(mensajes.getValue() == null);

        registration = chatRepository.getQueryMensajes(conversacionId)
                .addSnapshotListener((snapshot, e) -> {
                    loading.setValue(false);
                    if (e != null) {
                        error.setValue("No se pudieron cargar los mensajes");
                        return;
                    }

                    List<Mensaje> lista = parseMensajes(snapshot);
                    mensajes.setValue(lista);

                    boolean hayNoLeidos = false;
                    if (miUid != null) {
                        for (Mensaje m : lista) {
                            if (!m.isLeido() && !miUid.equals(m.getRemitenteId())) {
                                hayNoLeidos = true;
                                break;
                            }
                        }
                    }

                    if (!lista.isEmpty() && miUid != null && (hayNoLeidos || !pendienteLimpiado)) {
                        pendienteLimpiado = true;
                        chatRepository.marcarLeidos(conversacionId, miUid);
                        chatRepository.limpiarPendiente(conversacionId, miUid);
                    }
                });
    }

    public void enviar(String texto) {
        String limpio = texto != null ? texto.trim() : "";
        if (limpio.isEmpty() || miUid == null || conversacionId == null) return;

        Mensaje m = new Mensaje();
        m.setConversacionId(conversacionId);
        m.setRemitenteId(miUid);
        m.setTexto(limpio);
        m.setFecha(System.currentTimeMillis());
        m.setLeido(false);

        chatRepository.enviarMensaje(m)
                .addOnSuccessListener(doc -> {
                    chatRepository.actualizarConversacion(conversacionId, limpio);
                    chatRepository.incrementarPendiente(conversacionId, otroId);
                    crearNotificacion(limpio);
                    enviado.setValue(System.currentTimeMillis());
                })
                .addOnFailureListener(e ->
                        aviso.setValue("Error al enviar: " + e.getMessage()));
    }

    private void crearNotificacion(String texto) {
        if (otroId == null) return;

        Notificacion n = new Notificacion();
        n.setReceptorId(otroId);
        n.setCreadaPor(miUid);
        n.setOrigenNombre(miNombre);
        n.setTipo("MENSAJE");
        n.setTitulo("Nuevo mensaje de " + (miNombre != null && !miNombre.isEmpty()
                ? miNombre : "Bienestar UPCT"));
        n.setCuerpo(texto.length() > 120 ? texto.substring(0, 120) + "…" : texto);
        n.setConversacionId(conversacionId);
        n.setLeida(false);
        n.setFecha(System.currentTimeMillis());
        notificacionRepository.crear(n);
    }

    public void detenerEscucha() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    public void clearError() {
        error.setValue(null);
    }

    public void clearAviso() {
        aviso.setValue(null);
    }

    public void clearEnviado() {
        enviado.setValue(null);
    }

    @Override
    protected void onCleared() {
        detenerEscucha();
    }

    private static List<Mensaje> parseMensajes(QuerySnapshot snapshot) {
        List<Mensaje> lista = new ArrayList<>();
        if (snapshot != null) {
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Mensaje m = doc.toObject(Mensaje.class);
                if (m != null) {
                    m.setId(doc.getId());
                    lista.add(m);
                }
            }
        }
        lista.sort((a, b) -> Long.compare(a.getFecha(), b.getFecha()));
        return lista;
    }
}
