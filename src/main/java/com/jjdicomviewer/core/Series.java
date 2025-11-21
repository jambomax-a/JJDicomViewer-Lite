package com.jjdicomviewer.core;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DICOM Series（シリーズ）を表すドメインモデル
 */
public class Series {
    
    private String seriesInstanceUID;
    private String studyInstanceUID;
    private Integer seriesNumber;
    private String modality;
    private LocalDate seriesDate;
    private LocalTime seriesTime;
    private String seriesDescription;
    private String bodyPartExamined;
    private String patientPosition;
    private int instanceCount;
    
    private List<Instance> instanceList = new ArrayList<>();
    
    public Series() {
    }
    
    public Series(String seriesInstanceUID) {
        this.seriesInstanceUID = seriesInstanceUID;
    }
    
    // Getters and Setters
    public String getSeriesInstanceUID() {
        return seriesInstanceUID;
    }
    
    public void setSeriesInstanceUID(String seriesInstanceUID) {
        this.seriesInstanceUID = seriesInstanceUID;
    }
    
    public String getStudyInstanceUID() {
        return studyInstanceUID;
    }
    
    public void setStudyInstanceUID(String studyInstanceUID) {
        this.studyInstanceUID = studyInstanceUID;
    }
    
    public Integer getSeriesNumber() {
        return seriesNumber;
    }
    
    public void setSeriesNumber(Integer seriesNumber) {
        this.seriesNumber = seriesNumber;
    }
    
    public String getModality() {
        return modality;
    }
    
    public void setModality(String modality) {
        this.modality = modality;
    }
    
    public LocalDate getSeriesDate() {
        return seriesDate;
    }
    
    public void setSeriesDate(LocalDate seriesDate) {
        this.seriesDate = seriesDate;
    }
    
    public LocalTime getSeriesTime() {
        return seriesTime;
    }
    
    public void setSeriesTime(LocalTime seriesTime) {
        this.seriesTime = seriesTime;
    }
    
    public String getSeriesDescription() {
        return seriesDescription;
    }
    
    public void setSeriesDescription(String seriesDescription) {
        this.seriesDescription = seriesDescription;
    }
    
    public String getBodyPartExamined() {
        return bodyPartExamined;
    }
    
    public void setBodyPartExamined(String bodyPartExamined) {
        this.bodyPartExamined = bodyPartExamined;
    }
    
    public String getPatientPosition() {
        return patientPosition;
    }
    
    public void setPatientPosition(String patientPosition) {
        this.patientPosition = patientPosition;
    }
    
    public int getInstanceCount() {
        return instanceCount;
    }
    
    public void setInstanceCount(int instanceCount) {
        this.instanceCount = instanceCount;
    }
    
    public List<Instance> getInstanceList() {
        return instanceList;
    }
    
    public void setInstanceList(List<Instance> instanceList) {
        this.instanceList = instanceList;
    }
    
    public void addInstance(Instance instance) {
        if (instanceList == null) {
            instanceList = new ArrayList<>();
        }
        instanceList.add(instance);
        instanceCount = instanceList.size();
    }
}

