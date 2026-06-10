package dto;

import java.sql.Timestamp;

public class Message {
    private int id;
    private String content;
    private Timestamp createdAt;
    private int userId;
    private int channelId;

    public Message() {}

    public Message(int id, String content, Timestamp createdAt, int userId, int channelId) {
        this.id = id;
        this.content = content;
        this.createdAt = createdAt;
        this.userId = userId;
        this.channelId = channelId;
    }

    // Récupère l'ID du message.
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    // Récupère le contenu du message.
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    // Récupère la date de création du message.
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    // Récupère l'ID de l'utilisateur qui a créé le message.
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    // Récupère l'ID du canal auquel le message appartient.
    public int getChannelId() { return channelId; }
    public void setChannelId(int channelId) { this.channelId = channelId; }
}