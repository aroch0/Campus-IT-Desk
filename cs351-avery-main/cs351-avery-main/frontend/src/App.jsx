import { useEffect, useState } from "react";

export default function App() {
  const [messages, setMessages] = useState([]);
  const [userId, setUserId] = useState("");
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [priority, setPriority] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [editRequest, setEditRequest] = useState(null);
  const [editField, setEditField] = useState(null);
  const [activeTab, setActiveTab] = useState("details");
  const [history, setHistory] = useState([]);
  const [newComment, setNewComment] = useState({ authorId: "", body: "" });
  const [filterType, setFilterType] = useState("");
  const [filterValue, setFilterValue] = useState("");
  const [searchText, setSearchText] = useState("");
  const [sortDirection, setSortDirection] = useState(null);

  const fetchMessages = async () => {
    const res = await fetch("http://localhost:8080/api/messages");
    const data = await res.json();
    setMessages(data);
  };

  const fetchHistory = async (requestId) => {
    const res = await fetch(`http://localhost:8080/api/messages/${requestId}/history`);
    const data = await res.json();
    setHistory(data);
  };

  useEffect(() => { fetchMessages(); }, []);

  useEffect(() => {
    if (editRequest && activeTab === "history") fetchHistory(editRequest.requestId);
  }, [activeTab, editRequest]);

  const handleKeyDown = (e) => { if (e.key === "Enter") handleUpdate(); };

  const openRequest = (message, index) => {
    setEditRequest({ ...message, ticketNumber: message.requestId.substring(0, 8).toUpperCase() });
    setEditField(null);
    setNewComment({ authorId: "", body: "" });
    setActiveTab("details");
    setHistory([]);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const newRequest = {
      requestId: crypto.randomUUID(),
      userId, title, description, priority,
      status: "Open", assignedTo: null,
    };
    try {
      await fetch("http://localhost:8080/api/messages", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(newRequest),
      });
      await fetchMessages();
      setUserId(""); setTitle(""); setDescription(""); setPriority("");
      setShowForm(false);
    } catch (err) { console.error("Create request failed:", err); }
  };

  const handleUpdate = async () => {
    await fetch(`http://localhost:8080/api/messages/${editRequest.requestId}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(editRequest),
    });
    await fetchMessages();
    setEditRequest(null);
    setEditField(null);
  };

  const handleAddComment = async () => {
    if (!newComment.body.trim()) return;
    try {
      const res = await fetch(
        `http://localhost:8080/api/messages/${editRequest.requestId}/comments`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            authorId: newComment.authorId.trim() || "anonymous",
            body: newComment.body.trim(),
          }),
        }
      );
      if (!res.ok) { console.error("Add comment failed:", await res.text()); return; }
      const updated = await res.json();
      setEditRequest({ ...updated, ticketNumber: editRequest.ticketNumber });
      setNewComment({ authorId: "", body: "" });
      await fetchMessages();
    } catch (err) { console.error("Add comment error:", err); }
  };

  const handleClear = () => {
    setFilterType(""); setFilterValue(""); setSearchText(""); setSortDirection(null);
  };

  const toggleSort = () => {
    setSortDirection((prev) => prev === null ? "desc" : prev === "desc" ? "asc" : null);
  };

  const filteredMessages = messages.filter((message) => {
    const value = filterValue.toLowerCase();
    let matches = true;
    switch (filterType) {
      case "userId":    matches = String(message.userId).includes(value); break;
      case "requestId": matches = String(message.requestId).includes(value); break;
      case "text":      matches = message.title?.toLowerCase().includes(value) || message.description?.toLowerCase().includes(value); break;
      case "status":    matches = filterValue === "" || message.status === filterValue; break;
      case "priority":  matches = filterValue === "" || message.priority === filterValue; break;
      case "date": {
        if (!filterValue) break;
        if (!message.createdAt) { matches = false; break; }
        const created = new Date(message.createdAt);
        const now = new Date();
        if (filterValue === "today") {
          matches = created.toLocaleDateString("en-CA") === now.toLocaleDateString("en-CA");
        } else {
          const days = parseInt(filterValue.replace("last", ""));
          const cutoff = new Date();
          cutoff.setDate(cutoff.getDate() - days);
          matches = created >= cutoff;
        }
        break;
      }
      default: matches = true;
    }
    const matchesSearch =
      searchText === "" ||
      message.title?.toLowerCase().includes(searchText.toLowerCase()) ||
      message.description?.toLowerCase().includes(searchText.toLowerCase());
    return matches && matchesSearch;
  });

  const sortedMessages = [...filteredMessages].sort((a, b) => {
  const dateA = a.createdAt ? new Date(a.createdAt) : new Date(0);
  const dateB = b.createdAt ? new Date(b.createdAt) : new Date(0);
  if (sortDirection === "asc") return dateA - dateB;
  return dateB - dateA;
});

  const formatDate = (date) => date ? new Date(date).toLocaleString() : "N/A";

  const formatAction = (action) => {
    switch (action) {
      case "CREATED":          return "🆕 Request created";
      case "UPDATED":          return "✏️ Request updated";
      case "STATUS_CHANGED":   return "🔄 Status changed";
      case "PRIORITY_CHANGED": return "⚑ Priority changed";
      case "COMMENT_ADDED":    return "💬 Comment added";
      default:                 return action;
    }
  };

  const sortLabel =
    sortDirection === "desc" ? "Date ↓" :
    sortDirection === "asc"  ? "Date ↑" : "Sort by Date";

  const statusClass = (status) => status?.toLowerCase().replace(" ", "-");

  const priorityBadge = (priority) => (
    <span className={`priority-badge ${priority?.toLowerCase()}`}>{priority}</span>
  );

  const statusBadge = (status) => (
    <span className={`status-badge ${statusClass(status)}`}>{status}</span>
  );

  return (
    <main className="container">
      <h1>Campus IT Help Desk Requests</h1>

      {!showForm && (
        <button className="primary" onClick={() => setShowForm(true)}>+ New Request</button>
      )}

      {/* CREATE FORM */}
      {showForm && (
        <form onSubmit={handleSubmit} className="create-form">
          <input type="number" placeholder="User ID" value={userId} onChange={(e) => setUserId(e.target.value)} />
          <input placeholder="Title" value={title} onChange={(e) => setTitle(e.target.value)} />
          <input placeholder="Description" value={description} onChange={(e) => setDescription(e.target.value)} />
          <select value={priority} onChange={(e) => setPriority(e.target.value)}>
            <option value="">Select Priority</option>
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
            <option value="URGENT">Urgent</option>
          </select>
          <button type="submit">Submit</button>
        </form>
      )}

      {/* FILTER BAR */}
      {!showForm && (
        <div className="filter-bar">
          <select value={filterType} onChange={(e) => { setFilterType(e.target.value); setFilterValue(""); }}>
            <option value="">Filter By</option>
            <option value="userId">User ID</option>
            <option value="requestId">Request ID</option>
            <option value="status">Status</option>
            <option value="priority">Priority</option>
            <option value="date">Date</option>
          </select>

          {(filterType === "status" || filterType === "priority") && (
            <select value={filterValue} onChange={(e) => setFilterValue(e.target.value)}>
              <option value="">Select</option>
              {filterType === "status" && (
                <>
                  <option value="Open">Open</option>
                  <option value="In Progress">In Progress</option>
                  <option value="Closed">Closed</option>
                  <option value="Resolved">Resolved</option>
                </>
              )}
              {filterType === "priority" && (
                <>
                  <option value="LOW">Low</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="HIGH">High</option>
                  <option value="URGENT">Urgent</option>
                </>
              )}
            </select>
          )}

          {(filterType === "userId" || filterType === "requestId" || filterType === "text") && (
            <input placeholder="Search..." value={filterValue} onChange={(e) => setFilterValue(e.target.value)} />
          )}

          {filterType === "date" && (
            <select value={filterValue} onChange={(e) => setFilterValue(e.target.value)}>
              <option value="">Any time</option>
              <option value="today">Today</option>
              <option value="last7">Last 7 days</option>
              <option value="last14">Last 14 days</option>
              <option value="last30">Last 30 days</option>
              <option value="last60">Last 2 months</option>
              <option value="last90">Last 3 months</option>
              <option value="last180">Last 6 months</option>
              <option value="last365">Last year</option>
            </select>
          )}

          <input placeholder="Search title or description..." value={searchText} onChange={(e) => setSearchText(e.target.value)} />

          <button onClick={toggleSort} className={`sort-btn${sortDirection !== null ? " sort-btn--active" : ""}`}>
            {sortLabel}
          </button>

          <button onClick={handleClear}>Clear All</button>
        </div>
      )}

      {/* TABLE */}
      {!showForm && (
        <div className="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>Priority</th>
                <th>ID</th>
                <th>Title</th>
                <th>Status</th>
                <th className="col-date" onClick={toggleSort} title="Click to sort by date">
                  Created {sortDirection === "desc" ? "↓" : sortDirection === "asc" ? "↑" : "↕"}
                </th>
              </tr>
            </thead>
            <tbody>
              {sortedMessages.length === 0 ? (
                <tr><td colSpan="5"><p className="empty-text">No results found</p></td></tr>
              ) : (
                sortedMessages.map((message, index) => (
                  <tr key={message.requestId || index} className={`status-${statusClass(message.status)}`}>
                    <td>{priorityBadge(message.priority)}</td>
                    <td>
                      <span className="ticket-link" onClick={() => openRequest(message, index)}>
                        #{message.requestId.substring(0, 8).toUpperCase()}
                      </span>
                    </td>
                    <td>{message.title}</td>
                    <td>{statusBadge(message.status)}</td>
                    <td className="date-cell">{formatDate(message.createdAt)}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* SLIDE PANEL */}
      {editRequest && (
        <>
          <div className="overlay" onClick={() => setEditRequest(null)} />
          <div className="slide-panel">
            <h2>Request #{editRequest.ticketNumber}</h2>

            <div className="panel-tabs">
              <button onClick={() => setActiveTab("details")} className={`tab-btn${activeTab === "details" ? " tab-btn--active" : ""}`}>Details</button>
              <button onClick={() => setActiveTab("history")} className={`tab-btn${activeTab === "history" ? " tab-btn--active" : ""}`}>History</button>
            </div>

            {activeTab === "details" && (
              <>
                <strong>User ID</strong>
                <p>{editRequest.userId}</p>

                <strong>Title</strong>
                {editField === "title" ? (
                  <input value={editRequest.title} onChange={(e) => setEditRequest({ ...editRequest, title: e.target.value })} onBlur={() => setEditField(null)} onKeyDown={handleKeyDown} autoFocus />
                ) : (
                  <p className="editable-field" onClick={() => setEditField("title")}>{editRequest.title}</p>
                )}

                <strong>Description</strong>
                {editField === "description" ? (
                  <input value={editRequest.description} onChange={(e) => setEditRequest({ ...editRequest, description: e.target.value })} onBlur={() => setEditField(null)} onKeyDown={handleKeyDown} autoFocus />
                ) : (
                  <p className="editable-field" onClick={() => setEditField("description")}>{editRequest.description}</p>
                )}

                <strong>Priority</strong>
                {editField === "priority" ? (
                  <select value={editRequest.priority} onChange={(e) => setEditRequest({ ...editRequest, priority: e.target.value })} onBlur={() => setEditField(null)} autoFocus>
                    <option value="LOW">Low</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="HIGH">High</option>
                    <option value="URGENT">Urgent</option>
                  </select>
                ) : (
                  <p className="editable-field" onClick={() => setEditField("priority")}>{priorityBadge(editRequest.priority)}</p>
                )}

                <strong>Status</strong>
                {editField === "status" ? (
                  <select value={editRequest.status} onChange={(e) => setEditRequest({ ...editRequest, status: e.target.value })} onBlur={() => setEditField(null)} autoFocus>
                    <option value="Open">Open</option>
                    <option value="In Progress">In Progress</option>
                    <option value="Closed">Closed</option>
                    <option value="Resolved">Resolved</option>
                  </select>
                ) : (
                  <p className="editable-field" onClick={() => setEditField("status")}>{statusBadge(editRequest.status)}</p>
                )}

                <strong>Created</strong>
                <p>{formatDate(editRequest.createdAt)}</p>

                <strong>Last Updated</strong>
                <p>{formatDate(editRequest.updatedAt)}</p>

                <button className="primary" onClick={handleUpdate}>Save Changes</button>

                <hr />
                <strong>Comments</strong>

                {editRequest.comments && editRequest.comments.length > 0 ? (
                  editRequest.comments.map((c) => (
                    <div key={c.commentId} className="comment-card">
                      <div className="comment-header">
                        {c.authorId} &middot; <span className="comment-date">{formatDate(c.createdAt)}</span>
                      </div>
                      <div>{c.body}</div>
                    </div>
                  ))
                ) : (
                  <p className="empty-text">No comments yet.</p>
                )}

                <input placeholder="Your name (optional)" value={newComment.authorId} onChange={(e) => setNewComment({ ...newComment, authorId: e.target.value })} className="comment-input" />
                <textarea placeholder="Add a comment..." value={newComment.body} onChange={(e) => setNewComment({ ...newComment, body: e.target.value })} rows={3} className="comment-textarea" />
                <button onClick={handleAddComment} className="comment-submit primary">Add Comment</button>
              </>
            )}

            {activeTab === "history" && (
              <>
                {history.length === 0 ? (
                  <p className="empty-text">No history yet.</p>
                ) : (
                  [...history].reverse().map((h) => (
                    <div key={h.historyId} className="history-entry">
                      <div className="history-action">{formatAction(h.action)}</div>
                      {h.oldValue && h.newValue && (
                        <div className="history-change">
                          <span className="history-old">{h.oldValue}</span>{" → "}
                          <span className="history-new">{h.newValue}</span>
                        </div>
                      )}
                      {!h.oldValue && h.newValue && (
                        <div className="history-change">{h.newValue}</div>
                      )}
                      <div className="history-date">{formatDate(h.timestamp)}</div>
                    </div>
                  ))
                )}
              </>
            )}
          </div>
        </>
      )}
    </main>
  );
}