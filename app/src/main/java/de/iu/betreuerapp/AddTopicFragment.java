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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.util.List;

import de.iu.betreuerapp.dto.Topic;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddTopicFragment extends Fragment {

    private EditText titleInput;
    private EditText descriptionInput;
    private Spinner areaSpinner;
    private Button saveButton;
    private Button cancelButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Nur Betreuer:innen dürfen Themen anlegen
        if (!AuthGuard.requireRole(this, "tutor")) {
            return new View(requireContext());
        }

        View view = inflater.inflate(R.layout.fragment_add_topic, container, false);

        titleInput       = view.findViewById(R.id.title_input);
        descriptionInput = view.findViewById(R.id.description_input);
        areaSpinner      = view.findViewById(R.id.area_spinner);
        saveButton       = view.findViewById(R.id.save_button);
        cancelButton     = view.findViewById(R.id.cancel_button);

        setupAreaSpinner();

        saveButton.setOnClickListener(v -> saveTopic());
        cancelButton.setOnClickListener(v -> navigateBackToTopics());

        return view;
    }

    /**
     * Füllt den Fachbereich-Spinner mit Werten aus resources (R.array.expertise_areas).
     */
    private void setupAreaSpinner() {
        ArrayAdapter<CharSequence> areaAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.expertise_areas,
                android.R.layout.simple_spinner_item
        );
        areaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        areaSpinner.setAdapter(areaAdapter);
    }

    private void saveTopic() {
        String title       = titleInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String area        = (areaSpinner.getSelectedItem() != null)
                ? areaSpinner.getSelectedItem().toString()
                : "";

        if (title.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Bitte geben Sie einen Titel ein",
                    Toast.LENGTH_SHORT).show();
            titleInput.requestFocus();
            return;
        }

        if (description.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Bitte geben Sie eine Beschreibung ein",
                    Toast.LENGTH_SHORT).show();
            descriptionInput.requestFocus();
            return;
        }

        // >>> NEU: "Alle Bereiche/Berreiche" darf NICHT als Fachbereich verwendet werden
        if ("Alle Bereiche".equalsIgnoreCase(area)
                || "Alle Berreiche".equalsIgnoreCase(area)) {
            Toast.makeText(requireContext(),
                    "Bitte wählen Sie einen konkreten Fachbereich aus.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        // <<<

        SessionManager sm = new SessionManager(requireContext());
        String tutorId = sm.userId();
        if (tutorId == null) {
            Toast.makeText(requireContext(),
                    "Fehler: Benutzer nicht erkannt. Bitte neu anmelden.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Topic-Objekt für Supabase
        Topic t = new Topic();
        t.title       = title;
        t.description = description;
        t.area        = area;
        t.status      = "available";   // neu erstellte Themen sind immer verfügbar
        t.owner_id    = tutorId;       // gehört dem eingeloggten Tutor

        SupabaseClient client = new SupabaseClient(requireContext());
        saveButton.setEnabled(false);

        client.restService()
                .createTopic(t)
                .enqueue(new Callback<List<Topic>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Topic>> call,
                                           @NonNull Response<List<Topic>> response) {
                        saveButton.setEnabled(true);

                        if (!response.isSuccessful()
                                || response.body() == null
                                || response.body().isEmpty()) {
                            Toast.makeText(requireContext(),
                                    "Fehler beim Erstellen des Themas: " + response.code(),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        Toast.makeText(requireContext(),
                                "Thema erstellt: " + title,
                                Toast.LENGTH_LONG).show();

                        navigateBackToTopics();
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<Topic>> call,
                                          @NonNull Throwable t) {
                        saveButton.setEnabled(true);
                        Toast.makeText(requireContext(),
                                "Netzwerkfehler: " + t.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
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
