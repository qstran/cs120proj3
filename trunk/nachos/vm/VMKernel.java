package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

import java.util.LinkedList;
import java.util.HashMap;
import java.util.Iterator;

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
	for (int spn=0; spn<initSwapSize; spn++) {
	    freePages.add(new Integer(spn));
        }       
	pageTableLock = new Lock();
	invPageTableLock = new Lock();
        TLBLock = new Lock();
	swapLock = new Lock();
        pageTable = new TranslationEntry[Machine.processor().getNumPhysPages()];
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

    // dummy variables to make javac smarter
    private static VMProcess dummy1 = null;

    private static final char dbgVM = 'v';

    /** This kernel's page table. */
    public static TranslationEntry[] pageTable;
    /** Guards access to the pageTable. */
    public static Lock pageTableLock;
    public static Lock TLBLock;

    public static Lock swapLock;

    /** This kernel's page table. */
    public static int[] invPageTable;
    /** Guards access to the inverse pageTable. */
    public static Lock invPageTableLock;

    /** The swap page free list. */
    private static LinkedList freeSwapPages = new LinkedList();
    private static final int initSwapSize = 64;

    private static class swapKey {
        public int vpn;
        public int pid;
    }
  
    private static HashMap<swapKey, Integer> swapPageMap = new HashMap<swapKey, Integer>();
   
    public static int getSwapPage(int vpn, int pid) {
        swapKey skey = new swapKey();
        skey.vpn = vpn;
        skey.pid = pid;

        // Now search for the vpn, pid pair within our map
        Integer index = swapPageMap.get(skey);
        return index; 
    }

}
