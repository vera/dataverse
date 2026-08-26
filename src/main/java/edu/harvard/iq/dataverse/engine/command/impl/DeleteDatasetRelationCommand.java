package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.dataset.DatasetRelation;
import edu.harvard.iq.dataverse.dataset.DatasetRelationIndexing;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.*;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

import java.util.List;

/**
 *
 * Deletes a dataset relation.
 * @author Vera Clemens
 */
@RequiredPermissions(Permission.EditDataset)
public class DeleteDatasetRelationCommand extends AbstractVoidCommand {

    private final DatasetRelation relation;

    public DeleteDatasetRelationCommand(DataverseRequest request, DatasetRelation relation) {
        super(request, relation.getDefinitionPoint().getDataset());
        this.relation = relation;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        ctxt.datasetRelations().deleteDatasetRelationById(relation.getId());
        DatasetRelationIndexing.schedule(ctxt, relation.getDefinitionPoint().getDataset(), List.of(relation));
    }
}
