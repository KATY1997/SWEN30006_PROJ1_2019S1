package strategies;

import java.util.LinkedList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.WeakHashMap;

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

	public MailPool(int nrobots) {
		// Start empty
		pool = new LinkedList<Item>();
		pairPool = new LinkedList<Item>();
		triplePool = new LinkedList<Item>();
		robots = new LinkedList<Robot>();
	}

	public void addToPool(MailItem mailItem) {
		// put item into different pool based on its weight, sort the pool in the same time
		Item item = new Item(mailItem);
		int weight = mailItem.getWeight();

		if (weight <= 2000) {
			pool.add(item);
			pool.sort(new ItemComparator());
		} else if (weight > 2000 && weight <= 2600) {
			pairPool.add(item);
			pairPool.sort(new ItemComparator());
		} else if (weight > 2600 && weight <= 3000) {
			triplePool.add(item);
			triplePool.sort(new ItemComparator());
		}
	}

	@Override
	public void step() throws ItemTooHeavyException {
		try {
			ListIterator<Robot> i = robots.listIterator();
			while (i.hasNext()) {
				loadRobot(i);
			}
		} catch (Exception e) {
			throw e;
		}
	}

	private void loadRobot(ListIterator<Robot> i) throws ItemTooHeavyException {

		/**
		 * choose which pool to use, the number that returned also indicates the
		 * numbers of robot that the item need
		 */
		int poolID = choosePool();

		switch (poolID) {
		case 1:
			Robot robot = i.next();
			assert (robot.isEmpty());
			ListIterator<Item> poolIterator = pool.listIterator();
			if (pool.size() > 0) {
				try {
					// hand first as we want higher priority delivered first
					robot.setTeamState(false);
					robot.numOfTeam = poolID;
					robot.addToHand(poolIterator.next().mailItem);
					poolIterator.remove();
					if (pool.size() > 0) {
						robot.addToTube(poolIterator.next().mailItem);
						poolIterator.remove();
					}
					// send the robot off if it has any items to deliver
					robot.dispatch();
					i.remove(); // remove from mailPool queue
				} catch (Exception e) {
					throw e;
				}
			}
			break;
		case 2:
			robotWorkInGroups(poolID, pairPool, i);
			break;
		case 3:
			robotWorkInGroups(poolID, triplePool, i);
			break;
		}
	}

	/**
	 * compare the Item in the three pools, return which pool has the highest priority
	 * (i.e) which pool need to deliver its item first
	 * 
	 * @return a num indicate the pool(1)/pairPool(2)/triplePool(3), the number also
	 *         indicates how many robots this item requires
	 */
	private int choosePool() {
		LinkedList<Item> items = new LinkedList<>();

		// if the pool is not empty, retrive the items and put them it a list
		if (pool.size() > 0) {
			Item poolItem = pool.element();
			items.add(poolItem);
		}
		if (pairPool.size() > 0) {
			Item pairItem = pairPool.element();
			items.add(pairItem);
		}
		if (triplePool.size() > 0) {
			Item tripleItem = triplePool.element();
			items.add(tripleItem);
		}

		// sort the List
		items.sort(new ItemComparator());

		if (items.size() > 0) {
			int weitht = items.getFirst().mailItem.getWeight();
			if (weitht <= 2000) {
				return 1; // use pool
			} else if (weitht > 2000 && weitht <= 2600) {
				return 2; // use pairPool
			} else if (weitht > 2600 && weitht <= 3000) {
				return 3; // use triplePool
			}
		}
		return 1; // nothing need to be delivered, all the pools are empty
	}

	@Override
	public void registerWaiting(Robot robot) { // assumes won't be there already
		robots.add(robot);
	}

	/**
	 * called when robots going to work in groups, either in a group of 2 or a group of 3
	 * @param poolID : a number that indicates how many robots are required for this item
	 * @param thePool : which pool to use
	 * @param i : the iterator of the linkedList<Robot>
	 * @throws ItemTooHeavyException
	 */
	public void robotWorkInGroups(int poolID, LinkedList<Item> thePool, ListIterator<Robot> i)
			throws ItemTooHeavyException {
		
		/// if we have enough robots, processing loading procedure
		if (robots.size() >= poolID && thePool.size() > 0) {
			ListIterator<Item> iterator = thePool.listIterator();
			MailItem item = iterator.next().mailItem;
			iterator.remove();
			// get robots concerning to the item's weight
			for (int k = 0; k < poolID; k++) {
				try {
					Robot robot = i.next();
					assert (robot.isEmpty());
					robot.setTeamState(true);// the robot working in team now
					robot.numOfTeam = poolID; //  how many robots in the team
					robot.addToHand(item);
					robot.dispatch();
					i.remove();
				} catch (Exception e) {
					throw e;
				}
			}
		} 
		else {	// wait for more robots coming
			Robot robot = i.next();
			assert (robot.isEmpty());
		}
	}
}
