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
import jakarta.persistence.*;

/**
 *
 * Describes a relationship between two datasets.
 *
 * @author Vera Clemens
 *
 */
@Entity
@Table(indexes = {
        @Index(name="index_datasetrelation_dataset", columnList="dataset_id"),
        @Index(name="index_datasetrelation_relateddataset", columnList="relateddataset_id")
        },
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"dataset_id", "relateddataset_id", "relationtype_id", "definitionpoint_id"}
        )
)
@NamedQueries({
        @NamedQuery(name = "DatasetRelation.getUniqueRelationsByDatasetId",
                query="""
                    SELECT rel FROM DatasetRelation rel
                    WHERE (rel.dataset.id=:datasetId OR rel.relatedDataset.id=:datasetId)
                            AND rel.id = (
                                SELECT MIN(r2.id)
                                FROM DatasetRelation r2
                                WHERE r2.dataset.id = rel.dataset.id
                                  AND r2.relatedDataset.id = rel.relatedDataset.id
                                  AND r2.relationType.id = rel.relationType.id
                            )
                    """),
        @NamedQuery(name = "DatasetRelation.getUniqueRelationsByDatasetIdAndType",
                query="""
                    SELECT rel FROM DatasetRelation rel
                    WHERE ((rel.dataset.id=:datasetId AND rel.relationType.name=:relationType)
                            OR (rel.relatedDataset.id=:datasetId AND rel.relationType.inverse.name=:relationType))
                                AND rel.id = (
                                    SELECT MIN(r2.id)
                                    FROM DatasetRelation r2
                                    WHERE r2.dataset.id = rel.dataset.id
                                      AND r2.relatedDataset.id = rel.relatedDataset.id
                                      AND r2.relationType.id = rel.relationType.id
                                )
                    """),
        @NamedQuery(name = "DatasetRelation.removeRelationsByDatasetId",
                query = "DELETE FROM DatasetRelation rel WHERE rel.definitionPoint.id=:datasetId"),
})
@NamedNativeQuery(
        name= "DatasetRelation.getTotalCountByDatasetId",
        query= """
            SELECT COUNT(DISTINCT CASE WHEN dr.dataset_id = ?1 THEN dr.relateddataset_id ELSE dr.dataset_id END)
            FROM datasetrelation dr
            WHERE dr.dataset_id = ?1 OR dr.relateddataset_id = ?1
        """
)
@NamedNativeQuery(
        name = "DatasetRelation.getRelationCountsByDatasetId",
        query = """
            SELECT
                relation_type_name,
                COUNT(*) AS related_datasets_count
            FROM (
                SELECT DISTINCT
                    CASE
                        WHEN dr.dataset_id = ?1 THEN rt.name
                        ELSE inv.name
                    END AS relation_type_name
                FROM datasetrelation dr
                JOIN datasetrelationtype rt ON dr.relationtype_id = rt.id
                JOIN datasetrelationtype inv ON rt.inverse_id = inv.id
                WHERE dr.dataset_id = ?1
                   OR dr.relateddataset_id = ?1
            ) t
            GROUP BY relation_type_name
            ORDER BY relation_type_name;
    """,
    resultSetMapping = "RelationCountMapping"
)
@SqlResultSetMapping(
        name = "RelationCountMapping",
        columns = {
                @ColumnResult(name = "relation_type_name", type = String.class),
                @ColumnResult(name = "related_datasets_count", type = Long.class)
        }
)
public class DatasetRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(nullable=false)
    private Dataset dataset;

    @ManyToOne
    @JoinColumn(nullable=false)
    private Dataset relatedDataset;

    @ManyToOne
    @JoinColumn(nullable=false)
    private Dataset definitionPoint;

    @ManyToOne
    @JoinColumn()
    private DatasetRelationType relationType;

    /**
     * Constructing a dataset relation for the given datasets.
     * @param datasetA First dataset that is part of the relation.  Cannot be {@code null}.
     * @param datasetB Second dataset that is part of the relation.  Cannot be {@code null}.
     * @param relationType The type of the relation. Cannot be {@code null}.
     * @param definitionPoint Which dataset the relation has been defined on.  Cannot be {@code null}.
     * @throws IllegalArgumentException if any of the parameters are null. That's
     *         because JPA would throw an exception later anyway.
     */
    public DatasetRelation(Dataset datasetA, Dataset datasetB, DatasetRelationType relationType, Dataset definitionPoint) {
        if ( datasetA == null || datasetB == null ) throw new IllegalArgumentException("Cannot create a relation for a null dataset");
        if ( relationType == null ) throw new IllegalArgumentException("Cannot create a dataset relation with a null type");

        // We enforce canonical order to ensure uniqueness of relations
        if (datasetA.getId() < datasetB.getId()) {
            dataset = datasetA;
            relatedDataset = datasetB;
            this.relationType = relationType;
        } else {
            dataset = datasetB;
            relatedDataset = datasetA;
            this.relationType = relationType.getInverse();
        }

        this.definitionPoint = definitionPoint;
    }

    /**
     * JPA no-args constructor. Client code should use the public constructor
     * and not this one.
     */
    protected DatasetRelation(){}

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

    public Dataset getRelatedDataset() {
        return relatedDataset;
    }

    public void setRelatedDataset(Dataset relatedDataset) {
        this.relatedDataset = relatedDataset;
    }

    public Dataset getDefinitionPoint() {
        return definitionPoint;
    }

    public void setDefinitionPoint(Dataset definitionPoint) {
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

}
