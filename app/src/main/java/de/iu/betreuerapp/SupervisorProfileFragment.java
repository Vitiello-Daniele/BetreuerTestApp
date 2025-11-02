package de.iu.betreuerapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

public class SupervisorProfileFragment extends Fragment {

    private TextView supervisorName, supervisorExpertise, supervisorEmail, supervisorDescription, supervisorTopics;
    private Button contactButton;

    private String currentSupervisorEmail;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_supervisor_profile, container, false);

        // UI Elemente initialisieren
        supervisorName = view.findViewById(R.id.supervisor_name);
        supervisorExpertise = view.findViewById(R.id.supervisor_expertise);
        supervisorEmail = view.findViewById(R.id.supervisor_email);
        supervisorDescription = view.findViewById(R.id.supervisor_description);
        supervisorTopics = view.findViewById(R.id.supervisor_topics);
        contactButton = view.findViewById(R.id.contact_button);

        // ✅ DYNAMISCHE DATEN VON DER SUCHE EMPFANGEN
        setDataFromArguments();

        // Kontakt-Button Click Listener
        contactButton.setOnClickListener(v -> contactSupervisor());

        return view;
    }

    private void setDataFromArguments() {
        // ✅ DATEN VON SEARCHFRAGMENT EMPFANGEN
        Bundle args = getArguments();
        if (args != null) {
            String name = args.getString("supervisor_name", "Prof. Dr. Elena Weber");
            String email = args.getString("supervisor_email", "elena.weber@iu.de");
            String expertise = args.getString("supervisor_expertise", "Wirtschaftsinformatik");
            String description = args.getString("supervisor_description", "Spezialisiert auf Mobile Development und Educational Technology.");

            // Daten anzeigen
            supervisorName.setText(name);
            supervisorExpertise.setText("Fachbereich: " + expertise);
            supervisorEmail.setText("E-Mail: " + email);
            supervisorDescription.setText(description);
            currentSupervisorEmail = email;

            // Themen basierend auf Fachbereich anzeigen
            setTopicsForExpertise(expertise);

        } else {
            // Fallback falls keine Daten übergeben wurden
            setSampleData();
        }
    }

    private void setTopicsForExpertise(String expertise) {
        // ✅ THEMEN BASIEREND AUF FACHRICHTUNG
        switch (expertise) {
            case "Wirtschaftsinformatik":
                supervisorTopics.setText("- Entwicklung einer Learning-Management-App\n- Mobile Payment-Sicherheit\n- E-Commerce Plattform Optimierung");
                break;
            case "Data Science":
                supervisorTopics.setText("- KI-gestützte Lernempfehlungen\n- Predictive Maintenance in Industrie 4.0\n- Data Mining in Bildungsdaten");
                break;
            case "IT-Sicherheit":
                supervisorTopics.setText("- Cybersecurity in Cloud-Infrastrukturen\n- Zero-Trust Security Modelle\n- Blockchain-Sicherheit");
                break;
            case "Software Engineering":
                supervisorTopics.setText("- Microservices Performance-Optimierung\n- DevOps Pipeline Automation\n- Code Quality Analysis Tools");
                break;
            case "Datenbanken":
                supervisorTopics.setText("- Big Data Processing Optimierung\n- NoSQL vs SQL Performance-Vergleich\n- Distributed Database Systems");
                break;
            case "Web Development":
                supervisorTopics.setText("- Progressive Web Apps (PWA)\n- Serverless Architecture Patterns\n- Real-time Web Applications");
                break;
            case "Projektmanagement":
                supervisorTopics.setText("- Agile Transformation in Unternehmen\n- Risk Management in IT-Projekten\n- Remote Team Collaboration Tools");
                break;
            case "Künstliche Intelligenz":
                supervisorTopics.setText("- Computer Vision für autonome Systeme\n- Natural Language Processing\n- Reinforcement Learning Applications");
                break;
            default:
                supervisorTopics.setText("- Entwicklung einer Betreuer-App\n- Machine Learning in mobilen Anwendungen\n- Cloud-Integration für Mobile Apps");
        }
    }

    private void setSampleData() {
        // Fallback-Daten
        supervisorName.setText("Prof. Dr. Elena Weber");
        supervisorExpertise.setText("Fachbereich: Wirtschaftsinformatik");
        supervisorEmail.setText("E-Mail: elena.weber@iu.de");
        supervisorDescription.setText("Spezialisiert auf Mobile Development und Educational Technology. 10+ Jahre Erfahrung in der Betreuung von Abschlussarbeiten im Bereich Software Engineering.");
        supervisorTopics.setText("- Entwicklung einer Betreuer-App\n- Machine Learning in mobilen Anwendungen\n- Cloud-Integration für Mobile Apps");
        currentSupervisorEmail = "elena.weber@iu.de";
    }

    private void contactSupervisor() {
        // E-Mail Intent erstellen mit AKTUELLER E-MAIL
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:" + currentSupervisorEmail));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Anfrage Betreuung Abschlussarbeit");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Sehr geehrte/r Betreuer/in,\n\nich interessiere mich für eine Betreuung meiner Abschlussarbeit.\n\nMit freundlichen Grüßen");

        // E-Mail App öffnen
        if (emailIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(emailIntent);
        }
    }
}