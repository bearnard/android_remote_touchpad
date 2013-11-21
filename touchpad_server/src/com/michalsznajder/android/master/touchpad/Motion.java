package com.michalsznajder.android.master.touchpad;

import java.io.Serializable;

public class Motion implements Serializable {

	private static final long serialVersionUID = 5769191836884262635L;
	
	public Motion(int type, int x, int y) {
        this.type = type;
		this.x = x;
        this.y = y;
    }
	
	public int type;
    public int x;
    public int y;
}
