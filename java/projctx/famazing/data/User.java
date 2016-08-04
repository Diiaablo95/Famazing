package projctx.famazing.data;

import android.support.annotation.Nullable;

import java.sql.Date;
import projctx.famazing.data.Family.Membership;

/**
 * Model class for a single user.
 */
public class User {

    private Integer id;
    private String name;
    private Date birthday;
    private Membership membership;
    private int familyId;

    public User(@Nullable Integer id, String name, Date birthday, Membership membership, int familyId) {
        this.id = id;
        this.name = name;
        this.birthday = birthday;
        this.membership = membership;
        this.familyId = familyId;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setMembership(Membership membership) {
        this.membership = membership;
    }

    public Membership getMembership() {
        return membership;
    }

    public void setFamilyId(int familyId) {
        this.familyId = familyId;
    }

    public int getFamilyId() {
        return familyId;
    }
}