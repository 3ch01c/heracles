package com.wernicke.android.utils.RootTools;

import com.stericson.RootTools.execution.Command;

public class ResultCommand extends Command {
	protected String result = null;

	public ResultCommand(int id, int timeout, String command) {
		super(id, timeout, command);
	}

	public ResultCommand(int id, String command) {
		super(id, command);
	}

	@Override
	public void output(int id, String line) {
		// checksum should be the first thing in the output
		result = line;
	}
	
	public String getResult() {
		return result;
	}

	@Override
	public void commandCompleted(int arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void commandOutput(int arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void commandTerminated(int arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

}
