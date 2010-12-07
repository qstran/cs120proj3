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
    }


    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
				 int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();
	
	int amount = 0;

	while (length > 0) {
	    int vpn = Processor.pageFromAddress(vaddr);
	    int off = Processor.offsetFromAddress(vaddr);

	    int transfer = Math.min(length, pageSize-off);

	    UserKernel.memoryLock.acquire();
	    if (vpn < 0 || vpn >= pageTable.length){
	        System.out.println("readVirtualMemory: Invalid VPN");
	    }

	    if(Machine.processor().hasTLB()){
	        TranslationEntry entry = pageTable[vpn];
	        if (entry == null || !entry.valid){
		    pageTable[vpn] = handlePageFault(vpn);
	        }
	    }

	    
	    int ppn = pinVirtualPage(vpn, false);
	    if (ppn == -1)
		break;

	    int rand = (int)(Machine.processor().getTLBSize() * Math.random());
	    Machine.processor().writeTLBEntry(rand, pageTable[vpn]);

	    VMKernel.registerMemToProc(pageTable[vpn].ppn, vpn, (UserProcess) this);

	    //update PageTable (done by pin i think)
	    UserKernel.memoryLock.release();

	    System.arraycopy(memory, ppn*pageSize + off, data, offset,
			     transfer);

	    UserKernel.memoryLock.acquire();
	    unpinVirtualPage(vpn);
	    UserKernel.memoryLock.release();
	    
	    vaddr += transfer;
	    offset += transfer;
	    amount += transfer;
	    length -= transfer;	    
	}

	return amount;
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
				  int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();
	
	int amount = 0;

	while (length > 0) {
	    int vpn = Processor.pageFromAddress(vaddr);
	    int off = Processor.offsetFromAddress(vaddr);

	    int transfer = Math.min(length, pageSize-off);

	    UserKernel.memoryLock.acquire();
	    if (vpn < 0 || vpn >= pageTable.length){
	        System.out.println("writeVirtualMemory: Invalid VPN");
	    }

	    if(Machine.processor().hasTLB()){
	        TranslationEntry entry = pageTable[vpn];
	        if (entry == null || !entry.valid){
		    entry = handlePageFault(vpn);
	        }
	    }

	    int ppn = pinVirtualPage(vpn, true);
	    if (ppn == -1)
		break;

	    int rand = (int)(Machine.processor().getTLBSize() * Math.random());
	    Machine.processor().writeTLBEntry(rand, pageTable[vpn]);

	    VMKernel.registerMemToProc(pageTable[vpn].ppn, vpn, (UserProcess) this);

	    //update PageTable (done by pin i think)
	    UserKernel.memoryLock.release();

	    System.arraycopy(data, offset, memory, ppn*pageSize + off,
			     transfer);
	    
	    UserKernel.memoryLock.acquire();
	    unpinVirtualPage(vpn);
	    UserKernel.memoryLock.release();
	    
	    vaddr += transfer;
	    offset += transfer;
	    amount += transfer;
	    length -= transfer;	    
	}

	return amount;
    }


    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
	TranslationEntry currEntry;

	//logMsg("Saving state**************");

	for (int i=0; i<Machine.processor().getTLBSize(); i++){
	    currEntry = Machine.processor().readTLBEntry(i);
	    pageTable[currEntry.vpn].dirty = currEntry.dirty;
	    pageTable[currEntry.vpn].used = currEntry.used;
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
	UserKernel.memoryLock.acquire();

	pageTable = new TranslationEntry[numPages];
	logMsg("needed pages for pid " + super.processID() + " = " + numPages);
	for(int vpn=0; vpn<numPages; vpn++){
	    pageTable[vpn] = new TranslationEntry(vpn, 1, false, false, false, false);
	}

	UserKernel.memoryLock.release();

	return true;
	
//	return super.loadSections();
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
        for (int vpn=0; vpn<pageTable.length; vpn++){
	    if(pageTable[vpn] != null && pageTable[vpn].valid){
	        UserKernel.freePages.add(new Integer(pageTable[vpn].ppn));
	    }
	}
    }

    protected TranslationEntry handlePageFault(int vpn){
	Lib.assertTrue(Machine.processor().hasTLB());
	int ppn;

	Lib.assertTrue(UserKernel.memoryLock.isHeldByCurrentThread());

	//Get a free Physical page or Evict to make room
	if ( VMKernel.freePages.size() > 0){	//nothing needs to be evicted
	    ppn = ((Integer)VMKernel.freePages.removeFirst()).intValue();
	    //logMsg("Took a free phys page. " + VMKernel.freePages.size() + " remaining.");
	}else{					//something needs to be evicted
	    logMsg("evicting something");
	    ppn = VMKernel.pageEvict();

	}

	pageTable[vpn] = new TranslationEntry(vpn, ppn, true, false, false, false);
	VMKernel.registerMemToProc(pageTable[vpn].ppn, vpn, (UserProcess) this);


	//TODO: how do we know which of these to do?
	// load page contents, either from swap or coff or new
	int swapPage = VMKernel.getSwapPage(vpn, (UserProcess) this);
        if(swapPage >= 0){		//page was in swap
	    logMsg("page was in swap space*************************");
	    VMKernel.swapLock.acquire();
	    //TODO: if pinned somehow wait here without holding all prev locks forever
	    byte[] swapData = new byte[pageSize];
	    byte[] memory = Machine.processor().getMemory();
	    VMKernel.swapFile.read(swapPage*pageSize, swapData, 0, pageSize); //read swapage
	    System.arraycopy(swapData, ppn*pageSize, memory, 0, pageSize);
	    VMKernel.swapPageMap.remove(new UserKernel.ProcessInfo(vpn, (UserProcess) this));
            //Don't think the following line is needed, it gets consumed right away
	    //UserKernel.freePages.add(new Integer(ppn));
	    VMKernel.swapLock.release();
        }

	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);

	    for (int i=0; i<section.getLength(); i++) {
		int sectVPN = section.getFirstVPN()+i;

		if(sectVPN == vpn){
		    //logMsg("\tinitializing " + section.getName()
		    //  + " section (" + section.getLength() + " pages)");
		    //logMsg("vpn: " + sectVPN);
		    //logMsg("page offset in this section: " + i);
		    //logMsg("ppn: " + pinVirtualPage(vpn, false));
		    pageTable[vpn].readOnly = section.isReadOnly();
		    section.loadPage(i, pinVirtualPage(vpn, false));
		}
	    }
	}

	return pageTable[vpn];
	
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

	    int vaddr = Machine.processor().readRegister(Processor.regBadVAddr);
	    int vpn = Processor.pageFromAddress(vaddr);
	    //logMsg("vpn of TLB miss: " + vpn);

	    UserKernel.memoryLock.acquire();

            //if(pageTable == null)
            //    logMsg("page table is null");                

	    if (vpn < 0 || vpn >= pageTable.length){
	        logMsg("TLB handle: Invalid VPN " + vpn);
		UserKernel.memoryLock.release();
		//return;
	    }  

	    if (pageTable[vpn] == null || !pageTable[vpn].valid){
		//logMsg("pagetable entry is null or invalid");
		pageTable[vpn] = handlePageFault(vpn);
	    }

	    UserKernel.memoryLock.release();

	    int rand = (int)(Machine.processor().getTLBSize() * Math.random());
	    //logMsg("Our random num: " + rand);
	    //TODO: write back something if dirty/valid?
	    Machine.processor().writeTLBEntry(rand, pageTable[vpn]);
	    //logMsg("TLB evicted # " + rand);

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
    
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    private static final char dbgVM = 'v';
}
