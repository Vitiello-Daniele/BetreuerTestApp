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
import java.util.Locale;

import de.iu.betreuerapp.dto.ContactRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Zweitprüfer-Sicht:
 * Zeigt alle contact_requests, bei denen:
 * - second_reviewer_id oder second_reviewer_email zum eingeloggten User passt
 * - second_reviewer_status = pending oder accepted
 *
 * Filter:
 *  - Alle
 *  - Ausstehend (pending)
 *  - Aktiv (accepted & Arbeit nicht finished)
 *  - Beendet (accepted & Arbeit finished)
 */
public class SecondReviewerThesesFragment extends Fragment {

    private TextView chipAll, chipPending, chipActive, chipFinished;
    private RecyclerView rvList;

    private final List<ContactRequest> all = new ArrayList<>();
    private final List<ContactRequest> visible = new ArrayList<>();

    private enum FilterType { ALL, PENDING, ACTIVE, FINISHED }

    private FilterType currentFilter = FilterType.ALL;

    private static final int ORANGE = Color.parseColor("#FF9800");
    private static final int GRAY_TEXT = Color.parseColor("#666666");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        if (!AuthGuard.requireRole(this, "tutor")) {
            return new View(requireContext());
        }

        View root = inflater.inflate(R.layout.fragment_second_reviewer_theses, container, false);

        chipAll      = root.findViewById(R.id.chip_filter_all);
        chipPending  = root.findViewById(R.id.chip_filter_pending);
        chipActive   = root.findViewById(R.id.chip_filter_active);
        chipFinished = root.findViewById(R.id.chip_filter_finished);
        rvList       = root.findViewById(R.id.rv_theses);

        rvList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvList.setAdapter(new SecondAdapter(visible, this::showDetailDialog));

        setupFilterChips();
        loadData();

