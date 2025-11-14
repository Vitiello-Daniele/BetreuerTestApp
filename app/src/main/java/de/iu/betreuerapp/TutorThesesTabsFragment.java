package de.iu.betreuerapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import de.iu.betreuerapp.AuthGuard;
import de.iu.betreuerapp.TutorThesesFragment;
import de.iu.betreuerapp.SecondReviewerThesesFragment;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.adapter.FragmentStateAdapter;



/**
 * Container-Fragment für "Betreute Arbeiten":
 * Tab 1: Hauptprüfer:in  -> TutorThesesFragment
 * Tab 2: Zweitprüfer:in  -> SecondReviewerThesesFragment
 */
public class TutorThesesTabsFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        if (!AuthGuard.requireRole(this, "tutor")) {
            return new View(requireContext());
        }

        View root = inflater.inflate(R.layout.fragment_tutor_theses_tabs, container, false);

        // Header-Titel setzen
        TextView headerTitle = root.findViewById(R.id.header_title);
        headerTitle.setText("Betreute Arbeiten");

        tabLayout = root.findViewById(R.id.tabLayoutTheses);
        viewPager = root.findViewById(R.id.viewPagerTheses);

        // ViewPager2 mit zwei Seiten: Hauptprüfer / Zweitprüfer
        viewPager.setAdapter(new ThesesPagerAdapter(this));

        // Tabs mit ViewPager2 verknüpfen
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Hauptprüfer:in");
            } else {
                tab.setText("Zweitprüfer:in");
            }
        }).attach();

        return root;
    }

    private static class ThesesPagerAdapter extends FragmentStateAdapter {

        ThesesPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                // Deine bestehende "Betreute Arbeiten" Sicht (Hauptbetreuer)
                return new TutorThesesFragment();
            } else {
                // Deine Zweitprüfer-Sicht von oben
                return new SecondReviewerThesesFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
