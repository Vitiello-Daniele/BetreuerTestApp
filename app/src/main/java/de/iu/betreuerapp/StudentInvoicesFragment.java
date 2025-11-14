package de.iu.betreuerapp;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import de.iu.betreuerapp.dto.ContactRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Rechnungsübersicht aus Sicht STUDENT.
 *
 * Zeigt pro tatsächlicher Rechnung EINEN Eintrag:
 *  - eine Zeile für Betreuer (falls invoice_supervisor_created = true)
 *  - eine Zeile für Zweitprüfer (falls invoice_reviewer_created = true)
 *
 * Filter:
 *  - Alle
 *  - Offen  (nicht bezahlt)
 *  - Bezahlt
 */
public class StudentInvoicesFragment extends Fragment {

    private RecyclerView rvInvoices;
    private TextView chipAll, chipOpen, chipPaid;

    private final List<Row> rows = new ArrayList<>();
    private final List<Row> visibleRows = new ArrayList<>();
    private StudentInvoicesAdapter adapter;

    private enum FilterType { ALL, OPEN, PAID }
    private FilterType currentFilter = FilterType.ALL;

    private static final int ORANGE = Color.parseColor("#FF9800");
    private static final int GRAY_TEXT = Color.parseColor("#666666");

    private static class Row {
        ContactRequest cr;
        boolean isSupervisorInvoice; // true = Betreuer-Rechnung, false = Zweitprüfer-Rechnung
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        if (!AuthGuard.requireRole(this, "student")) {
            return new View(requireContext());
        }

        View root = inflater.inflate(R.layout.fragment_student_invoices, container, false);

        rvInvoices = root.findViewById(R.id.rv_invoices);
        chipAll = root.findViewById(R.id.chip_filter_all);
        chipOpen = root.findViewById(R.id.chip_filter_open);
        chipPaid = root.findViewById(R.id.chip_filter_paid);

        setupRecyclerView();
        setupFilterChips();
        loadInvoices();

        return root;
    }

    private void setupRecyclerView() {
        rvInvoices.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new StudentInvoicesAdapter(visibleRows, this::showInvoiceDialog);
        rvInvoices.setAdapter(adapter);
    }

    private void setupFilterChips() {
        View.OnClickListener l = v -> {
            if (v == chipAll) currentFilter = FilterType.ALL;
            else if (v == chipOpen) currentFilter = FilterType.OPEN;
            else if (v == chipPaid) currentFilter = FilterType.PAID;

            updateChipUI();
            applyFilter();
        };

        chipAll.setOnClickListener(l);
        chipOpen.setOnClickListener(l);
        chipPaid.setOnClickListener(l);

        updateChipUI();
    }

    private void updateChipUI() {
        setChipActive(chipAll, currentFilter == FilterType.ALL);
        setChipActive(chipOpen, currentFilter == FilterType.OPEN);
        setChipActive(chipPaid, currentFilter == FilterType.PAID);
    }

    private void setChipActive(TextView chip, boolean active) {
        if (chip == null) return;
        chip.setSelected(active);
        chip.setAlpha(active ? 1f : 0.8f);
        chip.setTextColor(active ? ORANGE : GRAY_TEXT);
        chip.setBackgroundResource(
                active ? R.drawable.bg_filter_chip_active
                        : R.drawable.bg_filter_chip_inactive
        );
    }

    // --------------------------------------------------------
    // Laden
    // --------------------------------------------------------

