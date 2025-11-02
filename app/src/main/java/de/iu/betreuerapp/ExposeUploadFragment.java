package de.iu.betreuerapp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

public class ExposeUploadFragment extends Fragment {

    private Button selectFileButton, uploadButton;
    private TextView fileNameText, statusText;
    private Uri selectedFileUri;

    // Activity Result Launcher für Dateiauswahl
    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedFileUri = result.getData().getData();
                    if (selectedFileUri != null) {
                        String fileName = getFileName(selectedFileUri);
                        fileNameText.setText("Ausgewählte Datei: " + fileName);
                        statusText.setText("Datei ausgewählt - Bereit zum Hochladen");
                        uploadButton.setEnabled(true);
                    }
                }
            }
    );

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expose_upload, container, false);

        // UI Elemente initialisieren
        selectFileButton = view.findViewById(R.id.select_file_button);
        uploadButton = view.findViewById(R.id.upload_button);
        fileNameText = view.findViewById(R.id.file_name_text);
        statusText = view.findViewById(R.id.status_text);

        // Upload-Button zunächst deaktivieren
        uploadButton.setEnabled(false);

        // Dateiauswahl-Button Click Listener
        selectFileButton.setOnClickListener(v -> openFilePicker());

        // Upload-Button Click Listener
        uploadButton.setOnClickListener(v -> uploadFile());

        return view;
    }

    private void openFilePicker() {
        // Intent für Dateiauswahl erstellen
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Alle Dateitypen erlauben
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Dateiauswahl starten
        filePickerLauncher.launch(Intent.createChooser(intent, "Exposé auswählen"));
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (var cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex("_display_name");
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void uploadFile() {
        if (selectedFileUri == null) {
            Toast.makeText(requireContext(), "Bitte wählen Sie zuerst eine Datei aus", Toast.LENGTH_SHORT).show();
            return;
        }

        // Upload-Simulation (später mit Supabase)
        statusText.setText("Upload wird durchgeführt...");
        uploadButton.setEnabled(false);

        // Simuliere Upload (2 Sekunden)
        new android.os.Handler().postDelayed(() -> {
            statusText.setText("Upload erfolgreich! Exposé wurde hochgeladen.");
            fileNameText.setText("Datei hochgeladen");
            selectedFileUri = null;

            Toast.makeText(requireContext(), "Exposé erfolgreich hochgeladen!", Toast.LENGTH_LONG).show();
        }, 2000);
    }
}