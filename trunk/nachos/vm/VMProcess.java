package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
    /**
     * Allocate a new process.
     */
    public VMProcess() {
	super();
	pageTableLock = new Lock();
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
	TranslationEntry currEntry;

	for (int i=0; i<Machine.processor().getTLBSize(); i++){
		currEntry = Machine.processor().readTLBEntry(i);
		currEntry.valid = false;
		Machine.processor().writeTLBEntry(i, currEntry);
	}
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	//super.restoreState();	//Machine.processor().setPageTable(pageTable);
    }

    /**
     * Initializes page tables for this process so that the executable can be
     * demand-paged.
     *
     * @return	<tt>true</tt> if successful.
     */
    protected boolean loadSections() {
	return super.loadSections();
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
	super.unloadSections();
    }    

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
	Processor processor = Machine.processor();

	switch (cause) {
        case Processor.exceptionTLBMiss:
            //logMsg("Got TLB Miss exception!");
            int pid = super.processID();
            //logMsg("pid: " + pid);

	    pageTableLock.acquire();

	    int vaddr = Machine.processor().readRegister(Processor.regBadVAddr);
	    int vpn = Processor.pageFromAddress(vaddr);
 
            if(pageTable == null)
                logMsg("page table is null");
            if(pageTable[vpn] == null)
                logMsg("pagetable entry is null");


	    TranslationEntry entry = pageTable[vpn];
	    
	    if( pageTable[vpn] == null ){			//wasn't in pageTable, check in swap
		Integer filePageOffset = VMKernel.checkSwapSpace(vpn, (UserProcess) this);
		int ppn;
		VMKernel.memoryLock.acquire();
		if ( VMKernel.freePages.size() > 0){	//nothing needs to be evicted
		    ppn = ((Integer)UserKernel.freePages.removeFirst()).intValue();

		    if(filePageOffset != null){		//found page in swap
			VMKernel.swapLock.acquire();
			//move contents from swap into freepage
			//TODO: if pinned somehow wait here without holding all prev locks forever
			//maybe this should be a function call to VMKernel
			VMKernel.swapLock.release();
		    }
		}else{					//something needs to be evicted
		    //TODO: implement evict
		    ppn = VMKernel.pageEvict();
		}

		pageTable[vpn] = new TranslationEntry(vpn, ppn, true, false, false, false);
		VMKernel.memoryLock.release();
	    }

	    pageTableLock.release();

	    if (!pageTable[vpn].valid || pageTable[vpn].vpn != vpn){
		    logMsg("Invalid or mismatching TranslatinEntry");;
	    }

	    int rand = (int)(Machine.processor().getTLBSize() * Math.random());
	    //logMsg("Our random num: " + rand);
	    Machine.processor().writeTLBEntry(rand, pageTable[vpn]);

            break;
	default:
	    super.handleException(cause);
	    break;
	}
    }
	
    // shorter wrapper for log messages
    private static final void logMsg (String msg) {
        System.out.println(msg);
    }
    
    /** Guards access to this pageTable. */
    protected Lock pageTableLock;
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    private static final char dbgVM = 'v';
}
