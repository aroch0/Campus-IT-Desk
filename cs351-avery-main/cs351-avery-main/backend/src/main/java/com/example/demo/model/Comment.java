package com.example.demo.model;

import java.time.LocalDateTime;

import jakarta.persistence.Embeddable;

@Embeddable
public class Comment {

    private String commentId;
    private String authorId;
    private String body;
    private LocalDateTime createdAt;

    public Comment() {}

    public Comment(String commentId, String authorId, String body) {
        this.commentId = commentId;
        this.authorId = authorId;
        this.body = body;
        this.createdAt = LocalDateTime.now();
    }

    public String getCommentId()  { return commentId; }
    public String getAuthorId()   { return authorId; }
    public String getBody()       { return body; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCommentId(String commentId)   { this.commentId = commentId; }
    public void setAuthorId(String authorId)     { this.authorId = authorId; }
    public void setBody(String body)             { this.body = body; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}