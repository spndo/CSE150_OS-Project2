package nachos.threads;

import nachos.machine.*;
import nachos.threads.PriorityScheduler.PriorityQueue;
import nachos.threads.PriorityScheduler.ThreadState;

import java.util.Random;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */

public class LotteryScheduler extends PriorityScheduler {
    /**
     * Allocate a new lottery scheduler.
     */
	
    public LotteryScheduler() { 	
    }
    
    @Override
    public int getPriority(KThread thread) { // used from PriorityScheduler.Java
	Lib.assertTrue(Machine.interrupt().disabled());	 
	return getThreadState(thread).getPriority();
    }
    
    @Override
    public int getEffectivePriority(KThread thread) { // used from PriorityScheduler.Java
	Lib.assertTrue(Machine.interrupt().disabled());   
	return getThreadState(thread).getEffectivePriority();
    }
    
    @Override
    public void setPriority(KThread thread, int priority) { // used from PriorityScheduler.Java
	Lib.assertTrue(Machine.interrupt().disabled());
	Lib.assertTrue(priority >= priorityMinimum && priority <= priorityMaximum);
	getThreadState(thread).setPriority(priority);
    }
    
    @Override
    public boolean increasePriority() { // used from PriorityScheduler.Java
	boolean intStatus = Machine.interrupt().disable();
		       
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	
	if (priority == priorityMaximum){
		
	    return false;
	}
	
	setPriority(thread, priority+1);

	Machine.interrupt().restore(intStatus);
	
	return true;
    }
    
    @Override
    public boolean decreasePriority() { // used from PriorityScheduler.Java
	boolean intStatus = Machine.interrupt().disable();
		       
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMinimum)
	    return false;

	setPriority(thread, priority-1);

	Machine.interrupt().restore(intStatus);
	return true;
    }
    
    @Override
    protected LotThreadState getThreadState(KThread thread) { // used from PriorityScheduler.Java
	if (thread.schedulingState == null)
	    thread.schedulingState = new LotThreadState(thread);

	return (LotThreadState) thread.schedulingState;
    }
    
    
    /**
     * Allocate a new lottery thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer tickets from waiting threads
     *					to the owning thread.
     * @return	a new lottery thread queue.
     */
    
    public ThreadQueue newThreadQueue(boolean transferPriority) { // allocate new threadQueue
    	return new PriorityQueue(transferPriority);
    }
    
    protected class LotteryQueue extends PriorityScheduler.PriorityQueue{ // use this to run the test case from Priority Scheduler
        LotteryQueue(boolean transferPriority){ 
            super(transferPriority); // this is calling upon TransferPriority *super* the parent file
        }
        
        @Override
        protected LotThreadState pickNextThread() {
        	if (waitQueue.isEmpty()){ // if the queue is empty return NULL
				return null;
        	}
        	
        	int i = 0; //initialize i
			int[] sumoftix = new int[waitQueue.size()]; // create an array to store the new queue tickets
			int totaltix = 0; // total number of the tickets is initialized to 0
						
			for (KThread thread : waitQueue)
				sumoftix[i++] = totaltix += getThreadState(thread).getEffectivePriority();
			
			int j = 0; //initialize j
			int lot = random.nextInt(totaltix); //randomized total of tickets to test with higher values and other tests
			
			for (KThread thread : waitQueue)
				if (lot < sumoftix[j++])
					return (LotThreadState) getThreadState(thread);

			
			Lib.assertNotReached();
			return null;
        }
    }  
    
    protected class LotThreadState extends PriorityScheduler.ThreadState{
        private LotThreadState(KThread thread) {
            super(thread); // calls upon the parent function
        }
        
        @Override
		public int getEffectivePriority() {
			return getEffectivePriority(new HashSet<LotThreadState>());
		}
        
        private int getEffectivePriority(HashSet<LotThreadState> ep) { //based off the code given in PriorityScheduler.Java
        	//check if the priority if is the set already,
    		//since set has no duplicate numbers 
    		//this helps reduce calculation
    		if(ep.contains(this)) {
    			return priority;
    		}
    		
    		//store the new priority 
    		effectivePriority = priority;
    		
    		//if q needs to pass the priority 
    		for (PriorityQueue q : donation){
    			for (KThread thread : q.waitQueue){
    				ep.add(this); //put into the set 
    				effectivePriority += getThreadState(thread).getEffectivePriority(ep);
    				ep.remove(this); //take out the thread in case of confilict
    			}
    		}
    		
    		PriorityQueue queue = (PriorityQueue) thread.waitToJoin; // the queue to hold transfer tickets
			if (queue.transferPriority) // if the queue has priority it will add more tickets
				for (KThread thread : queue.waitQueue) {
					ep.add(this); //put into the set 
					effectivePriority += getThreadState(thread).getEffectivePriority(ep);
					ep.remove(this); //take out the thread in case of confilict
				}
    		return effectivePriority;
    	}
    }   
	
    protected Random random = new Random(25);
	public static final int priorityDefault = 1;
	public static final int priorityMinimum = 1;
	public static final int priorityMaximum = Integer.MAX_VALUE;    
}