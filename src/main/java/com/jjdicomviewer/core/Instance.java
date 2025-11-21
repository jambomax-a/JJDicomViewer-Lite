package com.jjdicomviewer.core;

import java.nio.file.Path;

/**
 * DICOM Instance（インスタンス）を表すドメインモデル
 */
public class Instance {
    
    private String sopInstanceUID;
    private String seriesInstanceUID;
    private Integer instanceNumber;
    private String sopClassUID;
    private Path filePath;
    private long fileSize;
    private String transferSyntaxUID;
    
    // 画像属性
    private Integer rows;
    private Integer columns;
    private Integer bitsAllocated;
    private Integer bitsStored;
    private Integer samplesPerPixel;
    private String photometricInterpretation;
    private Integer pixelRepresentation; // 0=unsigned, 1=signed
    
    // ウィンドウ/レベル
    private String windowCenter;
    private String windowWidth;
    private Double rescaleSlope;
    private Double rescaleIntercept;
    
    public Instance() {
    }
    
    public Instance(String sopInstanceUID) {
        this.sopInstanceUID = sopInstanceUID;
    }
    
    // Getters and Setters
    public String getSopInstanceUID() {
        return sopInstanceUID;
    }
    
    public void setSopInstanceUID(String sopInstanceUID) {
        this.sopInstanceUID = sopInstanceUID;
    }
    
    public String getSeriesInstanceUID() {
        return seriesInstanceUID;
    }
    
    public void setSeriesInstanceUID(String seriesInstanceUID) {
        this.seriesInstanceUID = seriesInstanceUID;
    }
    
    public Integer getInstanceNumber() {
        return instanceNumber;
    }
    
    public void setInstanceNumber(Integer instanceNumber) {
        this.instanceNumber = instanceNumber;
    }
    
    public String getSopClassUID() {
        return sopClassUID;
    }
    
    public void setSopClassUID(String sopClassUID) {
        this.sopClassUID = sopClassUID;
    }
    
    public Path getFilePath() {
        return filePath;
    }
    
    public void setFilePath(Path filePath) {
        this.filePath = filePath;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getTransferSyntaxUID() {
        return transferSyntaxUID;
    }
    
    public void setTransferSyntaxUID(String transferSyntaxUID) {
        this.transferSyntaxUID = transferSyntaxUID;
    }
    
    public Integer getRows() {
        return rows;
    }
    
    public void setRows(Integer rows) {
        this.rows = rows;
    }
    
    public Integer getColumns() {
        return columns;
    }
    
    public void setColumns(Integer columns) {
        this.columns = columns;
    }
    
    public Integer getBitsAllocated() {
        return bitsAllocated;
    }
    
    public void setBitsAllocated(Integer bitsAllocated) {
        this.bitsAllocated = bitsAllocated;
    }
    
    public Integer getBitsStored() {
        return bitsStored;
    }
    
    public void setBitsStored(Integer bitsStored) {
        this.bitsStored = bitsStored;
    }
    
    public Integer getSamplesPerPixel() {
        return samplesPerPixel;
    }
    
    public void setSamplesPerPixel(Integer samplesPerPixel) {
        this.samplesPerPixel = samplesPerPixel;
    }
    
    public String getPhotometricInterpretation() {
        return photometricInterpretation;
    }
    
    public void setPhotometricInterpretation(String photometricInterpretation) {
        this.photometricInterpretation = photometricInterpretation;
    }
    
    public Integer getPixelRepresentation() {
        return pixelRepresentation;
    }
    
    public void setPixelRepresentation(Integer pixelRepresentation) {
        this.pixelRepresentation = pixelRepresentation;
    }
    
    public String getWindowCenter() {
        return windowCenter;
    }
    
    public void setWindowCenter(String windowCenter) {
        this.windowCenter = windowCenter;
    }
    
    public String getWindowWidth() {
        return windowWidth;
    }
    
    public void setWindowWidth(String windowWidth) {
        this.windowWidth = windowWidth;
    }
    
    public Double getRescaleSlope() {
        return rescaleSlope;
    }
    
    public void setRescaleSlope(Double rescaleSlope) {
        this.rescaleSlope = rescaleSlope;
    }
    
    public Double getRescaleIntercept() {
        return rescaleIntercept;
    }
    
    public void setRescaleIntercept(Double rescaleIntercept) {
        this.rescaleIntercept = rescaleIntercept;
    }
}

