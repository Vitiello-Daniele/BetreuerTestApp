package de.iu.betreuerapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.iu.betreuerapp.dto.Topic;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentTopicsFragment extends Fragment {

    private RecyclerView rvTopics;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private Spinner areaFilterSpinner;

    private TopicsAdapter adapter;
    private final List<Topic> topics = new ArrayList<>();

    // aktuell gewählter Fachbereich (null = alle)
    private String selectedArea = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        if (!AuthGuard.requireRole(this, "student")) {
            return new View(requireContext());
        }

        View view = inflater.inflate(R.layout.fragment_student_topics, container, false);

        rvTopics = view.findViewById(R.id.rv_topics);
        progressBar = view.findViewById(R.id.progress_bar);
        tvEmpty = view.findViewById(R.id.tv_empty);
        areaFilterSpinner = view.findViewById(R.id.spinner_area_filter);

        rvTopics.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TopicsAdapter(topics, this::openTopicDetail);
        rvTopics.setAdapter(adapter);

        setupAreaFilterSpinner();

        return view;
    }

    private void setupAreaFilterSpinner() {
        // Basis-Einträge aus resources
        String[] baseAreas = getResources().getStringArray(R.array.expertise_areas);
        List<String> entries = new ArrayList<>(Arrays.asList(baseAreas));

        final String ALL_LABEL = "Alle Bereiche";

        // "Alle Bereiche" genau einmal an Position 0
        int idx = entries.indexOf(ALL_LABEL);
        if (idx >= 0) {
            if (idx != 0) {
                entries.remove(idx);
                entries.add(0, ALL_LABEL);
            }
        } else {
            entries.add(0, ALL_LABEL);
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                entries
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        areaFilterSpinner.setAdapter(spinnerAdapter);

        areaFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent,
                                       View view,
                                       int position,
                                       long id) {
                if (position == 0) {
                    selectedArea = null; // alle Bereiche
                } else {
                    selectedArea = entries.get(position);
                }
                loadTopics(selectedArea);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // ignorieren
            }
        });

        // Initial: alle laden
        selectedArea = null;
        loadTopics(null);
    }

    /**
     * Lädt verfügbare Themen von Supabase.
     * areaFilter = null → alle Bereiche.
     */
    private void loadTopics(@Nullable String areaFilter) {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        SupabaseClient client = new SupabaseClient(requireContext());

        String statusEq = "eq.available";
        String areaEq = (areaFilter != null && !areaFilter.isEmpty())
                ? "eq." + areaFilter
                : null;

        client.restService()
                .getAvailableTopics(statusEq, areaEq, "created_at.desc")
                .enqueue(new Callback<List<Topic>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Topic>> call,
                                           @NonNull Response<List<Topic>> response) {
                        progressBar.setVisibility(View.GONE);

                        if (!response.isSuccessful() || response.body() == null) {
                            tvEmpty.setText("Fehler beim Laden der Themen (" + response.code() + ")");
                            tvEmpty.setVisibility(View.VISIBLE);
                            topics.clear();
                            adapter.notifyDataSetChanged();
                            return;
                        }

                        topics.clear();
                        topics.addAll(response.body());
                        adapter.notifyDataSetChanged();

                        if (topics.isEmpty()) {
                            tvEmpty.setText("Aktuell keine offenen Themen verfügbar.");
                            tvEmpty.setVisibility(View.VISIBLE);
                        } else {
                            tvEmpty.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<Topic>> call,
                                          @NonNull Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        tvEmpty.setText("Netzwerkfehler: " + t.getMessage());
                        tvEmpty.setVisibility(View.VISIBLE);
                        topics.clear();
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void openTopicDetail(Topic topic) {
        TopicDetailFragment f = TopicDetailFragment.newInstanceFromTopic(topic);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, f)
                .addToBackStack("public_topics")
                .commit();
    }

    // -------------------- Adapter --------------------

    private static class TopicsAdapter extends RecyclerView.Adapter<TopicsAdapter.VH> {

        interface OnTopicClick {
            void onClick(Topic t);
        }

        private final List<Topic> data;
        private final OnTopicClick onClick;

        TopicsAdapter(List<Topic> data, OnTopicClick onClick) {
            this.data = data;
            this.onClick = onClick;
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvArea, tvTutor, tvDesc;
            VH(@NonNull View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tv_title);
                tvArea  = v.findViewById(R.id.tv_area);
                tvTutor = v.findViewById(R.id.tv_tutor);
                tvDesc  = v.findViewById(R.id.tv_desc);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_student_topic, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Topic t = data.get(position);
            if (t == null) return;

            // Titel
            String title = (t.title != null && !t.title.isEmpty())
                    ? t.title
                    : "Thema";
            holder.tvTitle.setText(title);

            // Fachgebiet
            String areaText = (t.area != null && !t.area.isEmpty())
                    ? t.area
                    : "–";
            holder.tvArea.setText("Fachgebiet: " + areaText);

            // Tutor (über SupervisorDirectory, falls vorhanden)
            String tutorLabel = "Tutor: –";
            if (t.owner_id != null) {
                SupervisorDirectory.Entry e = SupervisorDirectory.findById(t.owner_id);
                if (e != null) {
                    StringBuilder tb = new StringBuilder();
                    if (e.name != null && !e.name.isEmpty()) {
                        tb.append(e.name);
                    }
                    if (e.email != null && !e.email.isEmpty()) {
                        if (tb.length() > 0) tb.append(" (").append(e.email).append(")");
                        else tb.append(e.email);
                    }
                    if (tb.length() > 0) {
                        tutorLabel = "Tutor: " + tb.toString();
                    }
                }
            }
            holder.tvTutor.setText(tutorLabel);

            // Beschreibung (ggf. kürzen)
            String desc = (t.description != null) ? t.description.trim() : "";
            if (desc.isEmpty()) {
                holder.tvDesc.setText("Beschreibung: –");
            } else {
                if (desc.length() > 120) {
                    desc = desc.substring(0, 117) + "...";
                }
                holder.tvDesc.setText("Beschreibung: " + desc);
            }

            holder.itemView.setOnClickListener(v -> onClick.onClick(t));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }
}
