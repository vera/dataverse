package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Default implementation of DatasetRelationAlgorithm using JPA and SQL queries.
 */
@ApplicationScoped
public class SqlDirectDatasetRelationAlgorithm implements DatasetRelationAlgorithm {

    @PersistenceContext
    private EntityManager em;

    private static final String WITH_LATEST_RELEASED_VERSIONS =
            " WITH latest_released_versions AS MATERIALIZED ( " +
            "     SELECT DISTINCT ON (dataset_id) id, dataset_id " +
            "     FROM datasetversion " +
            "     WHERE versionstate = 'RELEASED' " +
            "     ORDER BY dataset_id, id DESC " +
            " ), ";

    private static final String WITH_CANDIDATE_RELATIONS =
            " candidate_relations AS ( " +
            // Relations defined on the requested version.
            "     SELECT dr.id, 0 AS definition_point_priority " +
            "     FROM datasetrelation dr " +
            "     WHERE dr.definitionpoint_id = ? " +
            "     UNION ALL " +
            // Incoming relations defined on the latest released version of another dataset.
            "     SELECT dr.id, 1 AS definition_point_priority " +
            "     FROM datasetrelation dr " +
            "     JOIN latest_released_versions lrv ON dr.definitionpoint_id = lrv.id " +
            "     WHERE lrv.dataset_id != ? " +
            "       AND (dr.dataset_id = ? OR dr.relateddataset_id = ?) " +
            " ) ";

    private static final String WITH_CANONICAL_RELATIONS =
            " , normalized_candidate_relations AS ( " +
            "     SELECT cr.id, cr.definition_point_priority, " +
            "         CASE WHEN dr.dataset_id = ? THEN dr.relationtype_id ELSE rt.inverse_id END AS normalized_relation_type_id, " +
            "         CASE WHEN dr.relation_source = 'internal' " +
            "             THEN CASE WHEN dr.dataset_id = ? THEN CAST(dr.relateddataset_id AS VARCHAR) ELSE CAST(dr.dataset_id AS VARCHAR) END " +
            "             ELSE dr.externalidentifier " +
            "         END AS normalized_related_dataset " +
            "     FROM candidate_relations cr " +
            "     JOIN datasetrelation dr ON cr.id = dr.id " +
            "     LEFT JOIN datasetrelationtype rt ON dr.relationtype_id = rt.id " +
            " ), " +
            " canonical_relations AS ( " +
            "     SELECT DISTINCT ON (normalized_relation_type_id, normalized_related_dataset) " +
            "         id, definition_point_priority " +
            "     FROM normalized_candidate_relations " +
            "     ORDER BY normalized_relation_type_id, normalized_related_dataset, definition_point_priority, id " +
            " ) ";

    private static final String GET_TOTAL_RELATION_COUNT_QUERY_BASE =
            // Count the number of unique related datasets
            " SELECT COUNT(DISTINCT " +
            "    CASE " +
            "        WHEN dr.relation_source = 'internal' THEN " +
            "            CASE WHEN dr.dataset_id = ? THEN CAST(dr.relateddataset_id AS VARCHAR) ELSE CAST(dr.dataset_id AS VARCHAR) END " +
            "        ELSE dr.externalidentifier " +
            "    END) " +
            " FROM candidate_relations cr " +
            " JOIN datasetrelation dr ON cr.id = dr.id ";

    private static final String GET_RELATIONS_QUERY_BASE =
            " SELECT dr.* " +
            " FROM canonical_relations cr " +
            " JOIN datasetrelation dr ON cr.id = dr.id ";

    private static final String JOIN_RELATION_TYPES = 
            // Get information about dataset relation types
            " JOIN datasetrelationtype rt ON dr.relationtype_id = rt.id " +
            " LEFT JOIN datasetrelationtype inv ON rt.inverse_id = inv.id ";

    private static final String WHERE_RELATION_TYPE_MATCHES = 
            // The relation type must match the given one (inverted if necessary)
            " ( " +
            "    (dr.dataset_id = ? AND rt.name IN (?)) " +
            "    OR " +
            "    (dr.relateddataset_id = ? AND inv.name IN (?)) " +
            " ) ";

