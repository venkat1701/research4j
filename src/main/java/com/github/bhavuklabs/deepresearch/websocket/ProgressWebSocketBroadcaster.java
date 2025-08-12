package com.github.bhavuklabs.deepresearch.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * WebSocket broadcaster for real-time deep research progress updates.
 * Handles multiple concurrent sessions and provides reliable message delivery.
 */
public class ProgressWebSocketBroadcaster extends TextWebSocketHandler {

    private static final Logger logger = Logger.getLogger(ProgressWebSocketBroadcaster.class.getName());

    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, ConcurrentMap<String, WebSocketSession>> sessionMap; // sessionId -> connectionId -> session
    private final ConcurrentMap<String, Instant> lastHeartbeat; // connectionId -> timestamp
    private final ScheduledExecutorService heartbeatExecutor;

    private static final long HEARTBEAT_INTERVAL_MS = 30000; // 30 seconds
    private static final long SESSION_TIMEOUT_MS = 60000; // 1 minute

    public ProgressWebSocketBroadcaster() {
        this.objectMapper = new ObjectMapper();
        this.sessionMap = new ConcurrentHashMap<>();
        this.lastHeartbeat = new ConcurrentHashMap<>();
        this.heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "WebSocket-Heartbeat");
            thread.setDaemon(true);
            return thread;
        });

        startHeartbeatTask();
    }

    /**
     * WebSocket message structure for progress updates
     */
    public static class ProgressMessage {
        private String type;
        private String sessionId;
        private String nodeId;
        private String parentNodeId;
        private Object data;
        private Instant timestamp;

        public ProgressMessage() {}

        public ProgressMessage(String type, String sessionId, String nodeId, Object data) {
            this.type = type;
            this.sessionId = sessionId;
            this.nodeId = nodeId;
            this.data = data;
            this.timestamp = Instant.now();
        }

        public ProgressMessage(String type, String sessionId, String nodeId, String parentNodeId, Object data) {
            this(type, sessionId, nodeId, data);
            this.parentNodeId = parentNodeId;
        }

        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }

        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }

        public String getParentNodeId() { return parentNodeId; }
        public void setParentNodeId(String parentNodeId) { this.parentNodeId = parentNodeId; }

        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }

        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String connectionId = session.getId();
        String sessionId = extractSessionId(session);

        if (sessionId == null) {
            logger.warning("No session ID found in WebSocket connection: " + connectionId);
            session.close(CloseStatus.BAD_DATA.withReason("Missing session ID"));
            return;
        }

        // Add session to the session map
        sessionMap.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>()).put(connectionId, session);
        lastHeartbeat.put(connectionId, Instant.now());

        logger.info("WebSocket connection established: " + connectionId + " for session: " + sessionId);

        // Send connection confirmation
        ProgressMessage confirmMessage = new ProgressMessage("connection_established", sessionId, null,
            Map.of("connectionId", connectionId, "timestamp", Instant.now()));
        sendMessageToSession(session, confirmMessage);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String connectionId = session.getId();
        String sessionId = extractSessionId(session);

        if (sessionId != null) {
            ConcurrentMap<String, WebSocketSession> connections = sessionMap.get(sessionId);
            if (connections != null) {
                connections.remove(connectionId);
                if (connections.isEmpty()) {
                    sessionMap.remove(sessionId);
                }
            }
        }

        lastHeartbeat.remove(connectionId);

        logger.info("WebSocket connection closed: " + connectionId + " (status: " + status + ")");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String connectionId = session.getId();
        lastHeartbeat.put(connectionId, Instant.now());

        // Handle heartbeat responses or other client messages
        String payload = message.getPayload();
        if ("pong".equals(payload)) {
            // Heartbeat response - already updated lastHeartbeat above
            return;
        }

        logger.fine("Received message from WebSocket: " + connectionId + " - " + payload);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String connectionId = session.getId();
        String sessionId = extractSessionId(session);

        logger.warning("WebSocket transport error for connection " + connectionId +
            " (session: " + sessionId + "): " + exception.getMessage());

        // Close the connection and clean up
        try {
            session.close(CloseStatus.SERVER_ERROR);
        } catch (Exception e) {
            logger.severe("Failed to close WebSocket session after transport error: " + e.getMessage());
        }
    }

    // Public API for broadcasting progress updates
    public void broadcastProgress(String sessionId, String nodeId, String messageType, Object data) {
        ProgressMessage message = new ProgressMessage(messageType, sessionId, nodeId, data);
        broadcastToSession(sessionId, message);
    }

    public void broadcastProgress(String sessionId, String nodeId, String parentNodeId, String messageType, Object data) {
        ProgressMessage message = new ProgressMessage(messageType, sessionId, nodeId, parentNodeId, data);
        broadcastToSession(sessionId, message);
    }

    public void broadcastNodeUpdate(String sessionId, String nodeId, String status, Object additionalData) {
        Map<String, Object> data = Map.of(
            "nodeId", nodeId,
            "status", status,
            "additionalData", additionalData != null ? additionalData : Map.of()
        );
        broadcastProgress(sessionId, nodeId, "node_update", data);
    }

    public void broadcastQueryGeneration(String sessionId, String nodeId, String parentNodeId, String query, String researchGoal) {
        Map<String, Object> data = Map.of(
            "nodeId", nodeId,
            "parentNodeId", parentNodeId,
            "query", query,
            "researchGoal", researchGoal
        );
        broadcastProgress(sessionId, nodeId, parentNodeId, "generating_query", data);
    }

    public void broadcastSearchComplete(String sessionId, String nodeId, Object searchResults) {
        Map<String, Object> data = Map.of(
            "nodeId", nodeId,
            "searchResults", searchResults
        );
        broadcastProgress(sessionId, nodeId, "search_complete", data);
    }

    public void broadcastNodeComplete(String sessionId, String nodeId, Object learnings, Object followUpQuestions) {
        Map<String, Object> data = Map.of(
            "nodeId", nodeId,
            "learnings", learnings != null ? learnings : Map.of(),
            "followUpQuestions", followUpQuestions != null ? followUpQuestions : Map.of()
        );
        broadcastProgress(sessionId, nodeId, "node_complete", data);
    }

    public void broadcastReasoningDelta(String sessionId, String nodeId, String reasoningType, String delta) {
        Map<String, Object> data = Map.of(
            "nodeId", nodeId,
            "reasoningType", reasoningType,
            "delta", delta
        );
        broadcastProgress(sessionId, nodeId, reasoningType, data);
    }

    public void broadcastError(String sessionId, String nodeId, String errorMessage) {
        Map<String, Object> data = Map.of(
            "nodeId", nodeId,
            "message", errorMessage
        );
        broadcastProgress(sessionId, nodeId, "error", data);
    }

    public void broadcastTreeComplete(String sessionId, Object learnings) {
        Map<String, Object> data = Map.of(
            "learnings", learnings
        );
        broadcastProgress(sessionId, null, "complete", data);
    }

    // Broadcast to all connections for a specific session
    private void broadcastToSession(String sessionId, ProgressMessage message) {
        ConcurrentMap<String, WebSocketSession> connections = sessionMap.get(sessionId);
        if (connections == null || connections.isEmpty()) {
            logger.fine("No WebSocket connections found for session: " + sessionId);
            return;
        }

        connections.values().forEach(session -> {
            try {
                sendMessageToSession(session, message);
            } catch (Exception e) {
                logger.warning("Failed to send message to WebSocket session " + session.getId() + ": " + e.getMessage());
                // Remove failed session
                connections.remove(session.getId());
                lastHeartbeat.remove(session.getId());
            }
        });
    }

    private void sendMessageToSession(WebSocketSession session, ProgressMessage message) throws IOException {
        if (session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
            } catch (JsonProcessingException e) {
                logger.severe("Failed to serialize progress message: " + e.getMessage());
                throw new IOException("Message serialization failed", e);
            }
        } else {
            throw new IOException("WebSocket session is closed");
        }
    }

    private String extractSessionId(WebSocketSession session) {
        // Extract session ID from WebSocket URI path
        String path = session.getUri() != null ? session.getUri().getPath() : "";
        String[] segments = path.split("/");

        // Expected path: /ws/research/{sessionId}
        if (segments.length >= 3 && "research".equals(segments[2])) {
            return segments.length > 3 ? segments[3] : null;
        }

        // Fallback: check query parameters
        String query = session.getUri() != null ? session.getUri().getQuery() : "";
        if (query != null && query.contains("sessionId=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("sessionId=")) {
                    return param.substring("sessionId=".length());
                }
            }
        }

        return null;
    }

    private void startHeartbeatTask() {
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            try {
                sendHeartbeats();
                cleanupStaleConnections();
            } catch (Exception e) {
                logger.warning("Error in heartbeat task: " + e.getMessage());
            }
        }, HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void sendHeartbeats() {
        Instant now = Instant.now();

        sessionMap.values().forEach(connections ->
            connections.values().forEach(session -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage("ping"));
                    }
                } catch (Exception e) {
                    logger.fine("Failed to send heartbeat to session " + session.getId());
                }
            })
        );
    }

    private void cleanupStaleConnections() {
        Instant cutoff = Instant.now().minusMillis(SESSION_TIMEOUT_MS);

        lastHeartbeat.entrySet().removeIf(entry -> {
            String connectionId = entry.getKey();
            Instant lastSeen = entry.getValue();

            if (lastSeen.isBefore(cutoff)) {
                logger.info("Cleaning up stale WebSocket connection: " + connectionId);

                // Remove from session map
                sessionMap.values().forEach(connections -> {
                    WebSocketSession session = connections.remove(connectionId);
                    if (session != null && session.isOpen()) {
                        try {
                            session.close(CloseStatus.SESSION_NOT_RELIABLE);
                        } catch (Exception e) {
                            logger.fine("Error closing stale WebSocket session: " + e.getMessage());
                        }
                    }
                });

                return true; // Remove from lastHeartbeat map
            }

            return false;
        });

        // Clean up empty session entries
        sessionMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    // Management and monitoring methods
    public int getActiveConnectionCount() {
        return sessionMap.values().stream()
            .mapToInt(Map::size)
            .sum();
    }

    public int getActiveSessionCount() {
        return sessionMap.size();
    }

    public Map<String, Integer> getConnectionsBySession() {
        Map<String, Integer> result = new ConcurrentHashMap<>();
        sessionMap.forEach((sessionId, connections) ->
            result.put(sessionId, connections.size()));
        return result;
    }

    public boolean hasActiveConnections(String sessionId) {
        ConcurrentMap<String, WebSocketSession> connections = sessionMap.get(sessionId);
        return connections != null && !connections.isEmpty();
    }

    public void closeAllConnections(String sessionId) {
        ConcurrentMap<String, WebSocketSession> connections = sessionMap.remove(sessionId);
        if (connections != null) {
            connections.values().forEach(session -> {
                try {
                    if (session.isOpen()) {
                        session.close(CloseStatus.NORMAL.withReason("Session ended"));
                    }
                } catch (Exception e) {
                    logger.fine("Error closing WebSocket session: " + e.getMessage());
                }
                lastHeartbeat.remove(session.getId());
            });
        }
    }

    // Shutdown cleanup
    public void shutdown() {
        logger.info("Shutting down ProgressWebSocketBroadcaster...");

        // Close all connections
        sessionMap.keySet().forEach(this::closeAllConnections);

        // Shutdown heartbeat executor
        heartbeatExecutor.shutdown();
        try {
            if (!heartbeatExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                heartbeatExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            heartbeatExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        logger.info("ProgressWebSocketBroadcaster shutdown complete");
    }
}