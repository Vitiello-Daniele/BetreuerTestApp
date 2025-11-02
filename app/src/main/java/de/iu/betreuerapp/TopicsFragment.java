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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import java.util.ArrayList;
import java.util.List;

public class TopicsFragment extends Fragment {

    private ListView topicsListView;
    private Spinner filterSpinner;
    private Button addTopicButton;
    private ArrayAdapter<SharedViewModel.Topic> adapter;
    private SharedViewModel sharedViewModel;
    private List<SharedViewModel.Topic> currentDisplayList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_topics_management, container, false);

        // Shared ViewModel holen
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // UI Elemente initialisieren
        topicsListView = view.findViewById(R.id.topics_listview);
        filterSpinner = view.findViewById(R.id.filter_spinner);
        addTopicButton = view.findViewById(R.id.add_topic_button);

        // Filter und Liste einrichten
        setupFilterSpinner();
        setupTopicsListObserver();

        // Add-Button Listener
        addTopicButton.setOnClickListener(v -> navigateToAddTopic());

        return view;
    }

    private void setupFilterSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.topic_status_filter,
                android.R.layout.simple_spinner_item
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(adapter);
    }

    private void setupTopicsListObserver() {
        sharedViewModel.getTopicList().observe(getViewLifecycleOwner(), topics -> {
            if (topics != null && !topics.isEmpty()) {
                currentDisplayList = new ArrayList<>(topics);
                setupTopicsList(currentDisplayList);
            }
        });
    }

    private void setupTopicsList(List<SharedViewModel.Topic> topics) {
        adapter = new ArrayAdapter<SharedViewModel.Topic>(
                requireContext(),
                android.R.layout.simple_list_item_2,
                android.R.id.text1,
                topics
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);

                SharedViewModel.Topic topic = getItem(position);
                if (topic != null) {
                    text1.setText(topic.getTitle());
                    text2.setText("Bereich: " + topic.getArea() + " | Status: " + getStatusText(topic.getStatus()));
                }
                return view;
            }
        };

        topicsListView.setAdapter(adapter);

        // ✅ KLICK: Zum Bearbeiten navigieren
        topicsListView.setOnItemClickListener((parent, view, position, id) -> {
            SharedViewModel.Topic selectedTopic = currentDisplayList.get(position);
            navigateToEditTopic(selectedTopic, position);
        });

        // ✅ LONG-CLICK: Löschen
        topicsListView.setOnItemLongClickListener((parent, view, position, id) -> {
            SharedViewModel.Topic selectedTopic = currentDisplayList.get(position);
            showDeleteConfirmation(selectedTopic, position);
            return true;
        });
    }

    private String getStatusText(String status) {
        switch (status) {
            case "available": return "Verfügbar";
            case "taken": return "Vergeben";
            case "completed": return "Abgeschlossen";
            default: return "Unbekannt";
        }
    }

    private void navigateToAddTopic() {
        AddTopicFragment addTopicFragment = new AddTopicFragment();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, addTopicFragment)
                .addToBackStack("topics")
                .commit();
    }

    private void navigateToEditTopic(SharedViewModel.Topic topic, int position) {
        EditTopicFragment editTopicFragment = new EditTopicFragment();

        // ✅ DATEN AN EDIT-FRAGMENT ÜBERGEBEN
        Bundle args = new Bundle();
        args.putInt("topic_position", position);
        args.putString("topic_title", topic.getTitle());
        args.putString("topic_area", topic.getArea());
        args.putString("topic_status", topic.getStatus());
        args.putString("topic_description", topic.getDescription());
        editTopicFragment.setArguments(args);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, editTopicFragment)
                .addToBackStack("topics")
                .commit();
    }

    private void showDeleteConfirmation(SharedViewModel.Topic topic, int position) {
        // ✅ ECHTES LÖSCHEN über ViewModel
        sharedViewModel.deleteTopic(position);
        android.util.Log.d("Topics", "Thema gelöscht: " + topic.getTitle());
    }
}