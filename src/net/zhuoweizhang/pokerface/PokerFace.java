package net.zhuoweizhang.pokerface;

import java.lang.reflect.*;

import java.nio.ByteBuffer;

public class PokerFace {

	/** Page can be read.  */
	public static final int PROT_READ = 0x1;
	/** Page can be executed.  */
	public static final int PROT_WRITE = 0x2;
	/** Page can be executed.  */
	public static final int PROT_EXEC = 0x4;
	/** Page cannot be accessed.  */
	public static final int PROT_NONE = 0x0;

	/** Query parameter for the memory page size, used for sysconf. */
	public static final int _SC_PAGESIZE = 0x0027;

	/**
	 * Changes the protection on a page of memory.
	 * @param addr The starting address of the memory. Must be page aligned.
	 * @param len The length of memory to change. Also page aligned.
	 * @param prot The new protection: the PROT_* parameters ORed together
	 */

	public static native int mprotect(long addr, long len, int prot);

	/** Get system configuration. Is here because the Libcore version of this is not available on (some old?) Gingerbread versions */
	public static native long sysconf(int name);

	/** Creates a direct ByteBuffer to an area of memory using a libcore implementation. */

	public static ByteBuffer createDirectByteBuffer(long address, long length) throws Exception {
		if (android.os.Build.VERSION.SDK_INT >= 18) { //Jelly Bean 4.3 simplified byte buffer creation
			try {
				return createDirectByteBufferNew(address, length);
			} catch (NoSuchMethodException nsme) {
			}
		}
		Constructor cons = Class.forName("java.nio.ReadWriteDirectByteBuffer").getDeclaredConstructor(Integer.TYPE, Integer.TYPE);
		cons.setAccessible(true);
		return (ByteBuffer) cons.newInstance((int) address, (int) length);
	}

	private static ByteBuffer createDirectByteBufferNew(long address, long length) throws Exception {
		Constructor cons = Class.forName("java.nio.DirectByteBuffer").getDeclaredConstructor(Long.TYPE, Integer.TYPE);
		cons.setAccessible(true);
		return (ByteBuffer) cons.newInstance(address, (int) length);
	}

	public static void init() {
	}

	static {
		//System.loadLibrary("gnustl_shared");
		//System.loadLibrary("mcpelauncher_tinysubstrate");
		//System.loadLibrary("mcpelauncher");
	}

}
