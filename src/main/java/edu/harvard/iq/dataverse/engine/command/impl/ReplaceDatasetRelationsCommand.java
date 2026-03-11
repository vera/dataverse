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

/**
 *
 * Replaces all relations defined for a given dataset.
 * @author Vera Clemens
 */
// the permission annotation is open, since this is a superuser-only command -
// and that's enforced in the command body:
@RequiredPermissions(Permission.EditDataset)
public class ReplaceDatasetRelationsCommand extends AbstractCommand<List<DatasetRelation>> {

    private final Dataset dataset;

    private final List<DatasetRelationDTO> relationDTOs;

    public ReplaceDatasetRelationsCommand(Dataset dataset, List<DatasetRelationDTO> relations, DataverseRequest aRequest) {
        super(aRequest, (Dataverse)null);
        this.dataset = dataset;
        this.relationDTOs = relations;
    }

    @Override
    public List<DatasetRelation> execute(CommandContext ctxt) throws CommandException {
        try {
            ctxt.datasetRelations().deleteDatasetRelationsFor(dataset);

            try {
                List<DatasetRelation> relations = relationDTOs.stream().map(relationDTO -> new DatasetRelation(
                        ctxt.datasets().findByGlobalId(relationDTO.getDatasetPid()),
                        ctxt.datasets().findByGlobalId(relationDTO.getRelatedDatasetPid()),
                        ctxt.datasetRelationTypes().findByName(relationDTO.getRelationTypeName()),
                        dataset
                )).toList();
                return ctxt.datasetRelations().addDatasetRelations(relations);
            }
            catch (IllegalArgumentException e) {
                throw new CommandException("Failed to create dataset relations: one of the dataset PIDs is invalid or the relation type is null", this);
            }
        } catch (Exception ex) {
            throw new CommandException("Failed to replace dataset relations", this);
        }
    }

}
