package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapters.MessagesAdapter;
import com.example.myapplication.services.SessionManager;
import com.example.myapplication.viewmodels.ChatDetailViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

public class ChatDetailActivity extends AppCompatActivity {

    public static final String EXTRA_CONVERSACION_ID = "conversacionId";
    public static final String EXTRA_OTRO_NOMBRE = "otroNombre";
    public static final String EXTRA_OTRO_ID = "otroId";

    private RecyclerView recycler;
    private TextView txtEmpty;
    private CircularProgressIndicator progress;
    private TextInputEditText editInput;

    private MessagesAdapter adapter;
    private ChatDetailViewModel viewModel;
    private String conversacionId;
    private String otroId;
    private String miUid;

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

        SessionManager sessionManager = new SessionManager(this);
        miUid = sessionManager.getUserId();

        MaterialToolbar toolbar = findViewById(R.id.toolbar_chat);
        toolbar.setTitle(otroNombre != null ? otroNombre : "Conversación");
        toolbar.setNavigationOnClickListener(v -> finish());

        recycler = findViewById(R.id.recycler_mensajes);
        txtEmpty = findViewById(R.id.txt_empty_chat);
        progress = findViewById(R.id.progress_chat);
        editInput = findViewById(R.id.edit_input);
        MaterialButton btnSend = findViewById(R.id.btn_send);

        adapter = new MessagesAdapter(miUid);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
        recycler.setItemAnimator(null);

        viewModel = new ViewModelProvider(this).get(ChatDetailViewModel.class);
        observarViewModel();

        btnSend.setOnClickListener(v -> enviar());
        editInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                enviar();
                return true;
            }
            return false;
        });
    }

    private void observarViewModel() {
        viewModel.getMensajes().observe(this, lista -> {
            boolean alFinal = !recycler.canScrollVertically(1);
            adapter.setMensajes(lista);
            txtEmpty.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
            if (alFinal && !lista.isEmpty()) {
                recycler.post(() -> recycler.scrollToPosition(lista.size() - 1));
            }
        });
        viewModel.getLoading().observe(this, cargando -> {
            boolean activo = Boolean.TRUE.equals(cargando);
            progress.setVisibility(activo ? View.VISIBLE : View.GONE);
            if (activo) txtEmpty.setVisibility(View.GONE);
        });
        viewModel.getError().observe(this, mensaje -> {
            if (mensaje == null) return;
            if (adapter.getItemCount() == 0) {
                txtEmpty.setText(mensaje);
                txtEmpty.setVisibility(View.VISIBLE);
            }
            Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
            viewModel.clearError();
        });
        viewModel.getAviso().observe(this, mensaje -> {
            if (mensaje == null) return;
            Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
            viewModel.clearAviso();
        });
        viewModel.getEnviado().observe(this, fecha -> {
            if (fecha == null) return;
            editInput.setText("");
            viewModel.clearEnviado();
        });
    }

    private void enviar() {
        String texto = editInput.getText() != null ? editInput.getText().toString() : "";
        viewModel.enviar(texto);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (viewModel == null || conversacionId == null) return;
        String nombre = new SessionManager(this).getUserName();
        viewModel.empezarEscucha(conversacionId, miUid, otroId, nombre);
    }

    @Override
    protected void onStop() {
        if (viewModel != null) {
            viewModel.detenerEscucha();
        }
        super.onStop();
    }
}
