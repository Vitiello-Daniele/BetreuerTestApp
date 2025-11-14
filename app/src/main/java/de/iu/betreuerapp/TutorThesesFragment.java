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
import de.iu.betreuerapp.dto.Topic;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Betreute Arbeiten (Tutor-Sicht)
 *
 * Zeigt nur contact_requests, bei denen der eingeloggte User HAUPTBETREUER ist
 * und Status in:
 * accepted, in_progress, submitted, colloquium_held, invoiced, finished
 *
 * Filter:
 *  - Alle
 *  - Abstimmung      -> status = accepted & second_reviewer_status != accepted
 *  - In Bearbeitung  -> alles dazwischen (accepted mit zugestimmtem 2. Prüfer bis invoiced)
 *  - Beendet         -> finished
 *
 * Regeln:
 *  - Nach "accepted" MUSS ein Zweitprüfer zugewiesen werden.
 *  - Zweitprüfer kann geändert werden, solange sein Status != accepted.
 *  - "In Arbeit" ist nur erlaubt, wenn second_reviewer_status == accepted.
 *  - "Beendet" ist nur erlaubt, wenn die Betreuerrechnung bezahlt ist
 *    und – falls Zweitprüfer akzeptiert – auch dessen Rechnung bezahlt ist.
 */
public class TutorThesesFragment extends Fragment {

    private TextView chipAllWide, chipAbstimmung, chipInProgress, chipFinished;
    private RecyclerView rvList;

    private final List<ContactRequest> all = new ArrayList<>();
    private final List<ContactRequest> visible = new ArrayList<>();

    private enum FilterType { ALL, ABSTIMMUNG, IN_PROGRESS, FINISHED }

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

        View root = inflater.inflate(R.layout.fragment_tutor_theses, container, false);

        chipAllWide    = root.findViewById(R.id.chip_filter_all);
        chipAbstimmung = root.findViewById(R.id.chip_filter_abstimmung);
        chipInProgress = root.findViewById(R.id.chip_filter_in_progress);
        chipFinished   = root.findViewById(R.id.chip_filter_finished);
        rvList         = root.findViewById(R.id.rv_theses);

        rvList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvList.setAdapter(new TutorThesesAdapter(visible, this::showThesisDialog));

        setupFilterChips();
        loadTheses();

