package com.michalsznajder.android.master.touchpad;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.michalsznajder.android.master.R;

public class MainActivity extends Activity {
	
	// App layout
	private TextView mTitle;
	private ImageView mImageView;
    private Button mLeftClick;
    private Button mRightClick;
	
	// BluetoothConnectionService 
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    
    private String mConnectedDeviceName = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothConnectionService mCommandService = null;
    
    // Touchpad
    public static final int MESSAGE_MOVE = 7;
    public static final int MESSAGE_LEFT_CLICK = 8;
    public static final int MESSAGE_LEFT_CLICK_DOWN = 9;
    public static final int MESSAGE_LEFT_CLICK_UP = 10;
    public static final int MESSAGE_RIGHT_CLICK = 11;
    
    private float movementDownX;
	private float movementDownY;
    private float movementPrevX;
    private float movementPrevY;
    private float movementResultX;
    private float movementResultY;
    
    private SharedPreferences preferences;
    private Vibrator vibrator;
    private boolean pressed;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Layout settings
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
        this.preferences = PreferenceManager.getDefaultSharedPreferences(this);
        
        // Interface elements
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
        mTitle = (TextView) findViewById(R.id.title_right_text);
        
        mImageView = (ImageView) findViewById(R.id.touchView);
        mLeftClick = (Button) findViewById(R.id.leftClick);
        mRightClick = (Button) findViewById(R.id.rightClick);
        
        // Touchpad settings
        pressed = false;
    	vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    	
    	// Bluetooth
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        // Touchpad listeners
        mImageView.setOnTouchListener(new OnTouchListener() {
    	    @Override
    	    public boolean onTouch(View view, MotionEvent event) {

    			switch (event.getAction())
    			{	
	    			case MotionEvent.ACTION_DOWN:
					{
						touchpadOnActionDown(event);
						break;
					}
						
    				case MotionEvent.ACTION_MOVE:
    				{    					
    					touchpadOnActionMove(event);
    		         	break;
    				}
    					
    				case MotionEvent.ACTION_UP:
    				{	
    					touchpadOnActionUp(event);
    					break;
    				}
    
    				default:
    					break;
    			}
    			return true;
    	    }
    	});
    
        mLeftClick.setOnTouchListener(new OnTouchListener() {
    	    @Override
    	    public boolean onTouch(View view, MotionEvent event) {
				switch (event.getAction())
				{
					case MotionEvent.ACTION_DOWN:
					{
						leftClickOnActionDown(event);
						break;
					}
					
					case MotionEvent.ACTION_MOVE:
					{
						leftClickOnActionMove(event);
						break;
					}
						
					case MotionEvent.ACTION_UP:
					{
						leftClickOnActionUp(event);
						break;
					}
						
					default:
						break;
				}
    			return false;
    	    }
    	});      
    	
