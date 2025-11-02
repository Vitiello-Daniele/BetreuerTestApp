package de.iu.betreuerapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.fragment.app.Fragment;

public class ContactFragment extends Fragment {

    private EditText nameInput, emailInput, messageInput;
    private Button sendButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contact, container, false);

        // UI Elemente initialisieren
        nameInput = view.findViewById(R.id.name_input);
        emailInput = view.findViewById(R.id.email_input);
        messageInput = view.findViewById(R.id.message_input);
        sendButton = view.findViewById(R.id.send_button);

        // Upload-Button initialisieren und Listener hinzufügen
        Button uploadButton = view.findViewById(R.id.upload_expose_button);

        // Send-Button Click Listener
        sendButton.setOnClickListener(v -> sendEmail());

        // Upload-Button Click Listener
        uploadButton.setOnClickListener(v -> navigateToExposeUpload());

        return view;
    }

    private void sendEmail() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String message = messageInput.getText().toString().trim();

        // Validierung
        if (name.isEmpty() || email.isEmpty() || message.isEmpty()) {
            Toast.makeText(requireContext(), "Bitte füllen Sie alle Felder aus", Toast.LENGTH_SHORT).show();
            return;
        }

        // E-Mail Intent erstellen
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:betreuer@iu.de")); // Allgemeine Betreuer-E-Mail

        // E-Mail Inhalt
        String subject = "Kontaktanfrage von " + name;
        String body = "Name: " + name +
                "\nE-Mail: " + email +
                "\n\nNachricht:\n" + message +
                "\n\nDiese Nachricht wurde über die Betreuer-App gesendet.";

        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, body);

        // E-Mail App öffnen
        try {
            startActivity(Intent.createChooser(emailIntent, "E-Mail senden mit..."));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Keine E-Mail-App gefunden", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToExposeUpload() {
        // ExposeUploadFragment erstellen
        ExposeUploadFragment uploadFragment = new ExposeUploadFragment();

        // Fragment wechseln
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, uploadFragment)
                .addToBackStack("contact") // Zurück-Navigation ermöglichen
                .commit();
    }
}