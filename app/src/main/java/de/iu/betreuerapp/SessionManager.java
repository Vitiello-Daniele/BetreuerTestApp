package de.iu.betreuerapp;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF = "session";

    private static final String KEY_TOKEN = "access_token";
    private static final String KEY_USER  = "user_id";
    private static final String KEY_ROLE  = "role";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_NAME  = "name";

    private final SharedPreferences sp;

    public SessionManager(Context ctx) {
        sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    // minimal (z.B. direkt nach Login vor Profil)
    public void save(String token, String userId) {
        save(token, userId, null, null, null);
    }

    // mit Rolle
    public void save(String token, String userId, String role) {
        save(token, userId, role, null, null);
    }

    // volle Ladung
    public void save(String token,
                     String userId,
                     String role,
                     String email,
                     String name) {

        SharedPreferences.Editor editor = sp.edit();

        if (token != null) editor.putString(KEY_TOKEN, token);
        if (userId != null) editor.putString(KEY_USER, userId);
        if (role != null) editor.putString(KEY_ROLE, role);
        if (email != null) editor.putString(KEY_EMAIL, email);
        if (name != null) editor.putString(KEY_NAME, name);

        editor.apply();
    }

    public String token()  { return sp.getString(KEY_TOKEN, null); }
    public String userId() { return sp.getString(KEY_USER, null); }
    public String role()   { return sp.getString(KEY_ROLE, null); }

    public String email()  { return sp.getString(KEY_EMAIL, null); }
    public String name()   { return sp.getString(KEY_NAME, null); }

    public void clear()    { sp.edit().clear().apply(); }
}
