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
                        SELECT MIN(rel2.id) FROM datasetrelation rel2
                        JOIN datasetversion dv_def2 ON rel2.definitionpoint_id = dv_def2.id
                        WHERE (rel2.dataset_id = ?1 OR (rel2.relation_source = 'internal' AND rel2.relateddataset_id = ?1))
                          AND rel2.definitionpoint_id = (
                              SELECT dv2.id FROM datasetversion dv2
                              WHERE dv2.dataset_id = dv_def2.dataset_id
                              AND dv2.id = (SELECT MAX(dv3.id) FROM datasetversion dv3 WHERE dv3.dataset_id = dv2.dataset_id AND dv3.versionstate = 'RELEASED')
                              AND dv2.versionstate = 'RELEASED'
                          )
                          AND (
                            CASE WHEN rel.dataset_id = ?1 THEN rel.relationtype_id ELSE (SELECT rt.inverse_id FROM datasetrelationtype rt WHERE rt.id = rel.relationtype_id) END
                            =
                            CASE WHEN rel2.dataset_id = ?1 THEN rel2.relationtype_id ELSE (SELECT rt2.inverse_id FROM datasetrelationtype rt2 WHERE rt2.id = rel2.relationtype_id) END
                          )
                          AND (
                            CASE WHEN rel.relation_source = 'internal' THEN 
                                CASE WHEN rel.dataset_id = ?1 THEN CAST(rel.relateddataset_id AS VARCHAR) ELSE CAST(rel.dataset_id AS VARCHAR) END
                            ELSE rel.externalidentifier END
                            =
                            CASE WHEN rel2.relation_source = 'internal' THEN 
                                CASE WHEN rel2.dataset_id = ?1 THEN CAST(rel2.relateddataset_id AS VARCHAR) ELSE CAST(rel2.dataset_id AS VARCHAR) END
                            ELSE rel2.externalidentifier END
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
                        SELECT MIN(rel2.id) FROM datasetrelation rel2
                        JOIN datasetversion dv_def2 ON rel2.definitionpoint_id = dv_def2.id
                        WHERE (
                            rel2.definitionpoint_id = ?2 
                            OR (
                                (rel2.dataset_id = ?1 OR (rel2.relation_source = 'internal' AND rel2.relateddataset_id = ?1)) 
                                AND dv_def2.dataset_id != ?1
                                AND rel2.definitionpoint_id = (
                                    SELECT dv2.id FROM datasetversion dv2
                                    WHERE dv2.dataset_id = dv_def2.dataset_id
                                    AND dv2.id = (SELECT MAX(dv3.id) FROM datasetversion dv3 WHERE dv3.dataset_id = dv2.dataset_id AND dv3.versionstate = 'RELEASED')
                                )
                            )
                          )
                          AND (
                            CASE WHEN rel.dataset_id = ?1 THEN rel.relationtype_id ELSE (SELECT rt.inverse_id FROM datasetrelationtype rt WHERE rt.id = rel.relationtype_id) END
                            =
                            CASE WHEN rel2.dataset_id = ?1 THEN rel2.relationtype_id ELSE (SELECT rt2.inverse_id FROM datasetrelationtype rt2 WHERE rt2.id = rel2.relationtype_id) END
                          )
                          AND (
                            CASE WHEN rel.relation_source = 'internal' THEN 
                                CASE WHEN rel.dataset_id = ?1 THEN CAST(rel.relateddataset_id AS VARCHAR) ELSE CAST(rel.dataset_id AS VARCHAR) END
                            ELSE rel.externalidentifier END
                            =
                            CASE WHEN rel2.relation_source = 'internal' THEN 
                                CASE WHEN rel2.dataset_id = ?1 THEN CAST(rel2.relateddataset_id AS VARCHAR) ELSE CAST(rel2.dataset_id AS VARCHAR) END
                            ELSE rel2.externalidentifier END
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
                        SELECT MIN(rel2.id) FROM datasetrelation rel2
                        JOIN datasetrelationtype rt2 ON rel2.relationtype_id = rt2.id
                        LEFT JOIN datasetrelationtype inv2 ON rt2.inverse_id = inv2.id
                        WHERE (
                                (rel2.dataset_id = ?1 AND rt2.name = ?2)
                                OR (rel2.relation_source = 'internal' AND rel2.relateddataset_id = ?1 AND inv2.name = ?2)
                              )
                          AND rel2.definitionpoint_id = (
                              SELECT dv2.id FROM datasetversion dv2
                              WHERE dv2.dataset_id = (SELECT dv3.dataset_id FROM datasetversion dv3 WHERE dv3.id = rel2.definitionpoint_id)
                              AND dv2.id = (SELECT MAX(dv4.id) FROM datasetversion dv4 WHERE dv4.dataset_id = dv2.dataset_id AND dv4.versionstate = 'RELEASED')
                              AND dv2.versionstate = 'RELEASED'
                          )
                          AND (
                            CASE WHEN rel.relation_source = 'internal' THEN 
                                CASE WHEN rel.dataset_id = ?1 THEN CAST(rel.relateddataset_id AS VARCHAR) ELSE CAST(rel.dataset_id AS VARCHAR) END
                            ELSE rel.externalidentifier END
                            =
                            CASE WHEN rel2.relation_source = 'internal' THEN 
                                CASE WHEN rel2.dataset_id = ?1 THEN CAST(rel2.relateddataset_id AS VARCHAR) ELSE CAST(rel2.dataset_id AS VARCHAR) END
                            ELSE rel2.externalidentifier END
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
                        SELECT MIN(rel2.id) FROM datasetrelation rel2
                        JOIN datasetversion dv_def2 ON rel2.definitionpoint_id = dv_def2.id
                        JOIN datasetrelationtype rt2 ON rel2.relationtype_id = rt2.id
                        LEFT JOIN datasetrelationtype inv2 ON rt2.inverse_id = inv2.id
                        WHERE (
                                (rel2.definitionpoint_id = ?2 AND rel2.dataset_id = ?1 AND rt2.name = ?3)
                                OR (rel2.definitionpoint_id = ?2 AND rel2.relation_source = 'internal' AND rel2.relateddataset_id = ?1 AND inv2.name = ?3)
                                OR (
                                    dv_def2.dataset_id != ?1 
                                    AND rel2.dataset_id = ?1 AND rt2.name = ?3
                                    AND rel2.definitionpoint_id = (
                                        SELECT dv2.id FROM datasetversion dv2
                                        WHERE dv2.dataset_id = dv_def2.dataset_id
                                        AND dv2.id = (SELECT MAX(dv3.id) FROM datasetversion dv3 WHERE dv3.dataset_id = dv2.dataset_id AND dv3.versionstate = 'RELEASED')
                                    )
                                )
                                OR (
                                    dv_def2.dataset_id != ?1 
                                    AND (rel2.relation_source = 'internal' AND rel2.relateddataset_id = ?1 AND inv2.name = ?3)
                                    AND rel2.definitionpoint_id = (
                                        SELECT dv2.id FROM datasetversion dv2
                                        WHERE dv2.dataset_id = dv_def2.dataset_id
                                        AND dv2.id = (SELECT MAX(dv3.id) FROM datasetversion dv3 WHERE dv3.dataset_id = dv2.dataset_id AND dv3.versionstate = 'RELEASED')
                                    )
                                )
                              )
                          AND (
                            CASE WHEN rel.relation_source = 'internal' THEN 
                                CASE WHEN rel.dataset_id = ?1 THEN CAST(rel.relateddataset_id AS VARCHAR) ELSE CAST(rel.dataset_id AS VARCHAR) END
                            ELSE rel.externalidentifier END
                            =
                            CASE WHEN rel2.relation_source = 'internal' THEN 
                                CASE WHEN rel2.dataset_id = ?1 THEN CAST(rel2.relateddataset_id AS VARCHAR) ELSE CAST(rel2.dataset_id AS VARCHAR) END
                            ELSE rel2.externalidentifier END
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
                relation_type_displayname,
                relation_type_description,
                COUNT(DISTINCT 
                    CASE 
                        WHEN relation_source = 'internal' THEN 
                            CASE WHEN dataset_id = ?1 THEN CAST(relateddataset_id AS VARCHAR) ELSE CAST(dataset_id AS VARCHAR) END
                        ELSE externalidentifier
                    END
                ) AS related_datasets_count
            FROM (
                SELECT
                    CASE
                        WHEN dr.dataset_id = ?1 THEN rt.name
                        ELSE inv.name
                    END AS relation_type_name,
                    CASE
                        WHEN dr.dataset_id = ?1 THEN rt.displayname
                        ELSE inv.displayname
                    END AS relation_type_displayname,
                    CASE
                        WHEN dr.dataset_id = ?1 THEN rt.description
                        ELSE inv.description
                    END AS relation_type_description,
                    dr.relation_source,
                    dr.dataset_id,
                    dr.relateddataset_id,
                    dr.externalidentifier
                FROM datasetrelation dr
                JOIN datasetrelationtype rt ON dr.relationtype_id = rt.id
                LEFT JOIN datasetrelationtype inv ON rt.inverse_id = inv.id
                WHERE (dr.dataset_id = ?1 OR dr.relateddataset_id = ?1)
                    AND dr.definitionpoint_id = (
                        SELECT dv.id FROM datasetversion dv
                        WHERE dv.dataset_id = (SELECT dv2.dataset_id FROM datasetversion dv2 WHERE dv2.id = dr.definitionpoint_id)
                        AND dv.id = (SELECT MAX(dv3.id) FROM datasetversion dv3 WHERE dv3.dataset_id = dv.dataset_id AND dv3.versionstate = 'RELEASED')
                    )
            ) t
            GROUP BY relation_type_name, relation_type_displayname, relation_type_description
            ORDER BY related_datasets_count DESC, relation_type_name ASC;
    """,
    resultSetMapping = "RelationCountMapping"
)
@NamedNativeQuery(
    name = "DatasetRelation.getRelationCountsByDatasetIdAndVersion",
    query = """
            SELECT
                relation_type_name,
                relation_type_displayname,
                relation_type_description,
                COUNT(DISTINCT 
                    CASE 
                        WHEN relation_source = 'internal' THEN 
                            CASE WHEN dataset_id = ?1 THEN CAST(relateddataset_id AS VARCHAR) ELSE CAST(dataset_id AS VARCHAR) END
                        ELSE externalidentifier
                    END
                ) AS related_datasets_count
            FROM (
                SELECT
                    CASE
                        WHEN dr.dataset_id = ?1 THEN rt.name
                        ELSE inv.name
                    END AS relation_type_name,
                    CASE
                        WHEN dr.dataset_id = ?1 THEN rt.displayname
                        ELSE inv.displayname
                    END AS relation_type_displayname,
                    CASE
                        WHEN dr.dataset_id = ?1 THEN rt.description
                        ELSE inv.description
                    END AS relation_type_description,
                    dr.relation_source,
                    dr.dataset_id,
                    dr.relateddataset_id,
                    dr.externalidentifier
                FROM datasetrelation dr
                JOIN datasetrelationtype rt ON dr.relationtype_id = rt.id
                LEFT JOIN datasetrelationtype inv ON rt.inverse_id = inv.id
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
            ) t
            GROUP BY relation_type_name, relation_type_displayname, relation_type_description
            ORDER BY related_datasets_count DESC, relation_type_name ASC;
    """,
    resultSetMapping = "RelationCountMapping"
)
@SqlResultSetMapping(
        name = "RelationCountMapping",
        columns = {
                @ColumnResult(name = "relation_type_name", type = String.class),
                @ColumnResult(name = "relation_type_displayname", type = String.class),
                @ColumnResult(name = "relation_type_description", type = String.class),
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
