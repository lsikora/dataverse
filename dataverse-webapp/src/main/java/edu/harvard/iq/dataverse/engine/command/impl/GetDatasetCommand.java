/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.user.Permission;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author Naomi
 */
// no annotations here, since permissions are dynamically decided
public class GetDatasetCommand extends AbstractCommand<Dataset> {

    private final Dataset ds;

    public GetDatasetCommand(DataverseRequest aRequest, Dataset anAffectedDataset) {
        super(aRequest, anAffectedDataset);
        ds = anAffectedDataset;
    }

    @Override
    public Dataset execute(CommandContext ctxt) {
        return ds;
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        return Collections.singletonMap("",
                                        ds.isReleased() ? Collections.emptySet()
                                                : Collections.singleton(Permission.ViewUnpublishedDataset));
    }

}
