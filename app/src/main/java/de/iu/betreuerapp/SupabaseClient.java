package de.iu.betreuerapp;

import static de.iu.betreuerapp.BuildConfig.SUPABASE_ANON_KEY;
import static de.iu.betreuerapp.BuildConfig.SUPABASE_URL;

import android.content.Context;

import androidx.annotation.Nullable;

import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SupabaseClient {

    private final Retrofit authRetrofit; // /auth/v1
    private final Retrofit restRetrofit; // /rest/v1
    private final SessionManager session;

    public SupabaseClient(Context ctx) {
        this.session = new SessionManager(ctx.getApplicationContext());

        HttpLoggingInterceptor log = new HttpLoggingInterceptor();
        log.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient authOk = new OkHttpClient.Builder()
                .addInterceptor(commonHeaders(null)) // Auth-Endpoints: Bearer = anon key
                .addInterceptor(log)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        OkHttpClient restOk = new OkHttpClient.Builder()
                .addInterceptor(commonHeaders(() -> session.token())) // REST: Bearer = user access token
                .addInterceptor(log)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        authRetrofit = new Retrofit.Builder()
                .baseUrl(SUPABASE_URL + "/auth/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(authOk)
                .build();

        restRetrofit = new Retrofit.Builder()
                .baseUrl(SUPABASE_URL + "/rest/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(restOk)
                .build();
    }

    /** Gemeinsame Header: apikey immer; Authorization: Bearer <anon|access_token> */
    private Interceptor commonHeaders(@Nullable TokenProvider tokenProvider) {
        return chain -> {
            Request orig = chain.request();

            Request.Builder builder = orig.newBuilder()
                    .header("apikey", SUPABASE_ANON_KEY)
                    .header("Accept-Profile", "public");

            // Prüfen, ob der Request bereits einen Authorization-Header hat
            String existingAuth = orig.header("Authorization");

            if (existingAuth == null || existingAuth.isEmpty()) {
                String bearer;

                if (tokenProvider == null) {
                    // z.B. für /auth: Standard = anon key
                    bearer = SUPABASE_ANON_KEY;
                } else {
                    String t = tokenProvider.get();
                    bearer = (t == null || t.isEmpty()) ? SUPABASE_ANON_KEY : t;
                }

                builder.header("Authorization", "Bearer " + bearer);
            }
            // wenn already set: lassen wir ihn in Ruhe

            return chain.proceed(builder.build());
        };
    }

    public SupabaseAuthService authService() { return authRetrofit.create(SupabaseAuthService.class); }
    public SupabaseRestService restService() { return restRetrofit.create(SupabaseRestService.class); }

    interface TokenProvider { String get(); }
}
