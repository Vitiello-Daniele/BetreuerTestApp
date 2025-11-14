package de.iu.betreuerapp;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;

import de.iu.betreuerapp.dto.ContactRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Allgemeine Betreuungsanfrage:
 * - strukturierte Nachricht
 * - Exposé ist Pflicht
 * - student_name / student_email aus Session
 * - Header: ORANGE, zeigt "Betreuer: <Name>"
 * - Buttons: YELLOW (bg_button_primary)
 *
 * Speziell für Themenbörse:
 * - Wenn topic_id gesetzt ist, kommen Titel / Beschreibung vom Thema
 *   und sind NICHT editierbar. Der Student lädt nur noch das Exposé hoch.
 */
public class ContactFragment extends Fragment {

    private TextView headerName;
    private TextView supervisorInfoText;
    private EditText thesisTitleInput;
    private EditText thesisDescriptionInput;
    private Button uploadExposeButton;
    private TextView exposeStatusText;
    private Button sendButton;

    private String supervisorId;
    private String supervisorName;
    private String supervisorEmail;

    private String topicId;
    private String topicTitle;
    private String topicArea;
    private String topicDesc;     // NEU: Beschreibung aus Themenbörse

    private String exposeUrl = null;
    private ActivityResultLauncher<String> exposePicker;

    // Flag: Anfrage kommt aus Themenbörse → Felder sperren
    private boolean lockTopicFields = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        exposePicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        exposeUrl = uri.toString();
                        if (exposeStatusText != null) {
                            String fileLabel = "Exposé: " + getDisplayName(uri);
                            exposeStatusText.setText(fileLabel);
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        if (!AuthGuard.requireRole(this, "student")) {
            return new View(requireContext());
        }

        View view = inflater.inflate(R.layout.fragment_contact, container, false);

        headerName             = view.findViewById(R.id.header_supervisor_name);
        supervisorInfoText     = view.findViewById(R.id.supervisor_info_text);
        thesisTitleInput       = view.findViewById(R.id.thesis_title_input);
        thesisDescriptionInput = view.findViewById(R.id.thesis_description_input);
        uploadExposeButton     = view.findViewById(R.id.upload_expose_button);
        exposeStatusText       = view.findViewById(R.id.expose_status_text);
        sendButton             = view.findViewById(R.id.send_button);

        readArgsAndBind();

        // Header-Name setzen (falls null → Strich bleibt aus XML)
        if (headerName != null && supervisorName != null && !supervisorName.trim().isEmpty()) {
            headerName.setText(supervisorName);
        }

        uploadExposeButton.setOnClickListener(v -> openExposePicker());
        sendButton.setOnClickListener(v -> sendContactRequest());

        return view;
    }

    private void readArgsAndBind() {
        Bundle args = getArguments();
        if (args != null) {
            supervisorId    = args.getString("supervisor_id", null);
            supervisorName  = args.getString("supervisor_name", null);
            supervisorEmail = args.getString("supervisor_email", null);

            topicId    = args.getString("topic_id", null);
            topicTitle = args.getString("topic_title", null);
            topicArea  = args.getString("topic_area", null);
            topicDesc  = args.getString("topic_desc", null); // NEU
        }

        // Anfrage stammt aus Themenbörse, wenn eine topic_id übergeben wurde
        lockTopicFields = (topicId != null);

        // Falls nur E-Mail bekannt → Directory-Daten anreichern (inkl. area)
        if (supervisorId == null && supervisorEmail != null) {
            SupervisorDirectory.Entry e = SupervisorDirectory.findByEmail(supervisorEmail);
            if (e != null) {
                supervisorId    = e.id;
                supervisorName  = e.name;
                supervisorEmail = e.email;
                if (topicArea == null) topicArea = e.area;
            }
        }

        // Info-Block oben
        if (supervisorName != null) {
            String txt = "Kontakt mit: " + supervisorName;
            if (supervisorEmail != null) txt += " (" + supervisorEmail + ")";
            if (topicTitle != null) txt += "\nThema: " + topicTitle;
            supervisorInfoText.setText(txt);
        } else {
            supervisorInfoText.setText("Keine gültige Betreuungsperson ausgewählt.");
        }

        // Titel / Beschreibung vorbelegen, wenn aus Themenbörse
        if (topicTitle != null && !topicTitle.isEmpty()) {
            thesisTitleInput.setText(topicTitle);
        }

        if (lockTopicFields) {
            // Beschreibung aus Thema verwenden, falls vorhanden
            if (topicDesc != null && !topicDesc.isEmpty()) {
                thesisDescriptionInput.setText(topicDesc);
            }

            // Felder sperren – Student kann Titel/Beschreibung NICHT ändern
            thesisTitleInput.setEnabled(false);
            thesisTitleInput.setFocusable(false);
            thesisTitleInput.setFocusableInTouchMode(false);

            thesisDescriptionInput.setEnabled(false);
            thesisDescriptionInput.setFocusable(false);
            thesisDescriptionInput.setFocusableInTouchMode(false);
        }

        // Default-Status für Exposé steht schon in XML
    }

