package com.reuf.tcp.ping;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class Pitcher {

	private static Timer timer = null;
	private static ReceiveTask recieveThread = null;
	private static byte[] buf;
	private static InetAddress address;
	private static int port;
	private static DatagramSocket socket;
	private static int numSendPacketsPerSec;
	private static int secondCounter = 0;
	private long timeOffsetNTP = 0;
	
	//Array list to hold all sent packets, Array list to hold all received packets, hash-map of arrival 
	//time relevant to sequence number
	private static ConcurrentHashMap<Integer, Package> sentPackets = new ConcurrentHashMap<Integer, Package>();
	private static ConcurrentHashMap<Integer, Package>  receivedPackets = new ConcurrentHashMap<Integer, Package>();
	private static ConcurrentHashMap<Integer, Timestamp> responseReceivedTimes = new ConcurrentHashMap<Integer, Timestamp>();
	
	private static int seqNum = 0;

	public Pitcher(int port, String hostname, int packetSize, int mps, long timeOffsetNTP) {
		Pitcher.port = port;
		try {
			address = InetAddress.getByName(hostname);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		buf = new byte[packetSize];

		try {
			socket = new DatagramSocket(port);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		numSendPacketsPerSec = mps;
		this.timeOffsetNTP = timeOffsetNTP;
		

		timer = new Timer();
		recieveThread = new ReceiveTask();
		recieveThread.start();
		timer.schedule(new SendTask(), 0, // initial delay
				1000/numSendPacketsPerSec); // subsequent rate
		timer.schedule(new StatisticsPerSecondTask (), 1000, // initial delay
				1 * 1000); // subsequent rate
	}

	class SendTask extends TimerTask {

		@Override
		public void run() {

				try {
					// Creating byte buffer to input necessary information
					ByteBuffer packet_info = ByteBuffer.allocate(Pitcher.buf.length);
					packet_info.putInt(seqNum);
					Timestamp ts = new Timestamp(new Date().getTime()+timeOffsetNTP);
					byte[] timestamp = ts.toString().getBytes();
					packet_info.putInt(timestamp.length);
					packet_info.put(timestamp);
					byte[] buf = packet_info.array();

					// Adding Package object to array list for statistics
					Package p = new Package(seqNum, ts, secondCounter); 
					sentPackets.put(seqNum,p);
					
					// Creating a datagram to send
					DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
					socket.send(packet);
				} catch (SocketException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				seqNum++;	
		}
	}

	class ReceiveTask extends Thread {

		@Override
		public void run() {
			while(true){
				// Creating a buffer array to place the received packet into
				byte[] buf = new byte[Pitcher.buf.length];
				
				// Create a datagram packet with buf 
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				
				// Receiving a pakcet through socket
				try {
					socket.receive(packet);					
					
					//Wrapping recieved data
					ByteBuffer packetReceivedInfo = ByteBuffer.wrap(buf);
	
					// Getting sequene number of the received datagram,
					Integer seqNumReceived = packetReceivedInfo.getInt();
					
					//Getting the time of reposne arrival and putting it into hashmap
					responseReceivedTimes.put(seqNumReceived, new Timestamp(new Date().getTime()+timeOffsetNTP));
					
					// Getting timestamp out of the received datagram
					Integer timestampLength = packetReceivedInfo.getInt();
					//System.out.println("ts len "+timestampLength);
					byte[] ts = new byte[timestampLength];
					packetReceivedInfo.get(ts, 0, timestampLength);				
					SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
					Timestamp timestamp = new Timestamp(s.parse(new String(ts)).getTime());
	
					//Creating Package object and adding it to array list
					Package p = new Package(seqNumReceived, timestamp, secondCounter); 
					receivedPackets.put(seqNumReceived,p);
					
				} catch (IOException e) {
					//e.printStackTrace();
				} catch (ParseException e) {
					//e.printStackTrace();
				}
			}

		}

	}

	class StatisticsPerSecondTask extends TimerTask {

		// There are 4 time-stamps for every sent and response packet
		
		// P1 - Moment when pitcher sends the packet - "ConcurrentHashMap<Integer, Package> sentPackets" keeps these records
		// C1 - Moment when catcher receives the packet - I assume that C1 ~= C2, because response preparation is negligible
		// C2 - Moment when catcher sends the response - "ConcurrentHashMap<Integer, Package>  receivedPackets" keeps these records
		// P2 - Moment when pitcher receives the response - "ConcurrentHashMap<Integer, Timestamp> responseReceivedTimes" keeps these recrods
		
		// A-B = C1 - P1 
		// B-A = P2 - C2
		// A-B-A(RTT) = P2 - P2
		
		@Override
		public void run() {

			secondCounter++;
			System.out.println(new SimpleDateFormat("hh:mm:ss").format(new Date())+": ");
			System.out.println("Sent packets in second before: " + getSentPacketsPerSec());
			System.out.println("Received packets in second before: " + getReceivedPacketsPerSec());
			System.out.println("Avreage A-B: "+avgAtoBperSec());
			System.out.println("Average B-A: "+avgBtoAperSec());
			System.out.println("Average A-B-A: "+avgAtoBtoAperSec());
			System.out.println("Total Max A-B: "+totalMaxAtoB());
			System.out.println("Total Max B-A: "+totalMaxBtoA());
			System.out.println("Total max A-B-A: "+totalMaxAtoBtoA());
			//System.out.println("Sent packets: " + sentPackets.toString());
			//System.out.println("Received packets" + receivedPackets.toString());
			if(secondCounter > 9){ //Schedule for 10 seconds, then cancel will terminate all scheduling
				System.out.println("-------------------------------------------------");
				System.out.println("Received packets: " + receivedPackets.size());
				System.out.println("Sent packets: " + sentPackets.size());
				System.out.println("Lost packets: " + (sentPackets.size()-receivedPackets.size()));
//				System.out.println("Total Max A-B: "+totalMaxAtoB());
//				System.out.println("Total Max B-A: "+totalMaxBtoA());
//				System.out.println("Total max A-B-A: "+totalMaxAtoBtoA());
				timer.cancel();
				recieveThread.interrupt();
				socket.close();
				System.exit(0);
			}
		}
		
		private int getSentPacketsPerSec() {
			int counter = 0;
			for (Package p : sentPackets.values()){
				if(p.getSecondCounter()==(secondCounter-1))
					counter++;
			}
			return counter;			
		}

		private int getReceivedPacketsPerSec() {
			int counter = 0;
			for (Package p : receivedPackets.values()){
				if(p.getSecondCounter()==(secondCounter-1))
					counter++;
			}
			return counter;		
		}
		
		private int avgAtoBperSec(){
			long sum=0;
			int counter=0;
			for (Integer i : receivedPackets.keySet()){
				if(receivedPackets.get(i).getSecondCounter() == (secondCounter-1)){
					sum +=  receivedPackets.get(i).getTimeStamp().getTime() - sentPackets.get(i).getTimeStamp().getTime();
					counter++;
//					System.out.println(receivedPackets.get(i).getTimeStamp()+"\t"+sentPackets.get(i).getTimeStamp());
				}	
			}
			if (counter==0)
				return 0;
			return (int)sum/counter;
		}

		private int avgBtoAperSec(){
			long sum=0;
			int counter=0;
			for (Integer i : receivedPackets.keySet()){
				if(receivedPackets.get(i).getSecondCounter() == (secondCounter-1)){
					sum +=  responseReceivedTimes.get(i).getTime() - receivedPackets.get(i).getTimeStamp().getTime();
					counter++;
				}	
			}
			if (counter==0)
				return 0;
			return (int)sum/counter;
		}
		
		private int avgAtoBtoAperSec(){
			long sum=0;
			int counter=0;
			for (Integer i : responseReceivedTimes.keySet()){
				if(sentPackets.get(i).getSecondCounter() == (secondCounter-1)){
					sum +=  responseReceivedTimes.get(i).getTime() - sentPackets.get(i).getTimeStamp().getTime();
					counter++;
				}	
			}
			if (counter==0)
				return 0;
			return (int)sum/counter;
		}
		
		private int totalMaxAtoB(){
			long max = 0;
			for (Integer i : receivedPackets.keySet()){
					long diff =  receivedPackets.get(i).getTimeStamp().getTime() - sentPackets.get(i).getTimeStamp().getTime();
					if (diff>max)
						max=diff;
			}
			return (int)max;
		}
		
		private int totalMaxBtoA(){
			long max = 0;
			for (Integer i : receivedPackets.keySet()){
				if(receivedPackets.get(i).getSecondCounter() == (secondCounter-1)){
					long diff =  responseReceivedTimes.get(i).getTime() - receivedPackets.get(i).getTimeStamp().getTime();
					if (diff>max)
						max=diff;
				}	
			}
			return (int)max;
		}
		
		private int totalMaxAtoBtoA(){
			long max = 0;
			for (Integer i : responseReceivedTimes.keySet()){
				if(sentPackets.get(i).getSecondCounter() == (secondCounter-1)){
					long diff =  responseReceivedTimes.get(i).getTime() - sentPackets.get(i).getTimeStamp().getTime();
					if (diff>max)
						max=diff;
				}	
			}
			return (int)max;
		}
		
	}	
}