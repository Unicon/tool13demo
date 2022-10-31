package net.unicon.lti.model.resourcesearch;

import lombok.Data;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import java.util.List;

@Data
@Entity
@Table(name = "csm")
public class CurriculumStandardsMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private long id;

    private String providerId;

    @ElementCollection
    @CollectionTable(name = "csm_set_of_guids", joinColumns = @JoinColumn(name = "csm_id"))
    private List<String> setOfGuids;
}
