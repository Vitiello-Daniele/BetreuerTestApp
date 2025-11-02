package de.iu.betreuerapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddThesisFragment extends Fragment {

    private EditText titleInput, studentNameInput, descriptionInput;
    private Button saveButton, cancelButton;
    private SharedViewModel sharedViewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_thesis, container, false);

        // Shared ViewModel holen
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // UI Elemente initialisieren
        titleInput = view.findViewById(R.id.title_input);
        studentNameInput = view.findViewById(R.id.student_name_input);
        descriptionInput = view.findViewById(R.id.description_input);
        saveButton = view.findViewById(R.id.save_button);
        cancelButton = view.findViewById(R.id.cancel_button);

        // Button Listener
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
            Toast.makeText(requireContext(), "Bitte geben Sie den Studentennamen ein", Toast.LENGTH_SHORT).show();
            studentNameInput.requestFocus();
            return;
        }

        // Aktuelles Datum für "Last Update"
        String currentDate = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(new Date());

        // Neue Arbeit erstellen
        String newId = String.valueOf(System.currentTimeMillis());
        SharedViewModel.Thesis newThesis = new SharedViewModel.Thesis(
                newId,
                title,
                studentName,
                "in_discussion", // Immer "In Abstimmung" als Start-Status
                currentDate
        );

        // Zur Liste hinzufügen
        sharedViewModel.addThesis(newThesis);

        // Erfolgsmeldung
        Toast.makeText(requireContext(), "Arbeit erfolgreich erstellt: " + title, Toast.LENGTH_LONG).show();

        // Zurück zum Dashboard
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void cancel() {
        // Zurück zum Dashboard ohne Speichern
        requireActivity().getSupportFragmentManager().popBackStack();
    }
}