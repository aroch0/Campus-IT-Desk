package com.example.demo.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.stereotype.Service;

import com.example.demo.model.Comment;
import com.example.demo.model.HistoryEntry;
import com.example.demo.model.ServiceRequest;
import com.example.demo.service.RequestNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Service
public class MessageService {

    static final String FILE_PATH    = "requests.txt";
    static final String HISTORY_PATH = "history.txt";

    private static final List<String> VALID_PRIORITIES =
            List.of("LOW", "MEDIUM", "HIGH", "URGENT");

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final List<ServiceRequest>            requests   = new ArrayList<>();
    private final Map<String, List<HistoryEntry>> historyMap = new HashMap<>();

    // FIX 4: read/write lock for concurrent access
    private final ReentrantReadWriteLock          lock       = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock  readLock  = lock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    public MessageService() {
        loadFromFile();
        loadHistoryFromFile();
    }

    // ---------------------------
    // FIX 1+2: ESCAPE HELPERS
    // ---------------------------

    /** Escapes commas, newlines, and literal {{COMMA}} tokens for safe CSV storage. */
    private String esc(String s) {
        if (s == null) return "";
        return s.replace("{{COMMA}}",   "{{LBRACE}}COMMA{{RBRACE}}")
                .replace(",",           "{{COMMA}}")
                .replace("\n",          "{{NEWLINE}}")
                .replace("\r",          "");
    }

    /** Reverses esc(). */
    private String unesc(String s) {
        if (s == null) return "";
        return s.replace("{{NEWLINE}}",              "\n")
                .replace("{{COMMA}}",                ",")
                .replace("{{LBRACE}}COMMA{{RBRACE}}", "{{COMMA}}");
    }

    /** FIX 2: Rejects newlines in any field that goes into the CSV. */
    private void validateFieldCharacters(String value, String field) {
        if (value == null) return;
        if (value.contains("\n") || value.contains("\r")) {
            throw new IllegalArgumentException(field + " must not contain newlines");
        }
    }

    // ---------------------------
    // CREATE
    // ---------------------------
    public void addRequest(ServiceRequest request) {
        writeLock.lock();
        try {
            if (request == null) {
                throw new IllegalArgumentException("Request must not be null");
            }

            // FIX 2: validate before anything else
            validateFieldCharacters(request.getUserId(),      "userId");
            validateFieldCharacters(request.getTitle(),       "title");
            validateFieldCharacters(request.getDescription(), "description");

            if (request.getRequestId() == null || request.getRequestId().isBlank()) {
                request.setRequestId(UUID.randomUUID().toString());
            }

            normalize(request);
            validate(request);

            if (request.getCreatedAt() == null) {
                request.setCreatedAt(LocalDateTime.now());
            }

            request.setUpdatedAt(LocalDateTime.now());

            requests.add(request);

            logHistory(request.getRequestId(), "CREATED", null,
                    "status=" + request.getStatus() + ", priority=" + request.getPriority());

            saveToFile();
            saveHistoryToFile();
        } finally {
            writeLock.unlock();
        }
    }

    // ---------------------------
    // UPDATE
    // ---------------------------
    public ServiceRequest updateRequest(String requestId, ServiceRequest updatedRequest) {
        writeLock.lock();
        try {
            ServiceRequest existing = getRequestByIdInternal(requestId);

            normalize(updatedRequest);
            validate(updatedRequest);

            if (!stringsEqual(existing.getStatus(), updatedRequest.getStatus())) {
                logHistory(requestId, "STATUS_CHANGED",
                        existing.getStatus(), updatedRequest.getStatus());
            }

            if (!stringsEqual(existing.getPriority(), updatedRequest.getPriority())) {
                logHistory(requestId, "PRIORITY_CHANGED",
                        existing.getPriority(), updatedRequest.getPriority());
            }

            existing.setUserId(updatedRequest.getUserId());
            existing.setTitle(updatedRequest.getTitle());
            existing.setDescription(updatedRequest.getDescription());
            existing.setPriority(updatedRequest.getPriority());
            existing.setStatus(updatedRequest.getStatus());
            existing.setAssignedTo(updatedRequest.getAssignedTo());
            existing.setUpdatedAt(LocalDateTime.now());

            logHistory(requestId, "UPDATED", null,
                    "updatedAt=" + existing.getUpdatedAt());

            saveToFile();
            saveHistoryToFile();
            return existing;
        } finally {
            writeLock.unlock();
        }
    }

