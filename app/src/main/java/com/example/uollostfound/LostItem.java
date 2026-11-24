package com.example.uollostfound;

/**
 * One post on the UOL Lost &amp; Found Board.
 *
 * Firebase uses the public getters to write a LostItem under the LostFound node and the
 * no-argument constructor plus the public setters to rebuild it from a DataSnapshot.
 * The field names become the child keys in the database:
 * id, itemName, type, location, contact and status.
 */
public class LostItem {

    private String id;
    private String itemName;
    private String type;      // "Lost" or "Found"
    private String location;
    private String contact;
    private String status;    // "Open" or "Returned"

    // Required by Firebase: getValue(LostItem.class) creates the object with this constructor
    public LostItem() {
    }

    public LostItem(String id, String itemName, String type, String location,
                    String contact, String status) {
        this.id = id;
        this.itemName = itemName;
        this.type = type;
        this.location = location;
        this.contact = contact;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