    private void openExposePicker() {
        exposePicker.launch("*/*");
    }

    private void sendContactRequest() {
        SessionManager sm = new SessionManager(requireContext());
        String studentId    = sm.userId();
        String studentName  = sm.name();
        String studentEmail = sm.email();

        if (studentId == null) {
            Toast.makeText(requireContext(),
                    "Fehler: Benutzer nicht erkannt. Bitte neu anmelden.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if ((supervisorId == null || supervisorId.trim().isEmpty())
                && (supervisorEmail == null || supervisorEmail.trim().isEmpty())) {
            Toast.makeText(requireContext(),
                    "Fehler: Keine gültige Betreuungsperson ausgewählt.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String title = safeTrim(thesisTitleInput);
        String desc  = safeTrim(thesisDescriptionInput);

        if (title.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Bitte gib einen Arbeitstitel ein.",
                    Toast.LENGTH_SHORT).show();
            thesisTitleInput.requestFocus();
            return;
        }

        if (desc.isEmpty()) {
            // Bei Themenbörse sollte desc eigentlich aus dem Thema kommen
            Toast.makeText(requireContext(),
                    "Bitte gib eine Kurzbeschreibung ein.",
                    Toast.LENGTH_SHORT).show();
            thesisDescriptionInput.requestFocus();
            return;
        }

        if (exposeUrl == null || exposeUrl.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Bitte lade dein Exposé hoch (Pflicht).",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Strukturierte Nachricht → Dashboard kann Fachgebiet auslesen
        StringBuilder msg = new StringBuilder();
        msg.append("Titel: ").append(title).append("\n");
        if (topicArea != null && !topicArea.isEmpty()) {
            msg.append("Fachgebiet: ").append(topicArea).append("\n");
        }
        msg.append("Beschreibung:\n").append(desc);

        ContactRequest req = new ContactRequest();
        req.student_id       = studentId;
        req.student_name     = studentName;
        req.student_email    = studentEmail;

        req.supervisor_id    = supervisorId;
        req.supervisor_name  = supervisorName;
        req.supervisor_email = supervisorEmail;

        req.topic_id         = topicId;
        req.message          = msg.toString();
        req.expose_url       = exposeUrl;
        req.status           = "open";
        req.second_reviewer_status = null;

        sendButton.setEnabled(false);

        SupabaseClient client = new SupabaseClient(requireContext());
        client.restService()
                .createContactRequest(req)
                .enqueue(new Callback<List<ContactRequest>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<ContactRequest>> call,
                                           @NonNull Response<List<ContactRequest>> response) {
                        sendButton.setEnabled(true);

                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(requireContext(),
                                    "Fehler beim Senden: " + response.code(),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        Toast.makeText(requireContext(),
                                "Anfrage gesendet. Du findest sie unter 'Meine Arbeiten'.",
                                Toast.LENGTH_LONG).show();
                        navigateToMyTheses();
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<ContactRequest>> call,
                                          @NonNull Throwable t) {
                        sendButton.setEnabled(true);
                        Toast.makeText(requireContext(),
                                "Netzwerkfehler: " + t.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void navigateToMyTheses() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openTab(R.id.nav_my_theses);
            requireActivity().getSupportFragmentManager()
                    .popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
            return;
        }
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new StudentDashboardFragment())
                .commit();
    }

    private String safeTrim(EditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private String getDisplayName(Uri uri) {
        String name = null;
        try (Cursor c = requireContext().getContentResolver()
                .query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = c.getString(idx);
            }
        } catch (Exception ignored) {}
        if (name == null) {
            String last = uri.getLastPathSegment();
            if (last != null) {
                int slash = last.lastIndexOf('/');
                name = (slash >= 0) ? last.substring(slash + 1) : last;
            }
        }
        return (name == null || name.isEmpty()) ? uri.toString() : name;
    }
}
