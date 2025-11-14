package de.iu.betreuerapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/**
 * "Betreuer finden" (Student-Sicht)
 */
public class SearchFragment extends Fragment {

    private EditText searchInput;
    private Spinner areaSpinner;
    private ListView listView;
    private TextView emptyView;
    private TextView tvTitle; // Überschrift angleichen

    private ArrayAdapter<SupervisorDirectory.Entry> adapter;

    private final List<SupervisorDirectory.Entry> allEntries =
            new ArrayList<>(Arrays.asList(SupervisorDirectory.getAll()));
    private final List<SupervisorDirectory.Entry> filteredEntries = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        if (!AuthGuard.requireRole(this, "student")) {
            return new View(requireContext());
        }

        View view = inflater.inflate(R.layout.fragment_search_supervisors, container, false);

        // Views
        searchInput = view.findViewById(R.id.et_search);
        areaSpinner = view.findViewById(R.id.spinner_area);
        listView    = view.findViewById(R.id.list_supervisors);
        emptyView   = view.findViewById(R.id.tv_empty);

        setupAreaSpinner();
        setupList();
        setupSearch();

        // initiale Liste
        applyFilter();

        return view;
    }

    // ----------------------------------------------------
    // Setup
    // ----------------------------------------------------

    private void setupAreaSpinner() {
        // Einmalige Liste mit "Alle Bereiche"
        List<String> areas = new ArrayList<>();
        areas.add("Alle Bereiche");

        HashSet<String> seen = new HashSet<>();
        for (SupervisorDirectory.Entry e : allEntries) {
            if (e == null) continue;
            if (e.area != null) {
                String a = e.area.trim();
                if (!a.isEmpty() && seen.add(a)) {
                    areas.add(a);
                }
            }
        }

        ArrayAdapter<String> areaAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                areas
        );
        areaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        areaSpinner.setAdapter(areaAdapter);
        areaSpinner.setSelection(0, false);

        // WICHTIG: sofort filtern, wenn Auswahl geändert wird
        areaSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilter();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void setupList() {
        adapter = new ArrayAdapter<SupervisorDirectory.Entry>(
                requireContext(),
                android.R.layout.simple_list_item_2,
                android.R.id.text1,
                filteredEntries
        ) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                TextView t1 = v.findViewById(android.R.id.text1);
                TextView t2 = v.findViewById(android.R.id.text2);

                SupervisorDirectory.Entry e = getItem(position);
                if (e != null) {
                    t1.setText(e.name != null ? e.name : "Unbekannte Betreuungsperson");

                    StringBuilder sb = new StringBuilder();
                    if (e.area != null && !e.area.isEmpty()) sb.append(e.area);
                    if (e.email != null && !e.email.isEmpty()) {
                        if (sb.length() > 0) sb.append(" • ");
                        sb.append(e.email);
                    }
                    if (e.areaInfo != null && !e.areaInfo.isEmpty()) {
                        if (sb.length() > 0) sb.append(" • ");
                        sb.append(e.areaInfo);
                    }
                    t2.setText(sb.toString());
                }

                return v;
            }
        };

        listView.setAdapter(adapter);
        listView.setEmptyView(emptyView);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= filteredEntries.size()) return;
            SupervisorDirectory.Entry e = filteredEntries.get(position);
            openSupervisorProfile(e);
        });
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) { applyFilter(); }
        });
    }

    // ----------------------------------------------------
    // Filter-Logik
    // ----------------------------------------------------
    private void applyFilter() {
        String query = (searchInput.getText() != null)
                ? searchInput.getText().toString().trim().toLowerCase(Locale.ROOT)
                : "";

        // "Alle Bereiche" => kein Area-Filter
        String selectedArea = null;
        Object sel = areaSpinner.getSelectedItem();
        if (sel != null) {
            String txt = sel.toString().trim();
            if (!txt.isEmpty() && !"Alle Bereiche".equalsIgnoreCase(txt)) {
                selectedArea = txt;
            }
        }

        filteredEntries.clear();

        for (SupervisorDirectory.Entry e : allEntries) {
            if (e == null) continue;

            // Fachbereich-Filter
            if (selectedArea != null) {
                String area = e.area != null ? e.area.trim() : "";
                if (!area.equalsIgnoreCase(selectedArea)) continue;
            }

            // Textsuche: Name + E-Mail + Fach + Beschreibung
            if (!query.isEmpty()) {
                String hay = (safe(e.name) + " " + safe(e.email) + " " + safe(e.area) + " " + safe(e.areaInfo))
                        .toLowerCase(Locale.ROOT);
                if (!hay.contains(query)) continue;
            }

            filteredEntries.add(e);
        }

        adapter.notifyDataSetChanged();

        if (emptyView != null) {
            boolean empty = filteredEntries.isEmpty();
            emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
            if (empty) emptyView.setText("Keine passenden Betreuer:innen gefunden.");
        }
    }

    private String safe(String s) { return s == null ? "" : s; }

    // ----------------------------------------------------
    // Navigation
    // ----------------------------------------------------
    private void openSupervisorProfile(SupervisorDirectory.Entry e) {
        if (e == null) return;

        Bundle args = new Bundle();
        args.putString("supervisor_id", e.id);
        args.putString("supervisor_name", e.name);
        args.putString("supervisor_email", e.email);
        args.putString("supervisor_expertise", e.area);
        args.putString("supervisor_description", e.areaInfo);

        SupervisorProfileFragment f = new SupervisorProfileFragment();
        f.setArguments(args);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, f)
                .addToBackStack("search_supervisors")
                .commit();
    }
}
