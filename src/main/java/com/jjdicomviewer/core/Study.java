package com.jjdicomviewer.core;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DICOM Study（検査）を表すドメインモデル
 */
public class Study {
    
    private String studyInstanceUID;
    private String patientID;
    private String patientName;
    private LocalDate patientBirthDate;
    private String patientSex;
    private LocalDate studyDate;
    private LocalTime studyTime;
    private String studyDescription;
    private String accessionNumber;
    private String referringPhysicianName;
    private LocalDate createdAt; // DB登録日（時間は不要）
    
    private List<Series> seriesList = new ArrayList<>();
    
    public Study() {
    }
    
    public Study(String studyInstanceUID) {
        this.studyInstanceUID = studyInstanceUID;
    }
    
    // Getters and Setters
    public String getStudyInstanceUID() {
        return studyInstanceUID;
    }
    
    public void setStudyInstanceUID(String studyInstanceUID) {
        this.studyInstanceUID = studyInstanceUID;
    }
    
    public String getPatientID() {
        return patientID;
    }
    
    public void setPatientID(String patientID) {
        this.patientID = patientID;
    }
    
    public String getPatientName() {
        return patientName;
    }
    
    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }
    
    public LocalDate getPatientBirthDate() {
        return patientBirthDate;
    }
    
    public void setPatientBirthDate(LocalDate patientBirthDate) {
        this.patientBirthDate = patientBirthDate;
    }
    
    public String getPatientSex() {
        return patientSex;
    }
    
    public void setPatientSex(String patientSex) {
        this.patientSex = patientSex;
    }
    
    public LocalDate getStudyDate() {
        return studyDate;
    }
    
    public void setStudyDate(LocalDate studyDate) {
        this.studyDate = studyDate;
    }
    
    public LocalTime getStudyTime() {
        return studyTime;
    }
    
    public void setStudyTime(LocalTime studyTime) {
        this.studyTime = studyTime;
    }
    
    public String getStudyDescription() {
        return studyDescription;
    }
    
    public void setStudyDescription(String studyDescription) {
        this.studyDescription = studyDescription;
    }
    
    public String getAccessionNumber() {
        return accessionNumber;
    }
    
    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }
    
    public String getReferringPhysicianName() {
        return referringPhysicianName;
    }
    
    public void setReferringPhysicianName(String referringPhysicianName) {
        this.referringPhysicianName = referringPhysicianName;
    }
    
    public LocalDate getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }
    
    public List<Series> getSeriesList() {
        return seriesList;
    }
    
    public void setSeriesList(List<Series> seriesList) {
        this.seriesList = seriesList;
    }
    
    public void addSeries(Series series) {
        if (seriesList == null) {
            seriesList = new ArrayList<>();
        }
        seriesList.add(series);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (patientName != null) {
            sb.append(patientName);
        } else {
            sb.append("Unknown");
        }
        sb.append(" - ");
        if (studyDate != null) {
            sb.append(studyDate);
        } else {
            sb.append("No Date");
        }
        if (studyDescription != null && !studyDescription.isEmpty()) {
            sb.append(" (").append(studyDescription).append(")");
        }
        return sb.toString();
    }
}

