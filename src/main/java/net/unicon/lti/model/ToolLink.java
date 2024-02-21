package net.unicon.lti.model;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "tool_link")
public class ToolLink extends BaseEntity {

    @Id
    @Getter
    @Column(name = "tool_link_id", length = 256, nullable = false)
    private String toolLinkId;
    @Getter
    @Setter
    @Basic
    @Column(name = "title", length = 4096, nullable = false)
    private String title;
    @Getter
    @Setter
    @Basic
    @Column(name = "description")
    private String description;
    @Getter
    @Setter
    @Basic
    @Column(name = "is_assignment")
    private Boolean isAssignment;
    @Getter
    @Setter
    @Basic
    @Column(name = "max_grade")
    private Float maxGrade;
    @OneToMany(mappedBy = "toolLink", fetch = FetchType.LAZY)
    private Set<LtiLinkEntity> links;


    protected ToolLink(){
    }
    public ToolLink(String toolLinkId, String title, String description, Boolean isAssignment, Float maxGrade) {
        this.toolLinkId = toolLinkId;
        this.title = title;
        this.description = description;
        this.isAssignment = isAssignment;
        this.maxGrade = maxGrade;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ToolLink that = (ToolLink) o;

        if (toolLinkId != that.toolLinkId) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result =  toolLinkId.hashCode();
        result = 31 * result + (this.getTitle().hashCode());
        return result;
    }

}
