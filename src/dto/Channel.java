package dto;

public class Channel {
    private int id;
    private String name;
    private String type; // "public" ou "private"
    private int creatorId;

    public Channel() {}

    public Channel(int id, String name, String type, int creatorId) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.creatorId = creatorId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getCreatorId() { return creatorId; }
    public void setCreatorId(int creatorId) { this.creatorId = creatorId; }
}