package com.example.demo;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.demo.model.Comment;
import com.example.demo.model.HistoryEntry;
import com.example.demo.model.ServiceRequest;
import com.example.demo.service.MessageService;
import com.example.demo.service.RequestNotFoundException;

public class MessageServiceTest {

    private MessageService service;

    @BeforeEach
    void setup() {
        new File("requests.txt").delete();
        new File("history.txt").delete();
        service = new MessageService();
    }

    // -------------------------------------------------
    // MessageService Tests
    // -------------------------------------------------

    @Test
    void addRequest_validRequest_savesSuccessfully() {

        ServiceRequest r = new ServiceRequest(
                "1", "user1", "Printer broken", "Office printer not working", "HIGH"
        );

        service.addRequest(r);

        List<ServiceRequest> all = service.getAllRequests();

        assertEquals(1, all.size());
        assertEquals("HIGH", all.get(0).getPriority());
        assertEquals("Open", all.get(0).getStatus());
    }

    @Test
    void addRequest_nullRequest_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> service.addRequest(null));
    }

    @Test
    void addRequest_missingTitle_throwsException() {

        ServiceRequest r = new ServiceRequest("1", "user1", "", "desc", "LOW");

        assertThrows(IllegalArgumentException.class, () -> service.addRequest(r));
    }

    @Test
    void addRequest_invalidPriority_throwsException() {

        ServiceRequest r = new ServiceRequest("1", "user1", "Title", "desc", "BAD");

        assertThrows(IllegalArgumentException.class, () -> service.addRequest(r));
    }

    @Test
    void addRequest_generatesIdWhenMissing() {

        ServiceRequest r = new ServiceRequest(null, "user1", "Title", "desc", "LOW");

        service.addRequest(r);

        assertNotNull(r.getRequestId());
        assertFalse(r.getRequestId().isBlank());
    }

    @Test
    void addRequest_setsTimestamps() {

        ServiceRequest r = new ServiceRequest("1", "u", "title", "desc", "LOW");

        service.addRequest(r);

        assertNotNull(r.getCreatedAt());
        assertNotNull(r.getUpdatedAt());
    }

    @Test
    void updateRequest_updatesFieldsCorrectly() {

        service.addRequest(new ServiceRequest("1", "user1", "Old", "desc", "LOW"));

        ServiceRequest updated = new ServiceRequest("1", "user2", "New", "updated desc", "HIGH");
        updated.setStatus("Closed");
        updated.setAssignedTo("Tech");

        ServiceRequest result = service.updateRequest("1", updated);

        assertEquals("user2", result.getUserId());
        assertEquals("New", result.getTitle());
        assertEquals("updated desc", result.getDescription());
        assertEquals("HIGH", result.getPriority());
        assertEquals("Closed", result.getStatus());
        assertEquals("Tech", result.getAssignedTo());
    }

    @Test
    void updateRequest_updatesTimestamp() throws Exception {

        ServiceRequest r = new ServiceRequest("1", "u", "t", "d", "LOW");

        service.addRequest(r);

        LocalDateTime oldTime = r.getUpdatedAt();

        Thread.sleep(5);

        service.updateRequest("1", new ServiceRequest("1", "u2", "new", "d", "HIGH"));

        assertTrue(r.getUpdatedAt().isAfter(oldTime));
    }

    @Test
    void getRequestById_returnsCorrectRequest() {

        service.addRequest(new ServiceRequest("1", "user", "title", "desc", "LOW"));

        ServiceRequest found = service.getRequestById("1");

        assertEquals("title", found.getTitle());
    }

    @Test
    void getRequestById_missingRequest_throwsException() {
        assertThrows(RequestNotFoundException.class, () -> service.getRequestById("missing"));
    }

    @Test
    void getAllRequests_returnsUnmodifiableList() {

        ServiceRequest r = new ServiceRequest("1", "user", "Title", "desc", "LOW");

        service.addRequest(r);

        List<ServiceRequest> all = service.getAllRequests();

        assertThrows(UnsupportedOperationException.class, () -> all.add(r));
    }

    // -------------------------------------------------
    // Normalization Tests
    // -------------------------------------------------

    @Test
    void normalize_priorityBecomesUppercase() {

        ServiceRequest r = new ServiceRequest("1", "user", "Title", "desc", "high");

        service.addRequest(r);

        assertEquals("HIGH", r.getPriority());
    }

    @Test
    void normalize_openStatus() {

        ServiceRequest r = new ServiceRequest("1", "u", "t", "d", "LOW");
        r.setStatus("open");

        service.addRequest(r);

        assertEquals("Open", r.getStatus());
    }

    @Test
    void normalize_inProgressStatus() {

        ServiceRequest r = new ServiceRequest("2", "u", "t", "d", "LOW");
        r.setStatus("in_progress");

        service.addRequest(r);

        assertEquals("In Progress", r.getStatus());
    }

    @Test
    void normalize_closedStatus() {

        ServiceRequest r = new ServiceRequest("3", "u", "t", "d", "LOW");
        r.setStatus("closed");

        service.addRequest(r);

        assertEquals("Closed", r.getStatus());
    }

    @Test
    void normalize_resolvedStatus() {

        ServiceRequest r = new ServiceRequest("4", "u", "t", "d", "LOW");
        r.setStatus("resolved");

        service.addRequest(r);

        assertEquals("Resolved", r.getStatus());
    }

    @Test
    void normalize_nullStatus_defaultsToOpen() {

        ServiceRequest r = new ServiceRequest("5", "u", "t", "d", "LOW");
        r.setStatus(null);

        service.addRequest(r);

        assertEquals("Open", r.getStatus());
    }

    @Test
    void normalize_unknownStatus_preserved() {

        ServiceRequest r = new ServiceRequest("6", "u", "t", "d", "LOW");
        r.setStatus("UNKNOWN_STATUS");

        service.addRequest(r);

        assertEquals("UNKNOWN_STATUS", r.getStatus());
    }

    // -------------------------------------------------
    // ServiceRequest Model Tests
    // -------------------------------------------------

    @Test
    void serviceRequest_defaultConstructor_setsDefaultStatus() {

        ServiceRequest r = new ServiceRequest();

        assertEquals("Open", r.getStatus());
    }

    @Test
    void serviceRequest_constructor_setsFields() {

        ServiceRequest r = new ServiceRequest("1", "user1", "title", "desc", "HIGH");

        assertEquals("1", r.getRequestId());
        assertEquals("user1", r.getUserId());
        assertEquals("title", r.getTitle());
        assertEquals("desc", r.getDescription());
        assertEquals("HIGH", r.getPriority());
    }

    @Test
    void serviceRequest_gettersAndSetters_work() {

        ServiceRequest r = new ServiceRequest();
        LocalDateTime now = LocalDateTime.now();

        r.setRequestId("1");
        r.setUserId("user");
        r.setTitle("title");
        r.setDescription("desc");
        r.setPriority("HIGH");
        r.setStatus("Closed");
        r.setAssignedTo("IT");
        r.setCreatedAt(now);
        r.setUpdatedAt(now);

        assertEquals("1", r.getRequestId());
        assertEquals("user", r.getUserId());
        assertEquals("title", r.getTitle());
        assertEquals("desc", r.getDescription());
        assertEquals("HIGH", r.getPriority());
        assertEquals("Closed", r.getStatus());
        assertEquals("IT", r.getAssignedTo());
        assertEquals(now, r.getCreatedAt());
        assertEquals(now, r.getUpdatedAt());
    }

    // -------------------------------------------------
    // HistoryEntry Tests
    // -------------------------------------------------

    @Test
    void historyEntry_defaultConstructor_setsTimestamp() {

        HistoryEntry h = new HistoryEntry();

        assertNotNull(h.getTimestamp());
    }

    @Test
    void historyEntry_constructor_setsFields() {

        HistoryEntry h = new HistoryEntry("H1", "REQ1", "STATUS_CHANGE", "Open", "Closed");

        assertEquals("H1", h.getHistoryId());
        assertEquals("REQ1", h.getRequestId());
        assertEquals("STATUS_CHANGE", h.getAction());
        assertEquals("Open", h.getOldValue());
        assertEquals("Closed", h.getNewValue());
        assertNotNull(h.getTimestamp());
    }

    @Test
    void addRequest_blankRequestId_generatesUuid() {

        ServiceRequest r = new ServiceRequest("   ", "user", "Title", "desc", "LOW");

        service.addRequest(r);

        assertNotNull(r.getRequestId());
        assertFalse(r.getRequestId().isBlank());
    }

    @Test
    void addRequest_blankUserId_throwsException() {

        ServiceRequest r = new ServiceRequest("1", "   ", "Title", "desc", "LOW");

        assertThrows(IllegalArgumentException.class, () -> service.addRequest(r));
    }

    @Test
    void addRequest_blankPriority_throwsException() {

        ServiceRequest r = new ServiceRequest("1", "user", "Title", "desc", "   ");

        assertThrows(IllegalArgumentException.class, () -> service.addRequest(r));
    }

    @Test
    void normalize_inProgressWithSpaces() {

        ServiceRequest r = new ServiceRequest("1", "user", "Title", "desc", "LOW");
        r.setStatus("in progress");

        service.addRequest(r);

        assertEquals("In Progress", r.getStatus());
    }

    @Test
    void normalize_priorityTrimmedAndUppercase() {

        ServiceRequest r = new ServiceRequest("1", "user", "Title", "desc", "  high  ");

        service.addRequest(r);

        assertEquals("HIGH", r.getPriority());
    }

    @Test
    void saveAndReload_requestsPersist() {

        ServiceRequest r = new ServiceRequest("10", "user", "Persist Test", "Saved", "LOW");
        r.setStatus("Closed");

        service.addRequest(r);

        MessageService loaded = new MessageService();
        ServiceRequest found = loaded.getRequestById("10");

        assertEquals("Persist Test", found.getTitle());
        assertEquals("Closed", found.getStatus());
    }

    @Test
    void loadFromFile_skipsBadRows() throws Exception {

        java.io.BufferedWriter bw =
                new java.io.BufferedWriter(new java.io.FileWriter("requests.txt"));

        bw.write("bad,data");
        bw.newLine();
        bw.write("1,user,title,desc,Open,LOW");
        bw.newLine();
        bw.close();

        MessageService loaded = new MessageService();

        assertEquals(1, loaded.getAllRequests().size());
    }

    @Test
    void updateRequest_normalizesFields() {

        service.addRequest(new ServiceRequest("1", "user", "Title", "desc", "LOW"));

        ServiceRequest updated = new ServiceRequest("1", "user2", "Updated", "new desc", " high ");
        updated.setStatus("closed");

        ServiceRequest result = service.updateRequest("1", updated);

        assertEquals("HIGH", result.getPriority());
        assertEquals("Closed", result.getStatus());
    }

    @Test
    void addRequest_nullStatus_defaultsToOpen() {

        ServiceRequest r = new ServiceRequest("1", "user", "Title", "desc", "LOW");
        r.setStatus(null);

        service.addRequest(r);

        assertEquals("Open", r.getStatus());
    }

    @Test
    void normalize_inProgressUnderscore_becomesFormatted() {

        ServiceRequest r = new ServiceRequest("55", "user", "Title", "desc", "LOW");
        r.setStatus("in_progress");

        service.addRequest(r);

        assertEquals("In Progress", r.getStatus());
    }

    @Test
    void normalize_unknownStatus_trimsButPreservesValue() {

        ServiceRequest r = new ServiceRequest("99", "user", "Title", "desc", "LOW");
        r.setStatus("  SOME_UNKNOWN_STATUS  ");

        service.addRequest(r);

        assertEquals("SOME_UNKNOWN_STATUS", r.getStatus());
    }

    @Test
    void loadFromFile_handlesIOException_branchCovered() {

        File file = new File("requests.txt");

        if (file.exists()) {
            file.delete();
        }

        boolean createdDir = file.mkdir();

        Assumptions.assumeTrue(
                createdDir && file.isDirectory(),
                "Skipping: could not create directory to simulate IOException"
        );

        try {
            new MessageService();
            fail("Expected RuntimeException due to invalid file access");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Failed to load"));
        } finally {
            file.delete();
        }
    }

    // -------------------------------------------------
    // FR-11 Filter by Status — getByStatus()
    // -------------------------------------------------

    @Test
    void getByStatus_returnsOnlyMatchingStatus() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));
        service.addRequest(new ServiceRequest("2", "user2", "Ticket B", "desc", "LOW"));
        service.addRequest(new ServiceRequest("3", "user3", "Ticket C", "desc", "LOW"));

        service.updateRequest("2", buildWith("2", "user2", "Ticket B", "desc", "LOW", "Closed"));

        List<ServiceRequest> results = service.getByStatus("Open");

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(r -> "Open".equals(r.getStatus())));
    }

    @Test
    void getByStatus_isCaseInsensitive() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));

        List<ServiceRequest> results = service.getByStatus("open");

        assertEquals(1, results.size());
    }

    @Test
    void getByStatus_inProgress_returnsCorrectSubset() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));
        service.addRequest(new ServiceRequest("2", "user2", "Ticket B", "desc", "LOW"));

        service.updateRequest("2", buildWith("2", "user2", "Ticket B", "desc", "LOW", "In Progress"));

        List<ServiceRequest> results = service.getByStatus("In Progress");

        assertEquals(1, results.size());
        assertEquals("2", results.get(0).getRequestId());
    }

    @Test
    void getByStatus_resolved_returnsOnlyResolved() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));
        service.addRequest(new ServiceRequest("2", "user2", "Ticket B", "desc", "HIGH"));

        service.updateRequest("1", buildWith("1", "user1", "Ticket A", "desc", "LOW", "Resolved"));

        List<ServiceRequest> results = service.getByStatus("Resolved");

        assertEquals(1, results.size());
        assertEquals("1", results.get(0).getRequestId());
    }

    @Test
    void getByStatus_noMatch_returnsEmptyList() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));

        List<ServiceRequest> results = service.getByStatus("Resolved");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void getByStatus_nullInput_returnsAllRequests() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));
        service.addRequest(new ServiceRequest("2", "user2", "Ticket B", "desc", "HIGH"));

        List<ServiceRequest> results = service.getByStatus(null);

        assertEquals(2, results.size());
    }

    @Test
    void getByStatus_blankInput_returnsAllRequests() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));
        service.addRequest(new ServiceRequest("2", "user2", "Ticket B", "desc", "HIGH"));

        List<ServiceRequest> results = service.getByStatus("   ");

        assertEquals(2, results.size());
    }

    @Test
    void getByStatus_emptyService_returnsEmptyList() {

        List<ServiceRequest> results = service.getByStatus("Open");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // -------------------------------------------------
    // FR-12 Filter by Priority — getByPriority()
    // -------------------------------------------------

    @Test
    void getByPriority_returnsOnlyMatchingPriority() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "HIGH"));
        service.addRequest(new ServiceRequest("2", "user2", "Ticket B", "desc", "LOW"));
        service.addRequest(new ServiceRequest("3", "user3", "Ticket C", "desc", "HIGH"));

        List<ServiceRequest> results = service.getByPriority("HIGH");

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(r -> "HIGH".equals(r.getPriority())));
    }

    @Test
    void getByPriority_isCaseInsensitive() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "MEDIUM"));

        List<ServiceRequest> results = service.getByPriority("medium");

        assertEquals(1, results.size());
    }

    @Test
    void getByPriority_low_excludesHighAndMedium() {

        service.addRequest(new ServiceRequest("1", "u1", "T1", "d", "HIGH"));
        service.addRequest(new ServiceRequest("2", "u2", "T2", "d", "MEDIUM"));
        service.addRequest(new ServiceRequest("3", "u3", "T3", "d", "LOW"));

        List<ServiceRequest> results = service.getByPriority("LOW");

        assertEquals(1, results.size());
        assertEquals("3", results.get(0).getRequestId());
    }

    @Test
    void getByPriority_urgent_returnsOnlyUrgent() {

        service.addRequest(new ServiceRequest("1", "u1", "T1", "d", "URGENT"));
        service.addRequest(new ServiceRequest("2", "u2", "T2", "d", "HIGH"));

        List<ServiceRequest> results = service.getByPriority("URGENT");

        assertEquals(1, results.size());
        assertEquals("1", results.get(0).getRequestId());
    }

    @Test
    void getByPriority_allLevels_correctCounts() {

        service.addRequest(new ServiceRequest("1", "u1", "T1", "d", "HIGH"));
        service.addRequest(new ServiceRequest("2", "u2", "T2", "d", "HIGH"));
        service.addRequest(new ServiceRequest("3", "u3", "T3", "d", "MEDIUM"));
        service.addRequest(new ServiceRequest("4", "u4", "T4", "d", "LOW"));

        assertEquals(2, service.getByPriority("HIGH").size());
        assertEquals(1, service.getByPriority("MEDIUM").size());
        assertEquals(1, service.getByPriority("LOW").size());
    }

    @Test
    void getByPriority_noMatch_returnsEmptyList() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));

        List<ServiceRequest> results = service.getByPriority("HIGH");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void getByPriority_nullInput_returnsAllRequests() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));
        service.addRequest(new ServiceRequest("2", "user2", "Ticket B", "desc", "HIGH"));

        List<ServiceRequest> results = service.getByPriority(null);

        assertEquals(2, results.size());
    }

    @Test
    void getByPriority_blankInput_returnsAllRequests() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));
        service.addRequest(new ServiceRequest("2", "user2", "Ticket B", "desc", "HIGH"));

        List<ServiceRequest> results = service.getByPriority("   ");

        assertEquals(2, results.size());
    }

    @Test
    void getByPriority_emptyService_returnsEmptyList() {

        List<ServiceRequest> results = service.getByPriority("HIGH");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // -------------------------------------------------
    // FR-13 Filter by Requester — getByUserId()
    // -------------------------------------------------

    @Test
    void getByUserId_returnsOnlyMatchingUser() {

        service.addRequest(new ServiceRequest("1", "alice", "Ticket A", "desc", "LOW"));
        service.addRequest(new ServiceRequest("2", "bob",   "Ticket B", "desc", "LOW"));
        service.addRequest(new ServiceRequest("3", "alice", "Ticket C", "desc", "HIGH"));

        List<ServiceRequest> results = service.getByUserId("alice");

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(r -> "alice".equals(r.getUserId())));
    }

    @Test
    void getByUserId_partialMatch_includesContainingUserIds() {

        service.addRequest(new ServiceRequest("1", "alice.smith", "Ticket A", "desc", "LOW"));
        service.addRequest(new ServiceRequest("2", "alice.jones", "Ticket B", "desc", "LOW"));
        service.addRequest(new ServiceRequest("3", "bob.jones",   "Ticket C", "desc", "LOW"));

        List<ServiceRequest> results = service.getByUserId("alice");

        assertEquals(2, results.size());
    }

    @Test
    void getByUserId_singleUserMultipleRequests_returnsAll() {

        service.addRequest(new ServiceRequest("1", "carol", "Ticket A", "desc", "LOW"));
        service.addRequest(new ServiceRequest("2", "carol", "Ticket B", "desc", "MEDIUM"));
        service.addRequest(new ServiceRequest("3", "carol", "Ticket C", "desc", "HIGH"));

        List<ServiceRequest> results = service.getByUserId("carol");

        assertEquals(3, results.size());
    }

    @Test
    void getByUserId_noMatch_returnsEmptyList() {

        service.addRequest(new ServiceRequest("1", "alice", "Ticket A", "desc", "LOW"));

        List<ServiceRequest> results = service.getByUserId("dave");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void getByUserId_nullInput_returnsAllRequests() {

        service.addRequest(new ServiceRequest("1", "alice", "Ticket A", "desc", "LOW"));
        service.addRequest(new ServiceRequest("2", "bob",   "Ticket B", "desc", "HIGH"));

        List<ServiceRequest> results = service.getByUserId(null);

        assertEquals(2, results.size());
    }

    @Test
    void getByUserId_blankInput_returnsAllRequests() {

        service.addRequest(new ServiceRequest("1", "alice", "Ticket A", "desc", "LOW"));
        service.addRequest(new ServiceRequest("2", "bob",   "Ticket B", "desc", "HIGH"));

        List<ServiceRequest> results = service.getByUserId("   ");

        assertEquals(2, results.size());
    }

    @Test
    void getByUserId_emptyService_returnsEmptyList() {

        List<ServiceRequest> results = service.getByUserId("alice");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // -------------------------------------------------
    // FR-14 Search Title / Description — searchRequests()
    // -------------------------------------------------

    @Test
    void searchRequests_matchesTitleExact() {

        service.addRequest(new ServiceRequest("1", "user1", "Printer broken", "desc", "HIGH"));
        service.addRequest(new ServiceRequest("2", "user2", "Network issue",  "desc", "LOW"));

        List<ServiceRequest> results = service.searchRequests("Printer broken");

        assertEquals(1, results.size());
        assertEquals("1", results.get(0).getRequestId());
    }

    @Test
    void searchRequests_matchesTitlePartialKeyword() {

        service.addRequest(new ServiceRequest("1", "user1", "Printer broken", "desc", "HIGH"));
        service.addRequest(new ServiceRequest("2", "user2", "Printer jammed", "desc", "MEDIUM"));
        service.addRequest(new ServiceRequest("3", "user3", "Network issue",  "desc", "LOW"));

        List<ServiceRequest> results = service.searchRequests("Printer");

        assertEquals(2, results.size());
    }

    @Test
    void searchRequests_isCaseInsensitive() {

        service.addRequest(new ServiceRequest("1", "user1", "Printer Broken", "desc", "HIGH"));

        List<ServiceRequest> results = service.searchRequests("printer broken");

        assertEquals(1, results.size());
    }

    @Test
    void searchRequests_matchesKeywordInDescription() {

        service.addRequest(new ServiceRequest(
                "1", "user1", "Hardware issue",
                "The office printer is jammed on floor 2", "MEDIUM"));
        service.addRequest(new ServiceRequest(
                "2", "user2", "Software issue",
                "Application crashes on startup", "HIGH"));

        List<ServiceRequest> results = service.searchRequests("jammed");

        assertEquals(1, results.size());
        assertEquals("1", results.get(0).getRequestId());
    }

    @Test
    void searchRequests_keywordInDescriptionOnly_notInTitle() {

        service.addRequest(new ServiceRequest(
                "1", "user1", "General issue",
                "VPN credentials expired for remote staff", "LOW"));
        service.addRequest(new ServiceRequest(
                "2", "user2", "Unrelated ticket",
                "Keyboard replacement needed", "LOW"));

        List<ServiceRequest> results = service.searchRequests("VPN");

        assertEquals(1, results.size());
        assertEquals("1", results.get(0).getRequestId());
    }

    @Test
    void searchRequests_keywordMatchesBothTitleAndDescription_noDuplicates() {

        service.addRequest(new ServiceRequest(
                "1", "user1", "Network outage",
                "Complete network failure in building A", "HIGH"));

        List<ServiceRequest> results = service.searchRequests("network");

        assertEquals(1, results.size());
    }

    @Test
    void searchRequests_matchesAcrossMultipleRequestsViaDifferentFields() {

        service.addRequest(new ServiceRequest("1", "u1", "VPN not connecting", "desc",           "HIGH"));
        service.addRequest(new ServiceRequest("2", "u2", "Slow performance",   "VPN causes lag", "LOW"));
        service.addRequest(new ServiceRequest("3", "u3", "Email issue",        "desc",           "MEDIUM"));

        List<ServiceRequest> results = service.searchRequests("VPN");

        assertEquals(2, results.size());
    }

    @Test
    void searchRequests_noMatch_returnsEmptyList() {

        service.addRequest(new ServiceRequest("1", "user1", "Printer broken", "desc", "HIGH"));

        List<ServiceRequest> results = service.searchRequests("xyzzy");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void searchRequests_nullKeyword_returnsAllRequests() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));
        service.addRequest(new ServiceRequest("2", "user2", "Ticket B", "desc", "HIGH"));

        List<ServiceRequest> results = service.searchRequests(null);

        assertEquals(2, results.size());
    }

    @Test
    void searchRequests_blankKeyword_returnsAllRequests() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));
        service.addRequest(new ServiceRequest("2", "user2", "Ticket B", "desc", "HIGH"));

        List<ServiceRequest> results = service.searchRequests("   ");

        assertEquals(2, results.size());
    }

    @Test
    void searchRequests_emptyService_returnsEmptyList() {

        List<ServiceRequest> results = service.searchRequests("anything");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // -------------------------------------------------
    // FR-16 Comments — addComment() / getComments()
    // -------------------------------------------------

    @Test
    void addComment_validComment_addsSuccessfully() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));

        Comment comment = new Comment("C1", "alice", "Please fix ASAP");

        service.addComment("1", comment);

        List<Comment> comments = service.getComments("1");

        assertEquals(1, comments.size());
        assertEquals("Please fix ASAP", comments.get(0).getBody());
        assertEquals("alice", comments.get(0).getAuthorId());
    }

    @Test
    void addComment_nullComment_throwsException() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));

        assertThrows(IllegalArgumentException.class, () -> service.addComment("1", null));
    }

    @Test
    void addComment_blankBody_throwsException() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));

        Comment comment = new Comment("C1", "alice", "   ");

        assertThrows(IllegalArgumentException.class, () -> service.addComment("1", comment));
    }

    @Test
    void addComment_missingCommentId_generatesUuid() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));

        Comment comment = new Comment();
        comment.setBody("No ID provided");
        comment.setAuthorId("bob");

        service.addComment("1", comment);

        assertNotNull(comment.getCommentId());
        assertFalse(comment.getCommentId().isBlank());
    }

    @Test
    void addComment_setsCreatedAt() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));

        Comment comment = new Comment("C1", "alice", "Timestamp test");

        service.addComment("1", comment);

        assertNotNull(comment.getCreatedAt());
    }

    @Test
    void addComment_unknownRequest_throwsException() {

        assertThrows(RequestNotFoundException.class,
                () -> service.addComment("nonexistent", new Comment("C1", "alice", "body")));
    }

    @Test
    void addComment_multipleComments_allStored() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));

        service.addComment("1", new Comment("C1", "alice", "First"));
        service.addComment("1", new Comment("C2", "bob",   "Second"));
        service.addComment("1", new Comment("C3", "carol", "Third"));

        assertEquals(3, service.getComments("1").size());
    }

    @Test
    void getComments_emptyByDefault() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));

        List<Comment> comments = service.getComments("1");

        assertNotNull(comments);
        assertTrue(comments.isEmpty());
    }

    @Test
    void addComment_updatesRequestUpdatedAt() throws Exception {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));

        LocalDateTime before = service.getRequestById("1").getUpdatedAt();

        Thread.sleep(5);

        service.addComment("1", new Comment("C1", "alice", "body"));

        assertTrue(service.getRequestById("1").getUpdatedAt().isAfter(before));
    }

    @Test
    void addComment_persistsAcrossReload() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));
        service.addComment("1", new Comment("C1", "alice", "Persisted comment"));

        MessageService reloaded = new MessageService();

        List<Comment> comments = reloaded.getComments("1");

        assertEquals(1, comments.size());
        assertEquals("Persisted comment", comments.get(0).getBody());
    }

    // -------------------------------------------------
    // FR-18 History — logHistory() / getHistory()
    // -------------------------------------------------

    @Test
    void addRequest_recordsCreatedHistoryEntry() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));

        List<HistoryEntry> history = service.getHistory("1");

        assertFalse(history.isEmpty());
        assertTrue(history.stream().anyMatch(h -> "CREATED".equals(h.getAction())));
    }

    @Test
    void updateRequest_statusChange_recordsStatusChangedEntry() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));
        service.updateRequest("1", buildWith("1", "user1", "Ticket A", "desc", "LOW", "Closed"));

        assertTrue(service.getHistory("1").stream()
                .anyMatch(h -> "STATUS_CHANGED".equals(h.getAction())));
    }

    @Test
    void updateRequest_statusChange_recordsOldAndNewValues() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));
        service.updateRequest("1", buildWith("1", "user1", "Ticket A", "desc", "LOW", "Closed"));

        HistoryEntry entry = service.getHistory("1").stream()
                .filter(h -> "STATUS_CHANGED".equals(h.getAction()))
                .findFirst()
                .orElseThrow();

        assertEquals("Open",   entry.getOldValue());
        assertEquals("Closed", entry.getNewValue());
    }

    @Test
    void updateRequest_priorityChange_recordsPriorityChangedEntry() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));
        service.updateRequest("1", buildWith("1", "user1", "Ticket A", "desc", "HIGH", "Open"));

        assertTrue(service.getHistory("1").stream()
                .anyMatch(h -> "PRIORITY_CHANGED".equals(h.getAction())));
    }

    @Test
    void updateRequest_priorityChange_recordsOldAndNewValues() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));
        service.updateRequest("1", buildWith("1", "user1", "Ticket A", "desc", "HIGH", "Open"));

        HistoryEntry entry = service.getHistory("1").stream()
                .filter(h -> "PRIORITY_CHANGED".equals(h.getAction()))
                .findFirst()
                .orElseThrow();

        assertEquals("LOW",  entry.getOldValue());
        assertEquals("HIGH", entry.getNewValue());
    }

    @Test
    void updateRequest_noStatusOrPriorityChange_doesNotLogChangeEntries() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));

        service.updateRequest("1", buildWith("1", "user2", "Ticket A updated", "desc", "LOW", "Open"));

        List<HistoryEntry> history = service.getHistory("1");

        assertFalse(history.stream().anyMatch(h -> "STATUS_CHANGED".equals(h.getAction())));
        assertFalse(history.stream().anyMatch(h -> "PRIORITY_CHANGED".equals(h.getAction())));
    }

    @Test
    void updateRequest_recordsUpdatedEntry() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));
        service.updateRequest("1", buildWith("1", "user1", "Ticket A", "desc", "LOW", "Open"));

        assertTrue(service.getHistory("1").stream()
                .anyMatch(h -> "UPDATED".equals(h.getAction())));
    }

    @Test
    void addComment_recordsCommentAddedEntry() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));
        service.addComment("1", new Comment("C1", "alice", "note"));

        assertTrue(service.getHistory("1").stream()
                .anyMatch(h -> "COMMENT_ADDED".equals(h.getAction())));
    }

    @Test
    void addComment_historyEntry_containsAuthorId() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));
        service.addComment("1", new Comment("C1", "alice", "note"));

        HistoryEntry entry = service.getHistory("1").stream()
                .filter(h -> "COMMENT_ADDED".equals(h.getAction()))
                .findFirst()
                .orElseThrow();

        assertTrue(entry.getNewValue().contains("alice"));
    }

    @Test
    void getHistory_unknownRequest_returnsEmptyList() {

        List<HistoryEntry> history = service.getHistory("nonexistent");

        assertNotNull(history);
        assertTrue(history.isEmpty());
    }

    @Test
    void historyEntry_hasTimestamp() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));

        HistoryEntry entry = service.getHistory("1").get(0);

        assertNotNull(entry.getTimestamp());
    }

    @Test
    void historyEntry_hasHistoryId() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));

        HistoryEntry entry = service.getHistory("1").get(0);

        assertNotNull(entry.getHistoryId());
        assertFalse(entry.getHistoryId().isBlank());
    }

    @Test
    void history_persistsAcrossReload() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));
        service.updateRequest("1", buildWith("1", "user1", "Ticket A", "desc", "LOW", "Closed"));

        MessageService reloaded = new MessageService();

        List<HistoryEntry> history = reloaded.getHistory("1");

        assertFalse(history.isEmpty());
        assertTrue(history.stream().anyMatch(h -> "STATUS_CHANGED".equals(h.getAction())));
    }

    @Test
    void history_multipleActions_allRecorded() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));
        service.updateRequest("1", buildWith("1", "user1", "Ticket A", "desc", "HIGH", "Closed"));
        service.addComment("1", new Comment("C1", "alice", "note"));

        List<HistoryEntry> history = service.getHistory("1");

        // CREATED + STATUS_CHANGED + PRIORITY_CHANGED + UPDATED + COMMENT_ADDED = 5
        assertTrue(history.size() >= 5);
    }

    // -------------------------------------------------
    // FR-15 Empty Result Handling
    // -------------------------------------------------

    @Test
    void getByStatus_emptyResult_returnsEmptyNotNull() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));

        List<ServiceRequest> results = service.getByStatus("Resolved");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void getByPriority_emptyResult_returnsEmptyNotNull() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));

        List<ServiceRequest> results = service.getByPriority("URGENT");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void getByUserId_emptyResult_returnsEmptyNotNull() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));

        List<ServiceRequest> results = service.getByUserId("nonexistent");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void searchRequests_emptyResult_returnsEmptyNotNull() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));

        List<ServiceRequest> results = service.searchRequests("zzznomatch");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void getHistory_emptyResult_returnsEmptyNotNull() {

        List<HistoryEntry> results = service.getHistory("nonexistent");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // -------------------------------------------------
    // FR-17 View Comments — getComments()
    // -------------------------------------------------

    @Test
    void getComments_returnsStoredComments() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));
        service.addComment("1", new Comment("C1", "alice", "First comment"));
        service.addComment("1", new Comment("C2", "bob", "Second comment"));

        List<Comment> comments = service.getComments("1");

        assertEquals(2, comments.size());
        assertEquals("First comment", comments.get(0).getBody());
        assertEquals("Second comment", comments.get(1).getBody());
    }

    @Test
    void getComments_returnsCorrectAuthor() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));
        service.addComment("1", new Comment("C1", "alice", "body"));

        List<Comment> comments = service.getComments("1");

        assertEquals("alice", comments.get(0).getAuthorId());
    }

    @Test
    void getComments_unknownRequest_throwsException() {

        assertThrows(RequestNotFoundException.class,
                () -> service.getComments("nonexistent"));
    }

    @Test
    void getComments_preservesInsertionOrder() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));
        service.addComment("1", new Comment("C1", "alice", "First"));
        service.addComment("1", new Comment("C2", "bob",   "Second"));
        service.addComment("1", new Comment("C3", "carol", "Third"));

        List<Comment> comments = service.getComments("1");

        assertEquals("First",  comments.get(0).getBody());
        assertEquals("Second", comments.get(1).getBody());
        assertEquals("Third",  comments.get(2).getBody());
    }

    @Test
    void getComments_hasCreatedAt() {

        service.addRequest(new ServiceRequest("1", "user1", "Ticket A", "desc", "LOW"));
        service.addComment("1", new Comment("C1", "alice", "body"));

        assertNotNull(service.getComments("1").get(0).getCreatedAt());
    }

    // -------------------------------------------------
    // Helper
    // -------------------------------------------------

    private ServiceRequest buildWith(
            String id, String userId, String title,
            String desc, String priority, String status) {

        ServiceRequest r = new ServiceRequest(id, userId, title, desc, priority);
        r.setStatus(status);
        return r;
    }
}