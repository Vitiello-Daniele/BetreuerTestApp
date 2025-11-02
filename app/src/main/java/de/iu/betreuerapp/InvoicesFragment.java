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
import java.util.ArrayList;
import java.util.List;

public class InvoicesFragment extends Fragment {

    private ListView assignmentsListView;
    private Spinner filterSpinner;
    private TextView summaryText;
    private Button sortDateButton, sortStatusButton, advancedFilterButton;
    private List<ReviewAssignment> assignmentList;
    private List<ReviewAssignment> originalAssignmentList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_invoices, container, false);

        // UI Elemente initialisieren
        assignmentsListView = view.findViewById(R.id.assignments_listview);
        filterSpinner = view.findViewById(R.id.filter_spinner);
        summaryText = view.findViewById(R.id.summary_text);
        sortDateButton = view.findViewById(R.id.sort_date_button);
        sortStatusButton = view.findViewById(R.id.sort_status_button);
        advancedFilterButton = view.findViewById(R.id.advanced_filter_button);

        // Beispiel-Daten laden
        loadSampleAssignments();

        // Original-Liste speichern für Reset
        originalAssignmentList = new ArrayList<>(assignmentList);

        // Filter und Liste einrichten
        setupFilterSpinner();
        setupAssignmentsList();

        // Zusammenfassung aktualisieren
        updateSummary();

        // Button Listener
        sortDateButton.setOnClickListener(v -> sortAssignmentsByDate());
        sortStatusButton.setOnClickListener(v -> sortAssignmentsByStatus());
        advancedFilterButton.setOnClickListener(v -> showAdvancedFilters());

        return view;
    }

    private void loadSampleAssignments() {
        assignmentList = new ArrayList<>();

        // Beispiel-Zuweisungen für Zweitgutachter
        assignmentList.add(new ReviewAssignment(
                "Entwicklung einer Betreuer-App",
                "Max Mustermann",
                "Prof. Dr. Müller",
                "in_review",
                "15.03.2024",
                "250,00 €",
                "nicht gestellt"
        ));

        assignmentList.add(new ReviewAssignment(
                "Machine Learning in Apps",
                "Anna Schmidt",
                "Dr. Weber",
                "completed",
                "20.02.2024",
                "250,00 €",
                "bezahlt"
        ));

        assignmentList.add(new ReviewAssignment(
                "IT-Sicherheit Analyse",
                "Tom Weber",
                "Prof. Fischer",
                "in_review",
                "10.01.2024",
                "250,00 €",
                "gestellt"
        ));

        assignmentList.add(new ReviewAssignment(
                "Cloud Computing Studie",
                "Lisa Fischer",
                "Dr. Schmidt",
                "completed",
                "05.12.2023",
                "250,00 €",
                "bezahlt"
        ));

        assignmentList.add(new ReviewAssignment(
                "Datenbank Optimierung",
                "David Becker",
                "Prof. Hoffmann",
                "in_review",
                "20.03.2024",
                "250,00 €",
                "nicht gestellt"
        ));
    }

    private void setupFilterSpinner() {
        // Filter-Spinner für Status/Rechnungen
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.review_filter_options,
                android.R.layout.simple_spinner_item
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(adapter);

        // Filter-Listener
        filterSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                handleFilterSelection(position);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void handleFilterSelection(int position) {
        String[] filterArray = getResources().getStringArray(R.array.review_filter_options);
        String selectedFilter = filterArray[position];

        // Zurück zur originalen Liste
        assignmentList = new ArrayList<>(originalAssignmentList);

        // Filter anwenden basierend auf Auswahl
        switch (position) {
            case 1: // In Begutachtung
                assignmentList.removeIf(assignment -> !assignment.getStatus().equals("in_review"));
                break;
            case 2: // Abgeschlossen
                assignmentList.removeIf(assignment -> !assignment.getStatus().equals("completed"));
                break;
            case 3: // Rechnungen gestellt
                assignmentList.removeIf(assignment -> !assignment.getInvoiceStatus().equals("gestellt"));
                break;
            case 4: // Rechnungen bezahlt
                assignmentList.removeIf(assignment -> !assignment.getInvoiceStatus().equals("bezahlt"));
                break;
            case 5: // Ohne Rechnung
                assignmentList.removeIf(assignment -> !assignment.getInvoiceStatus().equals("nicht gestellt"));
                break;
            // case 0: Alle anzeigen - keine Filterung nötig
        }

        // Liste aktualisieren
        setupAssignmentsList();
        updateSummary();

        android.util.Log.d("InvoicesFragment", "Filter angewendet: " + selectedFilter);
    }

    private void setupAssignmentsList() {
        // Custom ArrayAdapter für die ListView erstellen
        ArrayAdapter<ReviewAssignment> adapter = new ArrayAdapter<ReviewAssignment>(
                requireContext(),
                android.R.layout.simple_list_item_2,
                android.R.id.text1,
                assignmentList
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);

                ReviewAssignment assignment = getItem(position);
                if (assignment != null) {
                    text1.setText(assignment.getThesisTitle());
                    text2.setText("Student: " + assignment.getStudentName() +
                            " | Betreuer: " + assignment.getSupervisorName() +
                            " | Status: " + getStatusText(assignment.getStatus()) +
                            " | Rechnung: " + assignment.getInvoiceStatus());
                }
                return view;
            }
        };

        assignmentsListView.setAdapter(adapter);

        // Klick-Listener für Zuweisungen
        assignmentsListView.setOnItemClickListener((parent, view, position, id) -> {
            ReviewAssignment selectedAssignment = assignmentList.get(position);
            openAssignmentDetails(selectedAssignment);
        });
    }

    private void sortAssignmentsByDate() {
        // Sortierung nach Datum (absteigend - neueste zuerst)
        assignmentList.sort((a1, a2) -> a2.getAssignmentDate().compareTo(a1.getAssignmentDate()));

        // Liste aktualisieren
        setupAssignmentsList();

        Toast.makeText(requireContext(), "Nach Datum sortiert (neueste zuerst)", Toast.LENGTH_SHORT).show();
    }

    private void sortAssignmentsByStatus() {
        // Sortierung nach Status-Priorität
        assignmentList.sort((a1, a2) -> {
            int status1 = getStatusPriority(a1.getStatus());
            int status2 = getStatusPriority(a2.getStatus());
            return Integer.compare(status1, status2);
        });

        // Liste aktualisieren
        setupAssignmentsList();

        Toast.makeText(requireContext(), "Nach Status sortiert", Toast.LENGTH_SHORT).show();
    }

    private int getStatusPriority(String status) {
        switch (status) {
            case "in_review": return 1;  // Höchste Priorität
            case "completed": return 2;   // Niedrigere Priorität
            default: return 3;
        }
    }

    private void showAdvancedFilters() {
        // Einfache erweiterte Filterung demonstrieren
        // Filter nach Arbeiten ohne Rechnung und in Begutachtung
        assignmentList = new ArrayList<>(originalAssignmentList);
        assignmentList.removeIf(assignment ->
                !(assignment.getStatus().equals("in_review") && assignment.getInvoiceStatus().equals("nicht gestellt"))
        );

        // Liste aktualisieren
        setupAssignmentsList();
        updateSummary();

        Toast.makeText(requireContext(), "Erweiterter Filter: In Begutachtung + Ohne Rechnung", Toast.LENGTH_LONG).show();
    }

    private void updateSummary() {
        long inReview = assignmentList.stream().filter(a -> a.getStatus().equals("in_review")).count();
        long completed = assignmentList.stream().filter(a -> a.getStatus().equals("completed")).count();
        long invoiceIssued = assignmentList.stream().filter(a -> a.getInvoiceStatus().equals("gestellt")).count();
        long invoicePaid = assignmentList.stream().filter(a -> a.getInvoiceStatus().equals("bezahlt")).count();

        String summary = String.format(
                "Zweitgutachter Übersicht:\nIn Begutachtung: %d | Abgeschlossen: %d\nRechnungen gestellt: %d | Bezahlt: %d | Gesamt: %d",
                inReview, completed, invoiceIssued, invoicePaid, assignmentList.size()
        );

        summaryText.setText(summary);
    }

    private String getStatusText(String status) {
        switch (status) {
            case "in_review": return "In Begutachtung";
            case "completed": return "Abgeschlossen";
            default: return "Unbekannt";
        }
    }

    private void openAssignmentDetails(ReviewAssignment assignment) {
        // Hier kommt später die Detailansicht
        android.util.Log.d("InvoicesFragment", "Öffne Details für: " + assignment.getThesisTitle());

        // Zur Rechnungsverwaltung navigieren
        navigateToInvoiceManagement(assignment);
    }

    private void navigateToInvoiceManagement(ReviewAssignment assignment) {
        // InvoiceManagementFragment erstellen
        InvoiceManagementFragment invoiceFragment = new InvoiceManagementFragment();

        // Fragment wechseln
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, invoiceFragment)
                .addToBackStack("invoices")
                .commit();
    }

    // Innere Klasse für Zweitgutachter-Zuweisungen
    private static class ReviewAssignment {
        private String thesisTitle;
        private String studentName;
        private String supervisorName;
        private String status;
        private String assignmentDate;
        private String invoiceAmount;
        private String invoiceStatus;

        public ReviewAssignment(String thesisTitle, String studentName, String supervisorName,
                                String status, String assignmentDate, String invoiceAmount, String invoiceStatus) {
            this.thesisTitle = thesisTitle;
            this.studentName = studentName;
            this.supervisorName = supervisorName;
            this.status = status;
            this.assignmentDate = assignmentDate;
            this.invoiceAmount = invoiceAmount;
            this.invoiceStatus = invoiceStatus;
        }

        public String getThesisTitle() { return thesisTitle; }
        public String getStudentName() { return studentName; }
        public String getSupervisorName() { return supervisorName; }
        public String getStatus() { return status; }
        public String getAssignmentDate() { return assignmentDate; }
        public String getInvoiceAmount() { return invoiceAmount; }
        public String getInvoiceStatus() { return invoiceStatus; }
    }
}