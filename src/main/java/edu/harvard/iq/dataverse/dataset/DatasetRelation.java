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
        @Index(name="index_datasetrelation_relateddataset", columnList="relateddataset_id")
        }
)
@NamedQueries({
        @NamedQuery(name = "DatasetRelation.removeRelationsByDatasetVersionId",
                query = "DELETE FROM DatasetRelation rel WHERE rel.definitionPoint.id=:versionId"),
        @NamedQuery(name = "DatasetRelation.getRelationsDefinedAtDatasetVersionId",
                query="SELECT rel FROM DatasetRelation rel WHERE rel.definitionPoint.id=:versionId")
})
@NamedNativeQuery(
        name= "DatasetRelation.getUniqueRelationsByDatasetId",
        query="""
            SELECT rel.* FROM datasetrelation rel
            WHERE (rel.dataset_id = ?1 OR (rel.relation_source = 'internal' AND rel.relateddataset_id = ?1))
                    AND rel.definitionpoint_id = (
                        SELECT dv.id FROM datasetversion dv
                        WHERE dv.dataset_id = (SELECT dv2.dataset_id FROM datasetversion dv2 WHERE dv2.id = rel.definitionpoint_id)
                        AND dv.id = (SELECT MAX(dv2.id) FROM datasetversion dv2 WHERE dv2.dataset_id = dv.dataset_id AND dv2.versionstate = 'RELEASED')
                        AND dv.versionstate = 'RELEASED'
                    )
                    AND rel.id = (
                        SELECT MIN(r2.id)
                        FROM datasetrelation r2
                        WHERE r2.dataset_id = rel.dataset_id
                          AND ((rel.relationtype_id IS NULL AND r2.relationtype_id IS NULL) OR r2.relationtype_id = rel.relationtype_id)
                          AND (
                            (rel.relation_source = 'internal' AND r2.relation_source = 'internal' AND r2.relateddataset_id = rel.relateddataset_id)
                            OR
                            (rel.relation_source = 'external' AND r2.relation_source = 'external' AND r2.externalidentifier = rel.externalidentifier AND r2.identifierscheme = rel.identifierscheme)
                          )
                    )
            """,
        resultClass = DatasetRelation.class
)
@NamedNativeQuery(
        name= "DatasetRelation.getUniqueRelationsByDatasetIdAndVersion",
        query="""
            SELECT rel.* FROM datasetrelation rel
            JOIN datasetversion dv_def ON rel.definitionpoint_id = dv_def.id
            WHERE (
                    rel.definitionpoint_id = ?2 
                    OR (
                        (rel.dataset_id = ?1 OR (rel.relation_source = 'internal' AND rel.relateddataset_id = ?1)) 
                        AND dv_def.dataset_id != ?1
                        AND rel.definitionpoint_id = (
                            SELECT dv.id FROM datasetversion dv
                            WHERE dv.dataset_id = dv_def.dataset_id
                            AND dv.id = (SELECT MAX(dv2.id) FROM datasetversion dv2 WHERE dv2.dataset_id = dv.dataset_id AND dv2.versionstate = 'RELEASED')
                        )
                    )
                  )
                    AND rel.id = (
                        SELECT MIN(r2.id)
                        FROM datasetrelation r2
                        WHERE r2.dataset_id = rel.dataset_id
                          AND ((rel.relationtype_id IS NULL AND r2.relationtype_id IS NULL) OR r2.relationtype_id = rel.relationtype_id)
                          AND (
                            (rel.relation_source = 'internal' AND r2.relation_source = 'internal' AND r2.relateddataset_id = rel.relateddataset_id)
                            OR
                            (rel.relation_source = 'external' AND r2.relation_source = 'external' AND r2.externalidentifier = rel.externalidentifier AND r2.identifierscheme = rel.identifierscheme)
                          )
                    )
            """,
        resultClass = DatasetRelation.class
)
@NamedNativeQuery(
        name= "DatasetRelation.getUniqueRelationsByDatasetIdAndType",
        query="""
            SELECT rel.* FROM datasetrelation rel
            JOIN datasetrelationtype rt ON rel.relationtype_id = rt.id
            LEFT JOIN datasetrelationtype inv ON rt.inverse_id = inv.id
            WHERE (
                    (rel.dataset_id = ?1 AND rt.name = ?2)
                    OR (rel.relation_source = 'internal' AND rel.relateddataset_id = ?1 AND inv.name = ?2)
                  )
                    AND rel.definitionpoint_id = (
                        SELECT dv.id FROM datasetversion dv
                        WHERE dv.dataset_id = (SELECT dv2.dataset_id FROM datasetversion dv2 WHERE dv2.id = rel.definitionpoint_id)
                        AND dv.id = (SELECT MAX(dv2.id) FROM datasetversion dv2 WHERE dv2.dataset_id = dv.dataset_id AND dv2.versionstate = 'RELEASED')
                        AND dv.versionstate = 'RELEASED'
                    )
                    AND rel.id = (
                        SELECT MIN(r2.id)
                        FROM datasetrelation r2
                        WHERE r2.dataset_id = rel.dataset_id
                          AND ((rel.relationtype_id IS NULL AND r2.relationtype_id IS NULL) OR r2.relationtype_id = rel.relationtype_id)
                          AND (
                            (rel.relation_source = 'internal' AND r2.relation_source = 'internal' AND r2.relateddataset_id = rel.relateddataset_id)
                            OR
                            (rel.relation_source = 'external' AND r2.relation_source = 'external' AND r2.externalidentifier = rel.externalidentifier AND r2.identifierscheme = rel.identifierscheme)
                          )
                    )
            """,
        resultClass = DatasetRelation.class
)
@NamedNativeQuery(
        name= "DatasetRelation.getUniqueRelationsByDatasetIdAndVersionAndType",
        query="""
            SELECT rel.* FROM datasetrelation rel
            JOIN datasetversion dv_def ON rel.definitionpoint_id = dv_def.id
            JOIN datasetrelationtype rt ON rel.relationtype_id = rt.id
            LEFT JOIN datasetrelationtype inv ON rt.inverse_id = inv.id
            WHERE (
                    (rel.definitionpoint_id = ?2 AND rel.dataset_id = ?1 AND rt.name = ?3)
                    OR (rel.definitionpoint_id = ?2 AND rel.relation_source = 'internal' AND rel.relateddataset_id = ?1 AND inv.name = ?3)
                    OR (
                        dv_def.dataset_id != ?1 
                        AND rel.dataset_id = ?1 AND rt.name = ?3
                        AND rel.definitionpoint_id = (
                            SELECT dv.id FROM datasetversion dv
                            WHERE dv.dataset_id = dv_def.dataset_id
                            AND dv.id = (SELECT MAX(dv2.id) FROM datasetversion dv2 WHERE dv2.dataset_id = dv.dataset_id AND dv2.versionstate = 'RELEASED')
                        )
                    )
                    OR (
                        dv_def.dataset_id != ?1 
                        AND (rel.relation_source = 'internal' AND rel.relateddataset_id = ?1 AND inv.name = ?3)
                        AND rel.definitionpoint_id = (
                            SELECT dv.id FROM datasetversion dv
                            WHERE dv.dataset_id = dv_def.dataset_id
                            AND dv.id = (SELECT MAX(dv2.id) FROM datasetversion dv2 WHERE dv2.dataset_id = dv.dataset_id AND dv2.versionstate = 'RELEASED')
                        )
                    )
                  )
                    AND rel.id = (
                        SELECT MIN(r2.id)
                        FROM datasetrelation r2
                        WHERE r2.dataset_id = rel.dataset_id
                          AND ((rel.relationtype_id IS NULL AND r2.relationtype_id IS NULL) OR r2.relationtype_id = rel.relationtype_id)
                          AND (
                            (rel.relation_source = 'internal' AND r2.relation_source = 'internal' AND r2.relateddataset_id = rel.relateddataset_id)
                            OR
                            (rel.relation_source = 'external' AND r2.relation_source = 'external' AND r2.externalidentifier = rel.externalidentifier AND r2.identifierscheme = rel.identifierscheme)
                          )
                    )
            """,
        resultClass = DatasetRelation.class
)
@NamedNativeQuery(
        name= "DatasetRelation.getTotalCountByDatasetId",
        query= """
            SELECT COUNT(DISTINCT 
                CASE 
                    WHEN dr.relation_source = 'internal' THEN 
                        CASE WHEN dr.dataset_id = ?1 THEN CAST(dr.relateddataset_id AS VARCHAR) ELSE CAST(dr.dataset_id AS VARCHAR) END
                    ELSE dr.externalidentifier
                END)
            FROM datasetrelation dr
            WHERE (dr.dataset_id = ?1 OR dr.relateddataset_id = ?1)
              AND dr.definitionpoint_id = (
                  SELECT dv.id FROM datasetversion dv
                  WHERE dv.dataset_id = (SELECT dv2.dataset_id FROM datasetversion dv2 WHERE dv2.id = dr.definitionpoint_id)
                  AND dv.id = (SELECT MAX(dv3.id) FROM datasetversion dv3 WHERE dv3.dataset_id = dv.dataset_id AND dv3.versionstate = 'RELEASED')
              )
        """
)
@NamedNativeQuery(
        name= "DatasetRelation.getTotalCountByDatasetIdAndVersion",
        query= """
            SELECT COUNT(DISTINCT 
                CASE 
                    WHEN dr.relation_source = 'internal' THEN 
                        CASE WHEN dr.dataset_id = ?1 THEN CAST(dr.relateddataset_id AS VARCHAR) ELSE CAST(dr.dataset_id AS VARCHAR) END
                    ELSE dr.externalidentifier
                END)
            FROM datasetrelation dr
            JOIN datasetversion dv_def ON dr.definitionpoint_id = dv_def.id
            WHERE dr.definitionpoint_id = ?2 
               OR (
                   ((dr.dataset_id = ?1 OR dr.relateddataset_id = ?1) AND dv_def.dataset_id != ?1)
                   AND dr.definitionpoint_id = (
                       SELECT dv.id FROM datasetversion dv
                       WHERE dv.dataset_id = dv_def.dataset_id
                       AND dv.id = (SELECT MAX(dv3.id) FROM datasetversion dv3 WHERE dv3.dataset_id = dv.dataset_id AND dv3.versionstate = 'RELEASED')
                   )
               )
        """
)
@NamedNativeQuery(
        name= "DatasetRelation.getTotalCountByDatasetIdAndVersionAndType",
        query= """
            SELECT COUNT(DISTINCT 
                CASE 
                    WHEN rel.relation_source = 'internal' THEN 
                        CASE WHEN rel.dataset_id = ?1 THEN CAST(rel.relateddataset_id AS VARCHAR) ELSE CAST(rel.dataset_id AS VARCHAR) END
                    ELSE rel.externalidentifier
                END)
            FROM datasetrelation rel
            JOIN datasetversion dv_def ON rel.definitionpoint_id = dv_def.id
            JOIN datasetrelationtype rt ON rel.relationtype_id = rt.id
            LEFT JOIN datasetrelationtype inv ON rt.inverse_id = inv.id
            WHERE (
                    (rel.definitionpoint_id = ?2 AND rel.dataset_id = ?1 AND rt.name = ?3)
                    OR (rel.definitionpoint_id = ?2 AND rel.relation_source = 'internal' AND rel.relateddataset_id = ?1 AND inv.name = ?3)
                    OR (
                        dv_def.dataset_id != ?1 
                        AND rel.dataset_id = ?1 AND rt.name = ?3
                        AND rel.definitionpoint_id = (
                            SELECT dv.id FROM datasetversion dv
                            WHERE dv.dataset_id = dv_def.dataset_id
                            AND dv.id = (SELECT MAX(dv2.id) FROM datasetversion dv2 WHERE dv2.dataset_id = dv.dataset_id AND dv2.versionstate = 'RELEASED')
                        )
                    )
                    OR (
                        dv_def.dataset_id != ?1 
                        AND (rel.relation_source = 'internal' AND rel.relateddataset_id = ?1 AND inv.name = ?3)
                        AND rel.definitionpoint_id = (
                            SELECT dv.id FROM datasetversion dv
                            WHERE dv.dataset_id = dv_def.dataset_id
                            AND dv.id = (SELECT MAX(dv2.id) FROM datasetversion dv2 WHERE dv2.dataset_id = dv.dataset_id AND dv2.versionstate = 'RELEASED')
                        )
                    )
                  )
        """
)
@NamedNativeQuery(
        name= "DatasetRelation.getTotalCountByDatasetIdAndType",
        query= """
            SELECT COUNT(DISTINCT 
                CASE 
                    WHEN rel.relation_source = 'internal' THEN 
                        CASE WHEN rel.dataset_id = ?1 THEN CAST(rel.relateddataset_id AS VARCHAR) ELSE CAST(rel.dataset_id AS VARCHAR) END
                    ELSE rel.externalidentifier
                END)
            FROM datasetrelation rel
            JOIN datasetrelationtype rt ON rel.relationtype_id = rt.id
            LEFT JOIN datasetrelationtype inv ON rt.inverse_id = inv.id
            WHERE (
                   (rel.dataset_id = ?1 AND rt.name = ?2)
                   OR (rel.relation_source = 'internal' AND rel.relateddataset_id = ?1 AND inv.name = ?2)
                 )
                   AND rel.definitionpoint_id = (
                       SELECT dv.id FROM datasetversion dv
                       WHERE dv.dataset_id = (SELECT dv2.dataset_id FROM datasetversion dv2 WHERE dv2.id = rel.definitionpoint_id)
                       AND dv.id = (SELECT MAX(dv2.id) FROM datasetversion dv2 WHERE dv2.dataset_id = dv.dataset_id AND dv2.versionstate = 'RELEASED')
                   )
        """
)
@NamedNativeQuery(
        name = "DatasetRelation.getRelationCountsByDatasetId",
        query = """
            SELECT
                relation_type_name,
                COUNT(*) AS related_datasets_count
            FROM (
                SELECT
                    CASE
                        WHEN dr.dataset_id = ?1 THEN rt.name
                        ELSE inv.name
                    END AS relation_type_name
                FROM datasetrelation dr
                JOIN datasetrelationtype rt ON dr.relationtype_id = rt.id
                JOIN datasetrelationtype inv ON rt.inverse_id = inv.id
                WHERE (dr.dataset_id = ?1 OR dr.relateddataset_id = ?1)
                    AND dr.definitionpoint_id = (
                        SELECT dv.id FROM datasetversion dv
                        WHERE dv.dataset_id = (SELECT dv2.dataset_id FROM datasetversion dv2 WHERE dv2.id = dr.definitionpoint_id)
                        AND dv.id = (SELECT MAX(dv3.id) FROM datasetversion dv3 WHERE dv3.dataset_id = dv.dataset_id AND dv3.versionstate = 'RELEASED')
                    )
                    AND dr.id = (
                                    SELECT MIN(dr2.id)
                                    FROM datasetrelation dr2
                                    WHERE dr2.dataset_id = dr.dataset_id
                                      AND (
                                        (dr2.relation_source = 'internal' AND dr.relation_source = 'internal' AND dr2.relateddataset_id = dr.relateddataset_id)
                                        OR (dr2.relation_source = 'external' AND dr.relation_source = 'external' AND dr2.externalidentifier = dr.externalidentifier AND dr2.identifierscheme = dr.identifierscheme)
                                      )
                                      AND dr2.relationtype_id = dr.relationtype_id
                                )
            ) t
            GROUP BY relation_type_name
            ORDER BY relation_type_name;
    """,
    resultSetMapping = "RelationCountMapping"
)
@NamedNativeQuery(
    name = "DatasetRelation.getRelationCountsByDatasetIdAndVersion",
    query = """
            SELECT
                relation_type_name,
                COUNT(*) AS related_datasets_count
            FROM (
                SELECT
                    CASE
                        WHEN dr.dataset_id = ?1 THEN rt.name
                        ELSE inv.name
                    END AS relation_type_name
                FROM datasetrelation dr
                JOIN datasetrelationtype rt ON dr.relationtype_id = rt.id
                JOIN datasetrelationtype inv ON rt.inverse_id = inv.id
                JOIN datasetversion dv_def ON dr.definitionpoint_id = dv_def.id
                WHERE (
                       dr.definitionpoint_id = ?2
                       OR (
                           ((dr.dataset_id = ?1 OR dr.relateddataset_id = ?1) AND dv_def.dataset_id != ?1)
                           AND dr.definitionpoint_id = (
                               SELECT dv.id FROM datasetversion dv
                               WHERE dv.dataset_id = dv_def.dataset_id
                               AND dv.id = (SELECT MAX(dv3.id) FROM datasetversion dv3 WHERE dv3.dataset_id = dv.dataset_id AND dv3.versionstate = 'RELEASED')
                           )
                       )
                ) 
                    AND dr.id = (
                                    SELECT MIN(dr2.id)
                                    FROM datasetrelation dr2
                                    WHERE dr2.dataset_id = dr.dataset_id
                                      AND (
                                        (dr2.relation_source = 'internal' AND dr.relation_source = 'internal' AND dr2.relateddataset_id = dr.relateddataset_id)
                                        OR (dr2.relation_source = 'external' AND dr.relation_source = 'external' AND dr2.externalidentifier = dr.externalidentifier AND dr2.identifierscheme = dr.identifierscheme)
                                      )
                                      AND dr2.relationtype_id = dr.relationtype_id
                                )
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
