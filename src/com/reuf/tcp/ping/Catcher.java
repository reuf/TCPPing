package com.reuf.tcp.ping;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.Date;

public class Catcher extends Thread {

	private static DatagramSocket socket = null;
	private static byte[] buf = null;
	private static long timeOffsetNTP = 0;
	
	public Catcher(int port, String inetAddress, long timeOffsetNTP) throws IOException {
		super("CatcherThread");
		socket = new DatagramSocket(port, InetAddress.getByName(inetAddress));
		this.timeOffsetNTP = timeOffsetNTP;
		System.out.println("NTP Time offset: "+timeOffsetNTP);
		System.out.println("Waiting for requests. Serving time.");
	}

	public void run() {
		while (true) {
			try {
				// We create a maximum allowed size buffer
				buf = new byte[3000];				
			
				// Create a datagram packet to hold a received packet;
				DatagramPacket receivedPacket = new DatagramPacket(buf, buf.length);
				socket.receive(receivedPacket);

				// Wrapping a received buffer with ByteBuffer object for manipulation
				ByteBuffer packetRecievedInfo = ByteBuffer.wrap(buf);
				// Getting sequence number of received packet in order to set it to a response packet
				int seqNum = packetRecievedInfo.getInt();

				// Creating a new buffer for a packet to send
				ByteBuffer responsePacketBuffer = ByteBuffer.allocate(receivedPacket.getLength());
				responsePacketBuffer.putInt(seqNum);
				
				// Timestamping the buffer with NTP time
				Timestamp ts = new Timestamp(new Date().getTime()+timeOffsetNTP);
				byte[] timestamp = ts.toString().getBytes();
				responsePacketBuffer.putInt(timestamp.length);
				responsePacketBuffer.put(timestamp);
				//System.out.println(timestamp.length+"\t"+ts);
				
				buf = responsePacketBuffer.array();

				// Send the response to the client at "address" and "port"
				InetAddress address = receivedPacket.getAddress();
				int port = receivedPacket.getPort();
				DatagramPacket responsePacket = new DatagramPacket(buf, receivedPacket.getLength(), address, port);
				socket.send(responsePacket);
				
			} catch (IOException e) {
				e.printStackTrace();

			}
		}
//		socket.close(); //Basically never close the socket
	}	
}