package de.iu.betreuerapp;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

import de.iu.betreuerapp.dto.ContactRequest;

/**
 * Adapter für Betreute Arbeiten (Tutor-Sicht).
 *
 * Karte zeigt:
 * - Status-Badge oben (farbig)
 * - Titel (aus strukturierter message, sonst Fallback)
 * - Student + Fachgebiet (mit fetten Labels)
 * - Zweitprüfer (+ dessen Status, Label fett)
 */
public class TutorThesesAdapter extends RecyclerView.Adapter<TutorThesesAdapter.VH> {

    public interface OnThesisClick {
        void onClick(ContactRequest r);
    }

    private final List<ContactRequest> data;
    private final OnThesisClick onClick;

    public TutorThesesAdapter(List<ContactRequest> data, OnThesisClick onClick) {
        this.data = data;
        this.onClick = onClick;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvSubtitle;         // Student + Fachgebiet
        TextView tvSecondReviewer;   // Zweitprüfer
        TextView tvStatus;           // Status-Badge der Arbeit

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvSubtitle = itemView.findViewById(R.id.tv_subtitle);
            tvSecondReviewer = itemView.findViewById(R.id.tv_second_reviewer);
            tvStatus = itemView.findViewById(R.id.tv_status);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tutor_thesis, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ContactRequest r = data.get(position);
        if (r == null) return;

        Parsed p = parseMessage(r.message);

        // ----- Titel -----
        String title;
        if (p.title != null && !p.title.isEmpty()) {
            title = p.title;
        } else if (r.student_name != null && !r.student_name.isEmpty()) {
            title = "Arbeit von " + r.student_name;
        } else {
            title = "Betreute Arbeit";
        }
        h.tvTitle.setText(title);

        // ----- Student + Fachgebiet -----
        StringBuilder sub = new StringBuilder();

        if (r.student_name != null && !r.student_name.isEmpty()) {
            sub.append("Student: ").append(r.student_name);
        }

        if (p.area != null && !p.area.isEmpty()) {
            if (sub.length() > 0) sub.append(" • ");
            sub.append("Fachgebiet: ").append(p.area);
        }

        if (sub.length() == 0 && r.student_email != null) {
            sub.append("Student: ").append(r.student_email);
        }

        h.tvSubtitle.setText(styleSubtitle(sub.toString()));

        // ----- Zweitprüfer -----
        String secondLabel = buildSecondReviewerLabel(r);
        h.tvSecondReviewer.setText(styleSecondReviewer(secondLabel));

        // ----- Status-Badge (Label + Farbe) -----
        String statusLabel = mapStatusLabel(r.status);
        String normStatus = norm(r.status);

        // Spezielle Erweiterung für "In Rechnung":
        // In Rechnung (1. offen | 2. offen/bezahlt/–)
        if ("invoiced".equals(normStatus)) {
            // 1 = Hauptbetreuer
            String invSup = invoiceLabel(r.invoice_supervisor_created, r.paid_supervisor);

            // 2 = Zweitprüfer (nur relevant, wenn akzeptiert)
            String invSec;
            String srs = norm(r.second_reviewer_status);
            if ("accepted".equals(srs)) {
                invSec = invoiceLabel(r.invoice_reviewer_created, r.paid_reviewer);
            } else {
                invSec = "–";
            }

            statusLabel = statusLabel + " (1. " + invSup + " | 2. " + invSec + ")";
        }

        h.tvStatus.setText(statusLabel);
        h.tvStatus.setTextColor(mapStatusColor(h.itemView.getContext(), r.status));

        h.itemView.setOnClickListener(v -> {
            if (onClick != null) onClick.onClick(r);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    // =====================================================================
    // Helper
    // =====================================================================

    private static String norm(String s) {
        return (s == null) ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    public static String mapStatusLabel(String s) {
        if (s == null) return "-";
        String n = s.trim().toLowerCase(Locale.ROOT);
        switch (n) {
            case "accepted":
                return "Angenommen";
            case "in_progress":
                return "In Arbeit";
            case "submitted":
                return "Abgegeben";
            case "colloquium_held":
                return "Kolloquium gehalten";
            case "invoiced":
                return "In Rechnung";
            case "finished":
                return "Beendet";
            default:
                return s;
        }
    }

    public static int mapStatusColor(Context context, String s) {
        String n = (s == null) ? "" : s.trim().toLowerCase(Locale.ROOT);
        switch (n) {
            // alles noch im Workflow -> Gelb
            case "accepted":
            case "in_progress":
            case "submitted":
            case "colloquium_held":
            case "invoiced":
                return ContextCompat.getColor(context, R.color.status_in_progress);

            // Final abgeschlossen -> Grün
            case "finished":
                return ContextCompat.getColor(context, R.color.status_finished);

            default:
                return ContextCompat.getColor(context, R.color.status_unknown);
        }
    }

    /**
     * Liefert "offen" / "bezahlt" für eine Rechnungsrolle.
     * Wenn noch keine Rechnung erstellt wurde, behandeln wir das aus Sicht
     * des Betreuers als "offen".
     */
    private static String invoiceLabel(Boolean created, Boolean paid) {
        boolean c = created != null && created;
        boolean p = paid != null && paid;
        if (!c) return "offen";
        return p ? "bezahlt" : "offen";
    }

    private String buildSecondReviewerLabel(ContactRequest r) {
        String base;
        if (r.second_reviewer_name != null && !r.second_reviewer_name.isEmpty()) {
            base = r.second_reviewer_name;
        } else if (r.second_reviewer_email != null && !r.second_reviewer_email.isEmpty()) {
            base = r.second_reviewer_email;
        } else {
            return "Zweitprüfer: noch nicht zugewiesen";
        }

        String status = (r.second_reviewer_status != null)
                ? r.second_reviewer_status.trim().toLowerCase(Locale.ROOT)
                : "";

        String statusLabel;
        switch (status) {
            case "pending":
                statusLabel = "angefragt";
                break;
            case "accepted":
                statusLabel = "zugesagt";
                break;
            case "rejected":
                statusLabel = "abgelehnt";
                break;
            default:
                statusLabel = "";
        }

        if (!statusLabel.isEmpty()) {
            return "Zweitprüfer: " + base + " (" + statusLabel + ")";
        } else {
            return "Zweitprüfer: " + base;
        }
    }

    // Subtitle: "Student: ..." & "Fachgebiet: ..." Labels fett
    private static CharSequence styleSubtitle(String raw) {
        SpannableStringBuilder sb = new SpannableStringBuilder(raw);
        String studentLabel = "Student: ";
        String areaLabel = "Fachgebiet: ";

        int idx = raw.indexOf(studentLabel);
        if (idx >= 0) {
            sb.setSpan(new StyleSpan(Typeface.BOLD), idx, idx + studentLabel.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        idx = raw.indexOf(areaLabel);
        if (idx >= 0) {
            sb.setSpan(new StyleSpan(Typeface.BOLD), idx, idx + areaLabel.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return sb;
    }

    // Zweitprüfer-Label fett
    private static CharSequence styleSecondReviewer(String raw) {
        SpannableStringBuilder sb = new SpannableStringBuilder(raw);
        String label = "Zweitprüfer: ";
        if (raw.startsWith(label)) {
            sb.setSpan(new StyleSpan(Typeface.BOLD), 0, label.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return sb;
    }

    // --------- Parsing der strukturierten Nachricht (Titel / Fachgebiet) ---

    static class Parsed {
        String title;
        String area;
    }

    /**
     * Erwartet Format aus ContactFragment:
     * Titel: ...
     * Fachgebiet: ...
     * Beschreibung:
     * ...
     */
    private static Parsed parseMessage(String raw) {
        Parsed p = new Parsed();
        if (raw == null) return p;
        String msg = raw.trim();
        if (msg.isEmpty()) return p;

        String[] lines = msg.split("\n");
        for (String line : lines) {
            String l = line.trim();
            if (l.startsWith("Titel:")) {
                p.title = l.substring("Titel:".length()).trim();
            } else if (l.startsWith("Fachgebiet:")) {
                p.area = l.substring("Fachgebiet:".length()).trim();
            }
        }

        // Fallback für alte Nachrichten:
        if (p.title == null && msg.startsWith("Anfrage für Thema:")) {
            String t = msg.substring("Anfrage für Thema:".length()).trim();
            if (!t.isEmpty()) p.title = t;
        }

        return p;
    }
}
