package dto;

public class ChannelMember {
    private int channelId;
    private int userId;
    private boolean isRoomAdmin;

    public ChannelMember() {}

    public ChannelMember(int channelId, int userId, boolean isRoomAdmin) {
        this.channelId = channelId;
        this.userId = userId;
        this.isRoomAdmin = isRoomAdmin;
    }

    public int getChannelId() { return channelId; }
    public void setChannelId(int channelId) { this.channelId = channelId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public boolean isRoomAdmin() { return isRoomAdmin; }
    public void setRoomAdmin(boolean roomAdmin) { isRoomAdmin = roomAdmin; }
}