package de.iu.betreuerapp.dto;

public class TopicWithOwner {
    public String id;
    public String title;
    public String description;
    public String status;
    public String owner_id;

    // Nested Profile aus Supabase-Select: profiles(...)
    public Profile profiles;

    public String getTutorName() {
        if (profiles == null) return null;
        String fn = profiles.first_name != null ? profiles.first_name : "";
        String ln = profiles.last_name != null ? profiles.last_name : "";
        String full = (fn + " " + ln).trim();
        return full.isEmpty() ? null : full;
    }

    public String getTutorEmail() {
        if (profiles == null) return null;
        return profiles.email;
    }
}
