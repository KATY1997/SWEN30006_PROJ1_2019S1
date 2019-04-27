package strategies;

import java.util.LinkedList;
import java.util.Comparator;
import java.util.ListIterator;

import automail.MailItem;
import automail.PriorityMailItem;
import automail.Robot;
import exceptions.ItemTooHeavyException;

public class MailPool implements IMailPool {

	private class Item {
		int priority;
		int destination;
		MailItem mailItem;
		// Use stable sort to keep arrival time relative positions
		
		public Item(MailItem mailItem) {
			priority = (mailItem instanceof PriorityMailItem) ? ((PriorityMailItem) mailItem).getPriorityLevel() : 1;
			destination = mailItem.getDestFloor();
			this.mailItem = mailItem;
		}
	}
	
	public class ItemComparator implements Comparator<Item> {
		@Override
		public int compare(Item i1, Item i2) {
			int order = 0;
			if (i1.priority < i2.priority) {
				order = 1;
			} else if (i1.priority > i2.priority) {
				order = -1;
			} else if (i1.destination < i2.destination) {
				order = 1;
			} else if (i1.destination > i2.destination) {
				order = -1;
			}
			return order;
		}
	}
	

	private LinkedList<Item> pool;
	private LinkedList<Item> pairPool;
	private LinkedList<Item> triplePool;
	private LinkedList<Robot> robots;

	public MailPool(int nrobots){
		// Start empty
		pool = new LinkedList<Item>();
		pairPool = new LinkedList<Item>();
		triplePool = new LinkedList<Item>();
		robots = new LinkedList<Robot>();
	}

	public void addToPool(MailItem mailItem) {
		Item item = new Item(mailItem);
		pool.add(item);
		pool.sort(new ItemComparator());
	}
	
	@Override
	public void step() throws ItemTooHeavyException {
		try{
			ListIterator<Robot> i = robots.listIterator();
			while (i.hasNext()) loadRobot(i);
		} catch (Exception e) { 
            throw e; 
        } 
	}
	
	private void loadRobot(ListIterator<Robot> i) throws ItemTooHeavyException {
	
		int dummy = 1;
		// System.out.printf("P: %3d%n", pool.size());
		ListIterator<Item> j = pool.listIterator();
		
		
		
		if (pool.size() > 0) {
			
	
			

			
			try {
				
			MailItem mail = j.next().mailItem;
			
			if((mail.getWeight()>2000) && (mail.getWeight()<2600)) {
				Item item = new Item(mail);
				pairPool.add(item);
				pairPool.sort(new ItemComparator());
				j.remove();
				mail = j.next().mailItem;
				
			}
			
			else if((mail.getWeight()>2600) && (mail.getWeight()<3000)) {
				Item item = new Item(mail);
				triplePool.add(item);
				triplePool.sort(new ItemComparator());
				j.remove();
				mail = j.next().mailItem;
			}
			
			
				
				
			if (pairPool.size()>0)  {
			int k;
				ListIterator<Item> pair = pool.listIterator();
				for(k=0;k<2;k++) {
					Robot robot = i.next();
					assert(robot.isEmpty());
					robot.addToHand(mail);
					pair.remove();
					robot.dispatch(); // send the robot off if it has any items to deliver
					i.remove();       // remove from mailPool queue
				}	
			}
				
			else if (triplePool.size()>0 )  {
				int k;
				ListIterator<Item> triple = triplePool.listIterator();
				for(k=0;k<3;k++) {
					Robot robot = i.next();
					assert(robot.isEmpty());
					robot.addToHand(mail);
					triple.remove();
					robot.dispatch(); // send the robot off if it has any items to deliver
					i.remove();       // remove from mailPool queue
						
				}	
			}
				
			else {
				
				Robot robot = i.next();
				assert(robot.isEmpty());
				robot.addToHand(mail); // hand first as we want higher priority delivered first
				j.remove();
				if (pool.size() > 0) {
					robot.addToTube(j.next().mailItem);
					j.remove();
				}
				robot.dispatch(); // send the robot off if it has any items to deliver
				i.remove();       // remove from mailPool queue
			}
			
			
		
			} catch (Exception e) { 
	            throw e; 
	        } 
		}
	}

	@Override
	public void registerWaiting(Robot robot) { // assumes won't be there already
		robots.add(robot);
	}

}
