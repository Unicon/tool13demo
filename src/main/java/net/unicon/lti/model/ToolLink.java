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
    @Column(length = 256, nullable = false)
    private String toolLinkId;
    @Getter
    @Setter
    @Basic
    @Column(length = 4096, nullable = false)
    private String title;
    @Getter
    @Setter
    @Basic
    private String description;
    @Getter
    @Setter
    @Basic
    private Boolean isAssignment;
    @Getter
    @Setter
    @Basic
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

        return Objects.equals(toolLinkId, that.toolLinkId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(toolLinkId);
    }

}
