package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.user.Permission;

/**
 * Creates a new harvested dataset. Harvested datasets are indexed locally, while their
 * data actually lives in a different Dataverse installation.
 *
 * @author michael
 */
@RequiredPermissions(Permission.AddDataset)
public class CreateHarvestedDatasetCommand extends AbstractCreateDatasetCommand {

    public CreateHarvestedDatasetCommand(Dataset theDataset, DataverseRequest aRequest) {
        super(theDataset, aRequest);
    }

    @Override
    protected void handlePid(Dataset theDataset, CommandContext ctxt) {
        theDataset.setGlobalIdCreateTime(getTimestamp());
    }

}
