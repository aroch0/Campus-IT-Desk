# Campus IT Help Desk Ticketing System

A full-stack web application for submitting, tracking, and managing IT support requests in a campus environment.

## Structure

```
project/
├── frontend/   # React + Vite (port 5173)
└── backend/    # Java Spring Boot + Maven (port 8080)
```

## Features

- Create IT support tickets with title, description, and priority
- View all submitted requests in a sortable, filterable table
- View and edit detailed ticket information in a slide panel
- Update ticket status: Open, In Progress, Resolved, Closed
- Assign and update priority levels: Urgent, High, Medium, Low
- Filter tickets by status, priority, user ID, or date range
- Search tickets by title or description
- Add comments to tickets
- Track full change history per ticket (status changes, priority changes, comments)
- Data persisted to file between server restarts

## Prerequisites

- Node.js 18+ and npm
- Java 17 JDK
- Apache Maven 3.8+

## Running the Project

### 1. Start the Backend

```bash
cd backend
mvn spring-boot:run
```

Runs at: `http://localhost:8080`

Data files `requests.txt` and `history.txt` are created automatically in the `backend/` directory on first run.

### 2. Start the Frontend

```bash
cd frontend
npm install
npm run dev
```

Runs at: `http://localhost:5173`

Both services must be running simultaneously. The frontend connects to the backend at `http://localhost:8080`.

## API Reference

All endpoints are prefixed with `/api/messages`.

### Requests

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/messages` | Get all requests |
| POST | `/api/messages` | Create a new request |
| GET | `/api/messages/{id}` | Get request by ID |
| PUT | `/api/messages/{id}` | Update a request |

### Filtering and Search

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/messages/status/{status}` | Filter by status |
| GET | `/api/messages/priority/{priority}` | Filter by priority |
| GET | `/api/messages/user/{userId}` | Filter by user ID |
| GET | `/api/messages/search/{keyword}` | Search title and description |
| GET | `/api/messages/sorted/date?direction=asc\|desc` | Get requests sorted by date |

### Comments

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/messages/{id}/comments` | Get comments for a request |
| POST | `/api/messages/{id}/comments` | Add a comment to a request |

### History

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/messages/{id}/history` | Get change history for a request |

### Request Body — Create/Update

```json
{
  "userId": "12345",
  "title": "Printer not working",
  "description": "The printer on floor 2 won't connect.",
  "priority": "HIGH",
  "status": "Open",
  "assignedTo": null
}
```

Valid `priority` values: `LOW`, `MEDIUM`, `HIGH`, `URGENT`

Valid `status` values: `Open`, `In Progress`, `Resolved`, `Closed`

### Error Responses

All errors return JSON in this shape:

```json
{
  "status": 400,
  "message": "title is required",
  "timestamp": "2025-01-01T12:00:00"
}
```

| Status | Meaning |
|--------|---------|
| 400 | Validation error (missing required field, invalid priority) |
| 404 | Request ID not found |
| 500 | Unexpected server error |

## Data Storage

Requests and history are stored as flat files in the `backend/` directory:

- `requests.txt` — one request per line, comma-delimited
- `history.txt` — one history entry per line, pipe-delimited

Both files are created automatically. Do not edit them manually. Commas and newlines in field values are escaped automatically.

## Known Limitations

- No user authentication — anyone can create or modify any ticket
- No pagination — all tickets are loaded at once
- File-based storage is not suitable for high-concurrency or large datasets
- `assignedTo` field is stored but not exposed in the UI