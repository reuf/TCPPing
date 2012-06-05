package com.reuf.tcp.ping;
import java.sql.Timestamp;

public class Package {

	private int seqNumber = 0;
	private Timestamp timeStamp;
	private int secondCounter = 0;
	
	public Package(int seqNumber, Timestamp timeStamp, int secondCounter) {
		setSeqNumber(seqNumber);
		setTimeStamp(timeStamp);
		setSecondCounter(secondCounter);
	}

	public int getSeqNumber() {
		return seqNumber;
	}

	public void setSeqNumber(int seqNumber) {
		this.seqNumber = seqNumber;
	}

	public Timestamp getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(Timestamp timeStamp) {
		this.timeStamp = timeStamp;
	}

	public int getSecondCounter() {
		return secondCounter;
	}

	public void setSecondCounter(int secondCounter) {
		this.secondCounter = secondCounter;
	}
	
	@Override
	public String toString(){
		return "Seq number: "+getSeqNumber()+" Timestamp: "+getTimeStamp().toString()+" Second"+getSecondCounter();
	}
	
}