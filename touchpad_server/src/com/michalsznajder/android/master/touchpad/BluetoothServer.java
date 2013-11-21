package com.michalsznajder.android.master.touchpad;

public class BluetoothServer{
	
	/**
	 * Running main server thread 
	 * 
	 */
	public static void main(String[] args) {
		Thread serverThread = new Thread(new ServerThread());
		serverThread.start();
	}
}
