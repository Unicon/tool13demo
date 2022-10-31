/**
 * Copyright 2021 Unicon (R)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.unicon.lti.model.resourcesearch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.unicon.lti.model.BaseEntity;
import net.unicon.lti.model.resourcesearch.utils.AccessModeEnum;
import net.unicon.lti.model.resourcesearch.utils.AccessibilityAPIEnum;
import net.unicon.lti.model.resourcesearch.utils.AccessibilityInputEnum;
import net.unicon.lti.model.resourcesearch.utils.EducationalAudienceEnum;
import net.unicon.lti.model.resourcesearch.utils.HazardEnum;
import net.unicon.lti.model.resourcesearch.utils.LRTEnum;
import net.unicon.lti.model.resourcesearch.utils.RatingEnum;
import net.unicon.lti.utils.resourcesearch.RsResourceSubjectsDeserializationConverter;
import net.unicon.lti.utils.resourcesearch.RsResourceSubjectsSerializationConverter;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Duration;
import java.util.HashSet;
import java.util.Date;
import java.util.Set;

@Getter
@Setter
@RequiredArgsConstructor
@Entity
@Table(name = "rs_resource")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(value = {"createdAt", "updatedAt", "version"})
public class RsResourceEntity extends BaseEntity {
    // Data model taken from here: https://www.imsglobal.org/sites/default/files/spec/lti-rs/v1p0/rest_binding/rsservicev1p0_restbindv1p0.html#TabUMLJSONMap_CoreClass_Resource

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private long id;

    @Basic
    @Column(name = "name", length = 1024)
    private String name;

    @Basic
    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "publisher", length = 2048)
    private String publisher;

    @ManyToMany(fetch = FetchType.EAGER,
            cascade = {
                    CascadeType.PERSIST,
                    CascadeType.MERGE
            })
    @JoinTable(name = "resource_subjects",
            joinColumns = @JoinColumn(name = "resource_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "subject_identifier", referencedColumnName = "identifier"))
    @JsonSerialize(converter = RsResourceSubjectsSerializationConverter.class)
    @JsonDeserialize(converter = RsResourceSubjectsDeserializationConverter.class)
    private Set<RsSubjectEntity> subject = new HashSet<>();

    @Basic
    @Column(name = "url")
    private String url;

    @ManyToOne(targetEntity = CCLTILinkEntity.class, fetch = FetchType.EAGER)
    @JoinColumn(name = "lti_link")
    private CCLTILinkEntity ltiLink;

    @Enumerated(EnumType.STRING)
    @ElementCollection
    @CollectionTable(name = "resource_learning_resource_types", joinColumns = @JoinColumn(name = "resource_id"))
    @Column(name = "learning_resource_type", nullable = false)
    private Set<LRTEnum> learningResourceType = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "resource_languages", joinColumns = @JoinColumn(name = "resource_id"))
    @Column(name = "language", nullable = false)
    private Set<String> language = new HashSet<>();

    @Basic
    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Basic
    @Column(name = "typical_age_range")
    private String typicalAgeRange;

    @ElementCollection
    @CollectionTable(name = "resource_text_complexities", joinColumns = @JoinColumn(name = "resource_id"))
    @Column(name = "text_complexities", nullable = false)
    private Set<TextComplexity> textComplexity = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "resource_learning_objectives", joinColumns = @JoinColumn(name = "resource_id"))
    @Column(name = "learning_objectives", nullable = false)
    private Set<LearningObjectives> learningObjectives = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "resource_authors", joinColumns = @JoinColumn(name = "resource_id"))
    @Column(name = "author", nullable = false)
    private Set<String> author = new HashSet<>();

    @Basic
    @Column(name = "use_rights_url")
    private String useRightsURL;

    @Basic
    @Column(name = "time_required")
    private Duration timeRequired;

    @Basic
    @Column(name = "technical_format")
    private String technicalFormat;

    @Enumerated(EnumType.STRING)
    @ElementCollection
    @Column(name = "educational_audience", nullable = false)
    @CollectionTable(name = "resource_educational_audiences", joinColumns = @JoinColumn(name = "resource_id"))
    private Set<EducationalAudienceEnum> educationalAudience = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @ElementCollection
    @CollectionTable(name = "resource_accessibility_apis", joinColumns = @JoinColumn(name = "resource_id"))
    @Column(name = "accessibility_api", nullable = false)
    private Set<AccessibilityAPIEnum> accessibilityAPI = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @ElementCollection
    @CollectionTable(name = "resource_accessibility_input_methods", joinColumns = @JoinColumn(name = "resource_id"))
    @Column(name = "accessibility_input_methods", nullable = false)
    private Set<AccessibilityInputEnum> accessibilityInputMethods = new HashSet<>();

    @ElementCollection
    @Column(name = "accessibility_features", nullable = false)
    @CollectionTable(name = "resource_accessibility_features", joinColumns = @JoinColumn(name = "resource_id"))
    private Set<String> accessibilityFeatures = new HashSet<>();

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @Column(name = "accessibility_hazards", nullable = false)
    @CollectionTable(name = "resource_accessibility_hazards", joinColumns = @JoinColumn(name = "resource_id"))
    private Set<HazardEnum> accessibilityHazards = new HashSet<>();

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @Column(name = "access_mode", nullable = false)
    @CollectionTable(name = "resource_access_modes", joinColumns = @JoinColumn(name = "resource_id"))
    private Set<AccessModeEnum> accessMode = new HashSet<>();

    @Basic
    @Column(name = "publish_date")
    private Date publishDate;

    @Basic
    @Enumerated(EnumType.STRING)
    @Column(name = "rating")
    private RatingEnum rating;

    @Basic
    @Column(name = "relevance")
    private Float relevance;

    @ElementCollection
    @Column(name = "extensions", nullable = false)
    @CollectionTable(name = "resource_extensions", joinColumns = @JoinColumn(name = "resource_id"))
    private Set<String> extensions = new HashSet<>();
}
