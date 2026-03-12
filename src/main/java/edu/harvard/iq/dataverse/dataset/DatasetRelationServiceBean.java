/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.Dataset;

import java.util.List;
import java.util.logging.Logger;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

/**
 *
 * @author Vera Clemens
 */
@Stateless
@Named
public class DatasetRelationServiceBean {

    private static final Logger logger = Logger.getLogger(DatasetRelationServiceBean.class.getCanonicalName());

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public void deleteDatasetRelationsFor(Dataset d) {
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

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<DatasetRelation> addDatasetRelations(List<DatasetRelation> relations) {
        for (DatasetRelation relation : relations) {
            em.persist(relation);
        }
        return relations;
    }

}
