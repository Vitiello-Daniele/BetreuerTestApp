package de.iu.betreuerapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

public class StatusUpdateFragment extends Fragment {

    private TextView thesisTitleText, studentNameText, currentStatusText;
    private Spinner newStatusSpinner;
    private Button updateStatusButton;

    private String thesisId;
    private SharedViewModel sharedViewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_status_update, container, false);

        // Shared ViewModel holen
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // UI Elemente initialisieren
        thesisTitleText = view.findViewById(R.id.thesis_title_text);
        studentNameText = view.findViewById(R.id.student_name_text);
        currentStatusText = view.findViewById(R.id.current_status_text);
        newStatusSpinner = view.findViewById(R.id.new_status_spinner);
        updateStatusButton = view.findViewById(R.id.update_status_button);

        // Daten setzen
        setDataFromArguments();

        // Status-Spinner einrichten
        setupStatusSpinner();

        // Update-Button Listener
        updateStatusButton.setOnClickListener(v -> updateThesisStatus());

        return view;
    }

    private void setDataFromArguments() {
        Bundle args = getArguments();
        if (args != null) {
            thesisId = args.getString("thesis_id", "");
            String thesisTitle = args.getString("thesis_title", "Unbekannte Arbeit");
            String studentName = args.getString("student_name", "Unbekannter Student");
            String currentStatus = args.getString("current_status", "unbekannt");

            thesisTitleText.setText(thesisTitle);
            studentNameText.setText("Student: " + studentName);
            currentStatusText.setText("Aktueller Status: " + getStatusText(currentStatus));
        } else {
            thesisId = "1";
            thesisTitleText.setText("Entwicklung einer Betreuer-App");
            studentNameText.setText("Student: Max Mustermann");
            currentStatusText.setText("Aktueller Status: In Abstimmung");
        }
    }

    private void setupStatusSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.status_transitions,
                android.R.layout.simple_spinner_item
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        newStatusSpinner.setAdapter(adapter);
    }

    private void updateThesisStatus() {
        String newStatus = getStatusFromSpinner(newStatusSpinner.getSelectedItem().toString());

        // ✅ GLOBALE AKTUALISIERUNG über SharedViewModel
        if (thesisId != null) {
            sharedViewModel.updateThesisStatus(thesisId, newStatus);
            android.util.Log.d("StatusUpdate", "Status global aktualisiert: " + thesisId + " -> " + newStatus);
        } else {
            android.util.Log.e("StatusUpdate", "Keine ThesisId verfügbar");
        }

        Toast.makeText(requireContext(), "Status erfolgreich aktualisiert!", Toast.LENGTH_LONG).show();

        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private String getStatusFromSpinner(String spinnerText) {
        switch (spinnerText) {
            case "In Abstimmung": return "in_discussion";
            case "Angemeldet": return "registered";
            case "Abgegeben": return "submitted";
            case "Kolloquium gehalten": return "colloquium_held";
            default: return "in_discussion";
        }
    }

    private String getStatusText(String status) {
        switch (status) {
            case "in_discussion": return "In Abstimmung";
            case "registered": return "Angemeldet";
            case "submitted": return "Abgegeben";
            case "colloquium_held": return "Kolloquium gehalten";
            default: return status;
        }
    }
}