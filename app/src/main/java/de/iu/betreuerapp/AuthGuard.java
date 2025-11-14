package de.iu.betreuerapp;

import android.widget.Toast;
import androidx.fragment.app.Fragment;

public class AuthGuard {

    public static boolean requireRole(Fragment fragment, String... allowed) {
        if (!(fragment.getActivity() instanceof MainActivity)) return true;

        MainActivity act = (MainActivity) fragment.getActivity();
        String role = act.getCurrentRole();
        if (role == null) {
            act.logout();
            return false;
        }

        for (String r : allowed) {
            if (r.equalsIgnoreCase(role)) return true;
        }

        Toast.makeText(fragment.requireContext(),
                "Keine Berechtigung für diese Ansicht", Toast.LENGTH_SHORT).show();
        act.getSupportFragmentManager().popBackStack();
        return false;
    }
}
