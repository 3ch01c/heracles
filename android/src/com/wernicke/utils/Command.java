package com.wernicke.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Command {
	private Runtime runtime;
	private Process process;
	private String command;
	private BufferedReader output;
	private StringBuffer result = new StringBuffer();
	
	public Command(String command) {
		this.runtime = Runtime.getRuntime();
		this.command = command;
	}
	
	public Command(String command, String user) {
		this.runtime = Runtime.getRuntime();
		this.command = "su " + user + " " + command;
	}
	
	public String getCommand() {
		return this.command;
	}
	
	public int exec() {
		try {
			this.process = this.runtime.exec(this.command);
			this.output = new BufferedReader(new InputStreamReader(process.getInputStream()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}
	
	public String getResult() {
		try {
			this.process.waitFor();
			String line;
			while((line = output.readLine()) != null) {
				result.append(line).append("\n");
			}
			return result.toString();
		} catch (InterruptedException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}