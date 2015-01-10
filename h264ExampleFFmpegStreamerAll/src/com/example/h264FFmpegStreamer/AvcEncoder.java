package com.example.h264FFmpegStreamer;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import android.util.Log;

import com.android.myffmpegx264lib.exampleEncoder;

public class AvcEncoder {

	// For logging
	private static final String TAG = "com.example.h264FFmpegStreamer.AvcEncoder";

	// Networking variables
	private int DATAGRAM_PORT = 4002;
	private int TCP_SERVER_PORT = DATAGRAM_PORT + 1;
	private static final int MAX_UDP_DATAGRAM_LEN = 1400;
	private InetAddress clientIp;
	private int clientPort;
	private static boolean isClientConnected = false;

	// FIFO queue
	private static final int MAX_BUFFER_QUEUE_SIZE = 1000000;
	private BufferQueue bufferQueue = new BufferQueue(MAX_BUFFER_QUEUE_SIZE);
	private byte[] outBytes = new byte[MAX_BUFFER_QUEUE_SIZE];
	private byte[] sendData = new byte[MAX_UDP_DATAGRAM_LEN];
	private int[] outFrameSize = new int[1];

	// Encoder
	private exampleEncoder encoder;

	// File variables
	File file = new File("/sdcard/sample_" + MainStreamerActivity.frameRate
			+ "_" + MainStreamerActivity.width + "_"
			+ MainStreamerActivity.height + "_" + MainStreamerActivity.bitrate
			+ "_" + MainStreamerActivity.maxBFrames + "_"
			+ MainStreamerActivity.gopSize + ".h264");
	FileOutputStream outStream = null;

	public AvcEncoder() {
		Log.i(TAG, "outputStream initialized");
		Thread udpThread = new Thread() {

			private DatagramPacket dp;
			private DatagramSocket ds;
			private int pCount = 0;

			@Override
			public void run() {
				try {
					ds = new DatagramSocket(DATAGRAM_PORT);
					dp = new DatagramPacket(sendData, sendData.length);
					ds.receive(dp);
					clientPort = dp.getPort();
					clientIp = dp.getAddress();
					ds.connect(dp.getAddress(), dp.getPort());
					Log.i(TAG, " Connected to: " + clientIp + ":" + clientPort);
					isClientConnected = true;

					while (isClientConnected) {

						if (MainStreamerActivity.getPreviewStatus()) {

							if(bufferQueue.getCount() > MAX_UDP_DATAGRAM_LEN ){
							bufferQueue.read(sendData, 0,
									MAX_UDP_DATAGRAM_LEN);
							DatagramPacket packet = new DatagramPacket(
									sendData, sendData.length, clientIp, clientPort);
							ds.send(packet);

							//Log.i("Packets",
							//		"Packet: " + pCount++ + " : "
							//				+ System.currentTimeMillis());
							}
						}
					}
					ds.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};

		udpThread.start();

		Thread tcpThread = new Thread() {

			private ServerSocket acceptSocket;

			@Override
			public void run() {
				try {
					acceptSocket = new ServerSocket(TCP_SERVER_PORT);
					Socket connectionSocket = acceptSocket.accept();
					BufferedReader inFromClient = new BufferedReader(
							new InputStreamReader(
									connectionSocket.getInputStream()));
					DataOutputStream outToClient = new DataOutputStream(
							connectionSocket.getOutputStream());
					String clientSentence = inFromClient.readLine();
					Log.i(TAG, "Received: " + clientSentence);
					isClientConnected = true;

					while (bufferQueue.read(sendData, 0, MAX_UDP_DATAGRAM_LEN) != 1) {
						outToClient.write(sendData, 0, sendData.length);
					}
					connectionSocket.close();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};

		tcpThread.start();
	}

	public void close() {
		try {
			encoder.close();
			encoder.delete();
			outStream.flush();
			outStream.close();
			isClientConnected = false;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// called from Camera.setPreviewCallbackWithBuffer(...) in other class

	public void setFFmpegEncoder(int width, int height, int fps, int bitrate,
			int maxBFrames, int gopSize) {
		encoder = new exampleEncoder();
		encoder.setBitrate(bitrate);
		encoder.setFps(fps);
		encoder.setGopSize(gopSize);
		encoder.setHeight(height);
		encoder.setWidth(width);
		encoder.setMaxBframes(maxBFrames);
		encoder.initialize();

		try {
			outStream = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void encodeFrame(byte[] inBytes, int counter) {

		if (isClientConnected) {

			// Log.i(TAG, "Frame sent: " + counter + " : " + inBytes.length +
			// " : "
			// + System.currentTimeMillis());

			encoder.video_encode(inBytes, inBytes.length, counter, outBytes,
					outFrameSize);

			// Log.i(TAG, "Frame Recv: " + counter + " : " + outFrameSize +
			// " : "
			// + System.currentTimeMillis());

			bufferQueue.append(outBytes, 0, outFrameSize[0]);
			
			/*try {
				outStream.write(outBytes, 0, outFrameSize[0]);
				Log.i("Frames", "Frame wrote: " + counter + " : " + outFrameSize
						+ " : " + System.currentTimeMillis());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/

		}
	}
}