package strategies;

import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.WeakHashMap;

import automail.MailItem;
import automail.PriorityMailItem;
import automail.Robot;
import exceptions.ItemTooHeavyException;

public class MailPool implements IMailPool {
	public static final int INDIVIDUAL_MAX_WEIGHT = 2000;
	public static final int PAIR_MAX_WEIGHT = 2600;
	public static final int TRIPLE_MAX_WEIGHT = 3000;
	public static final int NUM_POOLS = 3;

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

	// List of pools for items of different weights
	private ArrayList<LinkedList<Item>> poolList;
	private LinkedList<Robot> robots;
	private int nrobots;

	public MailPool(int nrobots) {
		// Start empty
		poolList = new ArrayList<LinkedList<Item>>();
		for (int i = 0; i < NUM_POOLS; i++) {
			poolList.add(new LinkedList<Item>());
		}
		robots = new LinkedList<Robot>();
		this.nrobots = nrobots;
	}

	public void addToPool(MailItem mailItem) throws ItemTooHeavyException {
		// puts items into different pools based on weight and sorts it
		// index: 0 = normal pool, 1 = pairPool, 2 = triplePool
		Item item = new Item(mailItem);
		int weight = mailItem.getWeight();

		if (weight <= INDIVIDUAL_MAX_WEIGHT) {
//			item.mailItem.setRobotsNeed(1);
			poolList.get(0).add(item);
			poolList.get(0).sort(new ItemComparator());
		} else if (weight > INDIVIDUAL_MAX_WEIGHT && weight <= PAIR_MAX_WEIGHT) {
//			item.mailItem.setRobotsNeed(2);
			poolList.get(1).add(item);
			poolList.get(1).sort(new ItemComparator());
		} else if (weight > PAIR_MAX_WEIGHT && weight <= TRIPLE_MAX_WEIGHT) {
//			item.mailItem.setRobotsNeed(3);s
			poolList.get(2).add(item);
			poolList.get(2).sort(new ItemComparator());
		}else {
			throw new ItemTooHeavyException();
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
		 * choose which pool to use, the number that is returned also indicates
		 * the numbers of robot that the item needs to carry it
		 */
		int poolID = choosePool();

		if (poolID == 1) {
			Robot robot = i.next();
			assert (robot.isEmpty());
			ListIterator<Item> poolIterator = poolList.get(poolID - 1).listIterator();
			if (poolList.get(poolID - 1).size() > 0) {
				try {
					// hand first as we want higher priority delivered first
					robot.setTeamState(false);
					robot.setNumTeamMembers(poolID);
					robot.addToHand(poolIterator.next().mailItem);
					poolIterator.remove();
					if (poolList.get(poolID - 1).size() > 0) {
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
		} else {
			groupRobots(poolID, i);
		}
	}

	/**
	 * compare items in the three pools, returning which pool has the highest
	 * priority (i.e) which pool need to deliver its item first
	 * 
	 * @return a number indicating the pool(1)/pairPool(2)/triplePool(3), the
	 *         number also indicates how many robots are required to deliver the
	 *         item
	 * @throws ItemTooHeavyException
	 */
	private int choosePool() {
		TreeMap<Item, Integer> map = new TreeMap<>(new ItemComparator());
		for (int i = 0; i < poolList.size(); i++) {
			if (poolList.get(i).size()>0) {
				Item item = poolList.get(i).element();
				map.put(item,i+1);
			}
		}
		
		if (!map.isEmpty()) {
			return map.get(map.firstKey());
		}
		
		return 1;
		
		
//		LinkedList<Item> items = new LinkedList<>();
//		
//		// iterator all the pools, if there is an item inside, retrieve the
//		// items and put them in a list
//		for (int i = 0; i < poolList.size(); i++) {
//			if (poolList.get(i).size() > 0) {
//				Item item = poolList.get(i).element();
//				items.add(item);
//			}
//		}
//
//		// sort the List to find highest priority item
//		items.sort(new ItemComparator());
//
//		if (items.size() > 0) {
//			int weight = items.getFirst().mailItem.getWeight();
//			if (weight <= INDIVIDUAL_MAX_WEIGHT) {
//				return 1; // use pool
//			} else if (weight > INDIVIDUAL_MAX_WEIGHT && weight <= PAIR_MAX_WEIGHT) {
//				return 2; // use pairPool
//			} else if (weight > PAIR_MAX_WEIGHT && weight <= TRIPLE_MAX_WEIGHT) {
//				return 3; // use triplePool
//			}
//		}
//		return 1; // nothing need to be delivered, all the pools are empty
	}

	@Override
	public void registerWaiting(Robot robot) { // assumes won't be there already
		robots.add(robot);
	}

	/**
	 * called when robots need to work in groups to deliver an item
	 * 
	 * @param poolID
	 *            : a number that indicates how many robots are required for
	 *            this item
	 * @param thePool
	 *            : which pool to use
	 * @param i
	 *            : the iterator of the linkedList<Robot>
	 * @throws ItemTooHeavyException
	 */
	public void groupRobots(int poolID, ListIterator<Robot> i) throws ItemTooHeavyException {
		// checks if there are enough robots in total to carry a heavy item
		if (poolID > this.nrobots) {
			throw new ItemTooHeavyException();
		}

		/// if we have enough robots, processing loading procedure
		if (robots.size() >= poolID && poolList.get(poolID - 1).size() > 0) {
			ListIterator<Item> iterator = poolList.get(poolID - 1).listIterator();
			MailItem item = iterator.next().mailItem;
			if (item.getWeight() > TRIPLE_MAX_WEIGHT) {
				throw new ItemTooHeavyException();
			}
			iterator.remove();
			// assigns robots to an item, based on the amount of robots needed
			// to carry it
			for (int k = 0; k < poolID; k++) {
				try {
					Robot robot = i.next();
					assert (robot.isEmpty());
					robot.setTeamState(true);// sets the robot to work in a team
					robot.setNumTeamMembers(poolID); // sets the number of
														// members in a team
					robot.addToHand(item);
					robot.dispatch();
					i.remove();
				} catch (Exception e) {
					throw e;
				}
			}
		} else { // wait for more robots to come
			Robot robot = i.next();
			assert (robot.isEmpty());
		}
	}
}
