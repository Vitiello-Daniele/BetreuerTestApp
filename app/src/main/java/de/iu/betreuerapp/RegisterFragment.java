package de.iu.betreuerapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;

import de.iu.betreuerapp.dto.AuthResponse;
import de.iu.betreuerapp.dto.AuthSignUpRequest;
import de.iu.betreuerapp.dto.Profile;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterFragment extends Fragment {

    private EditText etFirstName, etLastName, etEmail, etPassword, etPasswordRepeat;
    private Spinner spRole;
    private Button btnRegister;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_register, container, false);

        etFirstName = v.findViewById(R.id.etFirstName);
        etLastName = v.findViewById(R.id.etLastName);
        etEmail = v.findViewById(R.id.etEmail);
        etPassword = v.findViewById(R.id.etPassword);
        etPasswordRepeat = v.findViewById(R.id.etPasswordRepeat);
        spRole = v.findViewById(R.id.spRole);
        btnRegister = v.findViewById(R.id.btnRegister);

        // Beispiel: Rollen-Auswahl (an deine Strings anpassen)
        // z.B. im Layout oder per resources/arrays machen.
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.roles_array,              // ["Student", "Betreuer:in"]
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRole.setAdapter(adapter);

        btnRegister.setOnClickListener(view -> onRegisterClicked());

        return v;
    }

    private void onRegisterClicked() {
        String first = etFirstName.getText().toString().trim();
        String last = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();
        String pass2 = etPasswordRepeat.getText().toString().trim();

        if (TextUtils.isEmpty(first) || TextUtils.isEmpty(last)
                || TextUtils.isEmpty(email) || TextUtils.isEmpty(pass) || TextUtils.isEmpty(pass2)) {
            Toast.makeText(getContext(), "Bitte alle Felder ausfüllen", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!pass.equals(pass2)) {
            Toast.makeText(getContext(), "Passwörter stimmen nicht überein", Toast.LENGTH_SHORT).show();
            return;
        }

        String role = spRole.getSelectedItem().toString().trim();

        // Optional: Validierung, falls du später mal was änderst
        if (!"student".equals(role) && !"tutor".equals(role)) {
            Toast.makeText(getContext(), "Bitte eine Rolle wählen", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setEnabled(false);
        ((AuthActivity) requireActivity()).setLoading(true);

        SupabaseClient client = new SupabaseClient(requireContext());

        // 1) Supabase SignUp
        AuthSignUpRequest req = new AuthSignUpRequest(email, pass);
        client.authService().signUp(req).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call,
                                   @NonNull Response<AuthResponse> resp) {

                AuthResponse body = resp.body();

                if (!resp.isSuccessful() || body == null || body.user == null || body.user.id == null) {
                    finishWith("Registrierung fehlgeschlagen: " + resp.code());
                    return;
                }

                final String uid = body.user.id;
                final String token = body.access_token; // Kann je nach Supabase-Config null sein

                SessionManager sm = new SessionManager(requireContext());
                if (token != null) {
                    sm.save(token, uid); // Token + UserId
                }

                // 2) Profil upserten
                Profile profile = new Profile();
                profile.id = uid;
                profile.email = email;
                profile.first_name = first;
                profile.last_name = last;
                profile.role = role;

                client.restService()
                    .upsertProfile(profile)
                    .enqueue(new Callback<List<Profile>>() {
                        @Override
                        public void onResponse(@NonNull Call<List<Profile>> call2,
                                               @NonNull Response<List<Profile>> r2) {
                            if (!r2.isSuccessful()) {
                                finishWith("Profil konnte nicht gespeichert werden: " + r2.code());
                                return;
                            }

                            SessionManager sm2 = new SessionManager(requireContext());
                            sm2.save(token, uid, role);

                            ((AuthActivity) requireActivity()).setLoading(false);
                            btnRegister.setEnabled(true);

                            startActivity(new Intent(requireContext(), MainActivity.class));
                            requireActivity().finish();
                        }

                        @Override
                        public void onFailure(@NonNull Call<List<Profile>> call2,
                                              @NonNull Throwable t2) {
                            finishWith("Fehler beim Speichern des Profils: " + t2.getMessage());
                        }
                    });
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call,
                                  @NonNull Throwable t) {
                finishWith("Netzfehler: " + t.getMessage());
            }

            private void finishWith(String msg) {
                ((AuthActivity) requireActivity()).setLoading(false);
                btnRegister.setEnabled(true);
                Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Mappt die Anzeige-Strings im Spinner auf die Werte,
     * die du in der DB im Feld "role" speicherst.
     */
    private String mapRole(String selected) {
        if (selected == null) return null;
        selected = selected.toLowerCase();

        if (selected.contains("student")) {
            return "student";
        } else if (selected.contains("betreuer")) {
            return "tutor";
        }
        return null;
    }
}
