package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class ServiceRequest {

    @Id
    private String requestId;

    private String userId;
    private String title;
    private String description;
    private String priority;
    private String status;
    private String assignedTo;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ElementCollection
    private List<Comment> comments = new ArrayList<>();

    public ServiceRequest() {
        this.status = "Open";
        this.comments = new ArrayList<>();
    }

    public ServiceRequest(String requestId, String userId, String title,
                          String description, String priority) {
        this.requestId = requestId;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = "Open";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.comments = new ArrayList<>();
    }

    public String getRequestId()              { return requestId; }
    public void setRequestId(String v)        { this.requestId = v; }

    public String getUserId()                 { return userId; }
    public void setUserId(String v)           { this.userId = v; }

    public String getTitle()                  { return title; }
    public void setTitle(String v)            { this.title = v; }

    public String getDescription()            { return description; }
    public void setDescription(String v)      { this.description = v; }

    public String getPriority()               { return priority; }
    public void setPriority(String v)         { this.priority = v; }

    public String getStatus()                 { return status; }
    public void setStatus(String v)           { this.status = v; }

    public String getAssignedTo()             { return assignedTo; }
    public void setAssignedTo(String v)       { this.assignedTo = v; }

    public LocalDateTime getCreatedAt()       { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }

    public LocalDateTime getUpdatedAt()       { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }

    public List<Comment> getComments()        { return comments; }
    public void setComments(List<Comment> comments) {
        this.comments = comments != null ? comments : new ArrayList<>();
    }
}