package com.eyantra.android.speechbot;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.UUID;

import com.eyantra.android.speechbot.DeviceListActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

/**
 * @author govind
 * 
 */
public class SpeechBot extends Activity implements OnTouchListener,
		RecognitionListener {
	static {
		System.loadLibrary("pocketsphinx_jni");
	}
	/**
	 * Recognizer task, which runs in a worker thread.
	 */
	RecognizerTask rec;
	/**
	 * Thread in which the recognizer task runs.
	 */
	Thread rec_thread;
	/**
	 * Time at which current recognition started.
	 */
	Date start_date;
	/**
	 * Number of seconds of speech.
	 */
	float speech_dur;
	/**
	 * Are we listening?
	 */
	boolean listening;
	/**
	 * Progress dialog for final recognition.
	 */
	ProgressDialog rec_dialog;
	/**
	 * Performance counter view.
	 */
	TextView performance_text;
	/**
	 * Editable text view.
	 */
	EditText edit_text;
	/**
	 * Enable Debug logging
	 */
	private static final String TAG = "PocketSphinx Demo";
	private static final boolean D = true;
	private static UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	/**
	 * Intent request codes
	 */
	private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
	// private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
	private static final int REQUEST_ENABLE_BT = 3;

	/**
	 * Name of the connected device
	 */
	private String mConnectedDevice;
	/**
	 * Local Bluetooth adapter
	 */
	private BluetoothAdapter mBluetoothAdapter = null;
	/**
	 * Bluetooth Socket
	 */
	private BluetoothSocket mSocket = null;
	/**
	 * Associated Output Stream
	 */
	private OutputStream mmOutStream = null;

	/**
	 * Respond to touch events on the Speak button.
	 * 
	 * This allows the Speak button to function as a "push and hold" button, by
	 * triggering the start of recognition when it is first pushed, and the end
	 * of recognition when it is released.
	 * 
	 * @param v
	 *            View on which this event is called
	 * @param event
	 *            Event that was triggered.
	 */
	public NodeList listOfwords = null;
	public DocumentBuilderFactory docBuilderFactory = null;
	public DocumentBuilder docBuilder = null;
	public Document doc = null;

	 
	  public void initXML() { try{ docBuilderFactory =
	  DocumentBuilderFactory.newInstance(); docBuilder =
	  docBuilderFactory.newDocumentBuilder(); doc = docBuilder.parse (new
	  File("mapping.xml")); doc.getDocumentElement ().normalize (); listOfwords
	  = doc.getElementsByTagName("word"); } catch (Exception x){} }
	  
	  public String ValueOf(String search_str){ String value=""; try{ int
	  totalwords = listOfwords.getLength(); String pres_str = ""; Element
	  ele=null,string=null,valstring=null; NodeList
	  vallst=null,lst=null,text=null,valtext=null; Node pres=null; for(int s=0;
	  s<totalwords ; s++){ pres = listOfwords.item(s); if(pres.getNodeType() ==
	  Node.ELEMENT_NODE){ ele = (Element)pres; lst =
	  ele.getElementsByTagName("string"); string = (Element)lst.item(0); text =
	  string.getChildNodes(); pres_str =
	  ((Node)text.item(0)).getNodeValue().trim();
	  if(pres_str.equals(search_str)){ vallst =
	  ele.getElementsByTagName("value"); valstring = (Element)vallst.item(0);
	  valtext = valstring.getChildNodes(); value =
	  ((Node)valtext.item(0)).getNodeValue().trim();
	 
	  }
	  
	  } }
	  
	  } catch (Exception e){
	  
	  }
	  
	  return value; }
	 

	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			start_date = new Date();
			this.listening = true;
			this.rec.start();
			break;
		case MotionEvent.ACTION_UP:
			Date end_date = new Date();
			long nmsec = end_date.getTime() - start_date.getTime();
			this.speech_dur = (float) nmsec / 1000;
			if (this.listening) {
				Log.d(getClass().getName(), "Showing Dialog");
				this.rec_dialog = ProgressDialog.show(SpeechBot.this, "",
						"Recognizing speech...", true);
				this.rec_dialog.setCancelable(false);
				this.listening = false;
			}
			this.rec.stop();
			break;
		default:
			;
		}
		/* Let the button handle its own state */
		return false;
	}

	/** Called when the activity is first created. */
	public void onCreate(Bundle savedInstanceState) {
		initXML();
		// System.out.println("created\n");
		super.onCreate(savedInstanceState);
		if (D)
			Log.e(TAG, "+++ ON CREATE +++");
		setContentView(R.layout.main);
		this.rec = new RecognizerTask();
		this.rec_thread = new Thread(this.rec);
		this.listening = false;
		Button b = (Button) findViewById(R.id.Button01);
		b.setOnTouchListener(this);
		this.performance_text = (TextView) findViewById(R.id.PerformanceText);
		this.edit_text = (EditText) findViewById(R.id.EditText01);
		this.rec.setRecognitionListener(this);
		this.rec_thread.start();

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			Log.d(TAG, "connection failed");
			finish();
			return;
		} else {
			Log.d(TAG, "connection established");
		}

	}

	/** Called on application start */
	@Override
	public void onStart() {
		// System.out.println("started successfully\n");
		super.onStart();
		if (D)
			Log.e(TAG, "++ ON START ++");

		// Enable Bluetooth
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);

		}

		findDevice();
	}

	private int write(String x) {
		// if (!mSocket.isConnected())
		// return -1;
		try {
			 x = ValueOf(x);
			// x="F";
			// x += (char)10;
			byte[] byarr = x.getBytes("UTF_16LE");
			//byte[] byarr = new byte[2];
			//byarr[0] = (byte) 'F';
			mmOutStream = mSocket.getOutputStream();
			mmOutStream.write(byarr);

			Log.d(TAG, "message sent");
		} catch (IOException e) {
			findDevice();
		}
		return 0;
	}

	/** Called when partial results are generated. */
	public void onPartialResults(Bundle b) {
		final SpeechBot that = this;
		final String hyp = b.getString("hyp");
		that.edit_text.post(new Runnable() {
			public void run() {
				that.edit_text.setText(hyp);
			}
		});
	}

	/** Called with full results are generated. */
	public void onResults(Bundle b) {
		
		  final String hyp = b.getString("hyp"); final SpeechBot that = this;
		  this.edit_text.post(new Runnable() { public void run() {
		  that.edit_text.setText(hyp); Date end_date = new Date(); long nmsec =
		  end_date.getTime() - that.start_date.getTime(); float rec_dur =
		  (float) nmsec / 1000; that.performance_text.setText(String.format(
		  "%.2f seconds %.2f xRT", that.speech_dur, rec_dur /
		  that.speech_dur)); Log.d(getClass().getName(), "Hiding Dialog");
		  that.rec_dialog.dismiss(); } }); write(hyp);
		 
	}

	public void findDevice() {
		// Opens DeviceListActivity to choose device
		Intent deviceChoose;
		deviceChoose = new Intent(this, DeviceListActivity.class);
		startActivityForResult(deviceChoose, REQUEST_CONNECT_DEVICE_SECURE);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D)
			Log.d(TAG, "onActivityResult " + resultCode);
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE_SECURE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				connectDevice(data, true);
			}
			break;
		}
	}

	private void connectDevice(Intent data, boolean secure) {
		// System.out.println("connecting to device\n");
		Log.d(TAG, "trying");
		// Get the device MAC address
		mConnectedDevice = data.getExtras().getString(
				DeviceListActivity.EXTRA_DEVICE_ADDRESS);
		Log.e(TAG, "device id : " + mConnectedDevice);
		// Get the BLuetoothDevice object
		BluetoothDevice device = mBluetoothAdapter
				.getRemoteDevice(mConnectedDevice);
		// Attempt to connect to the device
		BluetoothSocket tmp = null;

		Method m;
		try {
			Log.d(TAG, "making");
			m = device.getClass().getMethod("createRfcommSocket",
					new Class[] { int.class });
			try {
				tmp = (BluetoothSocket) m.invoke(device, 1);
			} catch (IllegalArgumentException e) {
				Log.e(TAG, "IllegalArgumentException");
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				Log.e(TAG, "IllegalAccessException");
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				Log.e(TAG, "InvocationTargetException");
				e.printStackTrace();
			}
		} catch (SecurityException e) {
			Log.e(TAG, "SecurityException");
			// TODO Auto-generated catch block
		} catch (NoSuchMethodException e) {
			Log.e(TAG, "NoSuchMethodException");
			// TODO Auto-generated catch block
		}
		
		try {
			tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
		} catch (IOException e) {
			Log.e(TAG, "IOException");
		}

		mSocket = tmp;
		if (mSocket == null)
			Log.e(TAG, "OWW!! NULL!!");
		mBluetoothAdapter.cancelDiscovery();

		try {
			// Connect the device through the socket. This will block
			// until it succeeds or throws an exception
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mSocket.connect();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.e(TAG,"connect");
			if (mSocket == null)
				Log.e(TAG, "OWW!! NULL 2!!");
		} catch (IOException connectException) {
			// Unable to connect; close the socket and get out
			try {
				Log.e(TAG, "Closing Socket");
				mSocket.close();
			} catch (IOException closeException) {
			}
			Toast.makeText(this,
					"Could not connect to " + this.mConnectedDevice,
					Toast.LENGTH_LONG).show();
			Log.e(TAG, "could not connect");
			findDevice();
			return;
		}
		//write("F");
	}

	public void onError(int err) {
		final SpeechBot that = this;
		that.edit_text.post(new Runnable() {
			public void run() {
				that.rec_dialog.dismiss();
			}
		});
	}
}
