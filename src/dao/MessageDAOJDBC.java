package dao;

import dto.Message;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDAOJDBC implements MessageDAO {
    private String url = "jdbc:postgresql://localhost:5432/postgres";
    private String dbUser = "postgres";
    private String dbPassword = "19562004";

    public MessageDAOJDBC() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, dbUser, dbPassword);
    }

    // Récupère un message par son ID, utilisé pour vérifier les droits d'auteur.
    @Override
    public Message findById(int messageId) {
        String sql = "SELECT * FROM message WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, messageId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Message(
                        rs.getInt("id"),
                        rs.getString("content"),
                        rs.getTimestamp("created_at"),
                        rs.getInt("user_id"),
                        rs.getInt("channel_id")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Récupère la liste des messages d'un canal, triés par date de création.
    @Override
    public List<Message> findByChannelId(int channelId) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM message WHERE channel_id = ? ORDER BY created_at ASC";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, channelId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(new Message(
                        rs.getInt("id"),
                        rs.getString("content"),
                        rs.getTimestamp("created_at"),
                        rs.getInt("user_id"),
                        rs.getInt("channel_id")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    // Crée un message dans un canal, en associant le contenu, l'auteur et le canal.
    @Override
    public boolean create(Message message) {
        String sql = "INSERT INTO message (content, user_id, channel_id) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, message.getContent());
            pstmt.setInt(2, message.getUserId());
            pstmt.setInt(3, message.getChannelId());
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        message.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Met à jour le contenu d'un message existant, identifié par son ID.
    @Override
    public boolean update(Message message) {
        String sql = "UPDATE message SET content = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, message.getContent());
            pstmt.setInt(2, message.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Supprime un message identifié par son ID.
    @Override
    public boolean delete(int messageId) {
        String sql = "DELETE FROM message WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, messageId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}