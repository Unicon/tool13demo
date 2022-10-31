package net.unicon.lti.model.resourcesearch;

import lombok.Data;
import org.checkerframework.checker.units.qual.C;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.Map;

@Data
@Entity
@Table(name = "platform_property_set")
public class PlatformPropertySet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private long id;

    @Column(name = "platform")
    private String platform;

    @ElementCollection
    @CollectionTable(name = "platform_property_set_properties", joinColumns = @JoinColumn(name = "platform_property_set_id"))
    @Column(name = "properties")
    private Map<String, String> properties;

    @OneToOne(mappedBy = "extensions")
    private CCLTILinkEntity ccltiLinkEntity;
}
