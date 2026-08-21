package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.dataset.DatasetRelation;
import edu.harvard.iq.dataverse.api.dto.DatasetRelationDTO;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.*;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

import java.util.logging.Logger;

/**
 *
 * Creates a new relation for a given dataset.
 * @author Vera Clemens
 */
@RequiredPermissions(Permission.EditDataset)
public class CreateDatasetRelationCommand extends AbstractCommand<DatasetRelation> {

    private static final Logger logger = Logger.getLogger(CreateDatasetRelationCommand.class.getName());

    private final DatasetVersion version;

    private final DatasetRelationDTO relationDTO;

    public CreateDatasetRelationCommand(DatasetVersion version, DatasetRelationDTO relation, DataverseRequest aRequest) {
        super(aRequest, version.getDataset());
        this.version = version;
        this.relationDTO = relation;
    }

    @Override
    public DatasetRelation execute(CommandContext ctxt) throws CommandException {
        try {
            DatasetRelation relation = ctxt.datasetRelations().fromDTO(relationDTO, version);

            if (relation == null) {
                throw new CommandException("Dataset relation could not be created from the submitted data.", this);
            }

            DatasetRelation addedRelation = ctxt.datasetRelations().addDatasetRelation(relation);
            // Reindex dataset to update relatedDatasetCount
            ctxt.index().asyncIndexDataset(version.getDataset(), true);
            return addedRelation;
        }
        catch (Exception ex) {
            logger.severe("Failed to create dataset relation: " + ex.getMessage());
            throw new CommandException("Failed to create dataset relation", this);
        }
    }

}
