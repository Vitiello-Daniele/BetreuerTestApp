package de.iu.betreuerapp;

import de.iu.betreuerapp.dto.AuthResponse;
import de.iu.betreuerapp.dto.AuthSignInRequest;
import de.iu.betreuerapp.dto.AuthSignUpRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SupabaseAuthService {
    // Signup
    @Headers({"Content-Type: application/json"})
    @POST("signup")
    Call<AuthResponse> signUp(@Body AuthSignUpRequest body);

    // SignIn (grant_type=password)
    @Headers({"Content-Type: application/json"})
    @POST("token")
    Call<AuthResponse> signIn(@Query("grant_type") String grantType, @Body AuthSignInRequest body);
}
