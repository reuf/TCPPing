package com.reuf.tcp.ping;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.TypeHandler;

public class CLITest {
	private static Options options = null;
	// Create a Parser
	private static CommandLineParser parser = new PosixParser();
	//Create handler for numbers
	private static TypeHandler numberHandler = new TypeHandler(); 
	
	public static void main(String[] args) throws Exception {

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
		
		// Parse the program arguments
		CommandLine commandLine = parser.parse( options, args );

		// Set the appropriate variables based on supplied options
		if( commandLine.hasOption('c') ) {
		
			System.out.println("aha");
			
			if( commandLine.hasOption("port") ) {
				numberHandler.createNumber(commandLine.getOptionValue("port"));
				System.out.println(commandLine.getOptionValue("port"));
			}
			
			if( commandLine.hasOption("bind") ) {
				System.out.println(commandLine.getOptionValue("bind"));
			}
			
		}
		
		if( commandLine.hasOption('p') ) {
			
			System.out.println("aha");
			
			if( commandLine.hasOption("port") ) {
				numberHandler.createNumber(commandLine.getOptionValue("port")).intValue();
				System.out.println(commandLine.getOptionValue("port"));
			}
			
			if( commandLine.hasOption("mps") ) {
				numberHandler.createNumber(commandLine.getOptionValue("mps")).intValue();
				System.out.println(commandLine.getOptionValue("mps"));
			}
			
			if( commandLine.hasOption("size") ) {
				numberHandler.createNumber(commandLine.getOptionValue("mps")).intValue();
				System.out.println(commandLine.getOptionValue("size"));
			}
			
			if (commandLine.hasOption("hostname") ){
				System.out.println(commandLine.getOptionValue("hostname"));
			}
//			if (commandLine.getArgs().length==1)
//			hostname = commandLine.getArgs()[0];
		}			
	}
}
