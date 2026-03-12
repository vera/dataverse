/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.dataset.DatasetRelation;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.api.dto.DatasetRelationDTO;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.*;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

import java.util.List;
import java.util.logging.Logger;

/**
 *
 * Replaces all relations defined for a given dataset.
 * @author Vera Clemens
 */
@RequiredPermissions(Permission.EditDataset)
public class ReplaceDatasetRelationsCommand extends AbstractCommand<List<DatasetRelation>> {

    private static final Logger logger = Logger.getLogger(ReplaceDatasetRelationsCommand.class.getName());

    private final Dataset dataset;

    private final List<DatasetRelationDTO> relationDTOs;

    public ReplaceDatasetRelationsCommand(Dataset dataset, List<DatasetRelationDTO> relations, DataverseRequest aRequest) {
        super(aRequest, dataset);
        this.dataset = dataset;
        this.relationDTOs = relations;
    }

    @Override
    public List<DatasetRelation> execute(CommandContext ctxt) throws CommandException {
        try {
            try {
                List<DatasetRelation> relations = relationDTOs.stream().map(relationDTO -> {
                    Dataset d = relationDTO.getDatasetPid() != null ? ctxt.datasets().findByGlobalId(relationDTO.getDatasetPid()) : this.dataset;
                    return new DatasetRelation(
                        d,
                        ctxt.datasets().findByGlobalId(relationDTO.getRelatedDatasetPid()),
                        ctxt.datasetRelationTypes().findByName(relationDTO.getRelationTypeName()),
                        dataset
                    );
                }).toList();
                List<DatasetRelation> addedRelations = ctxt.datasetRelations().replaceAllDatasetRelationsFor(dataset, relations);
                // Reindex dataset to update relatedDatasetCount
                ctxt.index().asyncIndexDataset(dataset, true);
                return addedRelations;
            }
            catch (IllegalArgumentException e) {
                throw new CommandException("Failed to create dataset relations: one of the dataset PIDs is invalid", this);
            }
        } catch (Exception ex) {
            logger.severe("Failed to replace dataset relations: " + ex.getMessage());
            throw new CommandException("Failed to replace dataset relations", this);
        }
    }

}
