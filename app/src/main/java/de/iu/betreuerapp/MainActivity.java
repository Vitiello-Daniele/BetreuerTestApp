package de.iu.betreuerapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        // Wenn kein Token oder keine Rolle → zurück zum Login
        if (sessionManager.token() == null || sessionManager.role() == null) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_nav);

        String initialRole = sessionManager.role();
        setupNavigationByRole(initialRole);

        if (savedInstanceState == null) {
            openStartFragment(initialRole);
        }
    }

    // --- Rollen-Helper -------------------------------------------------------

    private boolean isStudent(@NonNull String role) {
        return "student".equalsIgnoreCase(role);
    }

    private boolean isTutorLike(@NonNull String role) {
        // hier kannst du später zusätzliche Rollen ergänzen
        return "tutor".equalsIgnoreCase(role);
    }

    // --- Navigation Setup ----------------------------------------------------

    private void setupNavigationByRole(String role) {
        bottomNav.getMenu().clear();

        if (isStudent(role)) {
            // Student-Menü:
            // Meine Arbeiten | Themenbörse | Rechnungen | Logout
            bottomNav.inflateMenu(R.menu.menu_student);
        } else if (isTutorLike(role)) {
            // Tutor-Menü:
            // Offene Themen | Anfragen | Betreute Arbeiten | Zweitprüfer | Rechnungen | Logout
            bottomNav.inflateMenu(R.menu.menu_tutor);
        } else {
            // Fallback: wie Student behandeln
            bottomNav.inflateMenu(R.menu.menu_student);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            // Rolle bei jedem Klick frisch lesen (falls sich was geändert hat)
            String currentRole = sessionManager.role();
            if (currentRole == null) {
                logout();
                return true;
            }

            int id = item.getItemId();
            Fragment target = null;

            // --- Student-Navigation ---
            if (isStudent(currentRole)) {
                if (id == R.id.nav_my_theses) {
                    // Meine Anfragen / betreute Arbeiten aus Studierenden-Sicht
                    target = new StudentDashboardFragment();
                } else if (id == R.id.nav_public_topics) {
                    // Themenbörse (öffentliche Themen der Tutoren)
                    target = new StudentTopicsFragment();
                } else if (id == R.id.nav_student_invoices) {
                    // Rechnungen aus Sicht Student (Offen / Bezahlt)
                    target = new StudentInvoicesFragment();
                } else if (id == R.id.nav_logout) {
                    logout();
                    return true;
                }
            }
            // --- Tutor-Navigation ---
            else if (isTutorLike(currentRole)) {
                if (id == R.id.nav_topics) {
                    // Offene Themen, eigene Ausschreibungen + "Neue hinzufügen"
                    target = new TopicsFragment();
                } else if (id == R.id.nav_requests) {
                    // Anfragen von Studierenden (contact_requests)
                    target = new TutorRequestsFragment();
                } else if (id == R.id.nav_theses) {
                    // Betreute Arbeiten (angenommene Anfragen, Statuspflege, Zweitgutachter)
                    target = new TutorThesesTabsFragment();
                } else if (id == R.id.nav_invoices) {
                    // Rechnungen / Zahlungsstatus für betreute Arbeiten
                    target = new TutorInvoicesFragment();
                } else if (id == R.id.nav_logout) {
                    logout();
                    return true;
                }
            }

            // Fragment-Wechsel ausführen
            if (target != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, target)
                        .commit();
                return true;
            }

            return false;
        });
    }

    // --- Start-Screen je Rolle -----------------------------------------------

    private void openStartFragment(String role) {
        Fragment start;

        if (role != null && isStudent(role)) {
            // Studierende starten auf "Meine Arbeiten"
            start = new StudentDashboardFragment();
        } else if (role != null && isTutorLike(role)) {
            // Tutor:innen starten auf "Offene Themen"
            start = new TopicsFragment();
        } else {
            // Fallback: Student-Dashboard
            start = new StudentDashboardFragment();
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, start)
                .commit();
    }

    // --- Utilities -----------------------------------------------------------

    public String getCurrentRole() {
        return sessionManager.role();
    }

    public void logout() {
        sessionManager.clear();
        startActivity(new Intent(this, AuthActivity.class));
        finish();
    }

    /**
     * Erlaubt programmatisch den Wechsel eines Tabs,
     * z.B. nach „Thema angenommen → springe zu Meine Arbeiten“.
     */
    public void openTab(int menuItemId) {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(menuItemId);
        }
    }
}
