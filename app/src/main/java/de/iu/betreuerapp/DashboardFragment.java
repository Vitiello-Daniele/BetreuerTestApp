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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {

    private ListView thesesListView;
    private Spinner statusFilterSpinner;
    private TextView summaryText;
    private ArrayAdapter<SharedViewModel.Thesis> adapter;
    private SharedViewModel sharedViewModel;
    private List<SharedViewModel.Thesis> currentDisplayList = new ArrayList<>();

    private static final String STATUS_IN_DISCUSSION = "in_discussion";
    private static final String STATUS_REGISTERED = "registered";
    private static final String STATUS_SUBMITTED = "submitted";
    private static final String STATUS_COLLOQUIUM = "colloquium_held";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // ✅ NEU: Header und Autoren-Text setzen
        TextView iuHeader = view.findViewById(R.id.iu_header);
        TextView authorsText = view.findViewById(R.id.authors_text);

        iuHeader.setText("IU Hochschule\nMobile Software Engineering II\nDLBCSEMSE02_D\nBetreuer-App");
        authorsText.setText("Geschrieben von Danielle Vitiello und Ivan Paunovic");

        // Shared ViewModel holen
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // UI Elemente initialisieren
        thesesListView = view.findViewById(R.id.theses_listview);
        statusFilterSpinner = view.findViewById(R.id.status_filter_spinner);
        summaryText = view.findViewById(R.id.summary_text);

        // ✅ NEU: Button Listener hinzufügen
        Button addThesisButton = view.findViewById(R.id.add_thesis_button);
        addThesisButton.setOnClickListener(v -> addNewThesis());

        Button refreshButton = view.findViewById(R.id.refresh_button);
        refreshButton.setOnClickListener(v -> refreshData());

        // Filter und Liste einrichten
        setupStatusFilter();
        setupThesesListObserver();

        return view;
    }

    private void setupStatusFilter() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.thesis_statuses,
                android.R.layout.simple_spinner_item
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusFilterSpinner.setAdapter(adapter);

        statusFilterSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                filterThesesByStatus(position);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void setupThesesListObserver() {
        sharedViewModel.getThesisList().observe(getViewLifecycleOwner(), theses -> {
            if (theses != null && !theses.isEmpty()) {
                currentDisplayList = new ArrayList<>(theses);
                setupThesesList(currentDisplayList);
                updateSummary(currentDisplayList);
            }
        });
    }

    private void setupThesesList(List<SharedViewModel.Thesis> theses) {
        adapter = new ArrayAdapter<SharedViewModel.Thesis>(
                requireContext(),
                android.R.layout.simple_list_item_2,
                android.R.id.text1,
                theses
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);

                SharedViewModel.Thesis thesis = getItem(position);
                if (thesis != null) {
                    text1.setText(thesis.getTitle());
                    text2.setText("Student: " + thesis.getStudentName() + " | Status: " + getStatusText(thesis.getStatus()) + " | " + thesis.getLastUpdate());
                }
                return view;
            }
        };

        thesesListView.setAdapter(adapter);

        thesesListView.setOnItemClickListener((parent, view, position, id) -> {
            SharedViewModel.Thesis selectedThesis = theses.get(position);
            openThesisDetails(selectedThesis);
        });
    }

    private void filterThesesByStatus(int statusPosition) {
        List<SharedViewModel.Thesis> allTheses = sharedViewModel.getThesisList().getValue();
        if (allTheses == null) return;

        List<SharedViewModel.Thesis> filteredList = new ArrayList<>();
        String selectedFilter = "";

        switch (statusPosition) {
            case 0: // "Alle"
                filteredList = new ArrayList<>(allTheses);
                break;
            case 1: // "In Abstimmung"
                selectedFilter = STATUS_IN_DISCUSSION;
                break;
            case 2: // "Angemeldet"
                selectedFilter = STATUS_REGISTERED;
                break;
            case 3: // "Abgegeben"
                selectedFilter = STATUS_SUBMITTED;
                break;
            case 4: // "Kolloquium"
                selectedFilter = STATUS_COLLOQUIUM;
                break;
        }

        if (statusPosition > 0) {
            for (SharedViewModel.Thesis thesis : allTheses) {
                if (thesis.getStatus().equals(selectedFilter)) {
                    filteredList.add(thesis);
                }
            }
        }

        currentDisplayList = filteredList;
        adapter.clear();
        adapter.addAll(filteredList);
        adapter.notifyDataSetChanged();
        updateSummary(filteredList);
    }

    private void updateSummary(List<SharedViewModel.Thesis> theses) {
        long inDiscussion = 0;
        long registered = 0;
        long submitted = 0;
        long colloquium = 0;

        for (SharedViewModel.Thesis thesis : theses) {
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
        switch (status) {
            case STATUS_IN_DISCUSSION: return "In Abstimmung";
            case STATUS_REGISTERED: return "Angemeldet";
            case STATUS_SUBMITTED: return "Abgegeben";
            case STATUS_COLLOQUIUM: return "Kolloquium";
            default: return "Unbekannt";
        }
    }

    private void openThesisDetails(SharedViewModel.Thesis thesis) {
        navigateToStatusUpdate(thesis);
    }

    private void navigateToStatusUpdate(SharedViewModel.Thesis thesis) {
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

    // ✅ KORRIGIERTE METHODE: Zum Formular navigieren
    private void addNewThesis() {
        // Zum AddThesisFragment navigieren
        AddThesisFragment addThesisFragment = new AddThesisFragment();

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, addThesisFragment)
                .addToBackStack("dashboard")
                .commit();

        android.util.Log.d("Dashboard", "Öffne Formular für neue Arbeit");
    }

    // ✅ NEUE METHODE: Daten aktualisieren
    private void refreshData() {
        // Liste neu laden
        setupThesesListObserver();
        Toast.makeText(requireContext(), "Daten aktualisiert", Toast.LENGTH_SHORT).show();
    }
}