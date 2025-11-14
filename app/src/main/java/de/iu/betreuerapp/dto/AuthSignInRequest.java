package de.iu.betreuerapp.dto;

public class AuthSignInRequest {
    public String email;
    public String password;
    public AuthSignInRequest(String e, String p) { email = e; password = p; }
}