    private static final String JOIN_DATASET_TYPES = 
            " JOIN dataset d_related ON (CASE WHEN dr.dataset_id = ? THEN dr.relateddataset_id ELSE dr.dataset_id END) = d_related.id " +
            " JOIN datasettype dt ON d_related.datasettype_id = dt.id ";

    private static final String WHERE_DATASET_TYPE_MATCHES = 
            " (dr.relation_source = 'internal' AND dt.name IN (?)) ";

    private static final String WHERE_RELATION_SOURCE_MATCHES =
            " (dr.relation_source IN (?)) ";

    private static final String ORDER_BY_REQUESTED_DATASET_FIRST =
            " ORDER BY cr.definition_point_priority ASC, dr.id ASC ";

    @SuppressWarnings("unchecked")
    @Override
    public List<DatasetRelation> getRelations(Dataset d, DatasetVersion v, List<String> relationTypeNames, List<String> datasetTypeNames, List<String> relationSources, Integer limit, Integer offset) {
        StringBuilder sql = new StringBuilder();

        sql.append(WITH_LATEST_RELEASED_VERSIONS)
                .append(WITH_CANDIDATE_RELATIONS)
                .append(WITH_CANONICAL_RELATIONS)
                .append(GET_RELATIONS_QUERY_BASE);

        if (relationTypeNames != null && !relationTypeNames.isEmpty()) {
            sql.append(JOIN_RELATION_TYPES);
        }
        
        if (datasetTypeNames != null && !datasetTypeNames.isEmpty()) {
            sql.append(JOIN_DATASET_TYPES);
        }

        sql.append(" WHERE 1 = 1 ");

        if (relationTypeNames != null && !relationTypeNames.isEmpty()) {
            sql.append(" AND ").append(WHERE_RELATION_TYPE_MATCHES.replace("(?)", 
                    "(" + relationTypeNames.stream().map(n -> "?").collect(Collectors.joining(",")) + ")"));
        }
        
        if (datasetTypeNames != null && !datasetTypeNames.isEmpty()) {
            sql.append(" AND ").append(WHERE_DATASET_TYPE_MATCHES.replace("(?)", 
                    "(" + datasetTypeNames.stream().map(n -> "?").collect(Collectors.joining(",")) + ")"));
        }

        if (relationSources != null && !relationSources.isEmpty()) {
            sql.append(" AND ").append(WHERE_RELATION_SOURCE_MATCHES.replace("(?)", 
                    "(" + relationSources.stream().map(n -> "?").collect(Collectors.joining(",")) + ")"));
        }

        sql.append(ORDER_BY_REQUESTED_DATASET_FIRST);

        Query query = em.createNativeQuery(sql.toString(), DatasetRelation.class);
        int i = 1;

        // WITH_CANDIDATE_RELATIONS
        query.setParameter(i++, v.getId());
        query.setParameter(i++, d.getId());
        query.setParameter(i++, d.getId());
        query.setParameter(i++, d.getId());

        // WITH_CANONICAL_RELATIONS
        query.setParameter(i++, d.getId());
        query.setParameter(i++, d.getId());

        if (datasetTypeNames != null && !datasetTypeNames.isEmpty()) {
            // JOIN_DATASET_TYPES
            query.setParameter(i++, d.getId());
        }

        if (relationTypeNames != null && !relationTypeNames.isEmpty()) {
            // WHERE_RELATION_TYPE_MATCHES
            query.setParameter(i++, d.getId());
            for (String typeName : relationTypeNames) {
                query.setParameter(i++, typeName);
            }
            query.setParameter(i++, d.getId());
            for (String typeName : relationTypeNames) {
                query.setParameter(i++, typeName);
            }
        }
        
        if (datasetTypeNames != null && !datasetTypeNames.isEmpty()) {
            // WHERE_DATASET_TYPE_MATCHES
            for (String typeName : datasetTypeNames) {
                query.setParameter(i++, typeName);
            }
        }

        if (relationSources != null && !relationSources.isEmpty()) {
            // WHERE_RELATION_SOURCE_MATCHES
            for (String source : relationSources) {
                query.setParameter(i++, source);
            }
        }

        return (List<DatasetRelation>) query.setMaxResults(limit)
                .setFirstResult(offset)
                .getResultList();
    }

