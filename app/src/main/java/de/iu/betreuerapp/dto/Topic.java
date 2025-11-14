package de.iu.betreuerapp.dto;

/**
 * Entspricht der Tabelle public.topics in Supabase.
 */
public class Topic {
    public String id;
    public String owner_id;
    public String title;
    public String description;
    public String area;
    public String status;
    public String created_at;
    public String updated_at;
}
