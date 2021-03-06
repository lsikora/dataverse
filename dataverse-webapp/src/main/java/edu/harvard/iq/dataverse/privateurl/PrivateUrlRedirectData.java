package edu.harvard.iq.dataverse.privateurl;

import edu.harvard.iq.dataverse.persistence.user.PrivateUrlUser;

/**
 * PrivateUrlRedirectData is for the person clicking the Private URL link, who
 * is often a reviewer. In a browser, we need to set the session to the
 * PrivateUrlUser (who has ViewUnpublishedDataset and related permission on the
 * dataset and then redirect that user to the draft version of the dataset.
 */
public class PrivateUrlRedirectData {

    private final PrivateUrlUser privateUrlUser;
    private final String datasetPageToBeRedirectedTo;

    /**
     * @throws java.lang.Exception The reason why a PrivateUrlRedirectData
     *                             object could not be instantiated.
     */
    public PrivateUrlRedirectData(PrivateUrlUser privateUrlUser, String datasetPageToBeRedirectedTo) throws Exception {
        if (privateUrlUser == null) {
            throw new Exception("PrivateUrlUser cannot be null");
        }
        if (datasetPageToBeRedirectedTo == null) {
            throw new Exception("draftDatasetPageToBeRedirectedTo cannot be null");
        }
        this.privateUrlUser = privateUrlUser;
        this.datasetPageToBeRedirectedTo = datasetPageToBeRedirectedTo;
    }

    public PrivateUrlUser getPrivateUrlUser() {
        return privateUrlUser;
    }

    public String getDatasetPageToBeRedirectedTo() {
        return datasetPageToBeRedirectedTo;
    }

}
