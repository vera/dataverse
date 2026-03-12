/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.Dataset;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 *
 * @author Vera Clemens
 */
@Stateless
@Named
public class DatasetRelationServiceBean {

    private static final Logger logger = Logger.getLogger(DatasetRelationServiceBean.class.getCanonicalName());

    @PersistenceContext
    private EntityManager em;

    /**
     * A reference to the current instance of the DatasetRelationServiceBean.
     * Used when self-invocation is required for internal method calls
     * within the same bean to ensure that all EJB functionalities
     * such as transactions and security are properly applied.
     */
    @EJB
    private DatasetRelationServiceBean self;

    public void deleteAllDatasetRelationsFor(Dataset d) {
        em.createNamedQuery("DatasetRelation.removeRelationsByDatasetId")
                .setParameter("datasetId", d.getId())
                .executeUpdate();
    }

    public List<DatasetRelation> getDatasetRelationsFor(Dataset d, String relationTypeName, Integer limit, Integer offset) {
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

    public List<Object[]> getDatasetRelationCountsFor(Dataset d) {
        return em.createNamedQuery("DatasetRelation.getRelationCountsByDatasetId", Object[].class)
                .setParameter(1, d.getId())
                .getResultList();
    }

    public Long getRelatedDatasetCountFor(Dataset d) {
        return em.createNamedQuery("DatasetRelation.getTotalCountByDatasetId", Long.class)
                .setParameter(1, d.getId())
                .getSingleResult();
    }

    public List<DatasetRelation> addDatasetRelations(List<DatasetRelation> relations) {
        for (DatasetRelation relation : relations) {
            em.persist(relation);
        }
        return relations;
    }

    public List<DatasetRelation> replaceAllDatasetRelationsFor(Dataset d, List<DatasetRelation> newRelations) {
        // Execute the update (in one atomic operation using a transaction)
        // Note: We need to call via self-reference so the EJB container can create a transaction as intended.
        return self.replaceAll(d, newRelations);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<DatasetRelation> replaceAll(Dataset d, List<DatasetRelation> newRelations) {
        List<DatasetRelation> existingRelations = em.createNamedQuery("DatasetRelation.getRelationsDefinedAtDatasetId", DatasetRelation.class)
                .setParameter("datasetId", d.getId())
                .getResultList();

        Set<String> existingKeys = existingRelations.stream()
                .map(DatasetRelation::toKey)
                .collect(Collectors.toSet());
        Set<String> newKeys = newRelations.stream()
                .map(DatasetRelation::toKey)
                .collect(Collectors.toSet());

        List<DatasetRelation> toAdd = newRelations.stream()
                .filter(r -> !existingKeys.contains(r.toKey()))
                .toList();
        List<DatasetRelation> toRemove = existingRelations.stream()
                .filter(r -> !newKeys.contains(r.toKey()))
                .toList();

        if (!toRemove.isEmpty()) {
            em.createQuery("DELETE FROM DatasetRelation dr WHERE dr IN :toRemove")
                    .setParameter("toRemove", toRemove)
                    .executeUpdate();
        }
        for (DatasetRelation r : toAdd) {
            em.persist(r);
        }

        em.flush();

        return newRelations;
    }

}
