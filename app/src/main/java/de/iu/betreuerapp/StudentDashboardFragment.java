package de.iu.betreuerapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import de.iu.betreuerapp.dto.ContactRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

//Parser imports
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.text.Spanned;
import android.graphics.Typeface;

import android.text.style.ForegroundColorSpan;

public class StudentDashboardFragment extends Fragment {

    private TextView tvEmpty, tvWelcome;

    // Chip-Labels (für Zähler)
    private TextView tvAllLabel, tvReqLabel, tvProgLabel, tvFinLabel;

    private ProgressBar progressBar;
    private RecyclerView rvRequests;
    private Button btnNewThesis;

    // Chips (Container)
    private LinearLayout chipAllWide;
    private LinearLayout chipRequested;
    private LinearLayout chipInProgress;
    private LinearLayout chipFinished;

    private final List<ContactRequest> allRequests = new ArrayList<>();
    private final List<ContactRequest> visibleRequests = new ArrayList<>();
    private StudentRequestsAdapter adapter;

    private enum FilterType { ALL, REQUESTED, IN_PROGRESS, FINISHED }
    private FilterType currentFilter = FilterType.ALL;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        if (!AuthGuard.requireRole(this, "student")) {
            return new View(requireContext());
        }

        View view = inflater.inflate(R.layout.fragment_student_dashboard, container, false);

        tvEmpty   = view.findViewById(R.id.tv_requests_empty);
        progressBar = view.findViewById(R.id.progress_bar);
        rvRequests  = view.findViewById(R.id.rv_requests);
        btnNewThesis = view.findViewById(R.id.btn_new_thesis);

        // Chips
        chipAllWide    = view.findViewById(R.id.chip_all_wide);
        chipRequested  = view.findViewById(R.id.chip_requested);
        chipInProgress = view.findViewById(R.id.chip_in_progress);
        chipFinished   = view.findViewById(R.id.chip_finished);

        // Chip-Labels
        tvAllLabel = view.findViewById(R.id.tv_chip_all_label);
        tvReqLabel = view.findViewById(R.id.tv_chip_req_label);
        tvProgLabel= view.findViewById(R.id.tv_chip_prog_label);
        tvFinLabel = view.findViewById(R.id.tv_chip_fin_label);


        setupRecyclerView();
        setupFilterChips();
        setupNewThesisButton();
        loadRequests();

