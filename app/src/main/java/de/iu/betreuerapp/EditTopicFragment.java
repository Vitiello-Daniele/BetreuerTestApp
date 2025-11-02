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

public class EditTopicFragment extends Fragment {

    private EditText titleInput, descriptionInput;
    private Spinner areaSpinner, statusSpinner;
    private Button updateButton, cancelButton, deleteButton;
    private SharedViewModel sharedViewModel;
    private int topicPosition = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_topic, container, false);

        // Shared ViewModel holen
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // UI Elemente initialisieren
        titleInput = view.findViewById(R.id.title_input);
        descriptionInput = view.findViewById(R.id.description_input);
        areaSpinner = view.findViewById(R.id.area_spinner);
        statusSpinner = view.findViewById(R.id.status_spinner);
        updateButton = view.findViewById(R.id.update_button);
        cancelButton = view.findViewById(R.id.cancel_button);
        deleteButton = view.findViewById(R.id.delete_button);

        // Spinner einrichten
        setupSpinners();

        // Daten laden
        setDataFromArguments();

        // Button Listener
        updateButton.setOnClickListener(v -> updateTopic());
        cancelButton.setOnClickListener(v -> cancel());
        deleteButton.setOnClickListener(v -> deleteTopic());

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

    private void setDataFromArguments() {
        Bundle args = getArguments();
        if (args != null) {
            topicPosition = args.getInt("topic_position", -1);
            String title = args.getString("topic_title", "");
            String area = args.getString("topic_area", "");
            String status = args.getString("topic_status", "");
            String description = args.getString("topic_description", "");

            titleInput.setText(title);
            descriptionInput.setText(description);

            // Spinner auf korrekte Werte setzen
            setSpinnerSelection(areaSpinner, area);
            setSpinnerSelectionByStatus(statusSpinner, status);
        }
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void setSpinnerSelectionByStatus(Spinner spinner, String status) {
        String statusText = "";
        switch (status) {
            case "available": statusText = "Verfügbar"; break;
            case "taken": statusText = "Vergeben"; break;
            case "completed": statusText = "Abgeschlossen"; break;
        }
        setSpinnerSelection(spinner, statusText);
    }

    private void updateTopic() {
        if (topicPosition == -1) {
            Toast.makeText(requireContext(), "Fehler: Thema nicht gefunden", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = titleInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String area = areaSpinner.getSelectedItem().toString();
        String status = getStatusFromSpinner(statusSpinner.getSelectedItem().toString());

        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(requireContext(), "Bitte füllen Sie alle Pflichtfelder aus", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ ECHTES UPDATE über ViewModel
        SharedViewModel.Topic updatedTopic = new SharedViewModel.Topic(title, area, status, description);
        sharedViewModel.updateTopic(topicPosition, updatedTopic);

        Toast.makeText(requireContext(), "Thema erfolgreich aktualisiert", Toast.LENGTH_LONG).show();
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void deleteTopic() {
        if (topicPosition == -1) {
            Toast.makeText(requireContext(), "Fehler: Thema nicht gefunden", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ ECHTES LÖSCHEN über ViewModel
        sharedViewModel.deleteTopic(topicPosition);

        Toast.makeText(requireContext(), "Thema erfolgreich gelöscht", Toast.LENGTH_LONG).show();
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