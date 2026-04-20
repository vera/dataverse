package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.Dataset;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * Default implementation of DatasetRelationAlgorithm using JPA and SQL queries.
 */
@ApplicationScoped
public class SqlDirectDatasetRelationAlgorithm implements DatasetRelationAlgorithm {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<DatasetRelation> getRelations(Dataset d, String relationTypeName, Integer limit, Integer offset) {
        if (relationTypeName != null) {
            return em.createNamedQuery("DatasetRelation.getUniqueRelationsByDatasetIdAndType", DatasetRelation.class)
                    .setParameter("datasetId", d.getId())
                    .setParameter("relationType", relationTypeName)
                    .setMaxResults(limit)
                    .setFirstResult(offset)
                    .getResultList();
        } else {
            return em.createNamedQuery("DatasetRelation.getUniqueRelationsByDatasetId", DatasetRelation.class)
                    .setParameter("datasetId", d.getId())
                    .setMaxResults(limit)
                    .setFirstResult(offset)
                    .getResultList();
        }
    }

    @Override
    public List<Object[]> getRelationCounts(Dataset d) {
        return em.createNamedQuery("DatasetRelation.getRelationCountsByDatasetId", Object[].class)
                .setParameter(1, d.getId())
                .getResultList();
    }

    @Override
    public Long getRelatedDatasetCount(Dataset d) {
        return em.createNamedQuery("DatasetRelation.getTotalCountByDatasetId", Long.class)
                .setParameter(1, d.getId())
                .getSingleResult();
    }
}
