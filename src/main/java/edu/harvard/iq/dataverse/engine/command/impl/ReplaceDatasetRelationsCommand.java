/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.dataset.DatasetRelation;
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
            List<DatasetRelation> relations = relationDTOs.stream().flatMap(relationDTO -> {
                Dataset d = relationDTO.getDatasetPid() != null ? ctxt.datasets().findByGlobalId(relationDTO.getDatasetPid()) : this.dataset;
                Dataset relatedDataset = ctxt.datasets().findByGlobalId(relationDTO.getRelatedDatasetPid());

                if (relatedDataset == null) {
                    logger.severe("Failed to find related dataset with PID " + relationDTO.getRelatedDatasetPid());
                    return java.util.stream.Stream.empty();
                }

                return java.util.stream.Stream.of(new DatasetRelation(
                        d,
                        relatedDataset,
                        ctxt.datasetRelationTypes().findByName(relationDTO.getRelationTypeName()),
                        dataset
                ));
            }).toList();
            List<DatasetRelation> addedRelations = ctxt.datasetRelations().replaceAllDatasetRelationsFor(dataset, relations);
            // Reindex dataset to update relatedDatasetCount
            ctxt.index().asyncIndexDataset(dataset, true);
            return addedRelations;
        }
        catch (Exception ex) {
            logger.severe("Failed to replace dataset relations: " + ex.getMessage());
            throw new CommandException("Failed to replace dataset relations", this);
        }
    }

}
