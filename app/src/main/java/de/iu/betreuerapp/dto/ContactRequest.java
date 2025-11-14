package de.iu.betreuerapp.dto;

public class ContactRequest {
    public String id;

    public String student_id;
    public String student_name;
    public String student_email;

    public String supervisor_id;
    public String supervisor_name;
    public String supervisor_email;

    public String topic_id;

    public String message;
    public String expose_url;

    public String status; // open, accepted, in_progress, submitted, colloquium_held, invoiced, finished, rejected

    // Zweitprüfer
    public String second_reviewer_id;
    public String second_reviewer_name;
    public String second_reviewer_email;
    public String second_reviewer_status; // pending, accepted, rejected

    // Rechnungs-Flags (NEU)
    public Boolean invoice_supervisor_created; // Rechnung Betreuer erstellt?
    public Boolean invoice_reviewer_created;   // Rechnung Zweitprüfer erstellt?

    // Zahlungs-Flags (Student klickt "bezahlt")
    public Boolean paid_supervisor;            // Anteil Betreuer bezahlt?
    public Boolean paid_reviewer;              // Anteil Zweitprüfer bezahlt?

    // Optional weitere Felder (created_at etc.)
}
