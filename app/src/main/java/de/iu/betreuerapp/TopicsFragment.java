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

import de.iu.betreuerapp.dto.Topic;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * "Meine Themenbörse" (Tutor-Sicht)
 *
 * - Zeigt alle eigenen Themen als Karten
 * - Filter: Alle / Eingestellt / Vergeben
 *
 *  Eingestellt  -> status = available
 *  Vergeben     -> status = taken ODER completed
 *
 *  Verfügbare Themen können gelöscht werden.
 */
public class TopicsFragment extends Fragment {

    private TextView tvTitle;

    private TextView chipAll;
    private TextView chipAvailable;
    private TextView chipTaken;

    private RecyclerView rvTopics;
    private View addButton;

    private final List<Topic> allTopics = new ArrayList<>();
    private final List<Topic> visibleTopics = new ArrayList<>();

    private TopicsAdapter adapter;

    private enum FilterType { ALL, AVAILABLE, TAKEN }
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

        View root = inflater.inflate(R.layout.fragment_topics_management, container, false);

        tvTitle = root.findViewById(R.id.tv_title);

        chipAll = root.findViewById(R.id.chip_filter_all);
        chipAvailable = root.findViewById(R.id.chip_filter_available);
        chipTaken = root.findViewById(R.id.chip_filter_taken);

        rvTopics = root.findViewById(R.id.rv_topics);
        addButton = root.findViewById(R.id.add_topic_button);

        tvTitle.setText("Meine Themenbörse");

        rvTopics.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TopicsAdapter(
                visibleTopics,
                this::onTopicClick,
                this::onTopicDeleteClick
        );
        rvTopics.setAdapter(adapter);

        setupFilterChips();

        if (addButton != null) {
            addButton.setOnClickListener(v -> openAddTopic());
        }

        loadTopics();

