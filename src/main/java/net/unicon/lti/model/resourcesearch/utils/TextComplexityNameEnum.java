package net.unicon.lti.model.resourcesearch.utils;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TextComplexityNameEnum {
    LEXILE("Lexile"),
    FLESCH_KINCAID("Flesch-Kincaid"),
    DALE_SCHALL("Dale-Schall"),
    DRA("DRA"),
    FOUNTAS_PINNELL("Fountas-Pinnell");

    private final String name;

    TextComplexityNameEnum(final String name) {
        this.name = name;
    }

    @Override
    @JsonValue
    public String toString() {
        return name;
    }
}
