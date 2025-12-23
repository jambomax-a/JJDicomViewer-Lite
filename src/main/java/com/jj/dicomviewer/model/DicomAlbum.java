package com.jj.dicomviewer.model;

import java.util.HashSet;
import java.util.Set;

/**
 * DicomAlbum - アルバムエンティティ
 * 
 * HOROS-20240407/Horos/Sources/DicomAlbum.h
 * HOROS-20240407/Horos/Sources/DicomAlbum.m
 * の写経
 */
public class DicomAlbum implements Comparable<DicomAlbum> {
    
    // HOROS: NSNumber* index
    private Integer index;
    
    // HOROS: NSString* name
    private String name;
    
    // HOROS: NSString* predicateString
    private String predicateString;
    
    // HOROS: NSNumber* smartAlbum
    private Boolean smartAlbum;
    
    // HOROS: NSSet* studies
    private Set<DicomStudy> studies;
    
    // HOROS: int numberOfStudies
    private int numberOfStudies;
    
    /**
     * コンストラクタ
     */
    public DicomAlbum() {
        this.studies = new HashSet<>();
        this.smartAlbum = false;
    }
    
    /**
     * コンストラクタ（名前指定）
     */
    public DicomAlbum(String name) {
        this();
        this.name = name;
    }
    
    /**
     * コンストラクタ（スマートアルバム）
     */
    public DicomAlbum(String name, String predicateString) {
        this();
        this.name = name;
        this.predicateString = predicateString;
        this.smartAlbum = true;
    }
    
    // HOROS: - (void)addStudiesObject:(DicomStudy *)value
    public void addStudiesObject(DicomStudy study) {
        if (study != null) {
            studies.add(study);
            numberOfStudies = studies.size();
        }
    }
    
    // HOROS: - (void)removeStudiesObject:(DicomStudy *)value
    public void removeStudiesObject(DicomStudy study) {
        if (study != null) {
            studies.remove(study);
            numberOfStudies = studies.size();
        }
    }
    
    // HOROS: - (void)addStudies:(NSSet *)value
    public void addStudies(Set<DicomStudy> studySet) {
        if (studySet != null) {
            studies.addAll(studySet);
            numberOfStudies = studies.size();
        }
    }
    
    // HOROS: - (void)removeStudies:(NSSet *)value
    public void removeStudies(Set<DicomStudy> studySet) {
        if (studySet != null) {
            studies.removeAll(studySet);
            numberOfStudies = studies.size();
        }
    }
    
    // Getters and Setters
    
    public Integer getIndex() {
        return index;
    }
    
    public void setIndex(Integer index) {
        this.index = index;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getPredicateString() {
        return predicateString;
    }
    
    public void setPredicateString(String predicateString) {
        this.predicateString = predicateString;
    }
    
    public Boolean isSmartAlbum() {
        return smartAlbum;
    }
    
    public void setSmartAlbum(Boolean smartAlbum) {
        this.smartAlbum = smartAlbum;
    }
    
    public Set<DicomStudy> getStudies() {
        return studies;
    }
    
    public void setStudies(Set<DicomStudy> studies) {
        this.studies = studies;
        this.numberOfStudies = studies != null ? studies.size() : 0;
    }
    
    public int getNumberOfStudies() {
        return numberOfStudies;
    }
    
    public void setNumberOfStudies(int numberOfStudies) {
        this.numberOfStudies = numberOfStudies;
    }
    
    @Override
    public String toString() {
        return name != null ? name : "Album";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        DicomAlbum that = (DicomAlbum) obj;
        if (index != null && that.index != null) {
            return index.equals(that.index);
        }
        if (name != null && that.name != null) {
            return name.equals(that.name);
        }
        return super.equals(obj);
    }
    
    @Override
    public int hashCode() {
        if (index != null) {
            return index.hashCode();
        }
        if (name != null) {
            return name.hashCode();
        }
        return super.hashCode();
    }
    
    /**
     * ソート用の比較メソッド
     * indexが設定されている場合はindexで比較、そうでない場合はnameで比較
     */
    @Override
    public int compareTo(DicomAlbum other) {
        if (other == null) {
            return 1;
        }
        
        // indexで比較
        if (index != null && other.index != null) {
            return index.compareTo(other.index);
        }
        if (index != null) {
            return -1;
        }
        if (other.index != null) {
            return 1;
        }
        
        // nameで比較
        if (name != null && other.name != null) {
            return name.compareTo(other.name);
        }
        if (name != null) {
            return -1;
        }
        if (other.name != null) {
            return 1;
        }
        
        return 0;
    }
}

