/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;

/**
 * @author Naomi
 */
// No permission needed to view published dvObjects
@RequiredPermissions({})
public class GetSpecificPublishedDatasetVersionCommand extends AbstractCommand<DatasetVersion> {
    private final Dataset ds;
    private final long majorVersion;
    private final long minorVersion;

    public GetSpecificPublishedDatasetVersionCommand(DataverseRequest aRequest, Dataset anAffectedDataset, long majorVersionNum, long minorVersionNum) {
        super(aRequest, anAffectedDataset);
        ds = anAffectedDataset;
        majorVersion = majorVersionNum;
        minorVersion = minorVersionNum;
    }

    @Override
    public DatasetVersion execute(CommandContext ctxt) {
        for (DatasetVersion dsv : ds.getVersions()) {
            if (dsv.isReleased()) {
                if (dsv.getVersionNumber().equals(majorVersion) && dsv.getMinorVersionNumber().equals(minorVersion)) {
                    return dsv;
                }
            }
        }
        return null;
    }

}