        return view;
    }

    private void setupNewThesisButton() {
        btnNewThesis.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new SearchFragment())
                    .addToBackStack("my_theses")
                    .commit();
        });
    }

    private void setupRecyclerView() {
        rvRequests.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new StudentRequestsAdapter(visibleRequests, this::showDetailDialog);
        rvRequests.setAdapter(adapter);

        // Swipe-to-delete für offene ODER abgelehnte Arbeiten
        ItemTouchHelper.SimpleCallback swipeCallback =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                    @Override public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder t){ return false; }
                    @Override public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
                        int pos = vh.getAdapterPosition();
                        if (pos < 0 || pos >= visibleRequests.size()) { adapter.notifyDataSetChanged(); return; }
                        ContactRequest cr = visibleRequests.get(pos);
                        if (cr == null || cr.id == null) { adapter.notifyItemChanged(pos); return; }

                        String status = normalizeStatus(cr.status);
                        if (!"open".equals(status) && !"rejected".equals(status)) {
                            Toast.makeText(requireContext(), "Nur angefragte oder abgelehnte Arbeiten können gelöscht werden.", Toast.LENGTH_SHORT).show();
                            adapter.notifyItemChanged(pos);
                            return;
                        }

                        // aus sichtbarer & Gesamtliste entfernen
                        visibleRequests.remove(pos);
                        adapter.notifyItemRemoved(pos);
                        allRequests.remove(cr);

                        updateFilterLabels();
                        deleteRequestOnServer(cr);
                    }
                };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(rvRequests);
    }

    private void setupFilterChips() {
        View.OnClickListener listener = v -> {
            if (v == chipAllWide) {
                currentFilter = FilterType.ALL;
            } else if (v == chipRequested) {
                currentFilter = FilterType.REQUESTED;
            } else if (v == chipInProgress) {
                currentFilter = FilterType.IN_PROGRESS;
            } else if (v == chipFinished) {
                currentFilter = FilterType.FINISHED;
            }
            applyFilter();
            updateChipUI();
        };

        chipAllWide.setOnClickListener(listener);
        chipRequested.setOnClickListener(listener);
        chipInProgress.setOnClickListener(listener);
        chipFinished.setOnClickListener(listener);
        updateChipUI();
    }
    private void updateChipUI() {
        setChipActive(chipAllWide,    currentFilter == FilterType.ALL);
        setChipActive(chipRequested,  currentFilter == FilterType.REQUESTED);
        setChipActive(chipInProgress, currentFilter == FilterType.IN_PROGRESS);
        setChipActive(chipFinished,   currentFilter == FilterType.FINISHED);

    }

    private void setChipActive(LinearLayout chip, boolean active) {
        if (chip == null) return;
        chip.setSelected(active);
        chip.setAlpha(active ? 1.0f : 0.8f);
        chip.setBackgroundResource(
                active ? R.drawable.bg_filter_chip_active
                        : R.drawable.bg_filter_chip_inactive
        );
        // Textfarbe für den breiten "Alle"-Chip umschalten
        // if (chip == chipAllWide && tvAllLabel != null) {
        //      tvAllLabel.setTextColor(active ? 0xFFFF9800 : 0xFF666666);
        //  }
    }

    private void applyFilter() {
        visibleRequests.clear();

        for (ContactRequest cr : allRequests) {
            if (cr != null && matchesFilter(cr)) {
                visibleRequests.add(cr);
            }
        }

        // In „Alle“: abgelehnte nach unten schieben
        if (currentFilter == FilterType.ALL) {
            Collections.sort(visibleRequests, new RejectedLastComparator());
        }

        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(visibleRequests.isEmpty() ? View.VISIBLE : View.GONE);
        updateFilterLabels();
    }

    private boolean matchesFilter(ContactRequest cr) {
        String status = normalizeStatus(cr.status);
        switch (currentFilter) {
            case ALL:
                return true;
            case REQUESTED:
                return "open".equals(status);
            case IN_PROGRESS:
                // alles, was über „open“ hinausgeht, außer „finished“ und „rejected“
                return !("open".equals(status) || "rejected".equals(status) || "finished".equals(status));
            case FINISHED:
                return "finished".equals(status);
            default:
                return true;
        }
    }

    private String normalizeStatus(String raw) {
        if (raw == null) return "open";
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private void updateFilterLabels() {
        int total = allRequests.size();
        int requested = 0, inProgress = 0, finished = 0;
        for (ContactRequest cr : allRequests) {
            String s = normalizeStatus(cr.status);
            if ("open".equals(s)) requested++;
            else if ("finished".equals(s)) finished++;
            else if (!"rejected".equals(s)) inProgress++;
        }

        tvAllLabel.setText("Alle (" + total + ")");
        tvReqLabel.setText("Angefragt (" + requested + ")");
        tvProgLabel.setText("In Bearbeitung (" + inProgress + ")");
        tvFinLabel.setText("Abgeschlossen (" + finished + ")");
    }

    private void loadRequests() {
        SessionManager sm = new SessionManager(requireContext());
        String studentId = sm.userId();

        if (studentId == null) {
            tvEmpty.setText("Keine Benutzer-ID gefunden. Bitte neu anmelden.");
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        SupabaseClient client = new SupabaseClient(requireContext());
        client.restService()
                .getContactRequests("eq." + studentId)
                .enqueue(new Callback<List<ContactRequest>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<ContactRequest>> call,
                                           @NonNull Response<List<ContactRequest>> response) {
                        progressBar.setVisibility(View.GONE);

                        if (response.code() == 401) {
                            handleUnauthorized();
                            return;
                        }

                        if (!response.isSuccessful() || response.body() == null) {
                            tvEmpty.setText("Fehler beim Laden der Arbeiten (" + response.code() + ")");
                            tvEmpty.setVisibility(View.VISIBLE);
                            return;
                        }

                        allRequests.clear();
                        allRequests.addAll(response.body());

                        applyFilter();
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<ContactRequest>> call,
                                          @NonNull Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        tvEmpty.setText("Netzwerkfehler: " + t.getMessage());
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void handleUnauthorized() {
        Toast.makeText(requireContext(),
                "Session abgelaufen oder ungültig. Bitte melde dich neu an.",
                Toast.LENGTH_LONG).show();

        SessionManager sm = new SessionManager(requireContext());
        sm.clear();

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).logout();
        }
    }

    private void deleteRequestOnServer(ContactRequest cr) {
        SupabaseClient client = new SupabaseClient(requireContext());
        client.restService()
                .deleteContactRequest("eq." + cr.id)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call,
                                           @NonNull Response<Void> response) {
                        if (response.code() == 401) {
                            handleUnauthorized();
                            return;
                        }
                        if (!response.isSuccessful()) {
                            Toast.makeText(requireContext(),
                                    "Löschen fehlgeschlagen: " + response.code(),
                                    Toast.LENGTH_LONG).show();
                            loadRequests(); // repariert Anzeige
                        } else {
                            updateFilterLabels();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call,
                                          @NonNull Throwable t) {
                        Toast.makeText(requireContext(),
                                "Netzwerkfehler: " + t.getMessage(),
                                Toast.LENGTH_LONG).show();
                        loadRequests();
                    }
                });
    }

    // -------- Detail-Dialog mit Löschen für open/rejected --------

    private void showDetailDialog(ContactRequest cr) {
        if (cr == null) return;

        // --- Parser (Titel/Fachgebiet/Beschreibung/Notiz) ---
        String raw = cr.message == null ? "" : cr.message.trim();
        String title = extractFirst(raw, "(?m)^Titel:\\s*(.*)$");
        String area  = extractFirst(raw, "(?m)^Fachgebiet:\\s*(.*)$");
        String desc  = extractBlock(raw, "Beschreibung:");

        if (title == null || title.isEmpty()) title = "Arbeit";
        if ((area == null || area.isEmpty()) && cr.supervisor_email != null) {
            SupervisorDirectory.Entry e = SupervisorDirectory.findByEmail(cr.supervisor_email);
            if (e != null && e.area != null && !e.area.trim().isEmpty()) area = e.area.trim();
        }
        if (area == null || area.isEmpty()) area = "-";

        String tutor = (cr.supervisor_name != null && !cr.supervisor_name.isEmpty())
                ? cr.supervisor_name : "Betreuer:in unbekannt";

        if (desc == null || desc.trim().isEmpty())
            desc = raw.isEmpty() ? "-" : raw;

        // --- kompaktes & gut lesbares Layout ---
        SpannableStringBuilder sb = new SpannableStringBuilder();

        // 1) Status ganz oben (farbig)
        String statusLabel = StudentRequestsAdapter.mapStatusToLabelInfo(cr.status);
        int statusColor = StudentRequestsAdapter.mapStatusToColor(requireContext(), cr.status);

        int stStart = sb.length();
        sb.append(statusLabel).append("\n\n");
        sb.setSpan(new StyleSpan(Typeface.BOLD), stStart, stStart + statusLabel.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new ForegroundColorSpan(statusColor), stStart, stStart + statusLabel.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        appendLabel(sb, "Fachgebiet: ");
        sb.append(area).append("\n\n");

        appendLabel(sb, "Tutor: ");
        sb.append(tutor).append("\n\n");

        appendLabel(sb, "Beschreibung:");
        sb.append("\n").append(desc.trim()).append("\n\n");

        AlertDialog.Builder b = new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(sb)
                .setNegativeButton("Schließen", null);

        String st = normalizeStatus(cr.status);
        if ("open".equals(st) || "rejected".equals(st)) {
            b.setPositiveButton("Löschen", (d, w) -> {
                int idx = visibleRequests.indexOf(cr);
                if (idx >= 0) {
                    visibleRequests.remove(idx);
                    adapter.notifyItemRemoved(idx);
                }
                allRequests.remove(cr);
                updateFilterLabels();     // counter updaten (n)
                deleteRequestOnServer(cr);
            });
        }

        AlertDialog dlg = b.create();
        dlg.setOnShowListener(di -> {
            if (dlg.getButton(AlertDialog.BUTTON_POSITIVE) != null)
                dlg.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK);
            if (dlg.getButton(AlertDialog.BUTTON_NEGATIVE) != null)
                dlg.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
            if (dlg.getButton(AlertDialog.BUTTON_NEUTRAL) != null)
                dlg.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.BLACK);
        });
        dlg.show();
    }

    // ---- kleine Helfer für Bold-Labels + Parsing ----
    private void appendLabel(SpannableStringBuilder sb, String label) {
        int start = sb.length();
        sb.append(label);
        sb.setSpan(new StyleSpan(Typeface.BOLD), start, start + label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private String extractFirst(String text, String regex) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(regex).matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    /** Nimmt alles nach "Header:" bis zum nächsten "Xxx:"-Header am Zeilenanfang oder Textende. */
    private String extractBlock(String text, String header) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "(?s)(?m)^" + java.util.regex.Pattern.quote(header) + "\\s*\\R" +   // Header-Zeile
                        "((?:(?!^\\S+?:).)*?)\\s*" +                                        // Inhalt non-gierig
                        "(?=^\\S+?:|\\z)"                                                   // Stop vor nächstem Header/Ende
        );
        java.util.regex.Matcher m = p.matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }
    // Comparator: „rejected“ nach unten
    private static class RejectedLastComparator implements Comparator<ContactRequest> {
        @Override public int compare(ContactRequest a, ContactRequest b) {
            String sa = (a==null || a.status==null) ? "" : a.status.toLowerCase(Locale.ROOT);
            String sb = (b==null || b.status==null) ? "" : b.status.toLowerCase(Locale.ROOT);
            boolean ra = "rejected".equals(sa);
            boolean rb = "rejected".equals(sb);
            if (ra == rb) return 0;
            return ra ? 1 : -1; // rejected kommt nach unten
        }
    }
}
