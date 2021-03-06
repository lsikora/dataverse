package edu.harvard.iq.dataverse.persistence.user;

/**
 * An entity that can be assigned roles to. In effect, this is either a
 * {@link User} or a {@link Group}, both of which have numerous subclasses.
 * Note that some {@code RoleAssignee}s, such as {@link GuestUser} and {@link AllUsers}, don't even live in the database.
 *
 * @author michael
 */
public interface RoleAssignee extends Comparable<RoleAssignee> {

    /**
     * A unique identifier of the role assignee within the installation. This
     * string is composed of a type prefix (@ for users, & for groups) and then
     * a unique suffix within the type.
     *
     * @return the unique identifier of the role assignee within the system.
     */
    String getIdentifier();

    RoleAssigneeDisplayInfo getDisplayInfo();

    default String getSortByString() {
        return this.getDisplayInfo().getTitle();
    }

    @Override
    default int compareTo(RoleAssignee o) {
        return this.getSortByString().toUpperCase().compareTo(o.getSortByString().toUpperCase());
    }

}
