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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.iu.betreuerapp.dto.ContactRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Rechnungsübersicht für eingeloggte Tutor-Rolle (Hauptbetreuer ODER Zweitprüfer).
 *
 * Filter:
 *  - Alle
 *  - Offen    = noch keine Rechnung (aber in Kolloquiums-/Abrechnungsphase)
 *  - Gestellt = Rechnung gestellt, aber noch nicht bezahlt
 *  - Bezahlt  = Rechnung gestellt & bezahlt
 *
 * WICHTIG: Nur hier wird eine Rechnung "gestellt" und ggf. der Status auf "invoiced" gesetzt
 * (für den Betreuer). Im Betreuungs-Fragment gibt es keinen "In Rechnung stellen"-Button mehr.
 */
public class TutorInvoicesFragment extends Fragment {

    private RecyclerView rvInvoices;

    private TextView chipAll, chipOpen, chipCreated, chipPaid;

    private final List<Row> allRows = new ArrayList<>();
    private final List<Row> visibleRows = new ArrayList<>();
    private TutorInvoicesAdapter adapter;

    private enum FilterType { ALL, OPEN, CREATED, PAID }
    private FilterType currentFilter = FilterType.ALL;

    private static final int ORANGE = Color.parseColor("#FF9800");
    private static final int GRAY_TEXT = Color.parseColor("#666666");

    private static class Row {
        ContactRequest cr;
        boolean isSupervisor;  // diese Zeile = Betreuer-Rechnung
        boolean isSecond;      // diese Zeile = Zweitprüfer-Rechnung
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        if (!AuthGuard.requireRole(this, "tutor")) {
            return new View(requireContext());
        }

        View root = inflater.inflate(R.layout.fragment_tutor_invoices, container, false);

        rvInvoices = root.findViewById(R.id.rv_invoices);

        chipAll    = root.findViewById(R.id.chip_filter_all);
        chipOpen   = root.findViewById(R.id.chip_filter_open);
        chipCreated= root.findViewById(R.id.chip_filter_created);
        chipPaid   = root.findViewById(R.id.chip_filter_paid);

        setupRecyclerView();
        setupFilterChips();
        loadData();

        return root;
    }

    // --------------------------------------------------------
    // RecyclerView + Adapter
    // --------------------------------------------------------

    private void setupRecyclerView() {
        rvInvoices.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TutorInvoicesAdapter(visibleRows, this::showInvoiceDialog);
        rvInvoices.setAdapter(adapter);
    }

    private void setupFilterChips() {
        View.OnClickListener l = v -> {
            if (v == chipAll)      currentFilter = FilterType.ALL;
            else if (v == chipOpen)   currentFilter = FilterType.OPEN;
            else if (v == chipCreated)currentFilter = FilterType.CREATED;
            else if (v == chipPaid)   currentFilter = FilterType.PAID;

            updateChipUI();
            applyFilter();
        };

        chipAll.setOnClickListener(l);
        chipOpen.setOnClickListener(l);
        chipCreated.setOnClickListener(l);
        chipPaid.setOnClickListener(l);

        updateChipUI();
    }

