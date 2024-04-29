package com.ceo.example.qrttracking.mapdirectiondata;

import com.google.gson.annotations.SerializedName;

public class OverviewPolyline{

	@SerializedName("points")
	private String points;

	public void setPoints(String points){
		this.points = points;
	}

	public String getPoints(){
		return points;
	}
}