package com.reuf.tcp.ping;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.TypeHandler;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

public class TCPPing {

	//Pitcher arguments
	private static int portP; 
	private static String hostname; 
	private static int packetSize = 300; 
	private static int mps = 1; 
	
	//Catcher arguments
	private static String bindAddress;
	private static int portC;
	
	//Using commons.cli 1.2 library for command line argument parsing
	//Creating options holder, parser, handler for numbers
	private static Options options = null;
	private static CommandLineParser parser = new PosixParser();
	private static CommandLine commandLine;
	
	public static void main(String[] args){

		//Creating an options object which will hold all of our options
		options = new Options( );
		
		//This assures that either pitcher or catcher can be chosen, but not both
		Option pitcher = new Option( "p", "pitcher", false, "Mode: Pitcher" );
		Option catcher = new Option( "c", "catcher", false, "Mode: Catcher" );
		OptionGroup group = new OptionGroup();
		group.addOption(pitcher);
		group.addOption(catcher);
		options.addOptionGroup(group);
		
		//Adding all the possible options
		options.addOption("bind", "bind", true, "Bind socket address for listening");
		options.addOption("port", "port", true, "" );
		options.addOption("mps", "mps", true, "Messages per second");
		options.addOption("size", "size", true, "Size of the packets");
		options.addOption("", "hostname", true, "Hostname");
		options.addOption("help", "help", true, "Call on helper method");
		
		try {
			// Parse the program arguments
			commandLine = parser.parse( options, args );
	
			// Call on helper if command line has no arguments or there is -help parameter  
			if( commandLine.hasOption("help") || commandLine.getOptions().length == 0){
				helper();
			}
			
			// Set the appropriate variables based on supplied options			
			if( commandLine.hasOption('c') ) {
				
				if( commandLine.hasOption("port") ) {
					int port = TypeHandler.createNumber(commandLine.getOptionValue("port")).intValue();
					if (port >1023 || port <65535) 
					portC = TypeHandler.createNumber(commandLine.getOptionValue("port")).intValue();
					System.out.println(commandLine.getOptionValue("port"));
				}
				
				if( commandLine.hasOption("bind") ) {
					bindAddress = commandLine.getOptionValue("bind");
					System.out.println(commandLine.getOptionValue("bind"));
				}
				
				new Catcher(portC, bindAddress, calculateTimeOffsetNTP()).start();
				
			}
			
			if( commandLine.hasOption('p') ) {
				
				if( commandLine.hasOption("port") ) {
					portP = TypeHandler.createNumber(commandLine.getOptionValue("port")).intValue();
					System.out.println(commandLine.getOptionValue("port"));
				}
				
				if( commandLine.hasOption("mps") ) {
					mps = TypeHandler.createNumber(commandLine.getOptionValue("mps")).intValue();
					System.out.println(commandLine.getOptionValue("mps"));
				}
				
				if( commandLine.hasOption("size") ) {
					
					packetSize = TypeHandler.createNumber(commandLine.getOptionValue("size")).intValue();					 
					System.out.println(commandLine.getOptionValue("size"));
				}
				
				if (commandLine.hasOption("hostname") ){
					hostname = commandLine.getOptionValue("hostname");
					System.out.println(commandLine.getOptionValue("hostname"));
				}
				
				if (commandLine.getArgs().length==1)
					hostname = commandLine.getArgs()[0];
				else throw new Exception("Command arguments are not correctlly formed");
				
				new Pitcher(portP, hostname, packetSize, mps, calculateTimeOffsetNTP());				
				
			}
		}catch (ParseException e) {
			e.printStackTrace();
		}catch (IOException e){
			e.printStackTrace();
		}catch (Exception e){
			System.out.println("The enetered port is outside of allowed range (1024 - 65535)");
		}
	}

	//Helper method to call if user calls on program with no arguments or with java TCPPing -help
	private static void helper(){
		System.out
		.println("Catcher > Usage: java TCPPing –c –bind <ip-address> –port <port>\n"
				+ "Pitcher > Usage: java TCPPing –p –port <port> [–mps <num_of_mess>] [–size <packet_size>] <hostname>\n"
				+ "-p Pitcher mode, -c Cather mode;\n"
				+ "-port <port> [Pitcher] TCP socket port used for connect, [Catcher] TCP socket port"
				+ " used for listening;\n"
				+ "-bind <ip_address> [Catcher] TCP socket bind address used for listening;\n"
				+ "-mps <rate> [Pitcher] send rate defined as „messages per second“ Default: 1;\n"
				+ "-size <size> [Pitcher] Packet size, Minimum: 50, Maximum: 3000, Default: 300;\n"
				+ "<hostname> [Pitcher] name of the machine on which Catcher runs;\n");
		return;
	}
	
	//Network Time Protocol time offset (Internet connection needed)
	//Using commons-net-3.0.1 library for NTP
	private static long calculateTimeOffsetNTP() {
		double average=0;		
		for (int i=0;i<10;i++){
		
			NTPUDPClient client = new NTPUDPClient();
			// We want to timeout if a response takes longer than 2 seconds
			client.setDefaultTimeout(3000);
			try {
				client.open();
				try {
					InetAddress hostAddr = InetAddress
							.getByName("ba.pool.ntp.org");
					//System.out.println("> " + hostAddr.getHostName() + "/"
					//		+ hostAddr.getHostAddress());
					TimeInfo info = client.getTime(hostAddr);
					//processResponse(info);
					info.computeDetails();
					average += info.getOffset();
				} catch (IOException ioe) {
					//ioe.printStackTrace();
				}

			} catch (SocketException e) {
				//e.printStackTrace();
			}
			client.close();
		}
		average/=10;
		return (long) average;
		
	}
}