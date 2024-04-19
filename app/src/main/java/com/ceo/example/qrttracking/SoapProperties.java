package com.ceo.example.qrttracking;

public class SoapProperties {
	
	public String action_name="";
	public String method_name="";
	
	public void setActionName(String action)
	{
		
		
	//	Properties properties = new Properties();
	   
		this.action_name ="http://tempuri.org/"+action;
	}
	public void setMethodName(String method)
	{
		this.method_name =method;
	}
	public String getActionName()
	{
		return this.action_name ;
	}
	public String getMethodName()
	{
		return this.method_name;
	}

}