    // ---------------------------
    // READ
    // ---------------------------
    public List<ServiceRequest> getAllRequests() {
        readLock.lock();
        try {
            return Collections.unmodifiableList(requests);
        } finally {
            readLock.unlock();
        }
    }

    public ServiceRequest getRequestById(String requestId) {
        readLock.lock();
        try {
            return getRequestByIdInternal(requestId);
        } finally {
            readLock.unlock();
        }
    }

    /** Internal lookup used inside write-locked methods (avoids re-entrant lock attempt). */
    private ServiceRequest getRequestByIdInternal(String requestId) {
        return requests.stream()
                .filter(r -> r.getRequestId().equals(requestId))
                .findFirst()
                .orElseThrow(() -> new RequestNotFoundException("Request not found: " + requestId));
    }

    // SORT BY DATE
    public List<ServiceRequest> getRequestsSortedByDate(String direction) {
        readLock.lock();
        try {
            List<ServiceRequest> sorted = new ArrayList<>(requests);
            if ("asc".equalsIgnoreCase(direction)) {
                sorted.sort(Comparator.comparing(
                        ServiceRequest::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ));
            } else {
                sorted.sort(Comparator.comparing(
                        ServiceRequest::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ));
            }
            return Collections.unmodifiableList(sorted);
        } finally {
            readLock.unlock();
        }
    }

    // ---------------------------
    // FR-11 FILTER BY STATUS
    // ---------------------------
    public List<ServiceRequest> getByStatus(String status) {
        readLock.lock();
        try {
            if (status == null || status.isBlank()) return getAllRequests();
            return requests.stream()
                    .filter(r -> r.getStatus() != null &&
                                 r.getStatus().equalsIgnoreCase(status))
                    .toList();
        } finally {
            readLock.unlock();
        }
    }

    // ---------------------------
    // FR-12 FILTER BY PRIORITY
    // ---------------------------
    public List<ServiceRequest> getByPriority(String priority) {
        readLock.lock();
        try {
            if (priority == null || priority.isBlank()) return getAllRequests();
            return requests.stream()
                    .filter(r -> r.getPriority() != null &&
                                 r.getPriority().equalsIgnoreCase(priority))
                    .toList();
        } finally {
            readLock.unlock();
        }
    }

    // ---------------------------
    // FR-13 FILTER BY REQUESTER
    // ---------------------------
    public List<ServiceRequest> getByUserId(String userId) {
        readLock.lock();
        try {
            if (userId == null || userId.isBlank()) return getAllRequests();
            return requests.stream()
                    .filter(r -> r.getUserId() != null &&
                                 r.getUserId().contains(userId))
                    .toList();
        } finally {
            readLock.unlock();
        }
    }

    // ---------------------------
    // FR-14 SEARCH TITLE/DESCRIPTION
    // ---------------------------
    public List<ServiceRequest> searchRequests(String keyword) {
        readLock.lock();
        try {
            if (keyword == null || keyword.isBlank()) return getAllRequests();
            String lower = keyword.toLowerCase();
            return requests.stream()
                    .filter(r ->
                            (r.getTitle() != null &&
                             r.getTitle().toLowerCase().contains(lower))
                            ||
                            (r.getDescription() != null &&
                             r.getDescription().toLowerCase().contains(lower)))
                    .toList();
        } finally {
            readLock.unlock();
        }
    }

