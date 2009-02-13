package net.luaforge.metalua.kahmetalua;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.LuaCallFrame;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaException;
import se.krka.kahlua.vm.LuaState;
import se.krka.kahlua.vm.LuaTable;

public class Kahmetalua {

	private final JavaFunction readfile = new JavaFunction() {
		@Override public int call(LuaCallFrame frame, int arguments) {

			if (arguments != 1 || !(frame.get(1) instanceof String))
				throw new LuaException("readfile() expects a string");

			StringBuffer content = new StringBuffer();
			try {
				String fileName = (String) frame.get(1);
				InputStream f = new FileInputStream (fileName);
				byte[] readbuffer = new byte[4096];
				int r;
				while (true) {
					r = f.read (readbuffer);
					if (r == -1) break; // EOF
					content.append (new String (readbuffer, 0, r));
				}
				f.close();
			} catch (IOException e) { return 0; }

			frame.push(content.toString());
			return 1;
		}
	};
	
	private final JavaFunction writefile = new JavaFunction(){

		@Override public int call(LuaCallFrame frame, int arguments) {
			if (arguments != 2 || !(frame.get(1) instanceof String) 
					|| !(frame.get(2) instanceof String))
				throw new LuaException("readfile() expects 2 strings");

			try {
				OutputStream f = new FileOutputStream((String) frame.get(1));
				byte[] data = ((String) frame.get(2)).getBytes();
				f.write(data);
				f.close();
			} 
			catch (FileNotFoundException e) { return 0; } 
			catch (IOException e) { return 0; }
			return 0;
		}		
	};
	
	private void loadPrimitives()
	{
		LuaTable table = new LuaTable();		
		table.rawset("readfile",  readfile);
		table.rawset("writefile", writefile);
		state.getEnvironment().rawset("kahmetalua", table);	
		state.loadByteCodeFromResource("io");
	}

	private final LuaState state;	
	
	public Kahmetalua(LuaState state) {
		this.state = state;
		loadPrimitives();
	}
		
	/**
	 * Call the metalua compiler, with args, as if they were passed from the
	 * command line.
	 */
	public void call(String[] args) {
		LuaClosure main = state.loadByteCodeFromResource("metalua");
		state.call(main, args);		
	}

	public static void main(String[] args) {
		System.out.println("Running metalua...");
		new Kahmetalua(new LuaState(System.out)).call(args);
	}	
}
