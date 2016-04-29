package com.capitalone.dashboard.model;

public class NameValueCount {
    private NameValue keyValue;
    private int count;
    private String name;
    private String value;
    
    public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public NameValueCount()
    {
    	
    }
    public NameValueCount(String name, String value, int count) {
    	this.keyValue = new NameValue(name, value);
    	this.count = count;
    }
    public NameValueCount(NameValue keyValue, int count) {
        this.keyValue = keyValue;
        this.count = count;
    }

    public NameValue getKeyValue() {
        return keyValue;
    }

    public void setKeyValue(NameValue keyValue) {
        this.keyValue = keyValue;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
