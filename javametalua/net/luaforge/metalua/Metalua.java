package net.luaforge.metalua;

import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaState;

public class Metalua {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Running metalua...");
		LuaState state = new LuaState(System.out);
		LuaClosure main = state.loadByteCodeFromResource("metalua", state.getEnvironment());
		state.call(main, args);		
	}

}