    // ---------------------------
    // FR-16 ADD COMMENT
    // ---------------------------
    public ServiceRequest addComment(String requestId, Comment comment) {
        writeLock.lock();
        try {
            if (comment == null) {
                throw new IllegalArgumentException("Comment must not be null");
            }
            if (comment.getBody() == null || comment.getBody().isBlank()) {
                throw new IllegalArgumentException("Comment body is required");
            }
            if (comment.getCommentId() == null || comment.getCommentId().isBlank()) {
                comment.setCommentId(UUID.randomUUID().toString());
            }
            if (comment.getCreatedAt() == null) {
                comment.setCreatedAt(LocalDateTime.now());
            }

            ServiceRequest existing = getRequestByIdInternal(requestId);
            existing.getComments().add(comment);
            existing.setUpdatedAt(LocalDateTime.now());

            logHistory(requestId, "COMMENT_ADDED", null,
                    "by=" + comment.getAuthorId());

            saveToFile();
            saveHistoryToFile();
            return existing;
        } finally {
            writeLock.unlock();
        }
    }

    // ---------------------------
    // FR-16 GET COMMENTS
    // ---------------------------
    public List<Comment> getComments(String requestId) {
        readLock.lock();
        try {
            return getRequestByIdInternal(requestId).getComments();
        } finally {
            readLock.unlock();
        }
    }

    // ---------------------------
    // FR-18 GET HISTORY
    // ---------------------------
    public List<HistoryEntry> getHistory(String requestId) {
        readLock.lock();
        try {
            return historyMap.getOrDefault(requestId, Collections.emptyList());
        } finally {
            readLock.unlock();
        }
    }

    // ---------------------------
    // FR-18 INTERNAL LOG HELPER
    // ---------------------------
    private void logHistory(String requestId, String action,
                             String oldValue, String newValue) {
        HistoryEntry entry = new HistoryEntry(
                UUID.randomUUID().toString(),
                requestId,
                action,
                oldValue,
                newValue
        );
        historyMap
                .computeIfAbsent(requestId, k -> new ArrayList<>())
                .add(entry);
    }

    // ---------------------------
    // VALIDATION
    // ---------------------------
    private void validate(ServiceRequest r) {
        requireNonBlank(r.getRequestId(), "requestId");
        requireNonBlank(r.getUserId(),    "userId");
        requireNonBlank(r.getTitle(),     "title");

        if (r.getPriority() != null &&
                !VALID_PRIORITIES.contains(r.getPriority())) {
            throw new IllegalArgumentException(
                    "Invalid priority: " + r.getPriority());
        }

        if (r.getStatus() == null || r.getStatus().isBlank()) {
            r.setStatus("Open");
        }
    }

    private void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    // ---------------------------
    // NORMALIZATION
    // ---------------------------
    private void normalize(ServiceRequest r) {
        if (r.getPriority() != null) {
            r.setPriority(r.getPriority().trim().toUpperCase());
        }

        if (r.getStatus() == null) {
            r.setStatus("Open");
            return;
        }

        String s = r.getStatus().trim().toLowerCase();
        switch (s) {
            case "open":         r.setStatus("Open");        break;
            case "in progress":
            case "in_progress":  r.setStatus("In Progress"); break;
            case "closed":       r.setStatus("Closed");      break;
            case "resolved":     r.setStatus("Resolved");    break;
            default:             r.setStatus(r.getStatus().trim());
        }
    }

