package projctx.famazing.data;

import android.support.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;

import java.io.Serializable;
import java.sql.Date;

/**
 * Model class for a single task.
 */
public class Task {

    private Integer id;
    private String name;
    private int giver;
    private Integer doer;
    private Integer completer;
    private Date deadline;
    private Integer locationId;
    private String description;
    private Status status;

    public Task(@Nullable Integer id, String name, int giver, @Nullable Integer doer, @Nullable Integer completer, @Nullable Date deadline, @Nullable Integer locationId, @Nullable String description, Status status) {
        this.id = id;
        this.name = name;
        this.giver = giver;
        this.doer = doer;
        this.completer = completer;
        this.deadline = deadline;
        this.locationId = locationId;
        this.description = description;
        this.status = status;
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

    public void setGiver(int giver) {
        this.giver = giver;
    }

    public int getGiver() {
        return giver;
    }

    public void setDoer(Integer doer) {
        this.doer = doer;
    }

    public Integer getDoer() {
        return doer;
    }

    public void setCompleter(Integer completer) {
        this.completer = completer;
    }

    public Integer getCompleter() {
        return completer;
    }

    public void setDeadline(Date deadline) {
        this.deadline = deadline;
    }

    public Date getDeadline() {
        return deadline;
    }

    public void setLocationId(Integer locationId) {
        this.locationId = locationId;
    }

    public Integer getLocationId() {
        return locationId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public enum Status {
        PENDING,
        COMPLETED,
        REFUSED
    }

}