        return root;
    }

    // ----------------------------------------------------
    // Filter-Chips
    // ----------------------------------------------------

    private void setupFilterChips() {
        View.OnClickListener l = v -> {
            if (v == chipAllWide) currentFilter = FilterType.ALL;
            else if (v == chipAbstimmung) currentFilter = FilterType.ABSTIMMUNG;
            else if (v == chipInProgress) currentFilter = FilterType.IN_PROGRESS;
            else if (v == chipFinished) currentFilter = FilterType.FINISHED;

            updateChipUI();
            applyFilter();
        };

        chipAllWide.setOnClickListener(l);
        chipAbstimmung.setOnClickListener(l);
        chipInProgress.setOnClickListener(l);
        chipFinished.setOnClickListener(l);

        updateChipUI();
    }

    private void updateChipUI() {
        setChipActive(chipAllWide,    currentFilter == FilterType.ALL);
        setChipActive(chipAbstimmung, currentFilter == FilterType.ABSTIMMUNG);
        setChipActive(chipInProgress, currentFilter == FilterType.IN_PROGRESS);
        setChipActive(chipFinished,   currentFilter == FilterType.FINISHED);
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

    // ----------------------------------------------------
    // Laden & Filtern
    // ----------------------------------------------------

    private void loadTheses() {
        SessionManager sm = new SessionManager(requireContext());
        String myId = sm.userId();

        if (myId == null) {
            Toast.makeText(requireContext(),
                    "Fehler: Benutzer nicht erkannt. Bitte neu anmelden.",
                    Toast.LENGTH_LONG).show();
            all.clear();
            visible.clear();
            if (rvList.getAdapter() != null) rvList.getAdapter().notifyDataSetChanged();
            updateFilterLabels();
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
                            updateFilterLabels();
                            return;
                        }

                        all.clear();

                        for (ContactRequest r : response.body()) {
                            if (r == null) continue;

                            // Nur meine betreuten Arbeiten (Hauptbetreuer)
                            if (!myId.equals(r.supervisor_id)) continue;

                            String s = norm(r.status);
                            if ("accepted".equals(s)
                                    || "in_progress".equals(s)
                                    || "submitted".equals(s)
                                    || "colloquium_held".equals(s)
                                    || "invoiced".equals(s)
                                    || "finished".equals(s)) {
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
                        updateFilterLabels();
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
        updateFilterLabels();
    }

    private boolean matchesFilter(ContactRequest r) {
        String st  = norm(r.status);
        String srs = norm(r.second_reviewer_status);

        boolean isFinished = "finished".equals(st);
        boolean isAbstimmung =
                "accepted".equals(st) && !"accepted".equals(srs);

        boolean isInProgressGroup =
                !isFinished && !isAbstimmung; // alles dazwischen

        switch (currentFilter) {
            case ALL:
                return true;
            case ABSTIMMUNG:
                return isAbstimmung;
            case IN_PROGRESS:
                return isInProgressGroup;
            case FINISHED:
                return isFinished;
            default:
                return true;
        }
    }

    private void updateFilterLabels() {
        int total      = all.size();
        int abstimmung = 0;
        int inProgress = 0;
        int finished   = 0;

        for (ContactRequest r : all) {
            String st  = norm(r.status);
            String srs = norm(r.second_reviewer_status);

            boolean isFinished = "finished".equals(st);
            boolean isAbstimmung =
                    "accepted".equals(st) && !"accepted".equals(srs);
            boolean isInProgressGroup =
                    !isFinished && !isAbstimmung;

            if (isFinished)       finished++;
            else if (isAbstimmung) inProgress += 0; // nix
            if (isAbstimmung)     abstimmung++;
            else if (!isFinished) inProgress++;
        }

        chipAllWide.setText("Alle (" + total + ")");
        chipAbstimmung.setText("Abstimmung (" + abstimmung + ")");
        chipInProgress.setText("In Bearbeitung (" + inProgress + ")");
        chipFinished.setText("Beendet (" + finished + ")");
    }

    private String norm(String s) {
        return (s == null) ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    // ----------------------------------------------------
    // Dialog & Status / Zweitprüfer
    // ----------------------------------------------------

    private void showThesisDialog(ContactRequest r) {
        if (r == null) return;

        StringBuilder msg = new StringBuilder();

        msg.append("Student: ")
                .append(r.student_name != null ? r.student_name : "Unbekannt")
                .append("\nE-Mail: ")
                .append(r.student_email != null ? r.student_email : "-")
                .append("\n\n");

        if (r.message != null && !r.message.isEmpty()) {
            msg.append("Thema / Anfrage:\n").append(r.message).append("\n\n");
        }

        if (r.expose_url != null && !r.expose_url.isEmpty()) {
            msg.append("Exposé:\n").append(r.expose_url).append("\n\n");
        }

        String srs = norm(r.second_reviewer_status);
        msg.append("Zweitprüfer: ");
        if (r.second_reviewer_name != null) {
            msg.append(r.second_reviewer_name);
            if (r.second_reviewer_email != null) {
                msg.append(" (").append(r.second_reviewer_email).append(")");
            }
        } else if (r.second_reviewer_email != null) {
            msg.append(r.second_reviewer_email);
        } else {
            msg.append("noch nicht zugewiesen");
        }
        if (!srs.isEmpty()) {
            msg.append(" – Status: ").append(mapSecondStatus(srs));
        }
        msg.append("\n\n");

        msg.append("Aktueller Status der Arbeit: ")
                .append(mapStatusLabel(r.status));

        String status = norm(r.status);
        boolean secondAccepted = "accepted".equals(srs);

        // Hinweis: Rechnungen nur im „Rechnungen“-Tab stellen
        if ("colloquium_held".equals(status)) {
            msg.append("\n\nHinweis: Rechnungen können im Tab „Rechnungen“ gestellt werden.");
        }

        AlertDialog.Builder b = new AlertDialog.Builder(requireContext())
                .setTitle("Betreute Arbeit")
                .setMessage(msg.toString())
                .setNeutralButton("Schließen", null);

        if ("accepted".equals(status)) {

            if (!secondAccepted) {
                // Noch keine Zusage → Zweitprüfer zuweisen/ändern
                b.setPositiveButton("Zweitprüfer zuweisen/ändern",
                        (d, w) -> openSecondReviewerSelection(r));
            } else {
                // Zweitprüfer hat akzeptiert → jetzt darf in Arbeit gesetzt werden
                b.setPositiveButton("Als 'In Arbeit' markieren",
                        (d, w) -> updateStatus(r, "in_progress"));
            }

        } else if ("in_progress".equals(status)) {
            b.setPositiveButton("Als 'Abgegeben' markieren",
                    (d, w) -> updateStatus(r, "submitted"));

        } else if ("submitted".equals(status)) {
            b.setPositiveButton("Kolloquium gehalten",
                    (d, w) -> updateStatus(r, "colloquium_held"));

        } else if ("invoiced".equals(status)) {
            // Status 'invoiced' kommt jetzt NUR aus TutorInvoicesFragment
            b.setPositiveButton("Als 'Beendet' markieren",
                    (d, w) -> updateStatus(r, "finished"));
        }

        b.show();
    }

    private void updateStatus(ContactRequest r, String newStatus) {
        if (r == null || r.id == null) {
            Toast.makeText(requireContext(),
                    "Fehler: Eintrag ohne ID.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Ohne akzeptierten Zweitprüfer nicht nach "in_progress"
        if ("in_progress".equals(newStatus)) {
            String srs = norm(r.second_reviewer_status);
            if (!"accepted".equals(srs)) {
                Toast.makeText(requireContext(),
                        "Bitte zuerst einen Zweitprüfer zuweisen und seine Zusage abwarten.",
                        Toast.LENGTH_LONG).show();
                return;
            }
        }

        // Für "finished": Rechnungs-Gating
        if ("finished".equals(newStatus)) {
            boolean supCreated = r.invoice_supervisor_created != null && r.invoice_supervisor_created;
            boolean supPaid    = r.paid_supervisor != null && r.paid_supervisor;

            String srs = norm(r.second_reviewer_status);
            boolean secondAccepted = "accepted".equals(srs);
            boolean secCreated = r.invoice_reviewer_created != null && r.invoice_reviewer_created;
            boolean secPaid    = r.paid_reviewer != null && r.paid_reviewer;

            if (!supCreated || !supPaid) {
                Toast.makeText(requireContext(),
                        "Bitte zuerst Ihre Betreuer-Rechnung stellen und als bezahlt markieren.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            if (secondAccepted && (!secCreated || !secPaid)) {
                Toast.makeText(requireContext(),
                        "Bitte warten, bis auch die Zweitprüfer-Rechnung gestellt und als bezahlt markiert wurde.",
                        Toast.LENGTH_LONG).show();
                return;
            }
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

                        Toast.makeText(requireContext(),
                                "Status geändert zu: " + mapStatusLabel(newStatus),
                                Toast.LENGTH_SHORT).show();
                        loadTheses();
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

    // ----------------------------------------------------
    // Zweitprüfer-Auswahl
    // ----------------------------------------------------

    private void openSecondReviewerSelection(ContactRequest r) {
        SupervisorDirectory.Entry[] entries = SupervisorDirectory.getAll();
        if (entries == null || entries.length == 0) {
            Toast.makeText(requireContext(),
                    "Kein Zweitprüfer-Verzeichnis hinterlegt.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Hauptbetreuer selbst rausfiltern
        List<SupervisorDirectory.Entry> options = new ArrayList<>();
        for (SupervisorDirectory.Entry e : entries) {
            if (e == null) continue;
            if (r.supervisor_email != null
                    && e.email != null
                    && r.supervisor_email.equalsIgnoreCase(e.email)) {
                continue;
            }
            options.add(e);
        }

        if (options.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Keine passenden Zweitprüfer gefunden.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String[] labels = new String[options.size()];
        for (int i = 0; i < options.size(); i++) {
            SupervisorDirectory.Entry e = options.get(i);
            labels[i] = e.name + (e.area != null ? " – " + e.area : "");
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Zweitprüfer auswählen")
                .setItems(labels, (dialog, which) -> {
                    if (which < 0 || which >= options.size()) return;
                    SupervisorDirectory.Entry sel = options.get(which);
                    assignSecondReviewer(r, sel);
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    private void assignSecondReviewer(ContactRequest r, SupervisorDirectory.Entry e) {
        if (r.id == null || e == null) {
            Toast.makeText(requireContext(),
                    "Fehler bei der Zweitprüfer-Zuordnung.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        ContactRequest patch = new ContactRequest();
        patch.second_reviewer_id     = e.id;
        patch.second_reviewer_name   = e.name;
        patch.second_reviewer_email  = e.email;
        patch.second_reviewer_status = "pending";

        SupabaseClient client = new SupabaseClient(requireContext());
        client.restService()
                .updateContactRequest("eq." + r.id, patch)
                .enqueue(new Callback<List<ContactRequest>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<ContactRequest>> call,
                                           @NonNull Response<List<ContactRequest>> response) {
                        if (!response.isSuccessful()) {
                            Toast.makeText(requireContext(),
                                    "Fehler beim Speichern des Zweitprüfers: " + response.code(),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        Toast.makeText(requireContext(),
                                "Zweitprüfer eingeladen: " + e.name,
                                Toast.LENGTH_SHORT).show();
                        loadTheses();
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

    // ----------------------------------------------------
    // Label-Helfer
    // ----------------------------------------------------

    private String mapStatusLabel(String s) {
        String n = norm(s);
        switch (n) {
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
}
