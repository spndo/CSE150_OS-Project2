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
    public int getPriority(KThread thread) { // used from priority scheduler
	Lib.assertTrue(Machine.interrupt().disabled());	 
	
	return getThreadState(thread).getPriority();
    }
    @Override
    public int getEffectivePriority(KThread thread) { //used from priority scheduler
	Lib.assertTrue(Machine.interrupt().disabled());   
	
	return getThreadState(thread).getEffectivePriority();
    }

    @Override
    public void setPriority(KThread thread, int priority) { // used from priority scheduler
	Lib.assertTrue(Machine.interrupt().disabled());
	Lib.assertTrue(priority >= priorityMinimum && priority <= priorityMaximum);
	getThreadState(thread).setPriority(priority);
    }

    @Override
    public boolean increasePriority() { // used from priority scheduler
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
    public boolean decreasePriority() { // used from priority scheduler
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
    protected ThreadState getThreadState(KThread thread) { // used from priority scheduler
	if (thread.schedulingState == null)
	    thread.schedulingState = new ThreadState(thread);

	return (ThreadState) thread.schedulingState;
    }
    
    
    /**
     * Allocate a new lottery thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer tickets from waiting threads
     *					to the owning thread.
     * @return	a new lottery thread queue.
     */
    
    public ThreadQueue newThreadQueue(boolean transferPriority) { // used from priority scheduler
	return new PriorityQueue(transferPriority);
    }
    
    protected class LotteryQueue extends PriorityScheduler.PriorityQueue{
        LotteryQueue(boolean transferPriority){
            super(transferPriority);
        }
    }  

    @Override
    protected ThreadState pickNextThread() {

        int tickets = 0;
		//getEffectivePriority will give you the total amount of tickets available
		tickets = getEffectivePriority();
		
		
		//if there are no tickets available it will return null
		//past this if statement, fucntion should not return null;
        if(tickets == 0){
			return null;
		}			
		
		//select random winner within range of available tickets
        int winner = new Random().nextInt(tickets);

        ThreadState state = null;

        for(ThreadState threadstate : priorityQueue){
            state = threadstate;
			
            if(winner <= threadstate.getPriority()){
                return state;
            }
        }

		//double check to make sure state is not null
        Lib.assertTrue(state != null);
        return state;
    }
    protected class LotThreadState extends PriorityScheduler.ThreadState{
        private LotThreadState(KThread thread) {
            super(thread);
        }
    }
	
    @Override
    public int getEffectivePriority() {
    	
        int tickets = getPriority();

        for (PriorityQueue myQueue : capturedResources)  {
            for( ThreadState currentThread : myQueue.priorityQueue) {
                if (currentThread == this)
                    continue;
                tickets += currentThread.getEffectivePriority();
            }
        }
        return tickets;
    }
}
    
    
	public static final int priorityDefault = 1;
	public static final int priorityMinimum = 1;
	public static final int priorityMaximum = Integer.MAX_VALUE; // initialize according to the task 4 requirements  
}
