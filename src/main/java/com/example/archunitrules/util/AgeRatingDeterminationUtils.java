package com.example.archunitrules.util;

import com.example.archunitrules.enumeration.AgeRating;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AgeRatingDeterminationUtils {
    public static AgeRating determineAgeRating(String content) {
        int pseudoRandomRating = content.length() % AgeRating.values().length;
        return AgeRating.values()[pseudoRandomRating];
    }
}
