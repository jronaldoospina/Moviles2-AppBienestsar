package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapters.MessagesAdapter;
import com.example.myapplication.models.Mensaje;
import com.example.myapplication.models.Notificacion;
import com.example.myapplication.repositories.ChatRepository;
import com.example.myapplication.repositories.NotificacionRepository;
import com.example.myapplication.services.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.List;

public class ChatDetailActivity extends AppCompatActivity {

    public static final String EXTRA_CONVERSACION_ID = "conversacionId";
    public static final String EXTRA_OTRO_NOMBRE = "otroNombre";
    public static final String EXTRA_OTRO_ID = "otroId";

    private static final int MAX_LONGITUD = 800;
    private static final int MAX_BYTES = 700 * 1024;

    private final List<Mensaje> mensajes = new ArrayList<>();

    private RecyclerView recycler;
    private TextView txtEmpty;
    private CircularProgressIndicator progress;
    private TextInputEditText editInput;
    private MaterialButton btnSend;

    private final ChatRepository chatRepository = new ChatRepository();
    private SessionManager sessionManager;
    private String conversacionId;
    private String otroId;
    private String miUid;
    private ListenerRegistration registration;
    private String tokenDestinatario;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_detail);

        conversacionId = getIntent().getStringExtra(EXTRA_CONVERSACION_ID);
        String otroNombre = getIntent().getStringExtra(EXTRA_OTRO_NOMBRE);
        otroId = getIntent().getStringExtra(EXTRA_OTRO_ID);
        if (conversacionId == null) {
            Toast.makeText(this, "Conversación no encontrada", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        sessionManager = new SessionManager(this);
        miUid = sessionManager.getUserId();

        // Obtener token FCM del destinatario
        obtenerTokenFCM(otroId);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_chat);
        toolbar.setTitle(otroNombre != null ? otroNombre : "Conversación");
        toolbar.setNavigationOnClickListener(v -> finish());

        recycler = findViewById(R.id.recycler_mensajes);
        txtEmpty = findViewById(R.id.txt_empty_chat);
        progress = findViewById(R.id.progress_chat);
        editInput = findViewById(R.id.edit_input);
        btnSend = findViewById(R.id.btn_send);

        MessagesAdapter adapter = new MessagesAdapter(miUid);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
        recycler.setItemAnimator(null);

        // ===== Escucha en tiempo real =====
        progress.setVisibility(View.VISIBLE);
        registration = chatRepository.getQueryMensajes(conversacionId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        progress.setVisibility(View.GONE);
                        txtEmpty.setText("No se pudieron cargar los mensajes.");
                        txtEmpty.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    mensajes.clear();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Mensaje m = doc.toObject(Mensaje.class);
                            if (m != null) {
                                m.setId(doc.getId());
                                mensajes.add(m);
                            }
                        }
                    }
                    mensajes.sort((a, b) -> Long.compare(a.getFecha(), b.getFecha()));
                    adapter.setMensajes(mensajes);

                    progress.setVisibility(View.GONE);
                    txtEmpty.setVisibility(mensajes.isEmpty() ? View.VISIBLE : View.GONE);

                    recycler.post(() -> {
                        if (!mensajes.isEmpty()) {
                            recycler.scrollToPosition(mensajes.size() - 1);
                        }
                    });

                    if (miUid != null) {
                        chatRepository.marcarLeidos(conversacionId, miUid);
                        chatRepository.limpiarPendiente(conversacionId, miUid);
                    }
                });

        // ===== Enviar =====
        btnSend.setOnClickListener(v -> enviar());
        editInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                enviar();
                return true;
            }
            return false;
        });
    }

    private void obtenerTokenFCM(String uid) {
        if (uid == null) return;
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    this.tokenDestinatario = token;
                    // Podemos almacenar este token si es necesario
                })
                .addOnFailureListener(e -> {
                    // Token no disponible aún
                });
    }

    private void enviar() {
        String texto = editInput.getText() != null ? editInput.getText().toString().trim() : "";
        if (texto.isEmpty() || miUid == null || conversacionId == null) return;

        Mensaje m = new Mensaje();
        m.setConversacionId(conversacionId);
        m.setRemitenteId(miUid);
        m.setTexto(texto);
        m.setFecha(System.currentTimeMillis());
        m.setLeido(false);

        chatRepository.enviarMensaje(m)
                .addOnSuccessListener(doc -> {
                    editInput.setText("");
                    chatRepository.actualizarConversacion(conversacionId, texto);
                    chatRepository.incrementarPendiente(conversacionId, otroId);
                    // CREAR NOTIFICACIÓN FCM PARA DESTINATARIO
                    crearNotificacionFCM(texto);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al enviar: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void crearNotificacionFCM(String texto) {
        if (otroId == null) return;

        // Guardar notificación en Firestore para historial y entrega
        String nombre = sessionManager.getUserName();
        Notificacion n = new Notificacion();
        n.setReceptorId(otroId);
        n.setCreadaPor(miUid);
        n.setOrigenNombre(nombre);
        n.setTipo("MENSAJE");
        n.setTitulo("Nuevo mensaje de " + (nombre != null && !nombre.isEmpty() ? nombre : "Bienestar UPCT"));
        n.setCuerpo(texto.length() > 120 ? texto.substring(0, 120) + "…" : texto);
        n.setConversacionId(conversacionId);
        n.setLeida(false);
        n.setFecha(System.currentTimeMillis());
        new NotificacionRepository().crear(n);
    }

    @Override
    protected void onDestroy() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
        super.onDestroy();
    }
}