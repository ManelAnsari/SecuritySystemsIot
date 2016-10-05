package com.example.securitysystem;

import android.app.Activity;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;
    String deviceName = "HC-06";
    TextView textArea;
    Button stopAlarm;
    Button open;
    Button close;
    int data;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		stopAlarm = (Button)findViewById(R.id.stopAlarm);
		stopAlarm.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
                try {
                    data = 1;
                    sendData();
                    stopAlarm.setEnabled(false);

                } catch (Exception ex) {
                	Toast.makeText(getApplicationContext(), "Can't close alarm!", Toast.LENGTH_SHORT).show();
                }					
			}
		});
		open=(Button)findViewById(R.id.open);
		open.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
                findBT();
                try {
                    openBT();                                 
                }catch(Exception ex){
                	Toast.makeText(getApplicationContext(), "Could not open Bluetooth Device", Toast.LENGTH_SHORT).show();
                }	
				
			}
		});
		close = (Button)findViewById(R.id.close);
		close.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {			
                try{
                    closeBT();                    
                }
                catch (Exception ex) {
                	Toast.makeText(getApplicationContext(), "Could not close connection", Toast.LENGTH_SHORT).show();
                }
			}
		});
		textArea= (TextView)findViewById(R.id.textArea);		

	}
    void findBT()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        	Toast.makeText(getApplicationContext(), "No bluetooth adapter available", Toast.LENGTH_SHORT).show();        	

        if(!mBluetoothAdapter.isEnabled()){
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals(deviceName)) {
                    mmDevice = device;
                    break;
                }
            }
        }
        if(mmDevice != null)
        	Toast.makeText(getApplicationContext(), "Bluetooth Device Found", Toast.LENGTH_SHORT).show();
    }

    void openBT() throws IOException
    {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();
        beginListenForData();
    	Toast.makeText(getApplicationContext(), "Bluetooth Opened", Toast.LENGTH_SHORT).show();
    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 10; //ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[2048];
        workerThread = new Thread(new Runnable(){
            public void run(){
                while(!Thread.currentThread().isInterrupted() && !stopWorker){
                    try{
                        int bytesAvailable = mmInputStream.available();                    
                        if(bytesAvailable > 0){                   
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++){
                                byte b = packetBytes[i];
                                if(b == delimiter){
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    handler.post(new Runnable(){
                                        public void run(){                                        
                                            textArea.setText(textArea.getText()+data+"\n");                                            
                                            stopAlarm.setEnabled(true);
                                        }
                                    });
                                }
                                else
                                    readBuffer[readBufferPosition++] = b;
                            }
                        }
                    }
                    catch (IOException ex) {
                    	Log.d("no", "no");
                        stopWorker = true;
                    }
                }
            }
        });
        workerThread.start();
    }

    void sendData() throws IOException {    	
        String msg = Integer.toString(data);
        msg += "\n";
        mmOutputStream.write(msg.getBytes());
        Toast.makeText(getApplicationContext(), "Data Sent", Toast.LENGTH_SHORT).show();
    }

    void closeBT() throws IOException {
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        Toast.makeText(getApplicationContext(), "Bluetooth Closed", Toast.LENGTH_SHORT).show();
    }
}

