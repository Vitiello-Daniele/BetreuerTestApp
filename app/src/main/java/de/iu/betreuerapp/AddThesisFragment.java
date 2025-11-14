package de.iu.betreuerapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddThesisFragment extends Fragment {

    private EditText titleInput;
    private EditText studentNameInput;
    private EditText descriptionInput;
    private Button saveButton;
    private Button cancelButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Nur Betreuer:innen dürfen Arbeiten anlegen
        if (!AuthGuard.requireRole(this, "tutor")) {
            return new View(requireContext());
        }

        View view = inflater.inflate(R.layout.fragment_add_thesis, container, false);

        titleInput = view.findViewById(R.id.title_input);
        studentNameInput = view.findViewById(R.id.student_name_input);
        descriptionInput = view.findViewById(R.id.description_input);
        saveButton = view.findViewById(R.id.save_button);
        cancelButton = view.findViewById(R.id.cancel_button);

        saveButton.setOnClickListener(v -> saveThesis());
        cancelButton.setOnClickListener(v -> cancel());

        return view;
    }

    private void saveThesis() {
        String title = titleInput.getText().toString().trim();
        String studentName = studentNameInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();

        // Validierung
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Bitte geben Sie einen Titel ein", Toast.LENGTH_SHORT).show();
            titleInput.requestFocus();
            return;
        }

        if (studentName.isEmpty()) {
            Toast.makeText(requireContext(), "Bitte geben Sie den Namen des Studierenden ein", Toast.LENGTH_SHORT).show();
            studentNameInput.requestFocus();
            return;
        }

        // Aktuelles Datum als Demo "Letzte Aktualisierung"
        String currentDate = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)
                .format(new Date());

        // Demo: hier würde ein echter API-Call (Supabase) kommen
        String newId = String.valueOf(System.currentTimeMillis());
        Log.d("AddThesis", "Neue Thesis erstellt: id=" + newId
                + ", title=" + title
                + ", student=" + studentName
                + ", status=in_discussion"
                + ", date=" + currentDate
                + ", desc=" + description);

        Toast.makeText(requireContext(),
                "Arbeit erfolgreich erstellt: " + title,
                Toast.LENGTH_LONG).show();

        // Zurück zum vorherigen Screen oder Dashboard
        FragmentManager fm = requireActivity().getSupportFragmentManager();
        if (!fm.popBackStackImmediate()) {
            fm.beginTransaction()
                    .replace(R.id.fragment_container, new DashboardFragment())
                    .commit();
        }
    }

    private void cancel() {
        // Abbrechen: Zurück oder Dashboard
        FragmentManager fm = requireActivity().getSupportFragmentManager();
        if (!fm.popBackStackImmediate()) {
            fm.beginTransaction()
                    .replace(R.id.fragment_container, new DashboardFragment())
                    .commit();
        }
    }
}
