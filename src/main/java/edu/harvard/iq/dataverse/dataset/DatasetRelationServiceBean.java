/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import edu.harvard.iq.dataverse.api.dto.DatasetRelationDTO;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
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
    private DatasetRelationTypeServiceBean relationTypeService;

    @EJB
    private DatasetServiceBean datasetService;

    @Inject
    private DatasetRelationAlgorithm algorithm;

    public void deleteAllDatasetRelationsFor(DatasetVersion v) {
        em.createNamedQuery("DatasetRelation.removeRelationsByDatasetVersionId")
                .setParameter("versionId", v.getId())
                .executeUpdate();
    }

    public DatasetRelation getDatasetRelationById(Long id) {
        try {
            return em.createNamedQuery("DatasetRelation.getRelationById", DatasetRelation.class)
                    .setParameter("id", id)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public void deleteDatasetRelationById(Long id) {
        em.createNamedQuery("DatasetRelation.deleteRelationById")
          .setParameter("id", id)
          .executeUpdate();
    }

    public List<DatasetRelation> getDatasetRelationsFor(Dataset d, DatasetVersion v, List<String> relationTypeNames, List<String> datasetTypeNames, List<String> relationSources, Integer limit, Integer offset) {
        return algorithm.getRelations(d, v, relationTypeNames, datasetTypeNames, relationSources, limit, offset);
    }

    public List<Object[]> getDatasetRelationCountsFor(Dataset d, DatasetVersion v, String groupBy) {
        return algorithm.getRelationCounts(d, v, groupBy);
    }

    public Long getTotalDatasetRelationCountFor(Dataset d, DatasetVersion v) {
        return algorithm.getTotalDatasetRelationCountFor(d, v, null, null, null);
    }

    public Long getTotalDatasetRelationCountFor(Dataset d, DatasetVersion v, List<String> relationTypeNames, List<String> datasetTypeNames, List<String> relationSources) {
        return algorithm.getTotalDatasetRelationCountFor(d, v, relationTypeNames, datasetTypeNames, relationSources);
    }

    public DatasetRelation addDatasetRelation(DatasetRelation relation) {
        em.persist(relation);
        em.flush();
        return relation;
    }

    /**
     * A relation without a type is a duplicate of every relation between the
     * same endpoints at the same definition point. Relations with different,
     * non-null types remain distinct. This must be checked outside the
     * database because its unique constraints treat NULL relation types as
     * distinct values.
     */
    public boolean isDuplicate(DatasetRelation relation) {
        String relationTypeClause = relation.getRelationType() == null
                ? ""
                : " AND (rel.relationType IS NULL OR rel.relationType.id = :relationTypeId)";
        if (relation instanceof InternalDatasetRelation internalRelation) {
            var query = em.createQuery("SELECT COUNT(rel) FROM InternalDatasetRelation rel "
                    + "WHERE rel.dataset = :dataset AND rel.relatedDataset = :relatedDataset "
                    + "AND rel.definitionPoint = :definitionPoint "
                    + relationTypeClause, Long.class)
                    .setParameter("dataset", relation.getDataset())
                    .setParameter("relatedDataset", internalRelation.getRelatedDataset())
                    .setParameter("definitionPoint", relation.getDefinitionPoint());
            if (relation.getRelationType() != null) {
                query.setParameter("relationTypeId", relation.getRelationType().getId());
            }
            return query.getSingleResult() > 0;
        } else if (relation instanceof ExternalDatasetRelation externalRelation) {
            var query = em.createQuery("SELECT COUNT(rel) FROM ExternalDatasetRelation rel "
                    + "WHERE rel.dataset = :dataset AND rel.externalIdentifier = :externalIdentifier "
                    + "AND rel.definitionPoint = :definitionPoint "
                    + relationTypeClause, Long.class)
                    .setParameter("dataset", relation.getDataset())
                    .setParameter("externalIdentifier", externalRelation.getExternalIdentifier())
                    .setParameter("definitionPoint", relation.getDefinitionPoint());
            if (relation.getRelationType() != null) {
                query.setParameter("relationTypeId", relation.getRelationType().getId());
            }
            return query.getSingleResult() > 0;
        }
        throw new IllegalArgumentException("Unknown dataset relation type");
    }

    public boolean containsDuplicates(List<DatasetRelation> relations) {
        for (int i = 0; i < relations.size(); i++) {
            for (int j = i + 1; j < relations.size(); j++) {
                if (areDuplicates(relations.get(i), relations.get(j))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean areDuplicates(DatasetRelation first, DatasetRelation second) {
        if (!first.getDataset().equals(second.getDataset())
                || !first.getDefinitionPoint().equals(second.getDefinitionPoint())
                || (first.getRelationType() != null && second.getRelationType() != null
                    && !first.getRelationType().equals(second.getRelationType()))) {
            return false;
        }
        if (first instanceof InternalDatasetRelation firstInternal
                && second instanceof InternalDatasetRelation secondInternal) {
            return firstInternal.getRelatedDataset().equals(secondInternal.getRelatedDataset());
        }
        if (first instanceof ExternalDatasetRelation firstExternal
                && second instanceof ExternalDatasetRelation secondExternal) {
            return firstExternal.getExternalIdentifier().equals(secondExternal.getExternalIdentifier());
        }
        return false;
    }

    public List<DatasetRelation> replaceAllDatasetRelationsFor(DatasetVersion v, List<DatasetRelation> newRelations) {
        List<DatasetRelation> existingRelations = em.createNamedQuery("DatasetRelation.getRelationsDefinedAtDatasetVersionId", DatasetRelation.class)
                .setParameter("versionId", v.getId())
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
            List<Long> toRemoveIds = toRemove.stream().map(DatasetRelation::getId).toList();
            em.createQuery("DELETE FROM DatasetRelation dr WHERE dr.id IN :toRemoveIds")
                    .setParameter("toRemoveIds", toRemoveIds)
                    .executeUpdate();
        }
        for (DatasetRelation r : toAdd) {
            em.persist(r);
        }

        em.flush();

        // Re-fetch to ensure IDs are populated
        return em.createNamedQuery("DatasetRelation.getRelationsDefinedAtDatasetVersionId", DatasetRelation.class)
                .setParameter("versionId", v.getId())
                .getResultList();
    }

    public DatasetRelation fromDTO(DatasetRelationDTO dto, DatasetVersion version) {
        Dataset d = version.getDataset();
        DatasetRelationType type = relationTypeService.findByName(dto.getRelationTypeName());

        if (dto.getRelatedDatasetPid() != null) {
            Dataset relatedDataset = datasetService.findByGlobalId(dto.getRelatedDatasetPid());
            if (relatedDataset == null) {
                logger.severe("Failed to find related dataset with PID " + dto.getRelatedDatasetPid());
                return null;
            }
            return new InternalDatasetRelation(d, relatedDataset, type, version);
        } else if (dto.getExternalIdentifier() != null) {
            return new ExternalDatasetRelation(d, dto.getExternalIdentifier(), dto.getIdentifierScheme(), dto.getDatasetType(), type, version);
        } else {
            logger.severe("Relation DTO must have either relatedDatasetPid or externalIdentifier");
            return null;
        }
    }

}
