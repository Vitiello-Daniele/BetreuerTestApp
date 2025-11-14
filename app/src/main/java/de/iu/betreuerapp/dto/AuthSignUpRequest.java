package de.iu.betreuerapp.dto;

public class AuthSignUpRequest {
    public String email;
    public String password;
    public AuthSignUpRequest(String e, String p) { email = e; password = p; }
}
