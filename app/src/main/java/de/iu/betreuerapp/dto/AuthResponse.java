package de.iu.betreuerapp.dto;

public class AuthResponse {
    public String access_token;
    public String token_type;
    public long   expires_in;
    public SupaUser user;

    public static class SupaUser {
        public String id;   // UUID
        public String email;
    }
}
