/*
   Copyright (C) 2005-2012, by the President and Fellows of Harvard College.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   Dataverse Network - A web application to share, preserve and analyze research data.
   Developed at the Institute for Quantitative Social Science, Harvard University.
   Version 3.0.
*/

package edu.harvard.iq.dataverse.dataset;

import java.io.Serializable;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import jakarta.persistence.*;

/**
 *
 * Describes a relationship between two datasets.
 *
 * @author Vera Clemens
 *
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "relation_source", discriminatorType = DiscriminatorType.STRING)
@Table(indexes = {
        @Index(name="index_datasetrelation_dataset", columnList="dataset_id"),
        @Index(name="index_datasetrelation_relateddataset", columnList="relateddataset_id"),
        @Index(name="index_datasetrelation_definitionpoint", columnList="definitionpoint_id"),
        @Index(name="index_datasetrelation_relateddataset_definitionpoint", columnList="relateddataset_id, definitionpoint_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "datasetrelation_internal_unique",
                        columnNames = {"dataset_id", "relateddataset_id", "relationtype_id", "definitionpoint_id"}
                ),
                @UniqueConstraint(
                        name = "datasetrelation_external_unique",
                        columnNames = {"dataset_id", "externalidentifier", "relationtype_id", "definitionpoint_id"}
                )
        }
)
@NamedQueries({
        @NamedQuery(name = "DatasetRelation.removeRelationsByDatasetVersionId",
                query = "DELETE FROM DatasetRelation rel WHERE rel.definitionPoint.id=:versionId"),
        @NamedQuery(name = "DatasetRelation.getRelationsDefinedAtDatasetVersionId",
                query="SELECT rel FROM DatasetRelation rel WHERE rel.definitionPoint.id=:versionId")
})
public abstract class DatasetRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(nullable=false)
    private Dataset dataset;

    @ManyToOne
    @JoinColumn(nullable=false)
    private DatasetVersion definitionPoint;

    @ManyToOne
    @JoinColumn()
    private DatasetRelationType relationType;

    /**
     * JPA no-args constructor. Client code should use the public constructor
     * and not this one.
     */
    protected DatasetRelation(){}

    protected DatasetRelation(Dataset dataset, DatasetRelationType relationType, DatasetVersion definitionPoint) {
        this.dataset = dataset;
        this.relationType = relationType;
        this.definitionPoint = definitionPoint;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public DatasetVersion getDefinitionPoint() {
        return definitionPoint;
    }

    public void setDefinitionPoint(DatasetVersion definitionPoint) {
        this.definitionPoint = definitionPoint;
    }

    public DatasetRelationType getRelationType() {
        return relationType;
    }

    public void setRelationType(DatasetRelationType type) {
        this.relationType = type;
    }

    @Override
    public int hashCode() {
        return (id != null ? id.hashCode() : 0);
    }

    @Override
    public boolean equals(Object object) {
        if ( object == null ) return false;
        if ( object == this ) return true;

        if (!(object instanceof DatasetRelation)) {
            return false;
        }
        DatasetRelation other = (DatasetRelation) object;

        return (id==null && other.id==null) || (id!=null && id.equals(other.getId()));
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.dataset.DatasetRelation[ id=" + id + " ]";
    }

    public abstract String toKey();

    public abstract DatasetRelation copy(DatasetVersion newDefinitionPoint);

}
