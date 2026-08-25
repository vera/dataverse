/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.dataset.DatasetRelation;
import edu.harvard.iq.dataverse.dataset.DatasetRelationType;
import edu.harvard.iq.dataverse.dataset.ExternalDatasetRelation;
import edu.harvard.iq.dataverse.dataset.InternalDatasetRelation;
import edu.harvard.iq.dataverse.api.dto.DatasetRelationDTO;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.*;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.InvalidCommandArgumentsException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import jakarta.ejb.EJBException;

import java.util.List;

/**
 *
 * Replaces all relations defined for a given dataset.
 * @author Vera Clemens
 */
@RequiredPermissions(Permission.EditDataset)
public class ReplaceDatasetRelationsCommand extends AbstractCommand<List<DatasetRelation>> {
    private final DatasetVersion version;

    private final List<DatasetRelationDTO> relationDTOs;

    public ReplaceDatasetRelationsCommand(DatasetVersion version, List<DatasetRelationDTO> relations, DataverseRequest aRequest) {
        super(aRequest, version.getDataset());
        this.version = version;
        this.relationDTOs = relations;
    }

    @Override
    public List<DatasetRelation> execute(CommandContext ctxt) throws CommandException {
        try {
            DatasetVersion effectiveVersion;

            // If the version is not a draft, we need to create/get an edit version (draft)
            if (!version.isDraft()) {
                effectiveVersion = version.getDataset().getOrCreateEditVersion();
                ctxt.engine().submit(new UpdateDatasetVersionCommand(version.getDataset(), getRequest()));
            } else {
                effectiveVersion = version;
            }

            List<DatasetRelation> relations = relationDTOs.stream()
                    .map(dto -> ctxt.datasetRelations().fromDTO(dto, effectiveVersion))
                    .toList();
            if (relations.contains(null)) {
                throw new InvalidCommandArgumentsException(BundleUtil.getStringFromBundle("datasets.api.datasetRelation.error.invalid"), this);
            }
            if (ctxt.datasetRelations().containsDuplicates(relations)) {
                throw new InvalidCommandArgumentsException(BundleUtil.getStringFromBundle("datasets.api.datasetRelation.error.duplicate"), this);
            }
            List<DatasetRelation> addedRelations = ctxt.datasetRelations().replaceAllDatasetRelationsFor(effectiveVersion, relations);

            // Reindex dataset to update relatedDatasetCount
            ctxt.index().asyncIndexDataset(version.getDataset(), true);
            return addedRelations;
        } catch (EJBException ex) {
            throw new CommandException(BundleUtil.getStringFromBundle("datasets.api.datasetRelation.error.replace"), ex, this);
        }
    }

}