        return root;
    }

    // ---------------- Filter-Chips ----------------

    private void setupFilterChips() {
        View.OnClickListener l = v -> {
            if (v == chipAll)        currentFilter = FilterType.ALL;
            else if (v == chipPending)  currentFilter = FilterType.PENDING;
            else if (v == chipActive)   currentFilter = FilterType.ACTIVE;
            else if (v == chipFinished) currentFilter = FilterType.FINISHED;
            updateChipUI();
            applyFilter();
        };

        chipAll.setOnClickListener(l);
        chipPending.setOnClickListener(l);
        chipActive.setOnClickListener(l);
        chipFinished.setOnClickListener(l);

        updateChipUI();
    }

    private void updateChipUI() {
        setChipActive(chipAll,      currentFilter == FilterType.ALL);
        setChipActive(chipPending,  currentFilter == FilterType.PENDING);
        setChipActive(chipActive,   currentFilter == FilterType.ACTIVE);
        setChipActive(chipFinished, currentFilter == FilterType.FINISHED);
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

    // ---------------- Laden & Filtern ----------------

    private void loadData() {
        SessionManager sm = new SessionManager(requireContext());
        String myId = sm.userId();
        String myEmail = sm.email();

        if (myId == null && myEmail == null) {
            Toast.makeText(requireContext(),
                    "Fehler: Benutzer nicht erkannt. Bitte neu anmelden.",
                    Toast.LENGTH_LONG).show();
            all.clear();
            visible.clear();
            if (rvList.getAdapter() != null) rvList.getAdapter().notifyDataSetChanged();
            updateSummary();
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
                            all.clear();
                            visible.clear();
                            if (rvList.getAdapter() != null) rvList.getAdapter().notifyDataSetChanged();
                            updateSummary();
                            return;
                        }

                        all.clear();

                        for (ContactRequest r : response.body()) {
                            if (r == null) continue;

                            String srs = norm(r.second_reviewer_status);
                            if (!"pending".equals(srs) && !"accepted".equals(srs)) {
                                continue; // nur relevante Stati
                            }

                            boolean isMine = false;

                            // Match nach ID
                            if (myId != null && r.second_reviewer_id != null
                                    && r.second_reviewer_id.equals(myId)) {
                                isMine = true;
                            }

                            // Fallback: Match nach E-Mail
                            if (!isMine && myEmail != null && r.second_reviewer_email != null
                                    && r.second_reviewer_email.equalsIgnoreCase(myEmail)) {
                                isMine = true;
                            }

                            if (isMine) {
                                all.add(r);
                            }
                        }

                        applyFilter();
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<ContactRequest>> call,
                                          @NonNull Throwable t) {
                        Toast.makeText(requireContext(),
                                "Netzwerkfehler: " + t.getMessage(),
                                Toast.LENGTH_LONG).show();
                        all.clear();
                        visible.clear();
                        if (rvList.getAdapter() != null) rvList.getAdapter().notifyDataSetChanged();
                        updateSummary();
                    }
                });
    }

    private void applyFilter() {
        visible.clear();

        for (ContactRequest r : all) {
            if (matchesFilter(r)) {
                visible.add(r);
            }
        }

        if (rvList.getAdapter() != null) rvList.getAdapter().notifyDataSetChanged();
        updateSummary();
    }

    private boolean matchesFilter(ContactRequest r) {
        String srs = norm(r.second_reviewer_status);
        String st  = norm(r.status);

        switch (currentFilter) {
            case ALL:
                return true;
            case PENDING:
                return "pending".equals(srs);
            case ACTIVE:
                return "accepted".equals(srs) && !"finished".equals(st);
            case FINISHED:
                return "accepted".equals(srs) && "finished".equals(st);
            default:
                return true;
        }
    }

    private void updateSummary() {
        int total    = all.size();
        int pending  = 0;
        int active   = 0;
        int finished = 0;

        for (ContactRequest r : all) {
            String srs = norm(r.second_reviewer_status);
            String st  = norm(r.status);
            if ("pending".equals(srs)) pending++;
            else if ("accepted".equals(srs)) {
                if ("finished".equals(st)) finished++;
                else active++;
            }
        }

        chipAll.setText("Alle (" + total + ")");
        chipPending.setText("Ausstehend (" + pending + ")");
        chipActive.setText("Aktiv (" + active + ")");
        chipFinished.setText("Beendet (" + finished + ")");
    }

    private String norm(String s) {
        return (s == null) ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    // ---------------- Detail-Dialog & Status ----------------

    private void showDetailDialog(ContactRequest r) {
        if (r == null) return;

        SecondAdapter.Parsed p = SecondAdapter.parseMessage(r.message);

        // Fachgebiet mit Fallback über Directory
        String area = p.area;
        if ((area == null || area.trim().isEmpty())
                && r.supervisor_email != null && !r.supervisor_email.trim().isEmpty()) {
            SupervisorDirectory.Entry e = SupervisorDirectory.findByEmail(r.supervisor_email);
            if (e != null && e.area != null && !e.area.trim().isEmpty()) {
                area = e.area.trim();
            }
        }
        if (area == null || area.isEmpty()) area = "-";

        StringBuilder msg = new StringBuilder();

        msg.append("Titel: ").append(p.title != null ? p.title : "Arbeit").append("\n");
        msg.append("Fachgebiet: ").append(area).append("\n\n");

        msg.append("Student: ")
                .append(r.student_name != null ? r.student_name : "Unbekannt")
                .append("\nE-Mail: ")
                .append(r.student_email != null ? r.student_email : "-")
                .append("\n\n");

        msg.append("Betreuer (Hauptprüfer): ")
                .append(r.supervisor_name != null ? r.supervisor_name : "-");
        if (r.supervisor_email != null) {
            msg.append(" (").append(r.supervisor_email).append(")");
        }
        msg.append("\n\n");

        if (p.desc != null && !p.desc.isEmpty()) {
            msg.append("Beschreibung:\n").append(p.desc).append("\n\n");
        }

        if (r.expose_url != null && !r.expose_url.isEmpty()) {
            msg.append("Exposé:\n").append(r.expose_url).append("\n\n");
        }

        String srs = norm(r.second_reviewer_status);
        String st  = norm(r.status);

        msg.append("Zweitprüfer-Status: ").append(mapSecondStatus(srs)).append("\n");
        msg.append("Arbeit-Status: ").append(mapMainStatus(st));

        AlertDialog.Builder b = new AlertDialog.Builder(requireContext())
                .setTitle("Zweitprüfung")
                .setMessage(msg.toString())
                .setNeutralButton("Schließen", null);

        if ("pending".equals(srs)) {
            b.setPositiveButton("Annehmen",
                    (d, w) -> updateSecondReviewerStatus(r, "accepted"));
            b.setNegativeButton("Ablehnen",
                    (d, w) -> updateSecondReviewerStatus(r, "rejected"));
        }

        AlertDialog dlg = b.create();
        dlg.setOnShowListener(di -> {
            if (dlg.getButton(AlertDialog.BUTTON_POSITIVE)  != null)
                dlg.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK);
            if (dlg.getButton(AlertDialog.BUTTON_NEGATIVE)  != null)
                dlg.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
            if (dlg.getButton(AlertDialog.BUTTON_NEUTRAL)   != null)
                dlg.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.BLACK);
        });
        dlg.show();
    }

    private void updateSecondReviewerStatus(ContactRequest r, String newStatus) {
        if (r.id == null) {
            Toast.makeText(requireContext(),
                    "Fehler: Eintrag ohne ID.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        ContactRequest patch = new ContactRequest();
        patch.second_reviewer_status = newStatus;

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

                        String label = "accepted".equalsIgnoreCase(newStatus)
                                ? "angenommen"
                                : "abgelehnt";

                        Toast.makeText(requireContext(),
                                "Zweitprüfer-Rolle " + label + ".",
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

    private String mapSecondStatus(String s) {
        switch (s) {
            case "pending":
                return "Ausstehend";
            case "accepted":
                return "Angenommen";
            case "rejected":
                return "Abgelehnt";
            default:
                return "-";
        }
    }

    private String mapMainStatus(String s) {
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
                return (s == null || s.isEmpty()) ? "-" : s;
        }
    }

    // ---------------- Adapter ----------------

    private static class SecondAdapter extends RecyclerView.Adapter<SecondAdapter.VH> {

        interface OnItemClick { void onClick(ContactRequest r); }

        private final List<ContactRequest> data;
        private final OnItemClick onClick;

        SecondAdapter(List<ContactRequest> data, OnItemClick onClick) {
            this.data = data;
            this.onClick = onClick;
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvInfo, tvStatus;
            VH(@NonNull View v) {
                super(v);
                tvTitle  = v.findViewById(R.id.tv_title);
                tvInfo   = v.findViewById(R.id.tv_info);
                tvStatus = v.findViewById(R.id.tv_status);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_second_reviewer_thesis, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            ContactRequest r = data.get(position);
            if (r == null) return;

            Parsed p = parseMessage(r.message);

            String title = (p.title != null && !p.title.isEmpty())
                    ? p.title : "Arbeit";

            String srs = (r.second_reviewer_status != null)
                    ? r.second_reviewer_status.toLowerCase(Locale.ROOT)
                    : "pending";
            String st  = (r.status != null)
                    ? r.status.trim().toLowerCase(Locale.ROOT)
                    : "";

            // Statusanzeige im Item:
            // - pending  -> Einladung ausstehend
            // - accepted -> Status der Arbeit (Angenommen / In Arbeit / ... / Beendet)
            String statusLabel;
            if ("pending".equals(srs)) {
                statusLabel = "Einladung ausstehend";
            } else {
                statusLabel = mapMainStatusForCard(st);
            }

            h.tvTitle.setText(title);

            StringBuilder info = new StringBuilder();
            if (p.area != null && !p.area.isEmpty()) {
                info.append(p.area);
            }
            if (r.student_name != null && !r.student_name.isEmpty()) {
                if (info.length() > 0) info.append(" • ");
                info.append("Student: ").append(r.student_name);
            }
            if (r.supervisor_name != null && !r.supervisor_name.isEmpty()) {
                if (info.length() > 0) info.append(" • ");
                info.append("Betreuer: ").append(r.supervisor_name);
            }
            h.tvInfo.setText(info.toString());

            h.tvStatus.setText(statusLabel);

            h.itemView.setOnClickListener(v -> {
                if (onClick != null) onClick.onClick(r);
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        // Mapping für die Status-Anzeige in der Karte
        private static String mapMainStatusForCard(String s) {
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
                    return "-";
            }
        }

        static class Parsed {
            String title;
            String area;
            String desc;
        }

        static Parsed parseMessage(String raw) {
            Parsed p = new Parsed();
            if (raw == null) return p;
            String msg = raw.trim();
            if (msg.isEmpty()) return p;

            String legacyPrefix = "Anfrage für Thema:";
            if (msg.startsWith(legacyPrefix)) {
                String t = msg.substring(legacyPrefix.length()).trim();
                if (!t.isEmpty()) p.title = t;
                p.desc = msg;
                return p;
            }

            // Titel (robust, mit evtl. Leerzeichen)
            java.util.regex.Matcher mTitle =
                    java.util.regex.Pattern.compile("(?im)^\\s*Titel\\s*:\\s*(.+)$")
                            .matcher(msg);
            if (mTitle.find()) {
                p.title = mTitle.group(1).trim();
            }

            // Fachgebiet oder Fachbereich
            java.util.regex.Matcher mArea =
                    java.util.regex.Pattern.compile("(?im)^\\s*(Fachgebiet|Fachbereich)\\s*:\\s*(.+)$")
                            .matcher(msg);
            if (mArea.find()) {
                p.area = mArea.group(2).trim();
            }

            // Beschreibung: alles nach "Beschreibung:" bis zum Ende
            String[] lines = msg.split("\n");
            boolean inDesc = false;
            StringBuilder descBuilder = new StringBuilder();

            for (String line : lines) {
                String l = line.trim();
                if (l.isEmpty()) continue;

                if (l.toLowerCase(Locale.ROOT).startsWith("beschreibung:")) {
                    inDesc = true;
                    String rest = l.substring("Beschreibung:".length()).trim();
                    if (!rest.isEmpty()) descBuilder.append(rest);
                } else if (inDesc) {
                    if (descBuilder.length() > 0) descBuilder.append("\n");
                    descBuilder.append(l);
                }
            }

            if (descBuilder.length() > 0) {
                p.desc = descBuilder.toString();
            }

            if (p.title == null && p.area == null && p.desc == null) {
                p.desc = msg;
            }
            return p;
        }
    }
}
