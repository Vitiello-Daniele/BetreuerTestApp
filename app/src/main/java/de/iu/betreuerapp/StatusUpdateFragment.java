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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class StatusUpdateFragment extends Fragment {

    private TextView thesisTitleText;
    private TextView studentNameText;
    private TextView currentStatusText;
    private Spinner newStatusSpinner;
    private Button updateStatusButton;

    private String thesisId;
    private String currentStatus;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Nur für Betreuer:innen zugänglich
        if (!AuthGuard.requireRole(this, "tutor")) {
            return new View(requireContext());
        }

        View view = inflater.inflate(R.layout.fragment_status_update, container, false);

        thesisTitleText = view.findViewById(R.id.thesis_title_text);
        studentNameText = view.findViewById(R.id.student_name_text);
        currentStatusText = view.findViewById(R.id.current_status_text);
        newStatusSpinner = view.findViewById(R.id.new_status_spinner);
        updateStatusButton = view.findViewById(R.id.update_status_button);

        setDataFromArguments();
        setupStatusSpinner();

        updateStatusButton.setOnClickListener(v -> updateThesisStatus());

        return view;
    }

    /**
     * Liest die übergebenen Thesis-Daten aus dem Bundle oder setzt Demo-Werte.
     */
    private void setDataFromArguments() {
        Bundle args = getArguments();
        if (args != null) {
            thesisId = args.getString("thesis_id", "");
            String thesisTitle = args.getString("thesis_title", "Unbekannte Arbeit");
            String studentName = args.getString("student_name", "Unbekannter Student");
            currentStatus = args.getString("current_status", "in_discussion");

            thesisTitleText.setText(thesisTitle);
            studentNameText.setText("Student: " + studentName);
            currentStatusText.setText("Aktueller Status: " + getStatusText(currentStatus));
        } else {
            // Fallback-Demo-Daten
            thesisId = "1";
            currentStatus = "in_discussion";
            thesisTitleText.setText("Entwicklung einer Betreuer-App");
            studentNameText.setText("Student: Max Mustermann");
            currentStatusText.setText("Aktueller Status: In Abstimmung");
        }
    }

    /**
     * Spinner mit möglichen Statusübergängen befüllen.
     */
    private void setupStatusSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.status_transitions,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        newStatusSpinner.setAdapter(adapter);
    }

    /**
     * Behandelt den Klick auf "Status aktualisieren".
     * Aktuell Demo: zeigt Toast + geht zurück.
     * (Später hier: Update in Supabase / Liste im Dashboard refreshen.)
     */
    private void updateThesisStatus() {
        String spinnerText = (String) newStatusSpinner.getSelectedItem();
        String newStatus = getStatusFromSpinner(spinnerText);

        if (thesisId == null || thesisId.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Fehler: Keine Thesis-ID vorhanden.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Hier könnte ein echter API-Call kommen (Supabase, etc.)
        android.util.Log.d("StatusUpdate",
                "Thesis " + thesisId + " Status geändert von " +
                        currentStatus + " zu " + newStatus);

        Toast.makeText(requireContext(),
                "Status erfolgreich aktualisiert!",
                Toast.LENGTH_LONG).show();

        // Zurück zur vorherigen Ansicht
        requireActivity()
                .getSupportFragmentManager()
                .popBackStack();
    }

    /**
     * Mappt die Anzeige-Texte im Spinner auf interne Status-Codes.
     */
    private String getStatusFromSpinner(String spinnerText) {
        if (spinnerText == null) return "in_discussion";

        switch (spinnerText) {
            case "In Abstimmung":
                return "in_discussion";
            case "Angemeldet":
                return "registered";
            case "Abgegeben":
                return "submitted";
            case "Kolloquium gehalten":
                return "colloquium_held";
            default:
                return "in_discussion";
        }
    }

    /**
     * Mappt interne Status-Codes auf lesbare Texte.
     */
    private String getStatusText(String status) {
        if (status == null) return "unbekannt";

        switch (status) {
            case "in_discussion":
                return "In Abstimmung";
            case "registered":
                return "Angemeldet";
            case "submitted":
                return "Abgegeben";
            case "colloquium_held":
                return "Kolloquium gehalten";
            default:
                return status;
        }
    }
}
