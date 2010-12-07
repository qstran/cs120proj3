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

	swapLock = new Lock();

	//UUID uuid = UUID.randomUUID();
	//swapFileName = uuid.toString();
	swapFileName = new String("nathanandseemantaarethebest.swap");

	swapFile = ThreadedKernel.fileSystem.open(swapFileName, true);
	if (swapFile == null){
	    Machine.halt();
	    Lib.assertNotReached("Machine.halt() did not halt machine!");
	}

       	allPinned = new Condition(UserKernel.memoryLock);
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
	Lib.assertTrue(UserKernel.memoryLock.isHeldByCurrentThread());
	int swapPage;

	//decide which proc based on clock. for now just pick randomly
	//TODO: CHANGE THIS TO CLOCK ALGO!!!!!!!!!
	int ppn = -1;// = (int)(Machine.processor().getNumPhysPages() * Math.random());
        
        while(ppn < 0){
            for(int rotation = 0; rotation < 2; rotation++) {
                for(int dialIndex=0; dialIndex<ipt.size(); dialIndex++){
                    ProcessInfo procInfoAtDial = ipt.get(new Integer(dialIndex));
                    if(((VMProcess)procInfoAtDial.proc).canBeEvicted(procInfoAtDial.vpn)){
                        ppn = ((VMProcess)procInfoAtDial.proc).getPPN(procInfoAtDial.vpn);
                        System.out.println("Found evictable page. ppn = " + ppn);
                        // We set the values forcibly so that we exit the loop if we
                        // find something evictable
                        dialIndex = ipt.size();
                        rotation = 2;
                    }else{
                        ((VMProcess)procInfoAtDial.proc).clearRecentlyUsedStatus(procInfoAtDial.vpn);
                    }
                }
            }

            if(ppn < 0){
                System.out.println("No evictable pages found. sleeping...");
                allPinned.sleep();
            }
        }

        //update IPT
	ProcessInfo evictedProcInfo = ipt.get(ppn);

	//no need to write it to swap if page is read-only
        if(!evictedProcInfo.proc.isReadOnlyPage(evictedProcInfo.vpn)){
	    if( freeSwapPages.size() > 0 ){
                System.out.println("Using gap in swapfile for vpn: " + evictedProcInfo.vpn);
	        swapPage = ((Integer)freeSwapPages.removeFirst()).intValue();
	    }else{
	        swapPage = (swapFile.length()/pageSize) + 1;
                System.out.println("new swap file page count: " + swapPage);
	        freeSwapPages.add(new Integer(swapPage));
	    }

	    byte[] pageData = new byte[pageSize];
	    byte[] memory = Machine.processor().getMemory();
	    System.arraycopy(memory, ppn*pageSize, pageData, 0, pageSize);
            //Don't think the following line is needed, it gets consumed right away
	    //UserKernel.freePages.add(new Integer(ppn));
	
	    swapFile.write(swapPage*pageSize, pageData, 0, pageSize); //write to swapage
	    swapPageMap.put(evictedProcInfo, new Integer(ppn));
        }

        //update pageTable
        evictedProcInfo.proc.invalidate(evictedProcInfo.vpn);

	UserKernel.unregisterMemToProc(ppn);

	return ppn;
    }

    public static int getSwapPage(int vpn, UserProcess proc) {
        ProcessInfo skey = new ProcessInfo(vpn, proc);

        // Now search for the vpn, pid pair within our map
	//TODO: should this be .remove();?
        Integer index = swapPageMap.get(skey);
        if(index == null){
            return -1;
        }else{
            return index.intValue(); 
        }
    }


    // dummy variables to make javac smarter
    private static VMProcess dummy1 = null;

    private static final char dbgVM = 'v';

    /** This kernel's page table. */
    public static int[] invPageTable;

    /** The swap page free list. */
    private static LinkedList freeSwapPages = new LinkedList();
    private static String swapFileName;
    public static OpenFile swapFile;
    public static Lock swapLock;

    private static Condition allPinned;

    private static final int pageSize = Processor.pageSize;
   


}
