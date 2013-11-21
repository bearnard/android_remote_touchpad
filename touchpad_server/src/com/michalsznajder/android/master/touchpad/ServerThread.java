package com.michalsznajder.android.master.touchpad;

import java.io.IOException;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

public class ServerThread implements Runnable{
	
	
	public ServerThread() {
	}
	
	/**
	 * Running thread waiting for connection 
	 * 
	 */
	@Override
	public void run() {
		waitingForConnection();		
	}
	
	/**
	 * Waiting for connection with client
	 * 
	 */
	private void waitingForConnection() {
		LocalDevice localDevice = null;
		StreamConnectionNotifier notifier;
		StreamConnection connection = null;
		
		try {
			localDevice = LocalDevice.getLocalDevice();
			localDevice.setDiscoverable(DiscoveryAgent.GIAC);
			
			UUID uuid = new UUID("1101", true);
			System.out.println(uuid.toString());
			
            String url = "btspp://localhost:" + uuid.toString() + ";name=BluetoothDevice";
            notifier = (StreamConnectionNotifier)Connector.open(url);
        } catch (BluetoothStateException e) {
        	System.out.println("Bluetooth nie jest w³¹czony.");
			e.printStackTrace();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		while(true) {
			try {
				System.out.println("Oczekiwanie na po³¹czenie...");
	            connection = notifier.acceptAndOpen();
	            
	            Thread processThread = new Thread(new ControlThread(connection));
	            processThread.start();
	            
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
	}
}
