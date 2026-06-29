package com.example.demo.model;

import java.time.LocalDateTime;

/**
 * Represents a log entry for tracking changes made to a service request.
 * Used for maintaining request history.
 */
public class HistoryEntry
{
    private String historyId;
    private String requestId;
    private String action;
    private String oldValue;
    private String newValue;
    private LocalDateTime timestamp;

    /* Default constructor sets timestamp automatically */
    public HistoryEntry()
    {
        this.timestamp = LocalDateTime.now();
    }

    /* Creates a history record for a request action. */
    public HistoryEntry(String historyId, String requestId, String action,
                        String oldValue, String newValue)
    {
        this.historyId = historyId;
        this.requestId = requestId;
        this.action = action;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.timestamp = LocalDateTime.now();
    }

    public String getHistoryId()       { return historyId; }
    public String getRequestId()       { return requestId; }
    public String getAction()          { return action; }
    public String getOldValue()        { return oldValue; }
    public String getNewValue()        { return newValue; }
    public LocalDateTime getTimestamp(){ return timestamp; }

    // Required for restoring timestamp from history.txt on startup
    public void setTimestamp(LocalDateTime timestamp)
    {
        this.timestamp = timestamp;
    }
}