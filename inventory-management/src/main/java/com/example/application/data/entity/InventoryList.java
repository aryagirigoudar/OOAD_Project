package com.example.application.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;

@Entity
public class InventoryList extends AbstractEntity {

    private String itemName;
    private String itemCategory;
    private Integer itemCounter;
    @Lob
    @Column(length = 1000000)
    private byte[] itemImage;

    public String getItemName() {
        return itemName;
    }
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
    public String getItemCategory() {
        return itemCategory;
    }
    public void setItemCategory(String itemCategory) {
        this.itemCategory = itemCategory;
    }
    public Integer getItemCounter() {
        return itemCounter;
    }
    public void setItemCounter(Integer itemCounter) {
        this.itemCounter = itemCounter;
    }
    public byte[] getItemImage() {
        return itemImage;
    }
    public void setItemImage(byte[] itemImage) {
        this.itemImage = itemImage;
    }

}
