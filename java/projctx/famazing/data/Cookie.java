package projctx.famazing.data;

import projctx.famazing.data.Family.Membership;

/**
 * Class representing a cookie which the system use to perform certain operations and to decide with which screen to start the application.
 */
public class Cookie {

    private int userId;
    private String userName;
    private Membership userMembership;
    private int familyId;

    public Cookie(int userId, String userName, Membership userMembership, int familyId) {
        this.userId = userId;
        this.userName = userName;
        this.userMembership = userMembership;
        this.familyId = familyId;
    }

    public int getUserId() {
        return userId;
    }


    public String getUserName() {
        return userName;
    }


    public Membership getUserMembership() {
        return userMembership;
    }

    public int getFamilyId() {
        return familyId;
    }
}
