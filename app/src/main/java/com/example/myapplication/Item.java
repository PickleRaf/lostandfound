package com.example.myapplication;

public class Item {
    private String name;
    private String description;

    private String location;
    private String UserID;
    private String ItemID;

    // Constructor (what we use to create a new Item)
    public Item(String name, String description,String location, String UserID, String ItemID) {
        this.name = name;
        this.description = description;
        this.location = location;
        this.UserID = UserID;
        this.ItemID = ItemID;

    }

    // Getters (we use these to get the info later)
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getLocation() { return location;  }

    public String getUserID(){ return UserID; }

    public String getItemID(){ return ItemID;}
}
