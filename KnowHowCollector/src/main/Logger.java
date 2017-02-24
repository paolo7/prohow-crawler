package main;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
	
	private static DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	
	protected static void log(String s){
		Date date = new Date();
		System.out.println(dateFormat.format(date)+"| "+s);
		
		try {
			FileWriter writer = new FileWriter("extractionLog.txt", true);
			writer.write(dateFormat.format(date)+"| "+s+ "\n");
			writer.flush();
			writer.close();
		} catch (IOException e) {
			System.out.println("Error saving log");
			e.printStackTrace();
		}
	}
	
	protected static void log_error(String s){
		Date date = new Date();
		System.out.println(dateFormat.format(date)+"| "+s);
		
		try {
			FileWriter writer = new FileWriter("extractionLog_error.txt", true);
			writer.write(dateFormat.format(date)+"| "+s+ "\n");
			writer.flush();
			writer.close();
		} catch (IOException e) {
			System.out.println("Error saving log");
			e.printStackTrace();
		}
	}
	
	protected static void log(String s, String instructions){
		Date date = new Date();
		System.out.println(dateFormat.format(date)+"| "+s);
		
		FileController.createFolders(new String[] {"data", instructions, "logs"});
		
		try {
			FileWriter writer = new FileWriter("data\\"+instructions+"\\logs\\extractionLog.txt", true);
			writer.write(dateFormat.format(date)+"| "+s+ "\n");
			writer.flush();
			writer.close();
		} catch (IOException e) {
			System.out.println("Error saving log");
			e.printStackTrace();
		}
		
	}


}
