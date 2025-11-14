package de.iu.betreuerapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SupervisorProfileFragment extends Fragment {

    private TextView headerTitle;
    private TextView tvArea;
    private TextView tvEmail;
    private TextView tvDescription;
    private Button btnContact;

    private String currentSupervisorId;
    private String currentSupervisorName;
    private String currentSupervisorEmail;
    private String currentSupervisorExpertise;
    private String currentSupervisorDescription;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        if (!AuthGuard.requireRole(this, "student")) {
            return new View(requireContext());
        }

        View view = inflater.inflate(R.layout.fragment_supervisor_profile, container, false);

        headerTitle   = view.findViewById(R.id.header_title);
        tvArea        = view.findViewById(R.id.tv_area);
        tvEmail       = view.findViewById(R.id.tv_email);
        tvDescription = view.findViewById(R.id.tv_description);
        btnContact    = view.findViewById(R.id.btn_contact);

        readArgsAndBind();

        if (headerTitle != null && currentSupervisorName != null) headerTitle.setText(currentSupervisorName);

        btnContact.setOnClickListener(v -> openContactFragment());

        return view;
    }

    private void readArgsAndBind() {
        Bundle args = getArguments();
        if (args != null) {
            currentSupervisorId          = args.getString("supervisor_id", null);
            currentSupervisorName        = args.getString("supervisor_name", null);
            currentSupervisorEmail       = args.getString("supervisor_email", null);
            currentSupervisorExpertise   = args.getString("supervisor_expertise", null);
            currentSupervisorDescription = args.getString("supervisor_description", null);
        }

        if (currentSupervisorEmail != null) {
            SupervisorDirectory.Entry e = SupervisorDirectory.findByEmail(currentSupervisorEmail);
            if (e != null) {
                currentSupervisorId          = e.id;
                currentSupervisorName        = e.name;
                currentSupervisorExpertise   = e.area;
                currentSupervisorDescription = e.areaInfo;
            }
        }

        if (isEmpty(currentSupervisorName))  currentSupervisorName  = "Unbekannter Betreuer";
        if (isEmpty(currentSupervisorEmail)) currentSupervisorEmail = "Keine E-Mail verfügbar";

        // Header: "Betreuer: Name XY"
        headerTitle.setText("Betreuer: " + currentSupervisorName);

        if (!isEmpty(currentSupervisorExpertise)) {
            tvArea.setText("Fachbereich: " + currentSupervisorExpertise);
            tvArea.setVisibility(View.VISIBLE);
        } else {
            tvArea.setVisibility(View.GONE);
        }

        tvEmail.setText("E-Mail: " + currentSupervisorEmail);

        if (!isEmpty(currentSupervisorDescription)) {
            tvDescription.setText(currentSupervisorDescription);
            tvDescription.setVisibility(View.VISIBLE);
        } else {
            tvDescription.setVisibility(View.GONE);
        }
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private void openContactFragment() {
        if (isEmpty(currentSupervisorName) || isEmpty(currentSupervisorEmail)
                || "Unbekannter Betreuer".equals(currentSupervisorName)) {
            Toast.makeText(requireContext(),
                    "Kein gültiger Betreuer ausgewählt.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Bundle args = new Bundle();
        if (!isEmpty(currentSupervisorId)) {
            args.putString("supervisor_id", currentSupervisorId);
        }
        args.putString("supervisor_name", currentSupervisorName);
        args.putString("supervisor_email", currentSupervisorEmail);

        ContactFragment f = new ContactFragment();
        f.setArguments(args);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, f)
                .addToBackStack("supervisor_profile")
                .commit();
    }
}
