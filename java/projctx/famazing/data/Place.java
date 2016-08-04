package projctx.famazing.data;

import android.support.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;

/**
 * Model class for a single place.
 **/
public class Place {

    private Integer id;
    private String name;
    private transient LatLng coordinates;
    private int familyId;

    public Place(@Nullable Integer id, String name, LatLng coordinates, int familyId) {
        this.id = id;
        this.name = name;
        this.coordinates = coordinates;
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

    public void setCoordinates(LatLng coordinates) {
        this.coordinates = coordinates;
    }

    public void setFamilyId(int familyId) {
        this.familyId = familyId;
    }

    public String getName() {
        return name;
    }

    public LatLng getCoordinates() {
        return coordinates;
    }

    public int getFamilyId() {
        return familyId;
    }

}
