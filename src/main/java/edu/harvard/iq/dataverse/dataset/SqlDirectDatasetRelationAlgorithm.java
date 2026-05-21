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

//    private static final Logger logger = Logger.getLogger(SqlDirectDatasetRelationAlgorithm.class.getCanonicalName());

    @PersistenceContext
    private EntityManager em;

    private static final String GET_TOTAL_RELATION_COUNT_QUERY_BASE = 
            // Count the number of unique related datasets
            " SELECT COUNT(DISTINCT " +
            "    CASE " +
            "        WHEN dr.relation_source = 'internal' THEN " +
            "            CASE WHEN dr.dataset_id = ? THEN CAST(dr.relateddataset_id AS VARCHAR) ELSE CAST(dr.dataset_id AS VARCHAR) END " +
            "        ELSE dr.externalidentifier " +
            "    END) " +
            " FROM datasetrelation dr " +
            " JOIN datasetversion dv_def ON dr.definitionpoint_id = dv_def.id ";

    private static final String GET_RELATIONS_QUERY_BASE = 
            " SELECT dr.* " +
            " FROM datasetrelation dr " +
            " JOIN datasetversion dv_def ON dr.definitionpoint_id = dv_def.id ";

    private static final String JOIN_RELATION_TYPES = 
            // Get information about dataset relation types
            " JOIN datasetrelationtype rt ON dr.relationtype_id = rt.id " +
            " LEFT JOIN datasetrelationtype inv ON rt.inverse_id = inv.id ";

    private static final String WHERE_DATASET_OR_DATASET_VERSION_MATCHES = 
            " ( " +
            // The relation must either be defined on the given dataset version...
            "    (dr.definitionpoint_id = ?) " +
            // ...or, even if it's not defined on the given dataset version, the relation might still be relevant if it involves the given dataset and is defined on the latest released version of another dataset
            "    OR ( " +
            "        dv_def.dataset_id != ? " +
            "        AND (dr.dataset_id = ? OR dr.relateddataset_id = ?) " +
            "        AND dr.definitionpoint_id = ( " +
            "            SELECT dv.id FROM datasetversion dv " +
            "            WHERE dv.dataset_id = dv_def.dataset_id " +
            "            AND dv.id = (SELECT MAX(dv2.id) FROM datasetversion dv2 WHERE dv2.dataset_id = dv.dataset_id AND dv2.versionstate = 'RELEASED') " +
            "        ) " +
            "    ) " +
            " ) ";

    private static final String WHERE_RELATION_TYPE_MATCHES = 
            // The relation type must match the given one (inverted if necessary)
            " ( " +
            "    (dr.dataset_id = ? AND rt.name = ?) " +
            "    OR " +
            "    (dr.relateddataset_id = ? AND inv.name = ?) " +
            " ) ";

    private static final String WHERE_RELATION_IS_NOT_A_DUPLICATE = 
            " dr.id = ( " +
            "    SELECT MIN(dr2.id) FROM datasetrelation dr2 " +
            "    JOIN datasetversion dv_def2 ON dr2.definitionpoint_id = dv_def2.id " +
            "    WHERE ( " +
            "        dr2.definitionpoint_id = ? " +
            "        OR ( " +
            "            (dr2.dataset_id = ? OR (dr2.relation_source = 'internal' AND dr2.relateddataset_id = ?)) " +
            "            AND dv_def2.dataset_id != ? " +
            "            AND dr2.definitionpoint_id = ( " +
            "                SELECT dv2.id FROM datasetversion dv2 " +
            "                WHERE dv2.dataset_id = dv_def2.dataset_id " +
            "                AND dv2.id = (SELECT MAX(dv3.id) FROM datasetversion dv3 WHERE dv3.dataset_id = dv2.dataset_id AND dv3.versionstate = 'RELEASED') " +
            "            ) " +
            "        ) " +
            "      ) " +
            "      AND ( " +
            "        CASE WHEN dr.dataset_id = ? THEN dr.relationtype_id ELSE (SELECT rt.inverse_id FROM datasetrelationtype rt WHERE rt.id = dr.relationtype_id) END " +
            "        = " +
            "        CASE WHEN dr2.dataset_id = ? THEN dr2.relationtype_id ELSE (SELECT rt2.inverse_id FROM datasetrelationtype rt2 WHERE rt2.id = dr2.relationtype_id) END " +
            "      ) " +
            "      AND ( " +
            "        CASE WHEN dr.relation_source = 'internal' THEN " +
            "            CASE WHEN dr.dataset_id = ? THEN CAST(dr.relateddataset_id AS VARCHAR) ELSE CAST(dr.dataset_id AS VARCHAR) END " +
            "        ELSE dr.externalidentifier END " +
            "        = " +
            "        CASE WHEN dr2.relation_source = 'internal' THEN " +
            "            CASE WHEN dr2.dataset_id = ? THEN CAST(dr2.relateddataset_id AS VARCHAR) ELSE CAST(dr2.dataset_id AS VARCHAR) END " +
            "        ELSE dr2.externalidentifier END " +
            "      ) " +
            " ) ";

    private static final String JOIN_DATASET_TYPES = 
            " JOIN dataset d_related ON (CASE WHEN dr.dataset_id = ? THEN dr.relateddataset_id ELSE dr.dataset_id END) = d_related.id " +
            " JOIN datasettype dt ON d_related.datasettype_id = dt.id ";

    private static final String WHERE_DATASET_TYPE_MATCHES = 
            " (dr.relation_source = 'internal' AND dt.name IN (?)) ";

    @SuppressWarnings("unchecked")
    @Override
    public List<DatasetRelation> getRelations(Dataset d, DatasetVersion v, String relationTypeName, List<String> datasetTypeNames, Integer limit, Integer offset) {
        StringBuilder sql = new StringBuilder();

        sql.append(GET_RELATIONS_QUERY_BASE);

        if (relationTypeName != null) {
            sql.append(JOIN_RELATION_TYPES);
        }
        
        if (datasetTypeNames != null && !datasetTypeNames.isEmpty()) {
            sql.append(JOIN_DATASET_TYPES);
        }

        sql.append(" WHERE ")
                .append(WHERE_DATASET_OR_DATASET_VERSION_MATCHES)
                .append(" AND ")
                .append(WHERE_RELATION_IS_NOT_A_DUPLICATE);

        if (relationTypeName != null) {
            sql.append(" AND ").append(WHERE_RELATION_TYPE_MATCHES);
        }
        
        if (datasetTypeNames != null && !datasetTypeNames.isEmpty()) {
            sql.append(" AND ").append(WHERE_DATASET_TYPE_MATCHES.replace("(?)", 
                    "(" + datasetTypeNames.stream().map(n -> "?").collect(Collectors.joining(",")) + ")"));
        }

        Query query = em.createNativeQuery(sql.toString(), DatasetRelation.class);
        int i = 1;
        
        if (datasetTypeNames != null && !datasetTypeNames.isEmpty()) {
            // JOIN_DATASET_TYPES
            query.setParameter(i++, d.getId());
        }

        // WHERE_DATASET_OR_DATASET_VERSION_MATCHES
        query.setParameter(i++, v.getId());
        query.setParameter(i++, d.getId());
        query.setParameter(i++, d.getId());
        query.setParameter(i++, d.getId());

        // WHERE_RELATION_IS_NOT_A_DUPLICATE
        query.setParameter(i++, v.getId());
        query.setParameter(i++, d.getId());
        query.setParameter(i++, d.getId());
        query.setParameter(i++, d.getId());
        query.setParameter(i++, d.getId());
        query.setParameter(i++, d.getId());
        query.setParameter(i++, d.getId());
        query.setParameter(i++, d.getId());

        if (relationTypeName != null) {
            // WHERE_RELATION_TYPE_MATCHES
            query.setParameter(i++, d.getId());
            query.setParameter(i++, relationTypeName);
            query.setParameter(i++, d.getId());
            query.setParameter(i++, relationTypeName);
        }
        
        if (datasetTypeNames != null && !datasetTypeNames.isEmpty()) {
            // WHERE_DATASET_TYPE_MATCHES
            for (String typeName : datasetTypeNames) {
                query.setParameter(i++, typeName);
            }
        }

        return (List<DatasetRelation>) query.setMaxResults(limit)
                .setFirstResult(offset)
                .getResultList();
    }

    @Override
    public List<Object[]> getRelationCounts(Dataset d, DatasetVersion v) {
        return em.createNamedQuery("DatasetRelation.getRelationCountsByDatasetIdAndVersion", Object[].class)
                .setParameter(1, d.getId())
                .setParameter(2, v.getId())
                .getResultList();
    }

    @Override
    public Long getTotalDatasetRelationCountFor(Dataset d, DatasetVersion v, String relationTypeName, List<String> datasetTypeNames) {
        StringBuilder sql = new StringBuilder();

        sql.append(GET_TOTAL_RELATION_COUNT_QUERY_BASE);

        if (relationTypeName != null) {
            sql.append(JOIN_RELATION_TYPES);
        }
        
        if (datasetTypeNames != null && !datasetTypeNames.isEmpty()) {
            sql.append(JOIN_DATASET_TYPES);
        }

        sql.append(" WHERE ").append(WHERE_DATASET_OR_DATASET_VERSION_MATCHES);

        if (relationTypeName != null) {
            sql.append(" AND ").append(WHERE_RELATION_TYPE_MATCHES);
        }
        
        if (datasetTypeNames != null && !datasetTypeNames.isEmpty()) {
            sql.append(" AND ").append(WHERE_DATASET_TYPE_MATCHES.replace("(?)", 
                    "(" + datasetTypeNames.stream().map(n -> "?").collect(Collectors.joining(",")) + ")"));
        }

        Query query = em.createNativeQuery(sql.toString());
        int i = 1;
        // GET_TOTAL_RELATION_COUNT_QUERY_BASE
        query.setParameter(i++, d.getId());

        if (datasetTypeNames != null && !datasetTypeNames.isEmpty()) {
            // JOIN_DATASET_TYPES
            query.setParameter(i++, d.getId());
        }

        // WHERE_DATASET_OR_DATASET_VERSION_MATCHES
        query.setParameter(i++, v.getId());
        query.setParameter(i++, d.getId());
        query.setParameter(i++, d.getId());
        query.setParameter(i++, d.getId());

        if (relationTypeName != null) {
            // WHERE_RELATION_TYPE_MATCHES
            query.setParameter(i++, d.getId());
            query.setParameter(i++, relationTypeName);
            query.setParameter(i++, d.getId());
            query.setParameter(i++, relationTypeName);
        }
        
        if (datasetTypeNames != null && !datasetTypeNames.isEmpty()) {
            // WHERE_DATASET_TYPE_MATCHES
            for (String typeName : datasetTypeNames) {
                query.setParameter(i++, typeName);
            }
        }

        return (Long) query.getSingleResult();
    }
}
