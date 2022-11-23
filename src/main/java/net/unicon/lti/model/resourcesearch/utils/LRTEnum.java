package net.unicon.lti.model.resourcesearch.utils;

import com.fasterxml.jackson.annotation.JsonValue;

public enum LRTEnum {
    ASSESSMENT_ITEM("Assessment/Item"),
    ASSESSMENT_FORMATIVE("Assessment/Formative"),
    ASSESSMENT_INTERIM("Assessment/Interim"),
    ASSESSMENT_RUBRIC("Assessment/Rubric"),
    ASSESSMENT_PREPARATION("Assessment/Preparation"),
    COLLECTION_COURSE("Collection/Course"),
    COLLECTION_UNIT("Collection/Unit"),
    COLLECTION_CURRICULUM_GUIDE("Collection/Curriculum Guide"),
    COLLECTION_LESSON("Collection/Lesson"),
    GAME("Game"),
    INTERACTIVE_SIMULATION("Interactive/Simulation"),
    INTERACTIVE_ANIMATION("Interactive/Animation"),
    INTERACTIVE_WHITEBOARD("Interactive/Whiteboard"),
    ACTIVITY_WORKSHEET("Activity/Worksheet"),
    ACTIVITY_LEARNING("Activity/Learning"),
    ACTIVITY_EXPERIMENT("Activity/Experiment"),
    LECTURE("Lecture"),
    TEXT_BOOK("Text/Book"),
    TEXT_CHAPTER("Text/Chapter"),
    TEXT_DOCUMENT("Text/Document"),
    TEXT_ARTICLE("Text/Article"),
    TEXT_PASSAGE("Text/Passage"),
    TEXT_TEXTBOOK("Text/Textbook"),
    TEXT_REFERENCE("Text/Reference"),
    TEXT_WEBSITE("Text/Website"),
    MEDIA_AUDIO("Media/Audio"),
    MEDIA_IMAGES_VISUALS("Media/Images/Visuals"),
    MEDIA_VIDEO("Media/Video"),
    OTHER("Other");

    private final String name;

    LRTEnum(final String name) {
        this.name = name;
    }

    @Override
    @JsonValue
    public String toString() {
        return name;
    }
}
