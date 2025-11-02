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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class InvoiceManagementFragment extends Fragment {

    private TextView thesisTitleText, studentNameText, supervisorNameText;
    private Spinner invoiceStatusSpinner;
    private Button updateInvoiceButton, createInvoiceButton;
    private TextView invoiceHistoryText;

    private List<Invoice> invoiceHistory;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_invoice_management, container, false);

        // UI Elemente initialisieren
        thesisTitleText = view.findViewById(R.id.thesis_title_text);
        studentNameText = view.findViewById(R.id.student_name_text);
        supervisorNameText = view.findViewById(R.id.supervisor_name_text);
        invoiceStatusSpinner = view.findViewById(R.id.invoice_status_spinner);
        updateInvoiceButton = view.findViewById(R.id.update_invoice_button);
        createInvoiceButton = view.findViewById(R.id.create_invoice_button);
        invoiceHistoryText = view.findViewById(R.id.invoice_history_text);

        // Beispiel-Daten laden
        loadSampleData();

        // Spinner einrichten
        setupInvoiceSpinner();

        // Rechnungshistorie anzeigen
        displayInvoiceHistory();

        // Button Listener
        updateInvoiceButton.setOnClickListener(v -> updateInvoiceStatus());
        createInvoiceButton.setOnClickListener(v -> createNewInvoice());

        return view;
    }

    private void loadSampleData() {
        // Beispiel-Daten (später von Intent/DB)
        thesisTitleText.setText("Entwicklung einer Betreuer-App");
        studentNameText.setText("Student: Max Mustermann");
        supervisorNameText.setText("Betreuer: Prof. Dr. Müller");

        // Beispiel-Rechnungshistorie
        invoiceHistory = new ArrayList<>();
        invoiceHistory.add(new Invoice("INV-2024-001", "250,00 €", "gestellt", "15.03.2024", null));
        invoiceHistory.add(new Invoice("INV-2023-045", "250,00 €", "bezahlt", "10.12.2023", "15.12.2023"));
    }

    private void setupInvoiceSpinner() {
        // Rechnungsstatus-Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.invoice_status_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        invoiceStatusSpinner.setAdapter(adapter);
    }

    private void displayInvoiceHistory() {
        if (invoiceHistory.isEmpty()) {
            invoiceHistoryText.setText("Keine Rechnungshistorie vorhanden");
            return;
        }

        StringBuilder historyBuilder = new StringBuilder();
        historyBuilder.append("Rechnungshistorie:\n\n");

        for (Invoice invoice : invoiceHistory) {
            historyBuilder.append("Rechnung: ").append(invoice.getInvoiceNumber()).append("\n");
            historyBuilder.append("Betrag: ").append(invoice.getAmount()).append("\n");
            historyBuilder.append("Status: ").append(invoice.getStatus()).append("\n");
            historyBuilder.append("Gestellt: ").append(invoice.getIssuedDate()).append("\n");

            if (invoice.getPaidDate() != null && !invoice.getPaidDate().isEmpty()) {
                historyBuilder.append("Bezahlt: ").append(invoice.getPaidDate()).append("\n");
            }
            historyBuilder.append("--------------------\n");
        }

        invoiceHistoryText.setText(historyBuilder.toString());
    }

    private void updateInvoiceStatus() {
        String newStatus = invoiceStatusSpinner.getSelectedItem().toString();

        // Hier kommt später die echte Update-Logik
        android.util.Log.d("InvoiceManagement", "Rechnungsstatus aktualisiert zu: " + newStatus);

        // Erfolgsmeldung
        Toast.makeText(requireContext(), "Rechnungsstatus aktualisiert zu: " + newStatus, Toast.LENGTH_LONG).show();

        // Beispiel: Aktualisiere die letzte Rechnung
        if (!invoiceHistory.isEmpty()) {
            Invoice lastInvoice = invoiceHistory.get(invoiceHistory.size() - 1);
            lastInvoice.setStatus(newStatus.toLowerCase());

            if (newStatus.equals("Bezahlt")) {
                String currentDate = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(new Date());
                lastInvoice.setPaidDate(currentDate);
            }

            displayInvoiceHistory(); // Historie aktualisieren
        }
    }

    private void createNewInvoice() {
        // Hier kommt später die echte Rechnungserstellung
        String newInvoiceNumber = "INV-2024-" + String.format("%03d", invoiceHistory.size() + 1);

        // Neue Rechnung erstellen
        Invoice newInvoice = new Invoice(
                newInvoiceNumber,
                "250,00 €",
                "gestellt",
                new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(new Date()),
                null
        );

        invoiceHistory.add(0, newInvoice); // Neue Rechnung am Anfang einfügen

        // Erfolgsmeldung
        Toast.makeText(requireContext(), "Neue Rechnung erstellt: " + newInvoiceNumber, Toast.LENGTH_LONG).show();

        // Historie aktualisieren
        displayInvoiceHistory();

        // Spinner auf "gestellt" setzen
        invoiceStatusSpinner.setSelection(1);
    }

    // Innere Klasse für Rechnungsdaten
    private static class Invoice {
        private String invoiceNumber;
        private String amount;
        private String status;
        private String issuedDate;
        private String paidDate;

        public Invoice(String invoiceNumber, String amount, String status, String issuedDate, String paidDate) {
            this.invoiceNumber = invoiceNumber;
            this.amount = amount;
            this.status = status;
            this.issuedDate = issuedDate;
            this.paidDate = paidDate;
        }

        public String getInvoiceNumber() { return invoiceNumber; }
        public String getAmount() { return amount; }
        public String getStatus() { return status; }
        public String getIssuedDate() { return issuedDate; }
        public String getPaidDate() { return paidDate; }

        public void setStatus(String status) { this.status = status; }
        public void setPaidDate(String paidDate) { this.paidDate = paidDate; }
    }
}