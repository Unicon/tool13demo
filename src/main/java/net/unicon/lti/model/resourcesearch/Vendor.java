package net.unicon.lti.model.resourcesearch;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Set;

@Data
@Entity
@Table(name = "vendor")
public class Vendor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private long id;

    private String code;
    private String name;
    private String description;
    private String url;
    private String emailContact;

    // properties for database
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "vendor")
    @JsonIgnore
    private Set<CCLTILinkEntity> ccltiLinkEntitySet;
}
