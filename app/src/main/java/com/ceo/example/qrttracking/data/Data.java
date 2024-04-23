
package com.ceo.example.qrttracking.data;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


public class Data {
    @SerializedName("part_info")
    @Expose
    private ArrayList<PartInfo> partInfo;

    public ArrayList<PartInfo> getPartInfo() {
        return partInfo;
    }

    public void setPartInfo(ArrayList<PartInfo> partInfo) {
        this.partInfo = partInfo;
    }

}
