package de.iu.betreuerapp;

import java.util.List;

import de.iu.betreuerapp.dto.Profile;
import de.iu.betreuerapp.dto.ContactRequest;
import de.iu.betreuerapp.dto.Topic;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.DELETE;
import retrofit2.http.PATCH;

public interface SupabaseRestService {

    // ---------- PROFILE ----------

    @GET("profiles")
    Call<List<Profile>> getProfileById(
            @Query("id") String filterEq        // "eq.<uuid>"
    );

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation,resolution=merge-duplicates"
    })
    @POST("profiles?on_conflict=id")
    Call<List<Profile>> upsertProfile(
            @Body Profile profile
    );

    // ---------- CONTACT REQUESTS (Anfragen / Betreute Arbeiten) ----------

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("contact_requests")
    Call<List<ContactRequest>> createContactRequest(
            @Body ContactRequest request
    );

    // Alle Anfragen; RLS sorgt dafür, dass jeder nur seine sieht
    @GET("contact_requests")
    Call<List<ContactRequest>> listContactRequests();

    // Anfragen eines Studierenden
    @GET("contact_requests")
    Call<List<ContactRequest>> getContactRequests(
            @Query("student_id") String studentIdEq   // "eq.<uuid>"
    );

    // Anfrage löschen (Student, nur eigene + offen)
    @DELETE("contact_requests")
    Call<Void> deleteContactRequest(
            @Query("id") String filterEq              // "eq.<id>"
    );

    // Anfrage updaten (Tutor ändert Status etc.)
    @PATCH("contact_requests")
    @Headers({"Prefer: return=representation"})
    Call<List<ContactRequest>> updateContactRequest(
            @Query("id") String idEq,                 // "eq.<id>"
            @Body ContactRequest patch
    );

    // ---------- TOPICS (Themen) ----------

    // Tutor: eigene Themen laden (Management-Ansicht)
    @GET("topics")
    Call<List<Topic>> getTutorTopics(
            @Query("owner_id") String ownerIdEq,      // "eq.<tutor_id>"
            @Query("select") String select            // z.B. "id,title,description,area,status,owner_id"
    );

    // Student: verfügbare Themen (Themenbörse)
    @GET("topics")
    Call<List<Topic>> getAvailableTopics(
            @Query("status") String statusEq,      // z.B. "eq.available"
            @Query("area") String areaEq,          // z.B. "eq.Wirtschaftsinformatik" oder null
            @Query("order") String order           // z.B. "created_at.desc"
    );

    // Offene Themen eines bestimmten Tutors (SupervisorProfile)
    @GET("topics")
    Call<List<Topic>> getAvailableTopicsForSupervisor(
            @Query("owner_id") String ownerIdEq,   // "eq.<tutor_uuid>"
            @Query("status") String statusEq,      // "eq.available"
            @Query("order") String order           // "created_at.desc"
    );

    // Alle Themen eines Tutors (falls du es irgendwo noch brauchst)
    @GET("topics")
    Call<List<Topic>> getTopicsForTutor(
            @Query("owner_id") String ownerIdEq,   // "eq.<tutor_uuid>"
            @Query("order") String order           // "created_at.desc"
    );

    // Neues Thema anlegen (Tutor)
    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("topics")
    Call<List<Topic>> createTopic(
            @Body Topic topic
    );

    // Thema updaten (z.B. Status auf 'taken', 'completed' etc.)
    @PATCH("topics")
    @Headers({"Prefer: return=representation"})
    Call<List<Topic>> updateTopic(
            @Query("id") String idEq,                 // "eq.<topic_id>"
            @Body Topic patch
    );

    // Thema löschen (RLS: nur owner + i.d.R. nur wenn available)
    @DELETE("topics")
    Call<Void> deleteTopic(
            @Query("id") String idEq                  // "eq.<topic_id>"
    );
}
