package projctx.famazing.data;

import android.support.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;

import java.sql.Date;

/**
 * Model class for a single alert.
 */
public class Alert {

    private Integer id;
    private Integer userId;
    private Date alertDate;
    private LatLng userLocation;

    public Alert(@Nullable Integer id, Integer userId, Date date, LatLng location) {
        this.id = id;
        this.userId = userId;
        this.alertDate = date;
        this.userLocation = location;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUseId(Integer userId) {
        this.userId = userId;
    }

    public Date getAlertDate() {
        return alertDate;
    }

    public void setAlertDate(Date alertDate) {
        this.alertDate = alertDate;
    }

    public LatLng getUserLocation() {
        return userLocation;
    }

    public void setUserLocation(LatLng userLocation) {
        this.userLocation = userLocation;
    }

}
