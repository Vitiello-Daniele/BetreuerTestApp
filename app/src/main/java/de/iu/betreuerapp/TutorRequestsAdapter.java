package de.iu.betreuerapp;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.iu.betreuerapp.dto.ContactRequest;

public class TutorRequestsAdapter extends RecyclerView.Adapter<TutorRequestsAdapter.VH> {

    public interface OnRequestClick { void onClick(ContactRequest r); }

    private final List<ContactRequest> data;
    private final OnRequestClick onClick;

    public TutorRequestsAdapter(List<ContactRequest> data, OnRequestClick onClick) {
        this.data = data;
        this.onClick = onClick;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvStatus, tvTitle, tvStudent, tvArea, tvDesc;
        VH(@NonNull View itemView) {
            super(itemView);
            tvStatus  = itemView.findViewById(R.id.tv_status);
            tvTitle   = itemView.findViewById(R.id.tv_title);
            tvStudent = itemView.findViewById(R.id.tv_student);
            tvArea    = itemView.findViewById(R.id.tv_area);
            tvDesc    = itemView.findViewById(R.id.tv_desc);
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tutor_request, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ContactRequest r = data.get(position);
        if (r == null) return;

        Parsed p = parseMessage(r.message);

        // Status (farben aus colors.xml)
        String sLabel = mapStatusLabel(r.status);
        h.tvStatus.setText(sLabel);
        int colorRes = colorResForStatus(r.status);
        h.tvStatus.setTextColor(ContextCompat.getColor(h.itemView.getContext(), colorRes));

        // Titel
        String title = (p.title != null && !p.title.isEmpty())
                ? p.title
                : (r.message != null && !r.message.isEmpty() ? trim(r.message, 40) : "Anfrage");
        h.tvTitle.setText(title);

        // Student (Label fett)
        String student = (r.student_name != null && !r.student_name.isEmpty())
                ? r.student_name
                : (r.student_email != null ? r.student_email : "Unbekannt");
        h.tvStudent.setText(boldLabel("Student: ", student));

        // Fachgebiet (Label fett) + Fallback über Directory (Supervisor-E-Mail)
        String area = (p.area != null && !p.area.isEmpty()) ? p.area : null;
        if ((area == null || area.isEmpty()) && r.supervisor_email != null && !r.supervisor_email.trim().isEmpty()) {
            SupervisorDirectory.Entry e = SupervisorDirectory.findByEmail(r.supervisor_email);
            if (e != null && e.area != null && !e.area.trim().isEmpty()) {
                area = e.area.trim();
            }
        }
        h.tvArea.setText(boldLabel("Fachgebiet: ", (area == null || area.isEmpty()) ? "-" : area));

        // Beschreibung (Label fett, Text in neuer Zeile)
        String desc = (p.desc != null && !p.desc.isEmpty()) ? p.desc
                : (r.message != null ? r.message : "-");
        desc = trim(desc, 220);

        SpannableStringBuilder sb = new SpannableStringBuilder();
        appendBold(sb, "Beschreibung:");
        sb.append("\n").append(desc);
        h.tvDesc.setText(sb);

        h.itemView.setOnClickListener(v -> { if (onClick != null) onClick.onClick(r); });
    }

    @Override public int getItemCount() { return data.size(); }

    // ---------- Helpers ----------

    private static CharSequence boldLabel(String label, String value) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        appendBold(sb, label);
        sb.append(value == null ? "-" : value);
        return sb;
    }

    private static void appendBold(SpannableStringBuilder sb, String text) {
        int start = sb.length();
        sb.append(text);
        sb.setSpan(new StyleSpan(Typeface.BOLD), start, start + text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private String trim(String msg, int max) {
        msg = msg == null ? "" : msg.trim();
        if (msg.length() <= max) return msg;
        return msg.substring(0, Math.max(0, max - 1)) + "…";
    }

    private int colorResForStatus(String sRaw) {
        String s = (sRaw == null) ? "open" : sRaw.trim().toLowerCase(Locale.ROOT);
        switch (s) {
            case "open":     return R.color.status_open;
            case "accepted": return R.color.status_in_progress;
            case "rejected": return R.color.status_rejected;
            case "finished": return R.color.status_finished;
            default:         return R.color.status_unknown;
        }
    }

    private String mapStatusLabel(String sRaw) {
        String n = (sRaw == null) ? "open" : sRaw.trim().toLowerCase(Locale.ROOT);
        switch (n) {
            case "open":     return "Offen";
            case "accepted": return "Angenommen";
            case "rejected": return "Abgelehnt";
            default:         return "Unbekannt";
        }
    }

    // --- Parsing ---
    static class Parsed { String title, area, desc; }

    private Parsed parseMessage(String raw) {
        Parsed p = new Parsed();
        if (raw == null) return p;
        String msg = raw.trim();
        if (msg.isEmpty()) return p;

        p.title = extractFirst(msg, "(?im)^\\s*Titel\\s*:\\s*(.+)$");
        p.area  = extractFirst(msg, "(?im)^\\s*(Fachgebiet|Fachbereich)\\s*:\\s*(.+)$");
        String desc = extractFirst(msg, "(?is)\\bBeschreibung\\s*:\\s*(.+)$");
        if (desc != null) p.desc = desc.trim();

        if (p.title == null && p.area == null && p.desc == null) p.desc = msg;
        return p;
    }

    private String extractFirst(String haystack, String regex) {
        Pattern pat = Pattern.compile(regex);
        Matcher m = pat.matcher(haystack);
        if (!m.find()) return null;
        return (m.groupCount() >= 2 ? m.group(2) : m.group(1)).trim();
    }
}