        mRightClick.setOnTouchListener(new OnTouchListener() {
    	    @Override
    	    public boolean onTouch(View view, MotionEvent event) {
    	    	rightClick(event);
    	    	return false;
    	    }
    	});
    }
    
    
    // Bluetooth
    @Override
	protected void onStart() {
		super.onStart();
		
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		}
		else {
			if (mCommandService==null)
				setupCommand();
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		if (mCommandService != null) {
			if (mCommandService.getState() == BluetoothConnectionService.STATE_NONE) {
				mCommandService.start();
			}
		}
	}

	private void setupCommand() {
        mCommandService = new BluetoothConnectionService(this, mHandler);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if (mCommandService != null)
			mCommandService.stop();
	}
	
	private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }
	
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                case BluetoothConnectionService.STATE_CONNECTED:
                    mTitle.setText(R.string.title_connected_to);
                    mTitle.append(mConnectedDeviceName);
                    break;
                case BluetoothConnectionService.STATE_CONNECTING:
                    mTitle.setText(R.string.title_connecting);
                    break;
                case BluetoothConnectionService.STATE_LISTEN:
                case BluetoothConnectionService.STATE_NONE:
                    mTitle.setText(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_DEVICE_NAME:
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            if (resultCode == Activity.RESULT_OK) {
                String address = data.getExtras()
                                     .getString(BluetoothListActivity.EXTRA_DEVICE_ADDRESS);
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                mCommandService.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            if (resultCode == Activity.RESULT_OK) {
                setupCommand();
            } else {
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.scan:
        	Intent serverIntent = new Intent(this, BluetoothListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
        case R.id.discoverable:
            ensureDiscoverable();
            return true;
        }
        return false;
    }
	
	// Sending touch and movement events to server
	
	private void sendMovePacket(int type, int x, int y) {
	    
		Motion motion = new Motion (type, x, y);
        
        mCommandService.write(motion);           
	}
	
	// Touchpad methods
	
    private void touchpadOnActionDown(MotionEvent event)
	{
		movementDownX = event.getRawX();
		movementDownY = event.getRawY();
		movementPrevX = event.getRawX();
		movementPrevY = event.getRawY();
		movementResultX = 0;
		movementResultY = 0;
	}
    
    private void touchpadOnActionMove(MotionEvent event) {
		
		float movementRawX = event.getRawX() - movementPrevX;
		float movementRawY = event.getRawY() - movementPrevY;
		movementRawX = (float) ((Math.pow(Math.abs(movementRawX), 1.5f) * Math.signum(movementRawX))) * 0.5f;
		movementRawY = (float) ((Math.pow(Math.abs(movementRawY), 1.5f) * Math.signum(movementRawY))) * 0.5f;
		int movementXFinal = Math.round(movementRawX);
		int movementYFinal = Math.round(movementRawY);
		if (movementXFinal != 0 || movementYFinal != 0)
		{
			sendMovePacket(MESSAGE_MOVE, movementXFinal, movementYFinal);
		}
		movementPrevX = event.getRawX();
		movementPrevY = event.getRawY();
	
	}
    
    private void touchpadOnActionUp(MotionEvent event)
	{
		if (event.getEventTime() - event.getDownTime() < 150 && checkIfMovenOnTouchpad(event) <= 12.0f)
		{
			if (mLeftClick.isPressed())
			{
				//nie robi nic
			}
			else
			{
				if (!pressed)
				{
					sendMovePacket(MESSAGE_LEFT_CLICK, 0, 0);
					this.vibrate(50);
				}
				else
				{
					this.pressed = false;
					sendMovePacket(MESSAGE_LEFT_CLICK_UP, 0, 0);
					this.vibrate(50);
					
				}	
			}
		}
	}
  
    private void leftClickOnActionDown(MotionEvent event)
	{
		if (!this.pressed)
		{
			sendMovePacket(MESSAGE_LEFT_CLICK_DOWN, 0, 0);
			
			mLeftClick.setPressed(true);
			this.vibrate(50);	
		}
		else
		{
			this.pressed = false;
		}
	}
	
	private void leftClickOnActionMove(MotionEvent event)
	{
		if (!this.pressed && event.getEventTime() - event.getDownTime() >= 250)
		{
			this.pressed = true;			
			this.vibrate(100);
		}
	}
	
	private void leftClickOnActionUp(MotionEvent event)
	{
		if (!this.pressed)
		{
			sendMovePacket(MESSAGE_LEFT_CLICK_UP, 0, 0);
			this.mLeftClick.setPressed(false);
		}
	}
	
    private void rightClick(MotionEvent event)
	{
    	if (this.mRightClick.isPressed())
		{
			//nie robimy nic
		}
		else
		{
			sendMovePacket(MESSAGE_RIGHT_CLICK, 0, 0);
			this.vibrate(50);
		}
	}
	
    
    private double checkIfMovenOnTouchpad(MotionEvent event)
	{
		return Math.abs((event.getRawX()-this.movementDownX)+(event.getRawY() - this.movementDownY));
				
	}

    public void vibrate(long l)
	{
		if (this.preferences.getBoolean("feedback_vibration", true))
		{
			this.vibrator.vibrate(l);
		}
	}
	    
}
	
	
	 
