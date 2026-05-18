package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
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
    public List<DatasetRelation> getRelations(Dataset d, DatasetVersion v, String relationTypeName, Integer limit, Integer offset) {
        if (v != null) {
            if (relationTypeName != null) {
                return em.createNamedQuery("DatasetRelation.getUniqueRelationsByDatasetIdAndVersionAndType", DatasetRelation.class)
                        .setParameter(1, d.getId())
                        .setParameter(2, v.getId())
                        .setParameter(3, relationTypeName)
                        .setMaxResults(limit)
                        .setFirstResult(offset)
                        .getResultList();
            } else {
                return em.createNamedQuery("DatasetRelation.getUniqueRelationsByDatasetIdAndVersion", DatasetRelation.class)
                        .setParameter(1, d.getId())
                        .setParameter(2, v.getId())
                        .setMaxResults(limit)
                        .setFirstResult(offset)
                        .getResultList();
            }
        } else {
            if (relationTypeName != null) {
                return em.createNamedQuery("DatasetRelation.getUniqueRelationsByDatasetIdAndType", DatasetRelation.class)
                        .setParameter(1, d.getId())
                        .setParameter(2, relationTypeName)
                        .setMaxResults(limit)
                        .setFirstResult(offset)
                        .getResultList();
            } else {
                return em.createNamedQuery("DatasetRelation.getUniqueRelationsByDatasetId", DatasetRelation.class)
                        .setParameter(1, d.getId())
                        .setMaxResults(limit)
                        .setFirstResult(offset)
                        .getResultList();
            }
        }
    }

    @Override
    public List<Object[]> getRelationCounts(Dataset d, DatasetVersion v) {
        if (v != null) {
            return em.createNamedQuery("DatasetRelation.getRelationCountsByDatasetIdAndVersion", Object[].class)
                    .setParameter(1, d.getId())
                    .setParameter(2, v.getId())
                    .getResultList();
        } else {
            return em.createNamedQuery("DatasetRelation.getRelationCountsByDatasetId", Object[].class)
                    .setParameter(1, d.getId())
                    .getResultList();
        }
    }

    @Override
    public Long getRelatedDatasetCount(Dataset d, DatasetVersion v, String relationTypeName) {
        if (relationTypeName != null) {
            if (v != null) {
                return em.createNamedQuery("DatasetRelation.getTotalCountByDatasetIdAndVersionAndType", Long.class)
                        .setParameter(1, d.getId())
                        .setParameter(2, v.getId())
                        .setParameter(3, relationTypeName)
                        .getSingleResult();
            } else {
                return em.createNamedQuery("DatasetRelation.getTotalCountByDatasetIdAndType", Long.class)
                        .setParameter(1, d.getId())
                        .setParameter(2, relationTypeName)
                        .getSingleResult();
            }
        } else {
            if (v != null) {
                return em.createNamedQuery("DatasetRelation.getTotalCountByDatasetIdAndVersion", Long.class)
                        .setParameter(1, d.getId())
                        .setParameter(2, v.getId())
                        .getSingleResult();
            } else {
                return em.createNamedQuery("DatasetRelation.getTotalCountByDatasetId", Long.class)
                        .setParameter(1, d.getId())
                        .getSingleResult();
            }
        }
    }
}
