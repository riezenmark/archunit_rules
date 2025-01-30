package com.example.archunitrules.entity;

import com.example.archunitrules.enumeration.AgeRating;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
@Table(name = "article")
public class Article {
    @Id
    private UUID id;

    private String title;

    private String content;

    @Enumerated(EnumType.STRING)
    private AgeRating ageRating;
}
