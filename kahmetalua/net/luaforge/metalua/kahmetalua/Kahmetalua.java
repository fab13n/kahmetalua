package net.luaforge.metalua.kahmetalua;

import java.io.ByteArrayInputStream;
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
import se.krka.kahlua.vm.LuaPrototype;
import se.krka.kahlua.vm.LuaState;
import se.krka.kahlua.vm.LuaTable;

public class Kahmetalua {

	private final JavaFunction osgetenv = new JavaFunction() {
		public int call(LuaCallFrame frame, int arguments) {
			String var = (String) frame.get(0);
			String val = System.getenv(var);
			frame.push(val);
			return 1;
		}
	};
	
	private final JavaFunction readfile = new JavaFunction() {
		public int call(LuaCallFrame frame, int arguments) {

			if (arguments != 1 || !(frame.get(0) instanceof String))
				throw new LuaException("readfile() expects a string");

			StringBuffer content = new StringBuffer();
			try {
				String fileName = (String) frame.get(0);
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

		public int call(LuaCallFrame frame, int arguments) {
			if (arguments != 2 || !(frame.get(0) instanceof String) 
					|| !(frame.get(1) instanceof String))
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
	
	private final JavaFunction stringundump = new JavaFunction(){

		public int call(LuaCallFrame frame, int arguments) {
			if (arguments < 1 || !(frame.get(0) instanceof String))
				throw new LuaException("string.undump() expects a string");
			String chunk = (String) frame.get(0);
			ByteArrayInputStream stream = new ByteArrayInputStream(chunk.getBytes());
			try {
				LuaClosure f = LuaPrototype.loadByteCode(stream, frame.getEnvironment());
				frame.push(f);
				return 1;
			} catch (IOException e) {
				throw new LuaException ("Can't undump this string");
			}
		}
	};
	
	private void loadPrimitives()
	{
		LuaTable table = new LuaTable();		
		table.rawset("readfile",  readfile);
		table.rawset("writefile", writefile);
		table.rawset("osgetenv", osgetenv);
		state.getEnvironment().rawset("kahmetalua", table);	
		state.pcallByteCodeFromResource("io");
		
		set("os", "getenv", osgetenv);
		set("string", "undump", stringundump);
	}

	private void set(Object... objects) {
		Object key   = objects[objects.length-2];
		Object value = objects[objects.length-1];
		LuaTable t = state.getEnvironment();
		for (int i=0; i<objects.length-2; i++)
			t = (LuaTable) t.rawget(objects[i]);
		t.rawset(key, value);
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
		state.callByteCodeFromResource("metalua", (Object[]) args);
	}

	public static void main(String[] args) {
		System.out.println("Running metalua...");
		new Kahmetalua(new LuaState(System.out)).call(args);
	}	
}
