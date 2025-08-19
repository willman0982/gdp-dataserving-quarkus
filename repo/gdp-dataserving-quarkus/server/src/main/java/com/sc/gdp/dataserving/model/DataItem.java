package com.sc.gdp.dataserving.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.microprofile.graphql.Type;
import io.quarkus.hibernate.orm.panache.PanacheEntity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Data item model for both REST and GraphQL APIs
 */
@Entity
@Table(name = "data_items")
@Type("DataItem")
public class DataItem extends PanacheEntity {
    
    @JsonProperty("name")
    @Column(name = "name", nullable = false)
    private String name;
    
    @JsonProperty("description")
    @Column(name = "description")
    private String description;
    
    @JsonProperty("value")
    @Column(name = "value")
    private String value;
    
    @JsonProperty("createdAt")
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @JsonProperty("updatedAt")
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @JsonProperty("category")
    @Column(name = "category")
    private String category;
    
    @JsonProperty("status")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DataStatus status;

    // Default constructor
    public DataItem() {
        this.status = DataStatus.ACTIVE;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Constructor with parameters
    public DataItem(String name, String description, String value, String category) {
        this();
        this.name = name;
        this.description = description;
        this.value = value;
        this.category = category;
    }

    // Getters and setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public DataStatus getStatus() {
        return status;
    }

    public void setStatus(DataStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataItem dataItem = (DataItem) o;
        return Objects.equals(id, dataItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DataItem{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", value='" + value + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", category='" + category + '\'' +
                ", status=" + status +
                '}';
    }
}