    @Override
    public Long getTotalDatasetRelationCountFor(Dataset d, DatasetVersion v, List<String> relationTypeNames, List<String> datasetTypeNames, List<String> relationSources) {
        StringBuilder sql = new StringBuilder();

        sql.append(WITH_LATEST_RELEASED_VERSIONS)
                .append(WITH_CANDIDATE_RELATIONS)
                .append(GET_TOTAL_RELATION_COUNT_QUERY_BASE);

        if (relationTypeNames != null && !relationTypeNames.isEmpty()) {
            sql.append(JOIN_RELATION_TYPES);
        }
        
        if (datasetTypeNames != null && !datasetTypeNames.isEmpty()) {
            sql.append(JOIN_DATASET_TYPES);
        }

        sql.append(" WHERE 1 = 1 ");

        if (relationTypeNames != null && !relationTypeNames.isEmpty()) {
            sql.append(" AND ").append(WHERE_RELATION_TYPE_MATCHES.replace("(?)", 
                    "(" + relationTypeNames.stream().map(n -> "?").collect(Collectors.joining(",")) + ")"));
        }
        
        if (datasetTypeNames != null && !datasetTypeNames.isEmpty()) {
            sql.append(" AND ").append(WHERE_DATASET_TYPE_MATCHES.replace("(?)", 
                    "(" + datasetTypeNames.stream().map(n -> "?").collect(Collectors.joining(",")) + ")"));
        }

        if (relationSources != null && !relationSources.isEmpty()) {
            sql.append(" AND ").append(WHERE_RELATION_SOURCE_MATCHES.replace("(?)", 
                    "(" + relationSources.stream().map(n -> "?").collect(Collectors.joining(",")) + ")"));
        }

        Query query = em.createNativeQuery(sql.toString());
        int i = 1;

        // WITH_CANDIDATE_RELATIONS
        query.setParameter(i++, v.getId());
        query.setParameter(i++, d.getId());
        query.setParameter(i++, d.getId());
        query.setParameter(i++, d.getId());

        // GET_TOTAL_RELATION_COUNT_QUERY_BASE
        query.setParameter(i++, d.getId());

        if (datasetTypeNames != null && !datasetTypeNames.isEmpty()) {
            // JOIN_DATASET_TYPES
            query.setParameter(i++, d.getId());
        }

        if (relationTypeNames != null && !relationTypeNames.isEmpty()) {
            // WHERE_RELATION_TYPE_MATCHES
            query.setParameter(i++, d.getId());
            for (String typeName : relationTypeNames) {
                query.setParameter(i++, typeName);
            }
            query.setParameter(i++, d.getId());
            for (String typeName : relationTypeNames) {
                query.setParameter(i++, typeName);
            }
        }
        
        if (datasetTypeNames != null && !datasetTypeNames.isEmpty()) {
            // WHERE_DATASET_TYPE_MATCHES
            for (String typeName : datasetTypeNames) {
                query.setParameter(i++, typeName);
            }
        }

        if (relationSources != null && !relationSources.isEmpty()) {
            // WHERE_RELATION_SOURCE_MATCHES
            for (String source : relationSources) {
                query.setParameter(i++, source);
            }
        }

        return (Long) query.getSingleResult();
    }

    private static final String GET_RELATION_COUNTS_QUERY_BASE =
            " SELECT " +
            "    %s, " + // Dynamic select for grouping fields
            "    COUNT(DISTINCT " +
            "        CASE " +
            "            WHEN t.relation_source = 'internal' THEN " +
            "                CASE WHEN t.dataset_id = ? THEN CAST(t.relateddataset_id AS VARCHAR) ELSE CAST(t.dataset_id AS VARCHAR) END " +
            "            ELSE t.externalidentifier " +
            "        END " +
            "    ) AS related_datasets_count " +
            " FROM ( " +
                    "    SELECT " +
                    "        %s " + // Dynamic select for columns in subquery
                    "        dr.relation_source, " +
                    "        dr.dataset_id, " +
                    "        dr.relateddataset_id, " +
                    "        dr.externalidentifier " +
                    "    FROM datasetrelation dr " +
                    "    JOIN datasetrelationtype rt ON dr.relationtype_id = rt.id " +
                    "    LEFT JOIN datasetrelationtype inv ON rt.inverse_id = inv.id " +
                    "    JOIN datasetversion dv_def ON dr.definitionpoint_id = dv_def.id " +
                    "    %s " + // Optional join (for dataset type)
                    "    WHERE %s " + // Combined WHERE clause
                    " ) t " +
                    " GROUP BY %s " + // Dynamic GROUP BY
                    " ORDER BY related_datasets_count DESC, %s ASC "; // Dynamic ORDER BY

