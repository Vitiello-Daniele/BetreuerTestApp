package de.iu.betreuerapp;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Zentrales ViewModel zum Teilen von
 * - Themen-Ausschreibungen (Topics)
 * - Betreuten Arbeiten (Theses)
 * zwischen mehreren Fragmenten (Tutor-Ansicht).
 *
 * Läuft komplett in-memory (für das Projekt völlig ok),
 * kann später leicht durch Supabase-Calls ersetzt werden.
 */
public class SharedViewModel extends ViewModel {

    // ===== TOPICS (Ausschreibungen) =====

    private final MutableLiveData<List<Topic>> topicsLiveData =
            new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<Topic>> getTopicList() {
        return topicsLiveData;
    }

    private List<Topic> getTopicsInternal() {
        List<Topic> list = topicsLiveData.getValue();
        if (list == null) {
            list = new ArrayList<>();
            topicsLiveData.setValue(list);
        }
        return list;
    }

    public void addTopic(@NonNull Topic topic) {
        List<Topic> list = getTopicsInternal();
        list.add(topic);
        topicsLiveData.setValue(new ArrayList<>(list));
    }

    public void updateTopic(int index, @NonNull Topic updated) {
        List<Topic> list = getTopicsInternal();
        if (index >= 0 && index < list.size()) {
            list.set(index, updated);
            topicsLiveData.setValue(new ArrayList<>(list));
        }
    }

    public void deleteTopic(int index) {
        List<Topic> list = getTopicsInternal();
        if (index >= 0 && index < list.size()) {
            list.remove(index);
            topicsLiveData.setValue(new ArrayList<>(list));
        }
    }

    public int indexOfTopic(@NonNull Topic topic) {
        List<Topic> list = getTopicsInternal();
        return list.indexOf(topic);
    }

    // ===== THESES (Betreute Arbeiten) =====
    // Optional: wird von Dashboard / StatusUpdate etc. genutzt.
    // Falls du es noch nicht brauchst, kannst du es trotzdem drinlassen.

    private final MutableLiveData<List<Thesis>> thesesLiveData =
            new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<Thesis>> getThesisList() {
        return thesesLiveData;
    }

    private List<Thesis> getThesesInternal() {
        List<Thesis> list = thesesLiveData.getValue();
        if (list == null) {
            list = new ArrayList<>();
            thesesLiveData.setValue(list);
        }
        return list;
    }

    public void addThesis(@NonNull Thesis thesis) {
        List<Thesis> list = getThesesInternal();
        list.add(thesis);
        thesesLiveData.setValue(new ArrayList<>(list));
    }

    public void updateThesisStatus(@NonNull String thesisId, @NonNull String newStatus) {
        List<Thesis> list = getThesesInternal();
        boolean changed = false;
        for (int i = 0; i < list.size(); i++) {
            Thesis t = list.get(i);
            if (t != null && thesisId.equals(t.getId())) {
                list.set(i, new Thesis(
                        t.getId(),
                        t.getTitle(),
                        t.getStudentName(),
                        newStatus,
                        t.getLastUpdate()
                ));
                changed = true;
                break;
            }
        }
        if (changed) {
            thesesLiveData.setValue(new ArrayList<>(list));
        }
    }

    // ===== Modelle =====

    public static class Topic {
        private final String title;
        private final String area;
        private final String status;      // "available", "taken", "completed"
        private final String description;

        public Topic(String title, String area, String status, String description) {
            this.title = title;
            this.area = area;
            this.status = status;
            this.description = description;
        }

        public String getTitle() {
            return title;
        }

        public String getArea() {
            return area;
        }

        public String getStatus() {
            return status;
        }

        public String getDescription() {
            return description;
        }

        @NonNull
        @Override
        public String toString() {
            return title != null ? title : "Thema";
        }
    }

    public static class Thesis {
        private final String id;
        private final String title;
        private final String studentName;
        private final String status;       // "in_discussion", "registered", ...
        private final String lastUpdate;   // z.B. Datum als String

        public Thesis(String id,
                      String title,
                      String studentName,
                      String status,
                      String lastUpdate) {
            this.id = id;
            this.title = title;
            this.studentName = studentName;
            this.status = status;
            this.lastUpdate = lastUpdate;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getStudentName() {
            return studentName;
        }

        public String getStatus() {
            return status;
        }

        public String getLastUpdate() {
            return lastUpdate;
        }

        @NonNull
        @Override
        public String toString() {
            return title != null ? title : "Arbeit";
        }
    }
}
