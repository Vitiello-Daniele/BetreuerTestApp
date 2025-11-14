package de.iu.betreuerapp;

public class SupervisorDirectory {

    public static class Entry {
        public String id;
        public String name;
        public String email;
        public String area;
        public String areaInfo;
    }

    // Statische Demo-Liste – IDs idealerweise = profiles.id aus Supabase
    public static final Entry[] ENTRIES = new Entry[]{

            e(
                    "903e132e-864e-45f8-b41d-bb41e5a6df0e",
                    "Arno Ardesti",
                    "a.a@iu.de",
                    "Wirtschaftsinformatik",
                    "Fokus auf Mobile Apps, digitale Geschäftsmodelle und Cloud-Architekturen."
            ),
            e(
                    "628fa59c-b12e-4501-a2d3-d160f3999263",
                    "Benjamin Bauer",
                    "b.b@iu.de",
                    "Data Science",
                    "Machine Learning, Datenanalyse, Predictive Analytics."
            ),
            e(
                    "b2f4d196-9887-4f2f-9b76-2d6e636f4c13",
                    "Carlo Cloud",
                    "c.c@iu.de",
                    "Software Engineering",
                    "Enterprise-Anwendungen, Clean Code, Architektur & Testing."
            ),
            e(
                    "71f74129-0f0b-4c13-8cef-ca3296a7b0a0",
                    "Diana Defense",
                    "d.d@iu.de",
                    "IT-Sicherheit",
                    "Security Audits, Penetration Testing, sichere Softwareentwicklung."
            ),
            e(
                    "16471d0e-ccae-4051-97e8-09c985d684e6",
                    "Elena Experience",
                    "e.e@iu.de",
                    "UX / UI Design",
                    "User-Centered Design, Prototyping, Usability-Tests."
            ),
            e(
                    "ed2b9702-d127-4f7c-9f1b-e4601ceb8825",
                    "Fabian Finance",
                    "f.f@iu.de",
                    "Finance & Controlling",
                    "Unternehmensbewertung, Reporting, Controlling-Systeme."
            ),
            e(
                    "96a85b9b-a10a-416b-bfda-842303ae6aa7",
                    "Greta Growth",
                    "g.g@iu.de",
                    "Marketing & Digital Business",
                    "Online-Marketing, Growth Hacking, Social Media Strategien."
            ),
            e(
                    "fdb1c76b-678d-4678-9803-e19fb6a44d2d",
                    "Hannah Health",
                    "h.h@iu.de",
                    "Gesundheitsmanagement",
                    "Versorgungsmanagement, Digitalisierung im Gesundheitswesen."
            ),
            e(
                    "90a8d61f-3c3b-4400-8769-fdda82f6c2d9",
                    "Ivan Infra",
                    "i.i@iu.de",
                    "Cloud & DevOps",
                    "CI/CD, Container, Infrastructure as Code."
            ),
            e(
                    "fc2515cd-ca2d-4ef8-a4c0-ea15f710a754",
                    "Jana Justice",
                    "j.j@iu.de",
                    "Wirtschaftsrecht",
                    "IT-Verträge, Datenschutz, Compliance."
            ),
            e(
                    "6ad91120-97d9-4c16-a205-ac55b5e3f064",
                    "Klemens Klabautermann",
                    "k.k@iu.de",
                    "Wirtschaftsrecht",
                    "Steuerrecht, Datenschutz, Wirtschaft."
            )

            // beliebig erweiterbar
    };

    private static Entry e(String id, String name, String email, String area, String info) {
        Entry x = new Entry();
        x.id = id;
        x.name = name;
        x.email = email;
        x.area = area;
        x.areaInfo = info;
        return x;
    }

    /** Alle Einträge (für Listen / Picker verwenden). */
    public static Entry[] getAll() {
        return ENTRIES;
    }

    /** Suche nach E-Mail (wird u.a. im Profil & Mapping verwendet). */
    public static Entry findByEmail(String email) {
        if (email == null) return null;
        for (Entry e : ENTRIES) {
            if (e.email != null && e.email.equalsIgnoreCase(email)) {
                return e;
            }
        }
        return null;
    }

    /** Suche nach ID (z.B. aus contact_requests.supervisor_id / second_reviewer_id). */
    public static Entry findById(String id) {
        if (id == null) return null;
        for (Entry e : ENTRIES) {
            if (e.id != null && e.id.equals(id)) {
                return e;
            }
        }
        return null;
    }
}
