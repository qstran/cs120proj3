package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

import java.util.LinkedList;
import java.util.Iterator;
import java.util.UUID;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {
    /**
     * Allocate a new VM kernel.
     */
    public VMKernel() {
	super();
    }

    /**
     * Initialize this kernel.
     */
    public void initialize(String[] args) {
	super.initialize(args);
	//for (int spn=0; spn<initSwapPages; spn++) {
	//    freePages.add(new Integer(spn));
        //}       
	invPageTableLock = new Lock();
        TLBLock = new Lock();
	swapLock = new Lock();

	UUID uuid = UUID.randomUUID();
	swapFileName = uuid.toString();

	swapFile = ThreadedKernel.fileSystem.open(swapFileName, true);
	if (swapFile == null){
	    Machine.halt();
	    Lib.assertNotReached("Machine.halt() did not halt machine!");
	}
    }

    /**
     * Test this kernel.
     */	
    public void selfTest() {
	super.selfTest();
    }

    /**
     * Start running user programs.
     */
    public void run() {
	super.run();
    }
    
    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
	super.terminate();
    }

    public static int pageEvict(){
	//TODO: go through this code carefully and figure out what needs locks


	//decide which proc based on clock
	//for now just pick first
	//TODO: CHANGE THIS!!!!!!!!!
	int ppn = 1;

	//acquire memLock?
	ProcessInfo evictedProcInfo = physPageMap.remove(ppn);

	//if FreeSwapPages.size() > 0
	//int swapPage = FreeSwapPages.removeFirst();
	byte[] pageData = new byte[pageSize];
	byte[] memory = Machine.processor().getMemory();
	System.arraycopy(memory, ppn*pageSize, pageData, 0, pageSize);
	UserKernel.freePages.add(new Integer(ppn));
	//free memlock?

	//SwapFile.write(pageData, swapPage*pageSize, pageSize); //write to swapage
	//else write new page
	//

	swapPageMap.put(evictedProcInfo, ppn);
	
	
	UserKernel.unregisterMemToProc(ppn);
	return ppn;
    }

    public static Integer checkSwapSpace(int vpn, UserProcess proc){
	ProcessInfo skey = new ProcessInfo(vpn, proc);
	return swapPageMap.get(skey);
    }

    // dummy variables to make javac smarter
    private static VMProcess dummy1 = null;

    private static final char dbgVM = 'v';

    /** This kernel's page table. */
    //public static TranslationEntry[] pageTable;
    /** Guards access to the pageTable. */
    //public static Lock pageTableLock;
    public static Lock TLBLock;

    public static Lock swapLock;

    /** This kernel's page table. */
    public static int[] invPageTable;
    /** Guards access to the inverse pageTable. */
    public static Lock invPageTableLock;

    /** The swap page free list. */
    private static LinkedList freeSwapPages = new LinkedList();
    //private static final int initSwapPages = 64;
    private static String swapFileName;
    private static OpenFile swapFile;

    private static final int pageSize = Processor.pageSize;
   
    public static int getSwapPage(int vpn, UserProcess proc) {
        ProcessInfo skey = new ProcessInfo(vpn, proc);

        // Now search for the vpn, pid pair within our map
        Integer index = swapPageMap.get(skey);
        return index; 
    }

}