    private void updateChipUI() {
        setChipActive(chipAll,      currentFilter == FilterType.ALL);
        setChipActive(chipOpen,     currentFilter == FilterType.OPEN);
        setChipActive(chipCreated,  currentFilter == FilterType.CREATED);
        setChipActive(chipPaid,     currentFilter == FilterType.PAID);
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

    private void loadData() {
        SessionManager sm = new SessionManager(requireContext());
        String myId    = sm.userId();
        String myEmail = sm.email();

        if (myId == null && myEmail == null) {
            Toast.makeText(requireContext(),
                    "Fehler: Benutzer nicht erkannt. Bitte neu anmelden.",
                    Toast.LENGTH_LONG).show();
            allRows.clear();
            visibleRows.clear();
            if (adapter != null) adapter.notifyDataSetChanged();
            updateChipCounts();
            return;
        }

        SupabaseClient client = new SupabaseClient(requireContext());
        client.restService()
                .listContactRequests()
                .enqueue(new Callback<List<ContactRequest>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<ContactRequest>> call,
                                           @NonNull Response<List<ContactRequest>> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(requireContext(),
                                    "Fehler beim Laden: " + response.code(),
                                    Toast.LENGTH_LONG).show();
                            allRows.clear();
                            visibleRows.clear();
                            if (adapter != null) adapter.notifyDataSetChanged();
                            updateChipCounts();
                            return;
                        }

                        allRows.clear();

                        for (ContactRequest r : response.body()) {
                            if (r == null) continue;

                            String status = (r.status != null)
                                    ? r.status.toLowerCase(Locale.ROOT)
                                    : "";

                            String srs = (r.second_reviewer_status != null)
                                    ? r.second_reviewer_status.trim().toLowerCase(Locale.ROOT)
                                    : "";

                            boolean isSup         = myId != null && myId.equals(r.supervisor_id);
                            boolean isSecCandidate= myId != null && myId.equals(r.second_reviewer_id);

                            // Fallback per Mail
                            if (!isSup && myEmail != null && r.supervisor_email != null
                                    && myEmail.equalsIgnoreCase(r.supervisor_email)) {
                                isSup = true;
                            }
                            if (!isSecCandidate && myEmail != null && r.second_reviewer_email != null
                                    && myEmail.equalsIgnoreCase(r.second_reviewer_email)) {
                                isSecCandidate = true;
                            }

                            // Zweitprüfer nur, wenn Rolle akzeptiert
                            boolean isSec = isSecCandidate && "accepted".equals(srs);

                            if (!isSup && !isSec) continue;

                            boolean hasMyInvoice =
                                    (isSup && bool(r.invoice_supervisor_created))
                                            || (isSec && bool(r.invoice_reviewer_created));

                            boolean isDonePhase =
                                    status.equals("colloquium_held")
                                            || status.equals("invoiced")
                                            || status.equals("finished");

                            // Anzeigen ab Kolloquium oder sobald eigene Rechnung existiert
                            if (!hasMyInvoice && !isDonePhase) {
                                continue;
                            }

                            Row row = new Row();
                            row.cr           = r;
                            row.isSupervisor = isSup;
                            row.isSecond     = isSec;
                            allRows.add(row);
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
                        allRows.clear();
                        visibleRows.clear();
                        if (adapter != null) adapter.notifyDataSetChanged();
                        updateChipCounts();
                    }
                });
    }

    private void applyFilter() {
        visibleRows.clear();

        for (Row row : allRows) {
            ContactRequest r = row.cr;
            boolean asSup = row.isSupervisor;

            boolean invCreated = asSup
                    ? bool(r.invoice_supervisor_created)
                    : bool(r.invoice_reviewer_created);
            boolean paid = asSup
                    ? bool(r.paid_supervisor)
                    : bool(r.paid_reviewer);

            boolean add;
            switch (currentFilter) {
                case OPEN:    // noch keine Rechnung
                    add = !invCreated;
                    break;
                case CREATED: // Rechnung gestellt, nicht bezahlt
                    add = invCreated && !paid;
                    break;
                case PAID:    // Rechnung gestellt & bezahlt
                    add = invCreated && paid;
                    break;
                case ALL:
                default:
                    add = true;
                    break;
            }

            if (add) visibleRows.add(row);
        }

        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void updateChipCounts() {
        int total   = allRows.size();
        int open    = 0;
        int created = 0;
        int paid    = 0;

        for (Row row : allRows) {
            ContactRequest r = row.cr;
            boolean asSup = row.isSupervisor;

            boolean invCreated = asSup
                    ? bool(r.invoice_supervisor_created)
                    : bool(r.invoice_reviewer_created);
            boolean paidFlag = asSup
                    ? bool(r.paid_supervisor)
                    : bool(r.paid_reviewer);

            if (!invCreated) {
                open++;
            } else if (!paidFlag) {
                created++;
            } else {
                paid++;
            }
        }

        chipAll.setText("Alle (" + total + ")");
        chipOpen.setText("Offen (" + open + ")");
        chipCreated.setText("Gestellt (" + created + ")");
        chipPaid.setText("Bezahlt (" + paid + ")");
    }

    // --------------------------------------------------------
    // Dialog + Aktionen
    // --------------------------------------------------------

    private void showInvoiceDialog(Row row) {
        if (row == null || row.cr == null) return;

        ContactRequest r = row.cr;
        boolean asSup = row.isSupervisor;

        String roleLabel = asSup ? "Betreuer" : "Zweitprüfer";

        boolean invCreated = asSup
                ? bool(r.invoice_supervisor_created)
                : bool(r.invoice_reviewer_created);

        boolean paid = asSup
                ? bool(r.paid_supervisor)
                : bool(r.paid_reviewer);

        String student      = safe(r.student_name, "Unbekannt");
        String thesisStatus = mapStatusLabel(r.status);

        StringBuilder msg = new StringBuilder();
        msg.append("Rolle: ").append(roleLabel).append("\n");
        msg.append("Student: ").append(student).append("\n");
        msg.append("Arbeit-Status: ").append(thesisStatus).append("\n\n");

        msg.append("Mein Rechnungsstatus: ");
        if (!invCreated) {
            msg.append("Noch nicht gestellt");
        } else if (!paid) {
            msg.append("Gestellt (nicht bezahlt)");
        } else {
            msg.append("Bezahlt");
        }

        msg.append("\n\nBetreuer-Rechnung: ")
                .append(bool(r.invoice_supervisor_created)
                        ? (bool(r.paid_supervisor) ? "bezahlt" : "offen")
                        : "nicht gestellt");
        msg.append("\nZweitprüfer-Rechnung: ")
                .append(bool(r.invoice_reviewer_created)
                        ? (bool(r.paid_reviewer) ? "bezahlt" : "offen")
                        : "nicht gestellt");

        androidx.appcompat.app.AlertDialog.Builder b =
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Rechnung / Arbeit")
                        .setMessage(msg.toString())
                        .setNegativeButton("Schließen", null);

        String s = (r.status != null) ? r.status.toLowerCase(Locale.ROOT) : "";
        boolean canInvoicePhase =
                s.equals("colloquium_held") || s.equals("invoiced") || s.equals("finished");

        if (!invCreated && canInvoicePhase) {
            b.setPositiveButton("Rechnung als " + roleLabel + " stellen",
                    (d, w) -> createMyInvoice(row));
        }

        b.show();
    }

    private void createMyInvoice(Row row) {
        ContactRequest r = row.cr;
        if (r == null || r.id == null) {
            Toast.makeText(requireContext(),
                    "Fehler: Eintrag ohne ID.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        boolean asSup = row.isSupervisor;
        boolean asSec = row.isSecond;

        ContactRequest patch = new ContactRequest();

        if (asSup) {
            patch.invoice_supervisor_created = true;
            String s = (r.status != null) ? r.status.toLowerCase(Locale.ROOT) : "";
            if (!"invoiced".equals(s) && !"finished".equals(s)) {
                patch.status = "invoiced";
            }
        } else if (asSec) {
            patch.invoice_reviewer_created = true;
            // Zweitprüfer ändert den globalen Status NICHT.
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
                                "Rechnung markiert als gestellt.",
                                Toast.LENGTH_SHORT).show();
                        loadData();
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

    private String mapStatusLabel(String s) {
        if (s == null) return "-";
        switch (s) {
            case "accepted":
                return "Angenommen";
            case "in_progress":
                return "In Arbeit";
            case "submitted":
                return "Abgegeben";
            case "colloquium_held":
                return "Kolloquium gehalten";
            case "invoiced":
                return "In Rechnung";
            case "finished":
                return "Beendet";
            default:
                return s;
        }
    }

    // --------------------------------------------------------
    // Adapter (inner class)
    // --------------------------------------------------------

    private static class TutorInvoicesAdapter
            extends RecyclerView.Adapter<TutorInvoicesAdapter.VH> {

        interface OnRowClick { void onClick(Row row); }

        private final List<Row> data;
        private final OnRowClick listener;

        TutorInvoicesAdapter(List<Row> data, OnRowClick listener) {
            this.data = data;
            this.listener = listener;
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvSubtitle, tvStatus;
            VH(@NonNull View itemView) {
                super(itemView);
                tvTitle    = itemView.findViewById(R.id.tv_title);
                tvSubtitle = itemView.findViewById(R.id.tv_subtitle);
                tvStatus   = itemView.findViewById(R.id.tv_status);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_tutor_invoice, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Row row = data.get(position);
            if (row == null || row.cr == null) return;

            ContactRequest r = row.cr;
            boolean asSup = row.isSupervisor;

            String roleLabel = asSup ? "Betreuer" : "Zweitprüfer";
            String studentName = (r.student_name != null && !r.student_name.isEmpty())
                    ? r.student_name
                    : "Unbekannter Student";

            boolean invCreated = asSup
                    ? (r.invoice_supervisor_created != null && r.invoice_supervisor_created)
                    : (r.invoice_reviewer_created != null && r.invoice_reviewer_created);

            boolean paid = asSup
                    ? (r.paid_supervisor != null && r.paid_supervisor)
                    : (r.paid_reviewer != null && r.paid_reviewer);

            String statusText;
            int statusColor;

            if (!invCreated) {
                statusText  = "Rechnung noch nicht gestellt";
                statusColor = Color.parseColor("#666666");
            } else if (!paid) {
                statusText  = "Rechnung gestellt (offen)";
                statusColor = ORANGE;
            } else {
                statusText  = "Rechnung bezahlt";
                statusColor = Color.parseColor("#4CAF50");
            }

            String thesisStatus = (r.status == null) ? "-" : r.status;
            switch (thesisStatus) {
                case "accepted":        thesisStatus = "Angenommen"; break;
                case "in_progress":     thesisStatus = "In Arbeit"; break;
                case "submitted":       thesisStatus = "Abgegeben"; break;
                case "colloquium_held": thesisStatus = "Kolloquium gehalten"; break;
                case "invoiced":        thesisStatus = "In Rechnung"; break;
                case "finished":        thesisStatus = "Beendet"; break;
            }

            h.tvTitle.setText(studentName + " (" + roleLabel + ")");
            h.tvSubtitle.setText("Arbeit: " + thesisStatus);
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
    }
}
