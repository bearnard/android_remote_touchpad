package com.michalsznajder.android.master.touchpad;

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import javax.microedition.io.StreamConnection;

public class ControlThread implements Runnable {
	
	private StreamConnection mConnection;
	
	// touchpad events
	public static final int MESSAGE_MOVE = 7;
    public static final int MESSAGE_LEFT_CLICK = 8;
    public static final int MESSAGE_LEFT_CLICK_DOWN = 9;
    public static final int MESSAGE_LEFT_CLICK_UP = 10;
    public static final int MESSAGE_RIGHT_CLICK = 11;
    
    // setup
    private Robot mRobot;
    private InputStream inputStream;
    private ObjectInputStream objectInputStream;
    
	public ControlThread(StreamConnection connection)
	{
		mConnection = connection;
	}
	
	/**
	 * Fields initialization 
	 * 
	 */
    private void init() throws AWTException, IOException {
        mRobot = new Robot();
        System.out.println("Inicjalizacja java.awt.robot");
		inputStream = mConnection.openInputStream();
		objectInputStream = new ObjectInputStream(inputStream);
		System.out.println("Oczekiwanie na dyspozycje");    	
    }
    
    /**
	 * Running initialization procedure
	 * 
	 */
	@Override
	public void run() {
		try {
			init(); 
			while (true) {	
			Motion move = (Motion) objectInputStream.readObject();
			processCommand(move);
			}
        } catch (Exception e) {
    		e.printStackTrace();
    	}
	}
	
	
	/**
	 * Received movement information procesing
	 * @param movement information comming from client app
	 */
	private void processCommand(Motion move) {
		try {
			switch (move.type) {
			
			//touchpad
			case MESSAGE_MOVE:
		    	PointerInfo pointerInfo = MouseInfo.getPointerInfo();
	        		
	        		if (pointerInfo != null)
	        		{
	        			Point point = pointerInfo.getLocation();
	        			
	        			if (point != null)
	        			{
	        				int x = point.x + move.x;
	        				int y = point.y + move.y;
	        				mRobot.mouseMove(x, y);
	        			}
	        		}
		            
		    		break;
	    		
	    	case MESSAGE_LEFT_CLICK:
	    		mRobot.mousePress(InputEvent.BUTTON1_MASK);
	    		mRobot.mouseRelease(InputEvent.BUTTON1_MASK);
	    		
	    		break;
	    		
	    	case MESSAGE_LEFT_CLICK_DOWN:
	    		mRobot.mousePress(InputEvent.BUTTON1_MASK);
	    		
	    		break;
	    	
	    	case MESSAGE_LEFT_CLICK_UP:    		
	    		mRobot.mouseRelease(InputEvent.BUTTON1_MASK);
	    		
	    		break;
	    	
	    	case MESSAGE_RIGHT_CLICK:
	    		mRobot.mousePress(InputEvent.BUTTON3_MASK);
	    		mRobot.mouseRelease(InputEvent.BUTTON3_MASK);
	    		
	    		break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
