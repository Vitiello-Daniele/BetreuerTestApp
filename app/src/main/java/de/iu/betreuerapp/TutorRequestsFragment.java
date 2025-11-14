package de.iu.betreuerapp;

import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.iu.betreuerapp.dto.ContactRequest;
import de.iu.betreuerapp.dto.Topic;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * "Anfragen" (Tutor-Sicht)
 *
 * Zeigt nur Anfragen, bei denen der eingeloggte User Hauptbetreuer ist.
 * Stati: open, accepted, rejected.
 *
 * UX:
 * - Vollbreite-Chip oben: "Alle (N)"
 * - Darunter Chips: "Offen (x)", "Angenommen (y)", "Abgelehnt (z)"
 *
 * Logik:
 * - Wenn eine Anfrage mit topic_id ACCEPTED wird:
 *      1) Topic.status -> "taken" (verschwindet aus der Börse, wenn Owner)
 *      2) Alle anderen Anfragen zu diesem topic_id -> "rejected"
 */
public class TutorRequestsFragment extends Fragment {

    // Chips
    private TextView chipAll, chipOpen, chipAccepted, chipRejected;
    private RecyclerView rvList;

    private final List<ContactRequest> all = new ArrayList<>();
    private final List<ContactRequest> visible = new ArrayList<>();

    private enum FilterType { ALL, OPEN, ACCEPTED, REJECTED }
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

        View root = inflater.inflate(R.layout.fragment_tutor_requests, container, false);

        chipAll      = root.findViewById(R.id.chip_filter_all);
        chipOpen     = root.findViewById(R.id.chip_filter_open);
        chipAccepted = root.findViewById(R.id.chip_filter_accepted);
        chipRejected = root.findViewById(R.id.chip_filter_rejected);
        rvList       = root.findViewById(R.id.rv_requests);

        rvList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvList.setAdapter(new TutorRequestsAdapter(visible, this::showRequestDialog));

        setupFilterChips();
        loadRequests();

