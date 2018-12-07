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
    public int getPriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());	 
	
	return getThreadState(thread).getPriority();
    }
    
    @Override
    public int getEffectivePriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());   
	
	
	return getThreadState(thread).getEffectivePriority();
    }
    
    @Override
    public void setPriority(KThread thread, int priority) {
	Lib.assertTrue(Machine.interrupt().disabled());
	Lib.assertTrue(priority >= priorityMinimum && priority <= priorityMaximum);
	getThreadState(thread).setPriority(priority);
    }
    
    @Override
    public boolean increasePriority() {
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
    public boolean decreasePriority() {
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
    protected LotThreadState getThreadState(KThread thread) {
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
    
    public ThreadQueue newThreadQueue(boolean transferPriority) {
	return new PriorityQueue(transferPriority);
    }
    
    protected class LotteryQueue extends PriorityScheduler.PriorityQueue{
        LotteryQueue(boolean transferPriority){
            super(transferPriority);
        }
        
        @Override
        protected LotThreadState pickNextThread() {

        	if (waitQueue.isEmpty())
				return null;

			int totalLottery = 0;
			
			int[] sum = new int[waitQueue.size()];
			
			int i = 0;
			for (KThread thread : waitQueue)
				sum[i++] = totalLottery += getThreadState(thread).getEffectivePriority();
			
			int lottery = random.nextInt(totalLottery);

			i = 0;
			for (KThread thread : waitQueue)
				if (lottery < sum[i++])
					return (LotThreadState) getThreadState(thread);

			Lib.assertNotReached();
			return null;
        }
    }  
    
    protected class LotThreadState extends PriorityScheduler.ThreadState{
        private LotThreadState(KThread thread) {
            super(thread);
        }
        
        @Override
		public int getEffectivePriority() {
			return getEffectivePriority(new HashSet<LotThreadState>());
		}
        
        private int getEffectivePriority(HashSet<LotThreadState> ep) {

    		
    		if(ep.contains(this)) {
    			return priority;
    		}
    		
    		effectivePriority = priority;
    		
    		for (PriorityQueue q : donation){
    			for (KThread thread : q.waitQueue){
    				ep.add(this);
    				effectivePriority += getThreadState(thread).getEffectivePriority(ep);
    				ep.remove(this);
    			}
    		}
    		PriorityQueue queue = (PriorityQueue) thread.waitToJoin;
			if (queue.transferPriority)
				for (KThread thread : queue.waitQueue) {
					ep.add(this);
					effectivePriority += getThreadState(thread).getEffectivePriority(ep);
					ep.remove(this);
				}

    		return effectivePriority;
    	}
    }   
	
    
    protected Random random = new Random(25);
	public static final int priorityDefault = 1;
	public static final int priorityMinimum = 1;
	public static final int priorityMaximum = Integer.MAX_VALUE;    
}