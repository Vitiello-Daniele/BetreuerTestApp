package de.iu.betreuerapp;

import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Back-Press Handling mit moderner API
        setupBackPressedHandler();

        // Start mit Search Fragment
        if (savedInstanceState == null) {
            loadFragment(new SearchFragment());
        }

        // BottomNavigation Listener
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.navigation_search) {
                selectedFragment = new SearchFragment();
            } else if (itemId == R.id.navigation_topics) {
                selectedFragment = new TopicsFragment();
            } else if (itemId == R.id.navigation_dashboard) {
                selectedFragment = new DashboardFragment();
            } else if (itemId == R.id.navigation_invoices) {
                selectedFragment = new InvoicesFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void setupBackPressedHandler() {
        // Moderner Back-Press Handler
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Prüfen ob wir im Such-Fragment sind und Back-Stack Einträge existieren
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    // Zurück zur Such-Ansicht
                    getSupportFragmentManager().popBackStack();
                } else {
                    // Standard Back-Verhalten - App schließen
                    finish();
                }
            }
        });
    }
}