    // ---------------------------
    // UTILITY
    // ---------------------------
    private boolean stringsEqual(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    // ---------------------------
    // FIX 1+3: SAVE REQUESTS (atomic write + escaping)
    // ---------------------------
    private void saveToFile() {
        File target = new File(FILE_PATH);
        File tmp    = new File(FILE_PATH + ".tmp");

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(tmp))) {
            for (ServiceRequest r : requests) {
                String commentsJson = MAPPER.writeValueAsString(r.getComments());
                bw.write(String.join(",",
                        esc(r.getRequestId()),
                        esc(r.getUserId()),
                        esc(r.getTitle()),
                        esc(r.getDescription()),
                        esc(r.getStatus()),
                        esc(r.getPriority()),
                        r.getCreatedAt() == null ? "" : r.getCreatedAt().toString(),
                        r.getUpdatedAt() == null ? "" : r.getUpdatedAt().toString(),
                        commentsJson.replace(",", "{{COMMA}}")
                ));
                bw.newLine();
            }
        } catch (IOException e) {
            tmp.delete();
            throw new RuntimeException("Failed to save requests", e);
        }

        try {
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            tmp.delete();
            throw new RuntimeException("Failed to replace requests file", e);
        }
    }

    // ---------------------------
    // FIX 3: SAVE HISTORY (atomic write)
    // ---------------------------
    private void saveHistoryToFile() {
        File target = new File(HISTORY_PATH);
        File tmp    = new File(HISTORY_PATH + ".tmp");

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(tmp))) {
            for (Map.Entry<String, List<HistoryEntry>> entry : historyMap.entrySet()) {
                for (HistoryEntry h : entry.getValue()) {
                    bw.write(String.join("|",
                            h.getHistoryId(),
                            h.getRequestId(),
                            h.getAction(),
                            h.getOldValue() == null ? "" : h.getOldValue(),
                            h.getNewValue() == null ? "" : h.getNewValue(),
                            h.getTimestamp() == null ? "" : h.getTimestamp().toString()
                    ));
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            tmp.delete();
            throw new RuntimeException("Failed to save history", e);
        }

        try {
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            tmp.delete();
            throw new RuntimeException("Failed to replace history file", e);
        }
    }

    // ---------------------------
    // FIX 1: LOAD REQUESTS (unescaping)
    // ---------------------------
    private void loadFromFile() {
        File file = new File(FILE_PATH);
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    String[] p = line.split(",", -1);
                    if (p.length < 6) continue;

                    ServiceRequest r = new ServiceRequest(
                            unesc(p[0]),
                            unesc(p[1]),
                            unesc(p[2]),
                            unesc(p[3]),
                            unesc(p[5])
                    );

                    r.setStatus(unesc(p[4]));
                    normalize(r);

                    if (p.length > 6 && !p[6].isBlank()) {
                        r.setCreatedAt(LocalDateTime.parse(p[6]));
                    }

                    if (p.length > 7 && !p[7].isBlank()) {
                        r.setUpdatedAt(LocalDateTime.parse(p[7]));
                    }

                    if (p.length > 8 && !p[8].isBlank()) {
                        String commentsJson = p[8].replace("{{COMMA}}", ",");
                        List<Comment> comments = MAPPER.readValue(
                                commentsJson,
                                MAPPER.getTypeFactory()
                                      .constructCollectionType(List.class, Comment.class)
                        );
                        r.setComments(comments);
                    }

                    requests.add(r);

                } catch (Exception ignored) {
                    // skip malformed rows
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to load requests", e);
        }
    }

    // ---------------------------
    // LOAD HISTORY
    // ---------------------------
    private void loadHistoryFromFile() {
        File file = new File(HISTORY_PATH);
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    String[] p = line.split("\\|", -1);
                    if (p.length < 6) continue;

                    HistoryEntry h = new HistoryEntry(
                            p[0],
                            p[1],
                            p[2],
                            p[3].isBlank() ? null : p[3],
                            p[4].isBlank() ? null : p[4]
                    );

                    if (!p[5].isBlank()) {
                        h.setTimestamp(LocalDateTime.parse(p[5]));
                    }

                    historyMap
                            .computeIfAbsent(p[1], k -> new ArrayList<>())
                            .add(h);

                } catch (Exception ignored) {
                    // skip malformed rows
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to load history", e);
        }
    }
}