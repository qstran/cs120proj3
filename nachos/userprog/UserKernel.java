package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.util.LinkedList;
import java.util.HashMap;

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
    /**
     * Allocate a new user kernel.
     */
    public UserKernel() {
	super();
    }

    /**
     * Initialize this kernel. Creates a synchronized console and sets the
     * processor's exception handler.
     */
    public void initialize(String[] args) {
	super.initialize(args);

	console = new SynchConsole(Machine.console());
	
	Machine.processor().setExceptionHandler(new Runnable() {
		public void run() { exceptionHandler(); }
	    });
	
	processLock = new Lock();
	
	memoryLock = new Lock();	
	for (int ppn=0; ppn<Machine.processor().getNumPhysPages(); ppn++)
	    freePages.add(new Integer(ppn));
    }

    /**
     * Test the console device.
     */	
    public void selfTest() {
	super.selfTest();

	System.out.println("Testing the console device. Typed characters");
	System.out.println("will be echoed until q is typed.");

	char c;

	do {
	    c = (char) console.readByte(true);
	    console.writeByte(c);
	}
	while (c != 'q');

	System.out.println("");
    }

    /**
     * Returns the current process.
     *
     * @return	the current process, or <tt>null</tt> if no process is current.
     */
    public static UserProcess currentProcess() {
	if (!(KThread.currentThread() instanceof UThread))
	    return null;
	
	return ((UThread) KThread.currentThread()).process;
    }

    /**
     * The exception handler. This handler is called by the processor whenever
     * a user instruction causes a processor exception.
     *
     * <p>
     * When the exception handler is invoked, interrupts are enabled, and the
     * processor's cause register contains an integer identifying the cause of
     * the exception (see the <tt>exceptionZZZ</tt> constants in the
     * <tt>Processor</tt> class). If the exception involves a bad virtual
     * address (e.g. page fault, TLB miss, read-only, bus error, or address
     * error), the processor's BadVAddr register identifies the virtual address
     * that caused the exception.
     */
    public void exceptionHandler() {
	Lib.assertTrue(KThread.currentThread() instanceof UThread);

	UserProcess process = ((UThread) KThread.currentThread()).process;
	int cause = Machine.processor().readRegister(Processor.regCause);
	process.handleException(cause);
    }

    /**
     * Start running user programs, by creating a process and running a shell
     * program in it. The name of the shell program it must run is returned by
     * <tt>Machine.getShellProgramName()</tt>.
     *
     * @see	nachos.machine.Machine#getShellProgramName
     */
    public void run() {
	super.run();

	UserProcess process = UserProcess.newUserProcess();
	
	String shellProgram = Machine.getShellProgramName();	
	Lib.assertTrue(process.execute(shellProgram, new String[] { }));

	KThread.currentThread().finish();
    }

    public static void registerMemToProc(int ppn, int vpn, UserProcess proc){
	ProcessInfo procInfo = new ProcessInfo(vpn, proc);
	physPageMap.put(ppn, procInfo);
    }

    public static void unregisterMemToProc(int ppn){
	physPageMap.remove(ppn);
    }

    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
	super.terminate();
    }

    public static class ProcessInfo {
	public ProcessInfo(int vpn, UserProcess proc){
	    this.vpn = vpn;
	    this.proc = proc;
	}

        public int vpn;
        public UserProcess proc;
    }
  
    public static HashMap<ProcessInfo, Integer> swapPageMap = new HashMap<ProcessInfo, Integer>();
    public static HashMap<Integer, ProcessInfo> physPageMap = new HashMap<Integer, ProcessInfo>();

    /** Globally accessible reference to the synchronized console. */
    public static SynchConsole console;
    /** Guards access to process data: lists, exit status tables, etc. */
    public static Lock processLock;
    /** The process ID to assign to the next process. */
    public static int nextProcessID = 0;
    /** The number of started processes that have not yet terminated. */
    public static int numRunningProcesses = 0;

    /** Guards access to the physical page free list. */
    public static Lock memoryLock;
    /** The physical page free list. */
    public static LinkedList freePages = new LinkedList();

    // dummy variables to make javac smarter
    private static Coff dummy1 = null;
}
