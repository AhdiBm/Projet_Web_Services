package dao;

import dto.Channel;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChannelDAOJDBC implements ChannelDAO {
    private String url = "jdbc:postgresql://localhost:5432/postgres";
    private String dbUser = "postgres";
    private String dbPassword = "19562004";

    public ChannelDAOJDBC() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, dbUser, dbPassword);
    }

    @Override
    public Channel findById(int channelId) {
        String sql = "SELECT * FROM channel WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, channelId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Channel(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getInt("creator_id")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Channel> findAll() {
        List<Channel> channels = new ArrayList<>();
        String sql = "SELECT * FROM channel";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                channels.add(new Channel(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("type"),
                    rs.getInt("creator_id")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return channels;
    }

    @Override
    public List<Channel> findPublicChannels() {
        List<Channel> channels = new ArrayList<>();
        String sql = "SELECT * FROM channel WHERE type = 'public'";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                channels.add(new Channel(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("type"),
                    rs.getInt("creator_id")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return channels;
    }

    @Override
    public List<Channel> findAvailableChannels(int userId) {
        List<Channel> channels = new ArrayList<>();
        String sql = "SELECT * FROM channel WHERE type = 'public' " +
                    "UNION " +
                    "SELECT c.* FROM channel c " +
                    "JOIN channel_member cm ON c.id = cm.channel_id " +
                    "WHERE c.type = 'private' AND cm.user_id = ? " +
                    "ORDER BY id ASC";
                    
        try (Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    channels.add(new Channel(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getInt("creator_id")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return channels;
    }

    @Override
    public boolean isUserMember(int channelId, int userId) {
        String sql = "SELECT COUNT(*) FROM channel_member WHERE channel_id = ? AND user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, channelId);
            pstmt.setInt(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean addMember(int channelId, int userId, boolean isRoomAdmin) {
        String sql = "INSERT INTO channel_member (channel_id, user_id, is_room_admin) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, channelId);
            pstmt.setInt(2, userId);
            pstmt.setBoolean(3, isRoomAdmin);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}