package de.iu.betreuerapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private EditText searchInput;
    private Spinner expertiseSpinner;
    private Button searchButton;
    private TextView infoText;

    // ✅ LISTE MIT VERSCHIEDENEN BETREUERN
    private List<Supervisor> supervisorList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        // UI Elemente initialisieren
        searchInput = view.findViewById(R.id.search_input);
        expertiseSpinner = view.findViewById(R.id.expertise_spinner);
        searchButton = view.findViewById(R.id.search_button);
        infoText = view.findViewById(R.id.info_text);

        // ✅ INFO-TEXT SETZEN
        infoText.setText("IU Hochschule\nMobile Software Engineering II\nDLBCSEMSE02_D\nBetreuer-App\n\nGemeinsam entwickelt von: Ivan Paunovic und Danielle Vitiello");

        // Betreuer-Daten laden
        loadSupervisors();

        // Spinner mit Fachbereichen füllen
        setupExpertiseSpinner();

        // Such-Button Click Listener
        searchButton.setOnClickListener(v -> performSearch());

        return view;
    }

    private void loadSupervisors() {
        supervisorList = new ArrayList<>();

        // ✅ VERSCHIEDENE BETREUER HINZUFÜGEN
        supervisorList.add(new Supervisor("1", "Prof. Dr. Elena Weber", "elena.weber@iu.de", "Wirtschaftsinformatik", "Spezialisiert auf Mobile Development und Educational Technology. 10+ Jahre Erfahrung."));
        supervisorList.add(new Supervisor("2", "Dr. Markus Schmidt", "markus.schmidt@iu.de", "Data Science", "Experte für Machine Learning und KI. Forschungsschwerpunkt: Natural Language Processing."));
        supervisorList.add(new Supervisor("3", "Prof. Dr. Sarah Fischer", "sarah.fischer@iu.de", "IT-Sicherheit", "Cybersecurity-Spezialistin mit Fokus auf Cloud Security und Netzwerksicherheit."));
        supervisorList.add(new Supervisor("4", "Dr. Thomas Wagner", "thomas.wagner@iu.de", "Software Engineering", "Agile Methoden, DevOps und Continuous Integration. Industrieerfahrung bei Tech-Konzernen."));
        supervisorList.add(new Supervisor("5", "Prof. Dr. Anna Bauer", "anna.bauer@iu.de", "Datenbanken", "Big Data, NoSQL und verteilte Datenbanksysteme. Forschung zu Performance-Optimierung."));
        supervisorList.add(new Supervisor("6", "Dr. Michael Hoffmann", "michael.hoffmann@iu.de", "Web Development", "Full-Stack Entwicklung, Cloud Computing und Microservices-Architekturen."));
        supervisorList.add(new Supervisor("7", "Prof. Dr. Julia Schulz", "julia.schulz@iu.de", "Projektmanagement", "SCRUM, Agile Coaching und IT-Projektleitung. Zertifizierte Projektmanagerin."));
        supervisorList.add(new Supervisor("8", "Dr. Robert Klein", "robert.klein@iu.de", "Künstliche Intelligenz", "Computer Vision, Deep Learning und autonome Systeme. Publikationen in Top-Konferenzen."));
    }

    private void setupExpertiseSpinner() {
        // ArrayAdapter für den Spinner erstellen
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.expertise_areas,
                android.R.layout.simple_spinner_item
        );

        // Layout für die Dropdown-Ansicht
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Adapter zum Spinner hinzufügen
        expertiseSpinner.setAdapter(adapter);
    }

    private void performSearch() {
        String searchTerm = searchInput.getText().toString().trim().toLowerCase();
        String expertise = expertiseSpinner.getSelectedItem().toString();

        // ✅ ECHTE SUCHE DURCHFÜHREN
        Supervisor foundSupervisor = searchSupervisors(searchTerm, expertise);

        if (foundSupervisor != null) {
            // Zur Profilansicht mit gefundenem Betreuer navigieren
            navigateToSupervisorProfile(foundSupervisor);
        } else {
            // Kein Betreuer gefunden
            Toast.makeText(requireContext(), "Kein Betreuer gefunden für: " + searchTerm, Toast.LENGTH_LONG).show();
        }
    }

    private Supervisor searchSupervisors(String searchTerm, String expertise) {
        // ✅ SUCHALGORITHMUS
        List<Supervisor> matchingSupervisors = new ArrayList<>();

        for (Supervisor supervisor : supervisorList) {
            boolean matchesSearch = searchTerm.isEmpty() ||
                    supervisor.getName().toLowerCase().contains(searchTerm) ||
                    supervisor.getExpertise().toLowerCase().contains(searchTerm) ||
                    supervisor.getDescription().toLowerCase().contains(searchTerm);

            boolean matchesExpertise = expertise.equals("Alle Bereiche") ||
                    supervisor.getExpertise().equals(expertise);

            if (matchesSearch && matchesExpertise) {
                matchingSupervisors.add(supervisor);
            }
        }

        // Rückgabe des ersten passenden Betreuers (oder null falls keiner)
        if (!matchingSupervisors.isEmpty()) {
            android.util.Log.d("Search", "Gefundene Betreuer: " + matchingSupervisors.size());
            return matchingSupervisors.get(0); // Ersten Betreuer zurückgeben
        }

        return null;
    }

    private void navigateToSupervisorProfile(Supervisor supervisor) {
        // SupervisorProfileFragment erstellen
        SupervisorProfileFragment profileFragment = new SupervisorProfileFragment();

        // ✅ BETREUER-DATEN ÜBERGEBEN
        Bundle args = new Bundle();
        args.putString("supervisor_name", supervisor.getName());
        args.putString("supervisor_email", supervisor.getEmail());
        args.putString("supervisor_expertise", supervisor.getExpertise());
        args.putString("supervisor_description", supervisor.getDescription());
        profileFragment.setArguments(args);

        // Fragment wechseln
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, profileFragment)
                .addToBackStack("search")
                .commit();

        android.util.Log.d("Search", "Zeige Profil von: " + supervisor.getName());
    }

    // ✅ INNERE KLASSE FÜR BETREUER-DATEN
    private static class Supervisor {
        private String id;
        private String name;
        private String email;
        private String expertise;
        private String description;

        public Supervisor(String id, String name, String email, String expertise, String description) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.expertise = expertise;
            this.description = description;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getExpertise() { return expertise; }
        public String getDescription() { return description; }
    }
}