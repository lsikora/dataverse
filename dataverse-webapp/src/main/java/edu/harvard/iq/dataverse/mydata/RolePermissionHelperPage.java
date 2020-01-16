package edu.harvard.iq.dataverse.mydata;

import edu.harvard.iq.dataverse.DatasetPage;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRolePermissionHelper;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import org.omnifaces.cdi.ViewScoped;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.inject.Named;

import java.util.List;
import java.util.logging.Logger;


/**
 * @author rmp553
 */
@ViewScoped
@Named("RolePermissionHelperPage")
public class RolePermissionHelperPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(DatasetPage.class.getCanonicalName());

    @Inject
    DataverseSession session;

    @EJB
    DataverseRoleServiceBean dataverseRoleService;
    @EJB
    RoleAssigneeServiceBean roleAssigneeService;


    private DataverseRolePermissionHelper rolePermissionHelper;// = new DataverseRolePermissionHelper();


    public String init() {
        // msgt("_YE_OLDE_QUERY_COUNTER_");  // for debug purposes

        List<DataverseRole> roleList = dataverseRoleService.findAll();
        rolePermissionHelper = new DataverseRolePermissionHelper(roleList);


        List<String> dtypes = MyDataFilterParams.defaultDvObjectTypes;
        //List<String> dtypes = Arrays.asList(DvObject.DATAFILE_DTYPE_STRING, DvObject.DATASET_DTYPE_STRING);
        //DvObject.DATAFILE_DTYPE_STRING, DvObject.DATASET_DTYPE_STRING, DvObject.DATAVERSE_DTYPE_STRING

        //List<String> dtypes = new ArrayList<>();

        return null;
    }


    public DataverseRolePermissionHelper getRolePermissionHelper() {
        return this.rolePermissionHelper;
    }

    private void msg(String s) {
        System.out.println(s);
    }

    private void msgt(String s) {
        msg("-------------------------------");
        msg(s);
        msg("-------------------------------");
    }

}
