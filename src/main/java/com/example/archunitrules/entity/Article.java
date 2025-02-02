package com.example.archunitrules.entity;

import com.example.archunitrules.enumeration.AgeRating;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;
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

    @Override
    public boolean equals(Object another) {
        if (this == another) return true;
        if (another == null) return false;
        Class<?> thisClass = this instanceof HibernateProxy thisHibernateProxy
                ? thisHibernateProxy.getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        Class<?> anotherClass = another instanceof HibernateProxy anotherHibernateProxy
                ? anotherHibernateProxy.getHibernateLazyInitializer().getPersistentClass()
                : another.getClass();
        if (thisClass != anotherClass) return false;
        Article article = (Article) another;
        return id != null && id.equals(article.id);
    }

    @Override
    public int hashCode() {
        return this instanceof HibernateProxy hibernateProxy
                ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }
}
