package de.iu.betreuerapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;

import de.iu.betreuerapp.dto.AuthResponse;
import de.iu.betreuerapp.dto.AuthSignInRequest;
import de.iu.betreuerapp.dto.Profile;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_login, container, false);

        EditText etEmail = v.findViewById(R.id.etEmail);
        EditText etPass  = v.findViewById(R.id.etPassword);
        Button btnLogin  = v.findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(view -> {
            String email = etEmail.getText().toString().trim();
            String pass  = etPass.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
                Toast.makeText(getContext(),
                        getString(R.string.msg_enter_email_pw),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            btnLogin.setEnabled(false);
            ((AuthActivity) requireActivity()).setLoading(true);

            SupabaseClient client = new SupabaseClient(requireContext());

            client.authService()
                    .signIn("password", new AuthSignInRequest(email, pass))
                    .enqueue(new Callback<AuthResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<AuthResponse> call,
                                               @NonNull Response<AuthResponse> resp) {

                            AuthResponse body = resp.body();

                            if (!resp.isSuccessful() || body == null
                                    || body.access_token == null
                                    || body.user == null
                                    || body.user.id == null) {
                                finishWith("Login fehlgeschlagen: " + resp.code());
                                return;
                            }

                            final String token = body.access_token;
                            final String uid   = body.user.id;

                            // Erstmal Token+UID speichern, damit REST-Calls laufen
                            SessionManager sm = new SessionManager(requireContext());
                            sm.save(token, uid);

                            // Profil nachladen → Rolle + Name + Email
                            client.restService()
                                    .getProfileById("eq." + uid)
                                    .enqueue(new Callback<List<Profile>>() {
                                        @Override
                                        public void onResponse(
                                                @NonNull Call<List<Profile>> call2,
                                                @NonNull Response<List<Profile>> r2) {

                                            ((AuthActivity) requireActivity()).setLoading(false);
                                            btnLogin.setEnabled(true);

                                            if (!r2.isSuccessful()
                                                    || r2.body() == null
                                                    || r2.body().isEmpty()) {
                                                Toast.makeText(getContext(),
                                                        "Profil nicht gefunden. Bitte Support kontaktieren.",
                                                        Toast.LENGTH_LONG).show();
                                                return;
                                            }

                                            Profile profile = r2.body().get(0);

                                            String role = profile.role;
                                            String emailFromProfile =
                                                    profile.email != null
                                                            ? profile.email
                                                            : body.user.email;

                                            String fullName = "";
                                            if (profile.first_name != null)
                                                fullName += profile.first_name + " ";
                                            if (profile.last_name != null)
                                                fullName += profile.last_name;
                                            fullName = fullName.trim();
                                            if (fullName.isEmpty()) fullName = null;

                                            // Alles in Session speichern
                                            SessionManager sm2 = new SessionManager(requireContext());
                                            sm2.save(token, uid, role, emailFromProfile, fullName);

                                            // Weiter ins Hauptmenü
                                            Intent intent = new Intent(requireContext(), MainActivity.class);
                                            startActivity(intent);
                                            requireActivity().finish();
                                        }

                                        @Override
                                        public void onFailure(
                                                @NonNull Call<List<Profile>> call2,
                                                @NonNull Throwable t2) {
                                            finishWith("Profil-Laden fehlgeschlagen: " + t2.getMessage());
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
                            btnLogin.setEnabled(true);
                            Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        return v;
    }
}
