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
import jakarta.inject.Inject;
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

    @EJB
    private DatasetRelationServiceBean self;

    @Inject
    private DatasetRelationAlgorithm algorithm;

    public void deleteAllDatasetRelationsFor(Dataset d) {
        em.createNamedQuery("DatasetRelation.removeRelationsByDatasetId")
                .setParameter("datasetId", d.getId())
                .executeUpdate();
    }

    public List<DatasetRelation> getDatasetRelationsFor(Dataset d, String relationTypeName, Integer limit, Integer offset) {
        return algorithm.getRelations(d, relationTypeName, limit, offset);
    }

    public List<Object[]> getDatasetRelationCountsFor(Dataset d) {
        return algorithm.getRelationCounts(d);
    }

    public Long getRelatedDatasetCountFor(Dataset d) {
        return algorithm.getRelatedDatasetCount(d);
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
