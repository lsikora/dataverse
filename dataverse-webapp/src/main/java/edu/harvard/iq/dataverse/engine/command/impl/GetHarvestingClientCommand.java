package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;
import edu.harvard.iq.dataverse.persistence.user.Permission;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author Leonid Andreev
 */
// One can view the configuration of a Harvesting Client if and only if 
// they have the permission to view the dataverse that owns the harvesting
// client. And for a Dataverse, we cannot define the permission with a 
// @RequiredPermission annotation - because the decision has to be made dynamically: 
// Everybody can view a published Dataverse; otherwise, an explicit 
// ViewUnpublishedDataverse is needed. 
// This is defined in the getRequiredPermissions() method, below. 
public class GetHarvestingClientCommand extends AbstractCommand<HarvestingClient> {
    private final Dataverse ownerDataverse;
    private final HarvestingClient harvestingClient;

    public GetHarvestingClientCommand(DataverseRequest aRequest, HarvestingClient harvestingClient) {
        super(aRequest, harvestingClient.getDataverse());
        this.ownerDataverse = harvestingClient.getDataverse();
        this.harvestingClient = harvestingClient;
    }

    @Override
    public HarvestingClient execute(CommandContext ctxt) {
        if (ownerDataverse == null) {
            throw new IllegalCommandException("GetHarvestingClientCommand called on a null Dataverse object", this);
        }
        if (harvestingClient == null) {
            throw new IllegalCommandException("GetHarvestigClientCommand called on a null HarvestingClient object", this);
        }
        return harvestingClient;
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        return Collections.singletonMap("",
                                        ownerDataverse.isReleased() ? Collections.emptySet()
                                                : Collections.singleton(Permission.ViewUnpublishedDataverse));
    }
}