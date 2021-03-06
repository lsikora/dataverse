package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;

import java.util.logging.Logger;

@RequiredPermissions(value = {Permission.ManageDatasetPermissions, Permission.ManageMinorDatasetPermissions}, isAllPermissionsRequired = false)
public class GetPrivateUrlCommand extends AbstractCommand<PrivateUrl> {

    private static final Logger logger = Logger.getLogger(GetPrivateUrlCommand.class.getCanonicalName());

    private final Dataset dataset;

    public GetPrivateUrlCommand(DataverseRequest aRequest, Dataset theDataset) {
        super(aRequest, theDataset);
        dataset = theDataset;
    }

    @Override
    public PrivateUrl execute(CommandContext ctxt) {
        logger.fine("GetPrivateUrlCommand called");
        Long datasetId = dataset.getId();
        if (datasetId == null) {
            // Perhaps a dataset is being created in the GUI.
            return null;
        }
        return ctxt.privateUrl().getPrivateUrlFromDatasetId(datasetId);
    }

}
