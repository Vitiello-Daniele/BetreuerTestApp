package de.iu.betreuerapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

public class AddTopicFragment extends Fragment {

    private EditText titleInput, descriptionInput;
    private Spinner areaSpinner, statusSpinner;
    private Button saveButton, cancelButton;
    private SharedViewModel sharedViewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_topic, container, false);

        // Shared ViewModel holen
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // UI Elemente initialisieren
        titleInput = view.findViewById(R.id.title_input);
        descriptionInput = view.findViewById(R.id.description_input);
        areaSpinner = view.findViewById(R.id.area_spinner);
        statusSpinner = view.findViewById(R.id.status_spinner);
        saveButton = view.findViewById(R.id.save_button);
        cancelButton = view.findViewById(R.id.cancel_button);

        // Spinner einrichten
        setupSpinners();

        // Button Listener
        saveButton.setOnClickListener(v -> saveTopic());
        cancelButton.setOnClickListener(v -> cancel());

        return view;
    }

    private void setupSpinners() {
        // Fachbereich-Spinner
        ArrayAdapter<CharSequence> areaAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.expertise_areas,
                android.R.layout.simple_spinner_item
        );
        areaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        areaSpinner.setAdapter(areaAdapter);

        // Status-Spinner
        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.topic_status_values,
                android.R.layout.simple_spinner_item
        );
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(statusAdapter);
    }

    private void saveTopic() {
        String title = titleInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String area = areaSpinner.getSelectedItem().toString();
        String status = getStatusFromSpinner(statusSpinner.getSelectedItem().toString());

        // Validierung
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Bitte geben Sie einen Titel ein", Toast.LENGTH_SHORT).show();
            return;
        }

        if (description.isEmpty()) {
            Toast.makeText(requireContext(), "Bitte geben Sie eine Beschreibung ein", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ ECHTES SPEICHERN über ViewModel
        SharedViewModel.Topic newTopic = new SharedViewModel.Topic(title, area, status, description);
        sharedViewModel.addTopic(newTopic);

        // Erfolgsmeldung
        Toast.makeText(requireContext(), "Thema erfolgreich erstellt: " + title, Toast.LENGTH_LONG).show();

        // Zurück zur Themen-Liste
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private String getStatusFromSpinner(String spinnerText) {
        switch (spinnerText) {
            case "Verfügbar": return "available";
            case "Vergeben": return "taken";
            case "Abgeschlossen": return "completed";
            default: return "available";
        }
    }

    private void cancel() {
        requireActivity().getSupportFragmentManager().popBackStack();
    }
}