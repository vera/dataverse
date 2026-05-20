package edu.harvard.iq.dataverse.dataset;

import jakarta.persistence.*;

import java.io.Serializable;

@Entity
@NamedQueries({
        @NamedQuery(name = "DatasetRelationType.getByName",
                query="SELECT drt FROM DatasetRelationType drt WHERE drt.name=:name"),
})
public class DatasetRelationType implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true)
    private String name;

    @Column(nullable=false, unique=true)
    private String displayName;

    @Column
    private String description;

    @OneToOne(cascade = {CascadeType.PERSIST})
    @JoinColumn(unique=true)
    private DatasetRelationType inverse;


    /**
     * Constructing a relation type with an inverse relation type.
     * @param name The name of the relation type.
     * @param displayName The display name of the relation type.
     * @param description The description of the relation type.
     * @param inverse The inverse relation type.
     */
    public DatasetRelationType(String name, String displayName, String description, DatasetRelationType inverse) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.inverse = inverse;
        if (inverse.getInverse() != this) {
            inverse.setInverse(this);
        }
    }

    /**
     * Constructing a relation type.
     * @param name The name of the relation type.
     * @param displayName The display name of the relation type.
     * @param description The description of the relation type.
     */
    public DatasetRelationType(String name, String displayName, String description) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Constructing a relation type.
     * @param name The name of the relation type.
     * @param displayName The display name of the relation type.
     */
    public DatasetRelationType(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }

    /**
     * JPA no-args constructor. Client code should use the public constructor
     * and not this one.
     */
    protected DatasetRelationType(){}


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DatasetRelationType getInverse() {
        return inverse;
    }

    public void setInverse(DatasetRelationType inverse) {
        this.inverse = inverse;
        if (inverse.getInverse() != this) {
            inverse.setInverse(this);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
