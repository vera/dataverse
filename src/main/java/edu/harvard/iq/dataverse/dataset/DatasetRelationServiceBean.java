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

    public List<DatasetRelation> getDatasetRelationsFor(Dataset d, boolean groupByRelationType, Integer limit) {
        if (groupByRelationType) {
            return em.createNamedQuery("DatasetRelation.getRelationsByDatasetIdLimitedPerType", DatasetRelation.class)
                    .setParameter(1, d.getId())
                    .setParameter(2, limit)
                    .getResultList();
        } else {
            return em.createNamedQuery("DatasetRelation.getRelationsByDatasetId", DatasetRelation.class)
                    .setParameter("datasetId", d.getId())
                    .setMaxResults(limit)
                    .getResultList();
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<DatasetRelation> addDatasetRelations(List<DatasetRelation> relations) {
        for (DatasetRelation relation : relations) {
            em.persist(relation);
        }
        return relations;
    }

}
