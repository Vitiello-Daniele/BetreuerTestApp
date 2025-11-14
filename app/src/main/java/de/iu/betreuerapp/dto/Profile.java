package de.iu.betreuerapp.dto;

public class Profile {
    public String id;         // = auth.user.id
    public String email;
    public String first_name;
    public String last_name;
    public String role;       // student|tutor|admin
    public String created_at; // optional
}
