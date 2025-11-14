package de.iu.betreuerapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {

    private ListView thesesListView;
    private Spinner statusFilterSpinner;
    private TextView summaryText;

    private ArrayAdapter<Thesis> adapter;
    private final List<Thesis> allTheses = new ArrayList<>();
    private final List<Thesis> currentDisplayList = new ArrayList<>();

    private static final String STATUS_IN_DISCUSSION = "in_discussion";
    private static final String STATUS_REGISTERED = "registered";
    private static final String STATUS_SUBMITTED = "submitted";
    private static final String STATUS_COLLOQUIUM = "colloquium_held";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Nur Betreuer:innen dürfen das Dashboard sehen
        if (!AuthGuard.requireRole(this, "tutor")) {
            return new View(requireContext());
        }

        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        thesesListView = view.findViewById(R.id.theses_listview);
        statusFilterSpinner = view.findViewById(R.id.status_filter_spinner);
        summaryText = view.findViewById(R.id.summary_text);

        Button addThesisButton = view.findViewById(R.id.add_thesis_button);
        Button refreshButton = view.findViewById(R.id.refresh_button);

        addThesisButton.setOnClickListener(v -> addNewThesis());
        refreshButton.setOnClickListener(v -> refreshData());

        // Demo-Daten laden (später: Supabase)
        loadSampleData();

        // Filter-Spinner + Liste einrichten
        setupStatusFilter();
        setupThesesList(allTheses);
        updateSummary(allTheses);

        return view;
    }

    /**
     * Demo-Daten für betreute Arbeiten.
     * (Kann später durch echte API-Daten ersetzt werden.)
     */
    private void loadSampleData() {
        allTheses.clear();

        allTheses.add(new Thesis("1", "Entwicklung einer Betreuer-App",
                "Max Mustermann", STATUS_IN_DISCUSSION, "15.03.2024"));

        allTheses.add(new Thesis("2", "Machine Learning in Apps",
                "Anna Schmidt", STATUS_REGISTERED, "20.02.2024"));

        allTheses.add(new Thesis("3", "IT-Sicherheit Analyse",
                "Tom Weber", STATUS_SUBMITTED, "10.01.2024"));

        allTheses.add(new Thesis("4", "Cloud Computing Studie",
                "Lisa Fischer", STATUS_IN_DISCUSSION, "05.03.2024"));

        allTheses.add(new Thesis("5", "Datenbank Optimierung",
                "David Becker", STATUS_COLLOQUIUM, "15.12.2023"));
    }

    private void setupStatusFilter() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.thesis_statuses,                  // "Alle", "In Abstimmung", ...
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusFilterSpinner.setAdapter(adapter);

        statusFilterSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent,
                                       View view, int position, long id) {
                if (DashboardFragment.this.adapter != null) {
                    filterThesesByStatus(position);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // nichts
            }
        });
    }

    private void setupThesesList(List<Thesis> theses) {
        currentDisplayList.clear();
        currentDisplayList.addAll(theses);

        adapter = new ArrayAdapter<Thesis>(
                requireContext(),
                android.R.layout.simple_list_item_2,
                android.R.id.text1,
                currentDisplayList
        ) {
            @NonNull
            @Override
            public View getView(int position,
                                @Nullable View convertView,
                                @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);

                Thesis thesis = getItem(position);
                if (thesis != null) {
                    text1.setText(thesis.getTitle());
                    text2.setText(
                            "Student: " + thesis.getStudentName()
                                    + " | Status: " + getStatusText(thesis.getStatus())
                                    + " | " + thesis.getLastUpdate()
                    );
                }
                return view;
            }
        };

        thesesListView.setAdapter(adapter);

        thesesListView.setOnItemClickListener((parent, view, position, id) -> {
            Thesis selectedThesis = currentDisplayList.get(position);
            openThesisDetails(selectedThesis);
        });
    }

    /**
     * Filtert die Liste nach dem im Spinner gewählten Status.
     */
    private void filterThesesByStatus(int statusPosition) {
        List<Thesis> filteredList = new ArrayList<>();

        if (statusPosition == 0) {
            // "Alle"
            filteredList.addAll(allTheses);
        } else {
            String selectedFilter = null;
            switch (statusPosition) {
                case 1:
                    selectedFilter = STATUS_IN_DISCUSSION;
                    break;
                case 2:
                    selectedFilter = STATUS_REGISTERED;
                    break;
                case 3:
                    selectedFilter = STATUS_SUBMITTED;
                    break;
                case 4:
                    selectedFilter = STATUS_COLLOQUIUM;
                    break;
            }

            if (selectedFilter != null) {
                for (Thesis thesis : allTheses) {
                    if (selectedFilter.equals(thesis.getStatus())) {
                        filteredList.add(thesis);
                    }
                }
            }
        }

        currentDisplayList.clear();
        currentDisplayList.addAll(filteredList);

        adapter.clear();
        adapter.addAll(filteredList);
        adapter.notifyDataSetChanged();

        updateSummary(filteredList);
    }

    /**
     * Aktualisiert die Zusammenfassungsanzeige.
     */
    private void updateSummary(List<Thesis> theses) {
        long inDiscussion = 0;
        long registered = 0;
        long submitted = 0;
        long colloquium = 0;

        for (Thesis thesis : theses) {
            switch (thesis.getStatus()) {
                case STATUS_IN_DISCUSSION:
                    inDiscussion++;
                    break;
                case STATUS_REGISTERED:
                    registered++;
                    break;
                case STATUS_SUBMITTED:
                    submitted++;
                    break;
                case STATUS_COLLOQUIUM:
                    colloquium++;
                    break;
            }
        }

        String summary = String.format(
                "Zusammenfassung:\nIn Abstimmung: %d | Angemeldet: %d\nAbgegeben: %d | Kolloquium: %d | Gesamt: %d",
                inDiscussion, registered, submitted, colloquium, theses.size()
        );

        summaryText.setText(summary);
    }

    private String getStatusText(String status) {
        if (status == null) return "Unbekannt";
        switch (status) {
            case STATUS_IN_DISCUSSION:
                return "In Abstimmung";
            case STATUS_REGISTERED:
                return "Angemeldet";
            case STATUS_SUBMITTED:
                return "Abgegeben";
            case STATUS_COLLOQUIUM:
                return "Kolloquium";
            default:
                return "Unbekannt";
        }
    }

    private void openThesisDetails(Thesis thesis) {
        navigateToStatusUpdate(thesis);
    }

    private void navigateToStatusUpdate(Thesis thesis) {
        StatusUpdateFragment statusFragment = new StatusUpdateFragment();

        Bundle args = new Bundle();
        args.putString("thesis_id", thesis.getId());
        args.putString("thesis_title", thesis.getTitle());
        args.putString("student_name", thesis.getStudentName());
        args.putString("current_status", thesis.getStatus());
        statusFragment.setArguments(args);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, statusFragment)
                .addToBackStack("dashboard")
                .commit();
    }

    private void addNewThesis() {
        AddThesisFragment addThesisFragment = new AddThesisFragment();

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, addThesisFragment)
                .addToBackStack("dashboard")
                .commit();
    }

    private void refreshData() {
        // In der Demo nur Anzeige aktualisieren
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        Toast.makeText(requireContext(), "Daten aktualisiert", Toast.LENGTH_SHORT).show();
    }

    /**
     * Interne Modellklasse für eine Abschlussarbeit (nur Demo).
     */
    private static class Thesis {
        private final String id;
        private final String title;
        private final String studentName;
        private final String status;
        private final String lastUpdate;

        Thesis(String id, String title, String studentName, String status, String lastUpdate) {
            this.id = id;
            this.title = title;
            this.studentName = studentName;
            this.status = status;
            this.lastUpdate = lastUpdate;
        }

        String getId() {
            return id;
        }

        String getTitle() {
            return title;
        }

        String getStudentName() {
            return studentName;
        }

        String getStatus() {
            return status;
        }

        String getLastUpdate() {
            return lastUpdate;
        }

        @NonNull
        @Override
        public String toString() {
            return title;
        }
    }
}
