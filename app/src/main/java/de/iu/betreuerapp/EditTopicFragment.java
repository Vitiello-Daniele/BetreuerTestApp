package de.iu.betreuerapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

public class EditTopicFragment extends Fragment {

    private EditText titleInput;
    private EditText descriptionInput;
    private Spinner areaSpinner;
    private Spinner statusSpinner;
    private Button updateButton;
    private Button cancelButton;
    private Button deleteButton;

    private int topicPosition = -1;
    private String originalTitle;
    private String originalArea;
    private String originalStatus;
    private String originalDescription;

    private SharedViewModel sharedViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Nur Betreuer:innen dürfen Themen bearbeiten
        if (!AuthGuard.requireRole(this, "tutor")) {
            return new View(requireContext());
        }

        View view = inflater.inflate(R.layout.fragment_edit_topic, container, false);

        titleInput = view.findViewById(R.id.title_input);
        descriptionInput = view.findViewById(R.id.description_input);
        areaSpinner = view.findViewById(R.id.area_spinner);
        statusSpinner = view.findViewById(R.id.status_spinner);
        updateButton = view.findViewById(R.id.update_button);
        cancelButton = view.findViewById(R.id.cancel_button);
        deleteButton = view.findViewById(R.id.delete_button);

        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        setupSpinners();
        setDataFromArguments();

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

    /**
     * Erwartet:
     * - topic_position
     * - topic_title
     * - topic_area
     * - topic_status
     * - topic_description
     */
    private void setDataFromArguments() {
        Bundle args = getArguments();
        if (args != null) {
            topicPosition = args.getInt("topic_position", -1);
            originalTitle = args.getString("topic_title", "");
            originalArea = args.getString("topic_area", "");
            originalStatus = args.getString("topic_status", "");
            originalDescription = args.getString("topic_description", "");

            titleInput.setText(originalTitle);
            descriptionInput.setText(originalDescription);

            setSpinnerSelection(areaSpinner, originalArea);
            setSpinnerSelectionByStatus(statusSpinner, originalStatus);
        } else {
            Toast.makeText(requireContext(),
                    "Fehler: Keine Themendaten übergeben.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        if (value == null) return;
        for (int i = 0; i < spinner.getCount(); i++) {
            if (value.equals(spinner.getItemAtPosition(i).toString())) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void setSpinnerSelectionByStatus(Spinner spinner, String status) {
        String statusText;
        switch (status) {
            case "available":
                statusText = "Verfügbar";
                break;
            case "taken":
                statusText = "Vergeben";
                break;
            case "completed":
                statusText = "Abgeschlossen";
                break;
            default:
                statusText = "Verfügbar";
                break;
        }
        setSpinnerSelection(spinner, statusText);
    }

    private void updateTopic() {
        if (topicPosition == -1) {
            Toast.makeText(requireContext(),
                    "Fehler: Thema nicht gefunden.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String newTitle = titleInput.getText().toString().trim();
        String newDescription = descriptionInput.getText().toString().trim();
        String newArea = areaSpinner.getSelectedItem() != null
                ? areaSpinner.getSelectedItem().toString()
                : "";
        String newStatus = getStatusFromSpinner(
                statusSpinner.getSelectedItem() != null
                        ? statusSpinner.getSelectedItem().toString()
                        : ""
        );

        if (newTitle.isEmpty() || newDescription.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Bitte füllen Sie alle Pflichtfelder aus.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        SharedViewModel.Topic updated = new SharedViewModel.Topic(
                newTitle,
                newArea,
                newStatus,
                newDescription
        );

        sharedViewModel.updateTopic(topicPosition, updated);

        Log.d("EditTopic", "Thema aktualisiert (Index " + topicPosition + "): "
                + newTitle + " [" + newArea + ", " + newStatus + "]");

        Toast.makeText(requireContext(),
                "Thema erfolgreich aktualisiert.",
                Toast.LENGTH_LONG).show();

        navigateBackToTopics();
    }

    private void deleteTopic() {
        if (topicPosition == -1) {
            Toast.makeText(requireContext(),
                    "Fehler: Thema nicht gefunden.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        sharedViewModel.deleteTopic(topicPosition);

        Log.d("EditTopic", "Thema gelöscht (Index " + topicPosition + "): "
                + originalTitle);

        Toast.makeText(requireContext(),
                "Thema erfolgreich gelöscht.",
                Toast.LENGTH_LONG).show();

        navigateBackToTopics();
    }

    private String getStatusFromSpinner(String spinnerText) {
        if (spinnerText == null) return "available";

        switch (spinnerText) {
            case "Verfügbar":
                return "available";
            case "Vergeben":
                return "taken";
            case "Abgeschlossen":
                return "completed";
            default:
                return "available";
        }
    }

    private void cancel() {
        navigateBackToTopics();
    }

    private void navigateBackToTopics() {
        FragmentManager fm = requireActivity().getSupportFragmentManager();
        if (!fm.popBackStackImmediate()) {
            fm.beginTransaction()
                    .replace(R.id.fragment_container, new TopicsFragment())
                    .commit();
        }
    }
}
