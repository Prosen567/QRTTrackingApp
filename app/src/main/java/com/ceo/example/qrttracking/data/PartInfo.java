
package com.ceo.example.qrttracking.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PartInfo {

    @SerializedName("ps_no")
    @Expose
    private Integer psNo;
    @SerializedName("ps_name")
    @Expose
    private String psName;
    @SerializedName("lat")
    @Expose
    private String lat;
    @SerializedName("lng")
    @Expose
    private String lng;

    public Integer getPsNo() {
        return psNo;
    }

    public void setPsNo(Integer psNo) {
        this.psNo = psNo;
    }

    public String getPsName() {
        return psName;
    }

    public void setPsName(String psName) {
        this.psName = psName;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLng() {
        return lng;
    }

    public void setLng(String lng) {
        this.lng = lng;
    }

}
