package edu.harvard.iq.dataverse.persistence.user;

import java.io.Serializable;

/**
 * A user of the dataverse system. Intuitively a single real person in real
 * life, but some corner cases exist (e.g. {@link GuestUser}, who stands for
 * many people, or {@link PrivateUrlUser}, another virtual user).
 */
public interface User extends RoleAssignee, Serializable {

    boolean isAuthenticated();

    boolean isSuperuser();

}
