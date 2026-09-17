package com.example.myapplication.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapters.MessageAdapter;
import com.example.myapplication.models.Message;
import com.example.myapplication.repositories.MessageRepository;
import com.example.myapplication.services.SessionManager;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {

    private MessageRepository messageRepository;
    private SessionManager sessionManager;
    private MessageAdapter adapter;
    private RecyclerView recyclerMessages;
    private EditText editMessage;
    private String userCurrentName;
    private String userCurrentId;
    private ListenerRegistration messageRegistration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        messageRepository = new MessageRepository();
        sessionManager = new SessionManager(requireContext());
        userCurrentName = sessionManager.getUserName();
        userCurrentId = sessionManager.getUserId();

        recyclerMessages = view.findViewById(R.id.recyclerMessages);
        editMessage = view.findViewById(R.id.editMessage);

        recyclerMessages.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MessageAdapter(new ArrayList<>(), userCurrentId);
        recyclerMessages.setAdapter(adapter);

        View btnSend = view.findViewById(R.id.btnSend);
        if (btnSend != null) {
            btnSend.setOnClickListener(v -> enviarMensaje());
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarMensajes();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (messageRegistration != null) {
            messageRegistration.remove();
            messageRegistration = null;
        }
    }

    private void cargarMensajes() {
        String uid = userCurrentId;
        if (uid == null) return;

        if (messageRegistration != null) {
            messageRegistration.remove();
            messageRegistration = null;
        }

        messageRegistration = messageRepository.escucharMensajes(uid, messages -> {
            if (!isAdded() || getView() == null || recyclerMessages == null || adapter == null) return;
            // Actualizar adapter con los mensajes recibidos sin recrearlo
            adapter.updateData(messages);
            if (adapter.getItemCount() > 0) {
                recyclerMessages.scrollToPosition(adapter.getItemCount() - 1);
            }
        });
    }

    public void actualizarListaMensajes() {
        // Método público para refrescar la lista
        cargarMensajes();
    }

    private void enviarMensaje() {
        if (editMessage == null) return;
        String texto = editMessage.getText().toString().trim();
        if (texto.isEmpty()) {
            Toast.makeText(requireContext(), "Escriba un mensaje", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = sessionManager.getUserId();
        if (uid == null) return;

        Message mensaje = new Message();
        mensaje.setRemitenteId(uid);
        mensaje.setReceptorId(""); // En chat bilateral, el receptor se determina por contexto
        mensaje.setMensaje(texto);
        mensaje.setTipo("CHAT");
        mensaje.setFecha(System.currentTimeMillis());
        mensaje.setLeido(false);

        // Si tenemos nombre del usuario actual
        String nombreActual = sessionManager.getUserName();
        if (nombreActual != null) {
            mensaje.setNombreRemitente(nombreActual);
        }

        messageRepository.crearMensaje(mensaje, () -> {
            if (!isAdded() || getView() == null) return;
            editMessage.setText("");
            // No es necesario llamar a actualizarListaMensajes aquí porque el snapshot listener actualiza en tiempo real
            Toast.makeText(requireContext(), "Mensaje enviado", Toast.LENGTH_SHORT).show();
        });
    }
}
