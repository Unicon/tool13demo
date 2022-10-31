package net.unicon.lti.model.resourcesearch;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import net.unicon.lti.model.BaseEntity;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@RequiredArgsConstructor
@Entity
@Table(name = "rs_cc_lti_link")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(value = {"createdAt", "updatedAt", "version"})
public class CCLTILinkEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private long id;

    // inherited LTILink properties
    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @ElementCollection
    @CollectionTable(name = "cc_lti_link_custom", joinColumns = @JoinColumn(name = "rs_cc_lti_link_id"))
    private List<Property> custom;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "extension_id", referencedColumnName = "id")
    private PlatformPropertySet extensions;

    @Column(name = "launch_url")
    private String launchUrl;

    @Column(name = "secure_launch_url")
    private String secureLaunchUrl;

    @Column(name = "icon")
    private String icon;

    @Column(name = "secure_icon")
    private String secureIcon;

    @ManyToOne
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    // CCLTILink properties
    @ElementCollection
    @CollectionTable(name = "cc_lti_link_cartridge_bundles", joinColumns = @JoinColumn(name = "rs_cc_lti_link_id"))
    @Column(name = "cartridge_bundle")
    private Map<String, String> cartridgeBundle;

    @ElementCollection
    @CollectionTable(name = "cc_lti_link_cartridge_icons", joinColumns = @JoinColumn(name = "rs_cc_lti_link_id"))
    @Column(name = "cartridge_icon")
    private Map<String, String> cartridgeIcon;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "metadata_id", referencedColumnName = "id")
    private CSMSet metadata;

    // properties for database
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "ltiLink")
    @JsonIgnore
    private Set<RsResourceEntity> rsResourceEntities;
}
