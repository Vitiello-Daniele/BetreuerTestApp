package de.iu.betreuerapp;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import androidx.core.content.ContextCompat;

import de.iu.betreuerapp.dto.ContactRequest;

public class StudentRequestsAdapter extends RecyclerView.Adapter<StudentRequestsAdapter.ViewHolder> {

    public interface OnItemClickListener { void onItemClick(ContactRequest cr); }

    private final List<ContactRequest> items;
    private final OnItemClickListener listener;

    public StudentRequestsAdapter(List<ContactRequest> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_request, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        ContactRequest cr = items.get(position);
        if (cr == null) return;

        Parsed p = parseMessage(cr.message);

        // Status-Label + Farbe
        String statusLabel = mapStatusToLabelInfo(cr.status);
        h.status.setText(statusLabel);
        h.status.setTextColor(mapStatusToColor(h.itemView.getContext(), cr.status));

        // Titel ohne Status-Klammern
        String baseTitle = (p.title != null && !p.title.isEmpty()) ? p.title : "Arbeit";
        h.title.setText(boldLabel("Titel: ", baseTitle));


        // Fachgebiet (mit Directory-Fallback) – Label fett
        String area = (p.area != null && !p.area.isEmpty()) ? p.area : null;
        if ((area == null || area.isEmpty()) && cr.supervisor_email != null) {
            SupervisorDirectory.Entry e = SupervisorDirectory.findByEmail(cr.supervisor_email);
            if (e != null && e.area != null && !e.area.trim().isEmpty()) area = e.area.trim();
        }
        h.area.setText(boldLabel("Fachgebiet: ", area != null && !area.isEmpty() ? area : "-"));

        // Tutor – Label fett
        String tutor = null;
        if (cr.supervisor_name != null && !cr.supervisor_name.isEmpty()) tutor = cr.supervisor_name;
        else if (p.tutor != null && !p.tutor.isEmpty()) tutor = p.tutor;
        if (tutor == null || tutor.isEmpty()) tutor = "Betreuer:in unbekannt";
        h.tutor.setText(boldLabel("Tutor: ", tutor));

        // Beschreibung – „Beschreibung:“ fett + Text in nächster Zeile
        String desc;
        if (p.desc != null && !p.desc.isEmpty()) desc = p.desc;
        else if (cr.message != null && !cr.message.trim().isEmpty()) desc = cr.message.trim();
        else desc = "-";

        SpannableStringBuilder sbDesc = new SpannableStringBuilder();
        appendBold(sbDesc, "Beschreibung:");
        sbDesc.append("\n").append(desc);
        h.desc.setText(sbDesc);

        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onItemClick(cr); });
    }
    public static String mapStatusToLabelInfo(String statusRaw) {
        String s = (statusRaw == null) ? "" : statusRaw.trim().toLowerCase(Locale.ROOT);
        switch (s) {
            case "accepted":
                // Thema zugesagt, Formales / 2. Prüfer / Anmeldung noch nicht komplett
                return "In Abstimmung";

            case "in_progress":
                // offiziell angemeldet, Studierende:r arbeitet
                return "Angemeldet";

            case "submitted":
                return "Abgegeben";

            case "colloquium_held":
                return "Kolloquium abgehalten";

            case "invoiced":
                return "Abgerechnet";

            case "finished":
                return "Beendet";

            // Falls du hier auch noch open/rejected für diese Liste anzeigen willst:
            case "open":
                return "Angefragt";
            case "rejected":
                return "Abgelehnt";

            default:
                return "Unbekannt";
        }
    }
    public static int mapStatusToColor(Context context, String statusRaw) {
        String s = (statusRaw == null) ? "open" : statusRaw.trim().toLowerCase(Locale.ROOT);

        switch (s) {
            case "open":    // Angefragt -> blau
                return ContextCompat.getColor(context, R.color.status_open);

            case "accepted":
            case "in_progress":
            case "submitted":
            case "colloquium_held":
            case "invoiced": // In Bearbeitung (Kollegium) -> gelb
                return ContextCompat.getColor(context, R.color.status_in_progress);

            case "finished": // Abgeschlossen -> grün
                return ContextCompat.getColor(context, R.color.status_finished);

            case "rejected": // Abgelehnt -> rot
                return ContextCompat.getColor(context, R.color.status_rejected);

            default:         // Fallback
                return ContextCompat.getColor(context, R.color.status_unknown);
        }
    }

    @Override public int getItemCount() { return items.size(); }

    // ----- ViewHolder -----
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView status, title, area, tutor, desc;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            status = itemView.findViewById(R.id.tv_request_status);  // NEU
            title  = itemView.findViewById(R.id.tv_request_title);
            area   = itemView.findViewById(R.id.tv_request_area);
            tutor  = itemView.findViewById(R.id.tv_request_tutor);
            desc   = itemView.findViewById(R.id.tv_request_desc);
        }
    }

    // ----- Helpers (fett) -----
    private static CharSequence boldLabel(String label, String value) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        appendBold(sb, label);
        sb.append(value == null ? "-" : value);
        return sb;
    }

    private static void appendBold(SpannableStringBuilder sb, String label) {
        int start = sb.length();
        sb.append(label);
        sb.setSpan(new StyleSpan(Typeface.BOLD), start, start + label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    // ---------- Parsing (wie bei dir, leicht robust) ----------
    static class Parsed { String title, area, tutor, desc; }

    private Parsed parseMessage(String raw) {
        Parsed p = new Parsed();
        if (raw == null) return p;
        String msg = raw.trim();
        if (msg.isEmpty()) return p;

        String legacyPrefix = "Anfrage für Thema:";
        if (msg.startsWith(legacyPrefix)) {
            String t = msg.substring(legacyPrefix.length()).trim();
            if (!t.isEmpty()) p.title = t;
            p.desc = msg;
            return p;
        }

        p.title = extractFirst(msg, "(?im)^\\s*Titel\\s*:\\s*(.+)$");
        p.area  = extractFirst(msg, "(?im)^\\s*(Fachgebiet|Fachbereich)\\s*:\\s*(.+)$");
        p.tutor = extractFirst(msg, "(?im)^\\s*Tutor\\s*:\\s*(.+)$");
        String desc = extractFirst(msg, "(?is)\\bBeschreibung\\s*:\\s*(.+)$");
        if (desc != null) p.desc = desc.trim();

        if (p.title == null && p.area == null && p.tutor == null && p.desc == null) p.desc = msg;
        return p;
    }

    private String extractFirst(String haystack, String regex) {
        Pattern pat = Pattern.compile(regex);
        Matcher m = pat.matcher(haystack);
        if (!m.find()) return null;
        String val = (m.groupCount() >= 2) ? m.group(2) : m.group(1);
        return val != null ? val.trim() : null;
    }

    private String mapStatusToLabel(String statusRaw) {
        String s = (statusRaw == null) ? "open" : statusRaw.trim().toLowerCase(Locale.ROOT);
        switch (s) {
            case "open":            return "Angefragt";
            case "accepted":        return "Angenommen";
            case "in_progress":     return "In Bearbeitung";
            case "submitted":       return "Eingereicht";
            case "colloquium_held": return "Kolloquium";
            case "invoiced":        return "Rechnung gestellt";
            case "finished":        return "Abgeschlossen";
            case "rejected":        return "Abgelehnt";
            default:                return "Unbekannt";
        }
    }
}
