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

import java.util.List;

import de.iu.betreuerapp.dto.Profile;
import de.iu.betreuerapp.dto.Topic;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TopicDetailFragment extends Fragment {

    private static final String ARG_ID = "topic_id";
    private static final String ARG_TITLE = "topic_title";
    private static final String ARG_DESC = "topic_desc";
    private static final String ARG_AREA = "topic_area";
    private static final String ARG_TUTOR_ID = "tutor_id";
    private static final String ARG_TUTOR_NAME = "tutor_name";
    private static final String ARG_TUTOR_EMAIL = "tutor_email";

    /** Wird aus StudentTopicsFragment aufgerufen. */
    public static TopicDetailFragment newInstanceFromTopic(Topic t) {
        TopicDetailFragment f = new TopicDetailFragment();
        Bundle b = new Bundle();
        b.putString(ARG_ID, t.id);
        b.putString(ARG_TITLE, t.title);
        b.putString(ARG_DESC, t.description);
        b.putString(ARG_AREA, t.area);
        b.putString(ARG_TUTOR_ID, t.owner_id);
        f.setArguments(b);
        return f;
    }

    private String topicId;
    private String title;
    private String desc;
    private String area;
    private String tutorId;
    private String tutorName;
    private String tutorEmail;

    private TextView tvTutor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        if (!AuthGuard.requireRole(this, "student")) {
            return new View(requireContext());
        }

        View v = inflater.inflate(R.layout.fragment_topic_detail, container, false);

        TextView tvTitle = v.findViewById(R.id.tv_title);
        tvTutor = v.findViewById(R.id.tv_tutor);
        TextView tvDesc = v.findViewById(R.id.tv_desc);
        Button btnRequest = v.findViewById(R.id.btn_request);

        Bundle args = getArguments();
        if (args != null) {
            topicId = args.getString(ARG_ID);
            title   = args.getString(ARG_TITLE);
            desc    = args.getString(ARG_DESC);
            area    = args.getString(ARG_AREA);
            tutorId = args.getString(ARG_TUTOR_ID);

            tutorName  = args.getString(ARG_TUTOR_NAME);
            tutorEmail = args.getString(ARG_TUTOR_EMAIL);
        }

        tvTitle.setText(title != null ? title : "Thema");

        // Beschreibung + Fachgebiet
        StringBuilder descBuilder = new StringBuilder();
        if (desc != null && !desc.isEmpty()) {
            descBuilder.append(desc);
        } else {
            descBuilder.append("Keine Beschreibung vorhanden.");
        }
        if (area != null && !area.isEmpty()) {
            descBuilder.append("\n\nFachgebiet: ").append(area);
        }
        tvDesc.setText(descBuilder.toString());

        // Betreuer anzeigen / nachladen
        if (tutorId != null) {
            SupervisorDirectory.Entry e = SupervisorDirectory.findById(tutorId);
            if (e != null) {
                tutorName  = e.name;
                tutorEmail = e.email;

                StringBuilder sb = new StringBuilder();
                if (e.name != null) sb.append(e.name);
                if (e.email != null) sb.append(" (").append(e.email).append(")");
                if (e.area != null && !e.area.isEmpty()) {
                    sb.append("\n").append(e.area);
                }
                tvTutor.setText(sb.toString());
            } else {
                tvTutor.setText("wird geladen...");
                loadTutorProfile(tutorId);
            }
        } else if (tutorName != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(tutorName);
            if (tutorEmail != null) sb.append(" (").append(tutorEmail).append(")");
            tvTutor.setText(sb.toString());
        } else {
            tvTutor.setText("unbekannt");
        }

        btnRequest.setOnClickListener(click -> openContactForm());

        return v;
    }

    private void loadTutorProfile(@NonNull String tutorId) {
        SupabaseClient client = new SupabaseClient(requireContext());
        client.restService()
                .getProfileById("eq." + tutorId)
                .enqueue(new Callback<List<Profile>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Profile>> call,
                                           @NonNull Response<List<Profile>> response) {
                        if (!response.isSuccessful()
                                || response.body() == null
                                || response.body().isEmpty()) {
                            if (tvTutor != null) {
                                tvTutor.setText("unbekannt");
                            }
                            return;
                        }

                        Profile p = response.body().get(0);
                        StringBuilder nameBuilder = new StringBuilder();
                        if (p.first_name != null) nameBuilder.append(p.first_name).append(" ");
                        if (p.last_name != null)  nameBuilder.append(p.last_name);
                        String fullName = nameBuilder.toString().trim();

                        tutorName  = fullName.isEmpty() ? "Unbekannt" : fullName;
                        tutorEmail = p.email;

                        if (tvTutor != null) {
                            StringBuilder sb = new StringBuilder();
                            sb.append(tutorName);
                            if (tutorEmail != null) sb.append(" (").append(tutorEmail).append(")");
                            tvTutor.setText(sb.toString());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<Profile>> call,
                                          @NonNull Throwable t) {
                        if (tvTutor != null) {
                            tvTutor.setText("unbekannt");
                        }
                    }
                });
    }

    /**
     * Öffnet das einheitliche Anfrage-Formular (ContactFragment).
     * Bei Aufruf aus der Themenbörse werden Titel/Beschreibung übergeben
     * und im Formular gesperrt (siehe ContactFragment).
     */
    private void openContactForm() {
        if (tutorId == null && tutorEmail == null) {
            Toast.makeText(requireContext(),
                    "Keine gültige Betreuungsperson gefunden.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        Bundle b = new Bundle();
        b.putString("supervisor_id", tutorId);
        b.putString("supervisor_name", tutorName);
        b.putString("supervisor_email", tutorEmail);
        b.putString("topic_id", topicId);
        b.putString("topic_title", title);
        b.putString("topic_area", area);
        b.putString("topic_desc", desc); // NEU: Beschreibung ins Formular übernehmen

        ContactFragment f = new ContactFragment();
        f.setArguments(b);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, f)
                .addToBackStack("topic_detail")
                .commit();
    }
}
