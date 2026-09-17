package com.example.myapplication.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapters.AuditAdapter;
import com.example.myapplication.models.AuditoriaLog;
import com.example.myapplication.repositories.AuditRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class AuditFragment extends Fragment {

    private final List<AuditoriaLog> logs = new ArrayList<>();
    private AuditAdapter adapter;
    private AuditRepository repo;
    private ListenerRegistration registration;

    private RecyclerView recycler;
    private TextView txtEmpty;
    private ProgressBar progress;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_audit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recycler = view.findViewById(R.id.recycler_audit);
        txtEmpty = view.findViewById(R.id.txt_audit_empty);
        progress = view.findViewById(R.id.progress_audit);

        repo = new AuditRepository();
        adapter = new AuditAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        progress.setVisibility(View.VISIBLE);
        registration = repo.escucharLogs(this::onLogsActualizados);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    private void onLogsActualizados(@Nullable QuerySnapshot snapshots,
                                    @Nullable com.google.firebase.firestore.FirebaseFirestoreException e) {
        progress.setVisibility(View.GONE);
        if (e != null) {
            txtEmpty.setText("No se pudieron cargar los registros.\n" + e.getMessage());
            txtEmpty.setVisibility(View.VISIBLE);
            return;
        }
        logs.clear();
        if (snapshots != null) {
            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                AuditoriaLog log = doc.toObject(AuditoriaLog.class);
                if (log != null) {
                    log.setId(doc.getId());
                    logs.add(log);
                }
            }
        }
        adapter.setData(logs);
        txtEmpty.setVisibility(logs.isEmpty() ? View.VISIBLE : View.GONE);
    }
}