        return root;
    }

    // ------------------------------------------------------------------
    // Filter-Chips
    // ------------------------------------------------------------------

    private void setupFilterChips() {
        View.OnClickListener l = v -> {
            if (v == chipAll) currentFilter = FilterType.ALL;
            else if (v == chipOpen) currentFilter = FilterType.OPEN;
            else if (v == chipAccepted) currentFilter = FilterType.ACCEPTED;
            else if (v == chipRejected) currentFilter = FilterType.REJECTED;

            updateChipUI();
            applyFilter();
        };

        chipAll.setOnClickListener(l);
        chipOpen.setOnClickListener(l);
        chipAccepted.setOnClickListener(l);
        chipRejected.setOnClickListener(l);

        updateChipUI();
    }

    private void updateChipUI() {
        setChipActive(chipAll, currentFilter == FilterType.ALL);
        setChipActive(chipOpen, currentFilter == FilterType.OPEN);
        setChipActive(chipAccepted, currentFilter == FilterType.ACCEPTED);
        setChipActive(chipRejected, currentFilter == FilterType.REJECTED);
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

    // ------------------------------------------------------------------
    // Laden & Filtern
    // ------------------------------------------------------------------

    private void loadRequests() {
        SessionManager sm = new SessionManager(requireContext());
        String myId = sm.userId();

        if (myId == null) {
            // kein Toast hier – stilles Fail, Benutzer meldet sich sonst neu an
            updateCountersAndHeader();
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
                            all.clear();
                            visible.clear();
                            if (rvList.getAdapter() != null) rvList.getAdapter().notifyDataSetChanged();
                            updateCountersAndHeader();
                            return;
                        }

                        all.clear();
                        for (ContactRequest r : response.body()) {
                            if (r == null) continue;
                            // Nur Anfragen, bei denen ich Hauptbetreuer bin
                            if (!myId.equals(r.supervisor_id)) continue;

                            String s = norm(r.status);
                            if ("open".equals(s) || "accepted".equals(s) || "rejected".equals(s)) {
                                all.add(r);
                            }
                        }

                        applyFilter();           // baut visible
                        updateCountersAndHeader(); // setzt Zähler
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<ContactRequest>> call,
                                          @NonNull Throwable t) {
                        all.clear();
                        visible.clear();
                        if (rvList.getAdapter() != null) rvList.getAdapter().notifyDataSetChanged();
                        updateCountersAndHeader();
                    }
                });
    }

    private void applyFilter() {
        visible.clear();
        for (ContactRequest r : all) {
            if (matchesFilter(r)) visible.add(r);
        }
        if (rvList.getAdapter() != null) rvList.getAdapter().notifyDataSetChanged();
        updateCountersAndHeader();
    }

    private boolean matchesFilter(ContactRequest r) {
        String s = norm(r.status);
        switch (currentFilter) {
            case ALL:      return true;
            case OPEN:     return "open".equals(s);
            case ACCEPTED: return "accepted".equals(s);
            case REJECTED: return "rejected".equals(s);
            default:       return true;
        }
    }

    private void updateCountersAndHeader() {
        int total = all.size();
        int open = 0, accepted = 0, rejected = 0;

        for (ContactRequest r : all) {
            String s = norm(r.status);
            if ("open".equals(s)) open++;
            else if ("accepted".equals(s)) accepted++;
            else if ("rejected".equals(s)) rejected++;
        }

        chipAll.setText("Alle (" + total + ")");
        chipOpen.setText("Offen (" + open + ")");
        chipAccepted.setText("Angenommen (" + accepted + ")");
        chipRejected.setText("Abgelehnt (" + rejected + ")");
    }

    private String norm(String s) {
        return (s == null) ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    // ------------------------------------------------------------------
    // Detail-Dialog
    // ------------------------------------------------------------------

    private void showRequestDialog(ContactRequest r) {
        if (r == null) return;

        String raw   = r.message == null ? "" : r.message.trim();
        String title = extractFirst(raw, "(?im)^\\s*Titel\\s*:\\s*(.+)$");
        String area  = extractFirst(raw, "(?im)^\\s*(Fachgebiet|Fachbereich)\\s*:\\s*(.+)$");
        String desc  = extractFirst(raw, "(?is)\\bBeschreibung\\s*:\\s*(.+)$");

        if (title == null || title.isEmpty()) title = "Anfrage";
        if (desc == null || desc.isEmpty()) desc = (raw.isEmpty() ? "-" : raw);

        // Fachgebiet-Fallback über Directory (Supervisor-E-Mail)
        if ((area == null || area.trim().isEmpty())
                && r.supervisor_email != null && !r.supervisor_email.trim().isEmpty()) {
            SupervisorDirectory.Entry e = SupervisorDirectory.findByEmail(r.supervisor_email);
            if (e != null && e.area != null && !e.area.trim().isEmpty()) {
                area = e.area.trim();
            }
        }
        if (area == null || area.isEmpty()) area = "-";

        // farbiger Status oben
        String sLabel = mapStatusLabel(r.status);
        int colorRes = colorResForStatus(r.status);
        int sColor = ContextCompat.getColor(requireContext(), colorRes);

        SpannableStringBuilder sb = new SpannableStringBuilder();

        int stStart = sb.length();
        sb.append(sLabel).append("\n\n");
        sb.setSpan(new StyleSpan(Typeface.BOLD), stStart, stStart + sLabel.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new ForegroundColorSpan(sColor), stStart, stStart + sLabel.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        appendBold(sb, "Student: ");
        sb.append(r.student_name != null && !r.student_name.trim().isEmpty()
                        ? r.student_name : (r.student_email != null ? r.student_email : "Unbekannt"))
                .append("\n");

        appendBold(sb, "E-Mail: ");
        sb.append(r.student_email != null ? r.student_email : "-").append("\n\n");

        appendBold(sb, "Fachgebiet: ");
        sb.append(area).append("\n\n");

        appendBold(sb, "Beschreibung:");
        sb.append("\n").append(desc.trim());

        if (r.expose_url != null && !r.expose_url.trim().isEmpty()) {
            sb.append("\n\n");
            appendBold(sb, "Exposé:");
            sb.append("\n").append(r.expose_url.trim());
        }

        // ---- Dialog-Buttons: Schließen | Ablehnen | Annehmen ----
        AlertDialog.Builder b = new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(sb);

        String s = norm(r.status);
        if ("open".equals(s)) {
            // links: NEUTRAL → Schließen
            b.setNeutralButton("Schließen", (d, w) -> d.dismiss());
            // Mitte: NEGATIVE → Ablehnen
            b.setNegativeButton("Ablehnen", (d, w) -> updateStatus(r, "rejected"));
            // rechts: POSITIVE → Annehmen
            b.setPositiveButton("Annehmen", (d, w) -> updateStatus(r, "accepted"));
        } else {
            // nur Schließen, wenn nicht open
            b.setNeutralButton("Schließen", (d, w) -> d.dismiss());
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

    private void appendBold(SpannableStringBuilder sb, String label) {
        int start = sb.length();
        sb.append(label);
        sb.setSpan(new StyleSpan(Typeface.BOLD), start, start + label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private String extractFirst(String text, String regex) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(regex).matcher(text);
        return m.find() ? (m.groupCount() >= 2 ? m.group(2) : m.group(1)).trim() : null;
    }

    private String mapStatusLabel(String sRaw) {
        String n = (sRaw == null) ? "open" : sRaw.trim().toLowerCase(Locale.ROOT);
        switch (n) {
            case "open":     return "Offen";
            case "accepted": return "Angenommen";
            case "rejected": return "Abgelehnt";
            default:         return "Unbekannt";
        }
    }

    private int colorResForStatus(String sRaw) {
        String s = (sRaw == null) ? "open" : sRaw.trim().toLowerCase(Locale.ROOT);
        switch (s) {
            case "open":     return R.color.status_open;        // Blau
            case "accepted": return R.color.status_in_progress; // Gelb
            case "rejected": return R.color.status_rejected;    // Rot
            case "finished": return R.color.status_finished;    // Grün (falls jemals vorkommt)
            default:         return R.color.status_unknown;     // Grau
        }
    }

    // ------------------------------------------------------------------
    // Statuswechsel + Topic-/Request-Kaskade
    // ------------------------------------------------------------------

    private void updateStatus(ContactRequest r, String newStatus) {
        if (r.id == null) {
            Toast.makeText(requireContext(), "Fehler: Anfrage ohne ID.", Toast.LENGTH_LONG).show();
            return;
        }

        ContactRequest patch = new ContactRequest();
        patch.status = newStatus;

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

                        // Wenn akzeptiert und Thema vorhanden:
                        if ("accepted".equalsIgnoreCase(newStatus)
                                && r.topic_id != null
                                && !r.topic_id.isEmpty()) {
                            markTopicTakenAndRejectOthers(r);
                        }

                        Toast.makeText(requireContext(),
                                "Status geändert zu: " + mapStatusLabel(newStatus),
                                Toast.LENGTH_SHORT).show();

                        loadRequests();
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

    /**
     * Nach ACCEPTED mit topic_id:
     * - setzt Topic -> taken (falls Owner)
     * - lehnt konkurrierende Anfragen ab
     */
    private void markTopicTakenAndRejectOthers(ContactRequest accepted) {
        SupabaseClient client = new SupabaseClient(requireContext());
        final String topicId = accepted.topic_id;
        final String acceptedId = accepted.id;

        // 1) Topic -> taken (RLS: nur Owner kann's)
        Topic patch = new Topic();
        patch.status = "taken";

        client.restService()
                .updateTopic("eq." + topicId, patch)
                .enqueue(new Callback<List<Topic>>() {
                    @Override public void onResponse(@NonNull Call<List<Topic>> call, @NonNull Response<List<Topic>> response) { }
                    @Override public void onFailure(@NonNull Call<List<Topic>> call, @NonNull Throwable t) { }
                });

        // 2) Alle anderen Anfragen zu diesem Topic ablehnen
        client.restService()
                .listContactRequests()
                .enqueue(new Callback<List<ContactRequest>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<ContactRequest>> call,
                                           @NonNull Response<List<ContactRequest>> response) {
                        if (!response.isSuccessful() || response.body() == null) return;

                        for (ContactRequest r : response.body()) {
                            if (r == null || r.id == null) continue;
                            if (r.topic_id == null) continue;

                            if (topicId.equals(r.topic_id) && !acceptedId.equals(r.id)) {
                                String s = norm(r.status);
                                if (!"rejected".equals(s)) {
                                    ContactRequest rejectPatch = new ContactRequest();
                                    rejectPatch.status = "rejected";
                                    client.restService()
                                            .updateContactRequest("eq." + r.id, rejectPatch)
                                            .enqueue(new Callback<List<ContactRequest>>() {
                                                @Override public void onResponse(@NonNull Call<List<ContactRequest>> call2, @NonNull Response<List<ContactRequest>> resp2) { }
                                                @Override public void onFailure(@NonNull Call<List<ContactRequest>> call2, @NonNull Throwable t2) { }
                                            });
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<ContactRequest>> call,
                                          @NonNull Throwable t) { }
                });
    }
}
