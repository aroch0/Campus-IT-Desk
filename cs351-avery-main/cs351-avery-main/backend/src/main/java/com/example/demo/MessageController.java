package com.example.demo;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Comment;
import com.example.demo.model.HistoryEntry;
import com.example.demo.model.ServiceRequest;
import com.example.demo.service.MessageService;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    @Autowired
    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public List<ServiceRequest> getRequests() {
        return messageService.getAllRequests();
    }

    @GetMapping("/sorted/date")
    public List<ServiceRequest> getRequestsSortedByDate(
            @RequestParam(defaultValue = "desc") String direction) {
        return messageService.getRequestsSortedByDate(direction);
    }

    @GetMapping("/{id}")
    public ServiceRequest getRequestById(@PathVariable String id) {
        return messageService.getRequestById(id);
    }

    @GetMapping("/status/{status}")
    public List<ServiceRequest> getByStatus(@PathVariable String status) {
        return messageService.getByStatus(status);
    }

    @GetMapping("/priority/{priority}")
    public List<ServiceRequest> getByPriority(@PathVariable String priority) {
        return messageService.getByPriority(priority);
    }

    @GetMapping("/user/{userId}")
    public List<ServiceRequest> getByUserId(@PathVariable String userId) {
        return messageService.getByUserId(userId);
    }

    @GetMapping("/search/{keyword}")
    public List<ServiceRequest> searchRequests(@PathVariable String keyword) {
        return messageService.searchRequests(keyword);
    }

    @PostMapping
    public ServiceRequest createRequest(@RequestBody ServiceRequest request) {
        System.out.println("Received request: " + request);
        messageService.addRequest(request);
        return request;
    }

    @PutMapping("/{id}")
    public ServiceRequest updateRequest(
            @PathVariable String id,
            @RequestBody ServiceRequest request) {
        messageService.updateRequest(id, request);
        return messageService.getRequestById(id);
    }

    // FR-16 COMMENTS
    @GetMapping("/{id}/comments")
    public List<Comment> getComments(@PathVariable String id) {
        return messageService.getComments(id);
    }

    @PostMapping("/{id}/comments")
    public ServiceRequest addComment(
            @PathVariable String id,
            @RequestBody Comment comment) {
        return messageService.addComment(id, comment);
    }

    // FR-18 HISTORY
    @GetMapping("/{id}/history")
    public List<HistoryEntry> getHistory(@PathVariable String id) {
        return messageService.getHistory(id);
    }
}