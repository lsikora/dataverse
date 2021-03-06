package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileCategory;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowComment;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.logging.Logger;

/**
 * @author qqmyers
 * <p>
 * Adapted from UpdateDatasetVersionCommand
 */
@RequiredPermissions(Permission.EditDataset)
public class CuratePublishedDatasetVersionCommand extends AbstractDatasetCommand<Dataset> {

    private static final Logger logger = Logger.getLogger(CuratePublishedDatasetVersionCommand.class.getCanonicalName());
    final private boolean validateLenient = true;

    public CuratePublishedDatasetVersionCommand(Dataset theDataset, DataverseRequest aRequest) {
        super(aRequest, theDataset);
    }

    public boolean isValidateLenient() {
        return validateLenient;
    }

    @Override
    public Dataset execute(CommandContext ctxt)  {
        if (!getUser().isSuperuser()) {
            throw new IllegalCommandException("Only superusers can curate published dataset versions", this);
        }

        ctxt.permissions().checkEditDatasetLock(getDataset(), getRequest(), this);
        // Invariant: Dataset has no locks preventing the update
        DatasetVersion updateVersion = getDataset().getLatestVersionForCopy();

        // Copy metadata from draft version to latest published version
        updateVersion.setDatasetFields(getDataset().getEditVersion().initDatasetFields());

        validateOrDie(updateVersion, isValidateLenient());

        // final DatasetVersion editVersion = getDataset().getEditVersion();
        tidyUpFields(updateVersion);

        // Merge the new version into out JPA context
        ctxt.em().merge(updateVersion);


        TermsOfUseAndAccess oldTerms = updateVersion.getTermsOfUseAndAccess();
        TermsOfUseAndAccess newTerms = getDataset().getEditVersion().getTermsOfUseAndAccess();
        newTerms.setDatasetVersion(updateVersion);
        updateVersion.setTermsOfUseAndAccess(newTerms);
        //Put old terms on version that will be deleted....
        getDataset().getEditVersion().setTermsOfUseAndAccess(oldTerms);

        List<WorkflowComment> newComments = getDataset().getEditVersion().getWorkflowComments();
        if (CollectionUtils.isNotEmpty(newComments)) {
            for (WorkflowComment wfc : newComments) {
                wfc.setDatasetVersion(updateVersion);
            }
            updateVersion.getWorkflowComments().addAll(newComments);
        }


        // we have to merge to update the database but not flush because
        // we don't want to create two draft versions!
        Dataset tempDataset = ctxt.em().merge(getDataset());

        // Look for file metadata changes and update published metadata if needed
        for (DataFile dataFile : tempDataset.getFiles()) {
            List<FileMetadata> fmdList = dataFile.getFileMetadatas();
            FileMetadata draftFmd = dataFile.getLatestFileMetadata();
            FileMetadata publishedFmd = null;
            for (FileMetadata fmd : fmdList) {
                if (fmd.getDatasetVersion().equals(updateVersion)) {
                    publishedFmd = fmd;
                    break;
                }
            }
            boolean metadataUpdated = false;
            if (draftFmd != null && publishedFmd != null) {
                if (!draftFmd.getLabel().equals(publishedFmd.getLabel())) {
                    publishedFmd.setLabel(draftFmd.getLabel());
                    metadataUpdated = true;
                }
                String draftDesc = draftFmd.getDescription();
                String pubDesc = publishedFmd.getDescription();
                if ((draftDesc != null && (!draftDesc.equals(pubDesc))) || (draftDesc == null && pubDesc != null)) {
                    publishedFmd.setDescription(draftDesc);
                    metadataUpdated = true;
                }
                if (!draftFmd.getCategories().equals(publishedFmd.getCategories())) {
                    publishedFmd.setCategories(draftFmd.getCategories());
                    metadataUpdated = true;
                }
                FileTermsOfUse draftTermsOfUse = draftFmd.getTermsOfUse();
                FileTermsOfUse publishedTermsOfUse = publishedFmd.getTermsOfUse();
                if (!ctxt.files().isSameTermsOfUse(draftTermsOfUse, publishedTermsOfUse)) {
                    publishedTermsOfUse.setLicense(draftTermsOfUse.getLicense());
                    publishedTermsOfUse.setAllRightsReserved(draftTermsOfUse.isAllRightsReserved());
                    publishedTermsOfUse.setRestrictType(draftTermsOfUse.getRestrictType());
                    publishedTermsOfUse.setRestrictCustomText(draftTermsOfUse.getRestrictCustomText());
                    
                    metadataUpdated = true;
                }

                String draftProv = draftFmd.getProvFreeForm();
                String pubProv = publishedFmd.getProvFreeForm();
                if ((draftProv != null && (!draftProv.equals(pubProv))) || (draftProv == null && pubProv != null)) {
                    publishedFmd.setProvFreeForm(draftProv);
                    metadataUpdated = true;
                }

            } else {
                throw new IllegalCommandException("Cannot change files in the dataset", this);
            }
            if (metadataUpdated) {
                dataFile.setModificationTime(getTimestamp());
            }
            // Now delete filemetadata from draft version before deleting the version itself
            FileMetadata mergedFmd = ctxt.em().merge(draftFmd);
            ctxt.em().remove(mergedFmd);
            // including removing metadata from the list on the datafile
            draftFmd.getDataFile().getFileMetadatas().remove(draftFmd);
            tempDataset.getEditVersion().getFileMetadatas().remove(draftFmd);
            // And any references in the list held by categories
            for (DataFileCategory cat : tempDataset.getCategories()) {
                cat.getFileMetadatas().remove(draftFmd);
            }
        }

        // Update modification time on the published version and the dataset
        updateVersion.setLastUpdateTime(getTimestamp());
        tempDataset.setModificationTime(getTimestamp());

        Dataset savedDataset = ctxt.em().merge(tempDataset);

        // Flush before calling DeleteDatasetVersion which calls
        // PrivateUrlServiceBean.getPrivateUrlFromDatasetId() that will query the DB and
        // fail if our changes aren't there
        ctxt.em().flush();

        // Now delete draft version
        DeleteDatasetVersionCommand cmd;

        cmd = new DeleteDatasetVersionCommand(getRequest(), savedDataset);
        ctxt.engine().submit(cmd);
        // Running the command above reindexes the dataset, so we don't need to do it
        // again in here.

        // And update metadata at PID provider
        ctxt.engine().submit(
                new UpdateDvObjectPIDMetadataCommand(savedDataset, getRequest()));

        // Update so that getDataset() in updateDatasetUser will get the up-to-date copy
        // (with no draft version)
        setDataset(savedDataset);
        updateDatasetUser(ctxt);


        return savedDataset;
    }

}