    private static final String SUBQUERY_COLS_RELATION_TYPE =
            " CASE WHEN dr.dataset_id = ? THEN rt.name ELSE inv.name END AS relation_type_name, " +
                    " CASE WHEN dr.dataset_id = ? THEN rt.displayname ELSE inv.displayname END AS relation_type_displayname, " +
                    " CASE WHEN dr.dataset_id = ? THEN rt.description ELSE inv.description END AS relation_type_description, ";

    private static final String SUBQUERY_COLS_DATASET_TYPE =
            " dt.name AS dataset_type_name, " +
                    " dt.displayname AS dataset_type_displayname, " +
                    " dt.description AS dataset_type_description, ";

    private static final String WHERE_FOR_COUNTS =
            " ( " +
                    "   dr.definitionpoint_id = ? " +
                    "   OR ( " +
                    "       ((dr.dataset_id = ? OR dr.relateddataset_id = ?) AND dv_def.dataset_id != ?) " +
                    "       AND dr.definitionpoint_id = ( " +
                    "           SELECT dv.id FROM datasetversion dv " +
                    "           WHERE dv.dataset_id = dv_def.dataset_id " +
                    "           AND dv.id = (SELECT MAX(dv3.id) FROM datasetversion dv3 WHERE dv3.dataset_id = dv.dataset_id AND dv3.versionstate = 'RELEASED') " +
                    "       ) " +
                    "   ) " +
                    " ) ";

    @SuppressWarnings("unchecked")
    @Override
    public List<Object[]> getRelationCounts(Dataset d, DatasetVersion v, String groupBy) {
        String selectCols;
        String subqueryCols;
        String join = "";
        String where = WHERE_FOR_COUNTS;
        String groupByCols;
        String orderByCol;

        boolean isGroupByDatasetType = "datasetType".equals(groupBy);

        if (isGroupByDatasetType) {
            selectCols = "dataset_type_name, dataset_type_displayname, dataset_type_description";
            subqueryCols = SUBQUERY_COLS_DATASET_TYPE;
            join = JOIN_DATASET_TYPES;
            where += " AND dr.relation_source = 'internal' ";
            groupByCols = selectCols;
            orderByCol = "dataset_type_name";
        } else {
            selectCols = "relation_type_name, relation_type_displayname, relation_type_description";
            subqueryCols = SUBQUERY_COLS_RELATION_TYPE;
            groupByCols = selectCols;
            orderByCol = "relation_type_name";
        }

        String sql = String.format(GET_RELATION_COUNTS_QUERY_BASE, selectCols, subqueryCols, join, where, groupByCols, orderByCol);

        Query query = em.createNativeQuery(sql);
        int i = 1;

        // Grouping columns (COUNT DISTINCT)
        query.setParameter(i++, d.getId());

        if (!isGroupByDatasetType) {
            // SUBQUERY_COLS_RELATION_TYPE
            query.setParameter(i++, d.getId());
            query.setParameter(i++, d.getId());
            query.setParameter(i++, d.getId());
        }

        if (isGroupByDatasetType) {
            // JOIN_DATASET_TYPES
            query.setParameter(i++, d.getId());
        }

        // WHERE_FOR_COUNTS
        query.setParameter(i++, v.getId());
        query.setParameter(i++, d.getId());
        query.setParameter(i++, d.getId());
        query.setParameter(i++, d.getId());

        return (List<Object[]>) query.getResultList();
    }
}
