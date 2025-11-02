package de.iu.betreuerapp;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.List;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<List<Thesis>> thesisList = new MutableLiveData<>();
    private final List<Thesis> currentTheses = new ArrayList<>();

    // ✅ THEMEN-FUNKTIONALITÄT HINZUFÜGEN
    private final MutableLiveData<List<Topic>> topicList = new MutableLiveData<>();
    private final List<Topic> currentTopics = new ArrayList<>();

    public SharedViewModel() {
        // Beispiel-Daten laden
        loadSampleData();
        thesisList.setValue(new ArrayList<>(currentTheses));

        // ✅ THEMEN-DATEN LADEN
        loadSampleTopics();
        topicList.setValue(new ArrayList<>(currentTopics));
    }

    private void loadSampleData() {
        currentTheses.add(new Thesis("1", "Entwicklung einer Betreuer-App", "Max Mustermann", "in_discussion", "15.03.2024"));
        currentTheses.add(new Thesis("2", "Machine Learning in Apps", "Anna Schmidt", "registered", "20.02.2024"));
        currentTheses.add(new Thesis("3", "IT-Sicherheit Analyse", "Tom Weber", "submitted", "10.01.2024"));
        currentTheses.add(new Thesis("4", "Cloud Computing Studie", "Lisa Fischer", "in_discussion", "05.03.2024"));
        currentTheses.add(new Thesis("5", "Datenbank Optimierung", "David Becker", "colloquium_held", "15.12.2023"));
    }

    // ✅ THEMEN-DATEN LADEN
    private void loadSampleTopics() {
        currentTopics.add(new Topic("App-Entwicklung", "Wirtschaftsinformatik", "available", "Mobile App mit Android Studio"));
        currentTopics.add(new Topic("Machine Learning in Apps", "Data Science", "available", "Integration von ML-Modellen in mobile Anwendungen"));
        currentTopics.add(new Topic("IT-Sicherheit Analyse", "IT-Sicherheit", "taken", "Sicherheitsanalyse bestehender Systeme"));
        currentTopics.add(new Topic("Cloud Computing Studie", "Software Engineering", "available", "Vergleich von Cloud-Plattformen"));
        currentTopics.add(new Topic("Datenbank Optimierung", "Datenbanken", "completed", "Performance-Optimierung von Datenbankabfragen"));
    }

    public void updateThesisStatus(String thesisId, String newStatus) {
        for (Thesis thesis : currentTheses) {
            if (thesis.getId().equals(thesisId)) {
                thesis.setStatus(newStatus);
                break;
            }
        }
        thesisList.setValue(new ArrayList<>(currentTheses));
    }

    // ✅ NEUE METHODE: Arbeit hinzufügen
    public void addThesis(Thesis thesis) {
        currentTheses.add(thesis);
        thesisList.setValue(new ArrayList<>(currentTheses));
    }

    public MutableLiveData<List<Thesis>> getThesisList() {
        return thesisList;
    }

    // ✅ THEMEN-METHODEN
    public void addTopic(Topic topic) {
        currentTopics.add(topic);
        topicList.setValue(new ArrayList<>(currentTopics));
    }

    public void updateTopic(int position, Topic updatedTopic) {
        if (position >= 0 && position < currentTopics.size()) {
            currentTopics.set(position, updatedTopic);
            topicList.setValue(new ArrayList<>(currentTopics));
        }
    }

    public void deleteTopic(int position) {
        if (position >= 0 && position < currentTopics.size()) {
            currentTopics.remove(position);
            topicList.setValue(new ArrayList<>(currentTopics));
        }
    }

    public MutableLiveData<List<Topic>> getTopicList() {
        return topicList;
    }

    public static class Thesis {
        private String id;
        private String title;
        private String studentName;
        private String status;
        private String lastUpdate;

        public Thesis(String id, String title, String studentName, String status, String lastUpdate) {
            this.id = id;
            this.title = title;
            this.studentName = studentName;
            this.status = status;
            this.lastUpdate = lastUpdate;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getStudentName() { return studentName; }
        public String getStatus() { return status; }
        public String getLastUpdate() { return lastUpdate; }
        public void setStatus(String status) { this.status = status; }
    }

    // ✅ TOPIC-KLASSE
    public static class Topic {
        private String title;
        private String area;
        private String status;
        private String description;

        public Topic(String title, String area, String status, String description) {
            this.title = title;
            this.area = area;
            this.status = status;
            this.description = description;
        }

        public String getTitle() { return title; }
        public String getArea() { return area; }
        public String getStatus() { return status; }
        public String getDescription() { return description; }

        public void setTitle(String title) { this.title = title; }
        public void setArea(String area) { this.area = area; }
        public void setStatus(String status) { this.status = status; }
        public void setDescription(String description) { this.description = description; }
    }
}