    private void loadInvoices() {
        SessionManager sm = new SessionManager(requireContext());
        String studentId = sm.userId();

        if (studentId == null) {
            Toast.makeText(requireContext(),
                    "Keine Benutzer-ID gefunden. Bitte neu anmelden.",
                    Toast.LENGTH_LONG).show();
            rows.clear();
            visibleRows.clear();
            adapter.notifyDataSetChanged();
            updateChipCounts();
            return;
        }

        SupabaseClient client = new SupabaseClient(requireContext());
        client.restService()
                .getContactRequests("eq." + studentId)
                .enqueue(new Callback<List<ContactRequest>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<ContactRequest>> call,
                                           @NonNull Response<List<ContactRequest>> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(requireContext(),
                                    "Fehler beim Laden: " + response.code(),
                                    Toast.LENGTH_LONG).show();
                            rows.clear();
                            visibleRows.clear();
                            adapter.notifyDataSetChanged();
                            updateChipCounts();
                            return;
                        }

                        rows.clear();

                        for (ContactRequest r : response.body()) {
                            if (r == null) continue;

                            if (bool(r.invoice_supervisor_created)) {
                                Row rowSup = new Row();
                                rowSup.cr = r;
                                rowSup.isSupervisorInvoice = true;
                                rows.add(rowSup);
                            }
                            if (bool(r.invoice_reviewer_created)) {
                                Row rowRev = new Row();
                                rowRev.cr = r;
                                rowRev.isSupervisorInvoice = false;
                                rows.add(rowRev);
                            }
                        }

                        applyFilter();
                        updateChipCounts();
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<ContactRequest>> call,
                                          @NonNull Throwable t) {
                        Toast.makeText(requireContext(),
                                "Netzwerkfehler: " + t.getMessage(),
                                Toast.LENGTH_LONG).show();
                        rows.clear();
                        visibleRows.clear();
                        adapter.notifyDataSetChanged();
                        updateChipCounts();
                    }
                });
    }

    private void applyFilter() {
        visibleRows.clear();

        for (Row row : rows) {
            ContactRequest r = row.cr;
            boolean isSup = row.isSupervisorInvoice;

            boolean paid = isSup
                    ? bool(r.paid_supervisor)
                    : bool(r.paid_reviewer);

            boolean add;
            switch (currentFilter) {
                case OPEN:
                    add = !paid;
                    break;
                case PAID:
                    add = paid;
                    break;
                case ALL:
                default:
                    add = true;
                    break;
            }

            if (add) visibleRows.add(row);
        }

        adapter.notifyDataSetChanged();
    }

    private void updateChipCounts() {
        int total = rows.size();
        int open = 0;
        int paid = 0;

        for (Row row : rows) {
            ContactRequest r = row.cr;
            boolean isSup = row.isSupervisorInvoice;

            boolean paidFlag = isSup
                    ? bool(r.paid_supervisor)
                    : bool(r.paid_reviewer);

            if (paidFlag) paid++;
            else open++;
        }

        chipAll.setText("Alle (" + total + ")");
        chipOpen.setText("Offen (" + open + ")");
        chipPaid.setText("Bezahlt (" + paid + ")");
    }

    // --------------------------------------------------------
    // Dialog & Aktionen
    // --------------------------------------------------------

    private void showInvoiceDialog(Row row) {
        if (row == null || row.cr == null) return;

        ContactRequest r = row.cr;
        boolean isSup = row.isSupervisorInvoice;

        String roleLabel = isSup ? "Betreuer" : "Zweitprüfer";
        String personName = isSup
                ? safe(r.supervisor_name, safe(r.supervisor_email, "-"))
                : safe(r.second_reviewer_name, safe(r.second_reviewer_email, "-"));

        boolean paid = isSup
                ? bool(r.paid_supervisor)
                : bool(r.paid_reviewer);

        String title = extractTitle(r);

        StringBuilder msg = new StringBuilder();
        msg.append("Thema: ").append(title).append("\n\n");
        msg.append(roleLabel).append(": ").append(personName).append("\n\n");
        msg.append("Status dieser Rechnung: ").append(paid ? "bezahlt" : "offen");

        AlertDialog.Builder b = new AlertDialog.Builder(requireContext())
                .setTitle("Rechnungsdetails")
                .setMessage(msg.toString())
                .setNegativeButton("Schließen", null);

        if (!paid) {
            b.setPositiveButton("Als bezahlt markieren",
                    (d, w) -> updatePaidFlag(row));
        }

        b.show();
    }

    private void updatePaidFlag(Row row) {
        ContactRequest r = row.cr;
        if (r == null || r.id == null) {
            Toast.makeText(requireContext(),
                    "Fehler: Eintrag ohne ID.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        boolean isSup = row.isSupervisorInvoice;

        ContactRequest patch = new ContactRequest();
        if (isSup) {
            patch.paid_supervisor = true;
        } else {
            patch.paid_reviewer = true;
        }

        SupabaseClient client = new SupabaseClient(requireContext());
        client.restService()
                .updateContactRequest("eq." + r.id, patch)
                .enqueue(new Callback<List<ContactRequest>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<ContactRequest>> call,
                                           @NonNull Response<List<ContactRequest>> response) {
                        if (!response.isSuccessful()) {
                            Toast.makeText(requireContext(),
                                    "Fehler beim Aktualisieren: " + response.code(),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        Toast.makeText(requireContext(),
                                "Zahlungsstatus aktualisiert.",
                                Toast.LENGTH_SHORT).show();
                        loadInvoices();
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<ContactRequest>> call,
                                          @NonNull Throwable t) {
                        Toast.makeText(requireContext(),
                                "Netzwerkfehler: " + t.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // --------------------------------------------------------
    // Helper
    // --------------------------------------------------------

    private boolean bool(Boolean b) {
        return b != null && b;
    }

    private String safe(String v, String fb) {
        return (v == null || v.isEmpty()) ? fb : v;
    }

    /** Titel aus strukturierter message holen, sonst Fallback. */
    private String extractTitle(ContactRequest r) {
        if (r == null || r.message == null) {
            return "Arbeit";
        }
        String msg = r.message.trim();
        if (msg.isEmpty()) return "Arbeit";

        String legacyPrefix = "Anfrage für Thema:";
        if (msg.startsWith(legacyPrefix)) {
            String t = msg.substring(legacyPrefix.length()).trim();
            if (!t.isEmpty()) return t;
        }

        String[] lines = msg.split("\n");
        for (String line : lines) {
            String l = line.trim();
            if (l.startsWith("Titel:")) {
                String t = l.substring("Titel:".length()).trim();
                if (!t.isEmpty()) return t;
            }
        }

        return "Arbeit";
    }

    // --------------------------------------------------------
    // Adapter
    // --------------------------------------------------------

    private static class StudentInvoicesAdapter
            extends RecyclerView.Adapter<StudentInvoicesAdapter.VH> {

        interface OnRowClick { void onClick(Row row); }

        private final List<Row> data;
        private final OnRowClick listener;

        StudentInvoicesAdapter(List<Row> data, OnRowClick listener) {
            this.data = data;
            this.listener = listener;
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvSubtitle, tvStatus;
            VH(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_title);
                tvSubtitle = itemView.findViewById(R.id.tv_subtitle);
                tvStatus = itemView.findViewById(R.id.tv_status);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_student_invoice, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Row row = data.get(position);
            if (row == null || row.cr == null) return;

            ContactRequest r = row.cr;
            boolean isSup = row.isSupervisorInvoice;

            String title = extractTitle(r);
            String roleLabel = isSup ? "Betreuer" : "Zweitprüfer";
            String personName = isSup
                    ? safeLocal(r.supervisor_name, safeLocal(r.supervisor_email, "-"))
                    : safeLocal(r.second_reviewer_name, safeLocal(r.second_reviewer_email, "-"));

            boolean paid = isSup
                    ? (r.paid_supervisor != null && r.paid_supervisor)
                    : (r.paid_reviewer != null && r.paid_reviewer);

            h.tvTitle.setText(title);
            h.tvSubtitle.setText("Rechnung " + roleLabel + " – " + personName);

            String statusText = paid ? "Status: bezahlt" : "Status: offen";
            int statusColor = paid
                    ? Color.parseColor("#4CAF50")
                    : ORANGE;

            h.tvStatus.setText(statusText);
            h.tvStatus.setTextColor(statusColor);

            h.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onClick(row);
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        private static String safeLocal(String v, String fb) {
            return (v == null || v.isEmpty()) ? fb : v;
        }

        private static String extractTitle(ContactRequest r) {
            if (r == null || r.message == null) return "Arbeit";
            String msg = r.message.trim();
            if (msg.isEmpty()) return "Arbeit";

            String legacyPrefix = "Anfrage für Thema:";
            if (msg.startsWith(legacyPrefix)) {
                String t = msg.substring(legacyPrefix.length()).trim();
                if (!t.isEmpty()) return t;
            }

            String[] lines = msg.split("\n");
            for (String line : lines) {
                String l = line.trim();
                if (l.startsWith("Titel:")) {
                    String t = l.substring("Titel:".length()).trim();
                    if (!t.isEmpty()) return t;
                }
            }

            return "Arbeit";
        }
    }
}
