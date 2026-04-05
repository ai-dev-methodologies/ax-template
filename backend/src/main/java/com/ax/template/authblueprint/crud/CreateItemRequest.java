package com.ax.template.authblueprint.crud;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateItemRequest {
    @NotBlank @Size(min = 1, max = 255)
    private String title;
    @Size(max = 2000)
    private String description;
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
}