        return root;
    }

    // ------------------------------------------------------------------------
    // Filter-Chips
    // ------------------------------------------------------------------------

    private void setupFilterChips() {
        View.OnClickListener listener = v -> {
            if (v == chipAll) {
                currentFilter = FilterType.ALL;
            } else if (v == chipAvailable) {
                currentFilter = FilterType.AVAILABLE;
            } else if (v == chipTaken) {
                currentFilter = FilterType.TAKEN;
            }
            updateChipUI();
            applyFilter();
        };

        chipAll.setOnClickListener(listener);
        chipAvailable.setOnClickListener(listener);
        chipTaken.setOnClickListener(listener);

        updateChipUI();
    }

    private void updateChipUI() {
        setChipActive(chipAll, currentFilter == FilterType.ALL);
        setChipActive(chipAvailable, currentFilter == FilterType.AVAILABLE);
        setChipActive(chipTaken, currentFilter == FilterType.TAKEN);
    }

    private void setChipActive(@Nullable TextView chip, boolean active) {
        if (chip == null) return;
        chip.setSelected(active);
        chip.setAlpha(active ? 1f : 0.8f);
        chip.setTextColor(active ? ORANGE : GRAY_TEXT);
        chip.setBackgroundResource(
                active ? R.drawable.bg_filter_chip_active
                        : R.drawable.bg_filter_chip_inactive
        );
    }

    // ------------------------------------------------------------------------
    // Laden & Filtern
    // ------------------------------------------------------------------------

    private void loadTopics() {
        SessionManager sm = new SessionManager(requireContext());
        String tutorId = sm.userId();
        if (tutorId == null) {
            Toast.makeText(requireContext(),
                    "Fehler: Tutor-ID fehlt. Bitte neu anmelden.",
                    Toast.LENGTH_LONG).show();
            allTopics.clear();
            visibleTopics.clear();
            adapter.notifyDataSetChanged();
            updateFilterLabels();
            return;
        }

        SupabaseClient client = new SupabaseClient(requireContext());
        String select = "id,title,description,area,status,owner_id";

        client.restService()
                .getTutorTopics("eq." + tutorId, select)
                .enqueue(new Callback<List<Topic>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Topic>> call,
                                           @NonNull Response<List<Topic>> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(requireContext(),
                                    "Fehler beim Laden: " + response.code(),
                                    Toast.LENGTH_LONG).show();
                            allTopics.clear();
                            visibleTopics.clear();
                            adapter.notifyDataSetChanged();
                            updateFilterLabels();
                            return;
                        }

                        allTopics.clear();
                        allTopics.addAll(response.body());

                        applyFilter();
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<Topic>> call,
                                          @NonNull Throwable t) {
                        Toast.makeText(requireContext(),
                                "Netzwerkfehler: " + t.getMessage(),
                                Toast.LENGTH_LONG).show();
                        allTopics.clear();
                        visibleTopics.clear();
                        adapter.notifyDataSetChanged();
                        updateFilterLabels();
                    }
                });
    }

    private void applyFilter() {
        visibleTopics.clear();

        for (Topic t : allTopics) {
            if (matchesFilter(t)) {
                visibleTopics.add(t);
            }
        }

        adapter.notifyDataSetChanged();
        updateFilterLabels();
    }

    private boolean matchesFilter(Topic t) {
        String s = (t.status == null) ? "" : t.status.trim().toLowerCase(Locale.ROOT);

        switch (currentFilter) {
            case ALL:
                return true;
            case AVAILABLE:
                return "available".equals(s);
            case TAKEN:
                // Vergeben = taken ODER completed
                return "taken".equals(s) || "completed".equals(s);
            default:
                return true;
        }
    }

    private void updateFilterLabels() {
        int total = allTopics.size();
        int available = 0;
        int assigned = 0; // taken oder completed

        for (Topic t : allTopics) {
            if (t.status == null) continue;
            String s = t.status.trim().toLowerCase(Locale.ROOT);
            switch (s) {
                case "available":
                    available++;
                    break;
                case "taken":
                case "completed":
                    assigned++;
                    break;
            }
        }

        chipAll.setText("Alle (" + total + ")");
        chipAvailable.setText("Eingestellt (" + available + ")");
        chipTaken.setText("Vergeben (" + assigned + ")");
    }

    // ------------------------------------------------------------------------
    // Aktionen
    // ------------------------------------------------------------------------

    private void onTopicClick(Topic t) {
        if (t == null) return;

        // aktuell nur kleiner Hinweis, erstmal auskommentiert
        // Toast.makeText(requireContext(),
        //         "Status: " + mapStatusLabel(t.status),
        //         Toast.LENGTH_SHORT).show();
    }

    private void onTopicDeleteClick(Topic t) {
        if (t == null || t.id == null) return;

        String s = (t.status == null) ? "" : t.status.trim().toLowerCase(Locale.ROOT);
        if (!"available".equals(s)) {
            Toast.makeText(requireContext(),
                    "Nur eingestellte Themen können gelöscht werden.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Thema löschen")
                .setMessage("Möchten Sie das Thema \"" +
                        (t.title != null ? t.title : "Ohne Titel") +
                        "\" wirklich löschen?")
                .setPositiveButton("Löschen", (d, w) -> deleteTopic(t))
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    private void deleteTopic(Topic t) {
        SupabaseClient client = new SupabaseClient(requireContext());
        client.restService()
                .deleteTopic("eq." + t.id)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call,
                                           @NonNull Response<Void> response) {
                        if (!response.isSuccessful()) {
                            Toast.makeText(requireContext(),
                                    "Löschen fehlgeschlagen: " + response.code(),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        allTopics.remove(t);
                        visibleTopics.remove(t);
                        adapter.notifyDataSetChanged();
                        updateFilterLabels();

                        Toast.makeText(requireContext(),
                                "Thema gelöscht.",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call,
                                          @NonNull Throwable err) {
                        Toast.makeText(requireContext(),
                                "Netzwerkfehler: " + err.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void openAddTopic() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new AddTopicFragment())
                .addToBackStack("topics")
                .commit();
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    private String mapStatusLabel(String s) {
        if (s == null) return "Unbekannt";
        switch (s) {
            case "available":
                return "Eingestellt";
            case "taken":
                return "Vergeben";
            case "completed":
                return "Abgeschlossen";
            default:
                return s;
        }
    }

    // ------------------------------------------------------------------------
    // Adapter (Card-Layout)
    // ------------------------------------------------------------------------

    private static class TopicsAdapter extends RecyclerView.Adapter<TopicsAdapter.VH> {

        interface OnTopicClick {
            void onClick(Topic t);
        }

        interface OnDeleteClick {
            void onDelete(Topic t);
        }

        private final List<Topic> data;
        private final OnTopicClick onClick;
        private final OnDeleteClick onDelete;

        TopicsAdapter(List<Topic> data,
                      OnTopicClick onClick,
                      OnDeleteClick onDelete) {
            this.data = data;
            this.onClick = onClick;
            this.onDelete = onDelete;
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvTitle;
            TextView tvMeta;
            TextView tvStatus;
            View btnDelete;

            VH(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_title);
                tvMeta = itemView.findViewById(R.id.tv_meta);
                tvStatus = itemView.findViewById(R.id.tv_status);
                btnDelete = itemView.findViewById(R.id.btn_delete);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent,
                                     int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_topic_tutor, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Topic t = data.get(position);
            if (t == null) return;

            String title = (t.title != null && !t.title.isEmpty())
                    ? t.title
                    : "Thema ohne Titel";
            h.tvTitle.setText(title);

            StringBuilder meta = new StringBuilder();
            if (t.area != null && !t.area.isEmpty()) {
                meta.append("Fachgebiet: ").append(t.area);
            }
            if (t.description != null && !t.description.isEmpty()) {
                if (meta.length() > 0) meta.append(" • ");
                String shortDesc = t.description.length() > 60
                        ? t.description.substring(0, 57) + "..."
                        : t.description;
                meta.append(shortDesc);
            }
            h.tvMeta.setText(meta.toString());

            String s = (t.status == null) ? "" : t.status.trim().toLowerCase(Locale.ROOT);
            String statusLabel;
            switch (s) {
                case "available":
                    statusLabel = "Eingestellt";
                    break;
                case "taken":
                    statusLabel = "Vergeben";
                    break;
                case "completed":
                    statusLabel = "Abgeschlossen";
                    break;
                default:
                    statusLabel = "Unbekannt";
            }
            h.tvStatus.setText(statusLabel);

            // Löschen nur bei eingestellten Themen
            if ("available".equals(s)) {
                h.btnDelete.setVisibility(View.VISIBLE);
                h.btnDelete.setOnClickListener(v -> {
                    if (onDelete != null) onDelete.onDelete(t);
                });
            } else {
                h.btnDelete.setVisibility(View.GONE);
                h.btnDelete.setOnClickListener(null);
            }

            h.itemView.setOnClickListener(v -> {
                if (onClick != null) onClick.onClick(t);
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }
}
