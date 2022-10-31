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
import java.util.Map;

@Data
@Entity
@Table(name = "csm_set")
public class CSMSet { // Curriculum Standards Metadata Set
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private long id;

    private String resourceLabel;

    private String resourcePartId;

    @ElementCollection
    @CollectionTable(name = "csm_set_csm", joinColumns = @JoinColumn(name = "csm_set_id"))
    private List<CurriculumStandardsMetadata> curriculumStandardsMetadata;
}
