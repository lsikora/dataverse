package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.persistence.group.ExplicitGroup;
import edu.harvard.iq.dataverse.persistence.user.Permission;

/**
 * @author michael
 */
@RequiredPermissions(Permission.ManageDataversePermissions)
public class DeleteExplicitGroupCommand extends AbstractVoidCommand {

    private final ExplicitGroup explicitGroup;

    public DeleteExplicitGroupCommand(DataverseRequest aRequest, ExplicitGroup anExplicitGroup) {
        super(aRequest, anExplicitGroup.getOwner());
        explicitGroup = anExplicitGroup;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) {
        ExplicitGroup merged = ctxt.em().merge(explicitGroup);

        // Remove this group from all explicit groups it belongs to.
        ctxt.em().createNativeQuery(
                "DELETE FROM explicitgroup_explicitgroup "
                        + "WHERE containedexplicitgroups_id=" + merged.getId()
        ).executeUpdate();

        ctxt.explicitGroups().removeGroup(merged);

    }

}
