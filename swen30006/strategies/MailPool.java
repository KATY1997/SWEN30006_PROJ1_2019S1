package strategies;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.TreeMap;

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
	private int nrobots;	// the number of robots alltogether

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
			poolList.get(0).add(item);
			poolList.get(0).sort(new ItemComparator());
		} else if (weight > INDIVIDUAL_MAX_WEIGHT && weight <= PAIR_MAX_WEIGHT) {
			poolList.get(1).add(item);
			poolList.get(1).sort(new ItemComparator());
		} else if (weight > PAIR_MAX_WEIGHT && weight <= TRIPLE_MAX_WEIGHT) {
			poolList.get(2).add(item);
			poolList.get(2).sort(new ItemComparator());
		} else {
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
		groupRobots(poolID, i, poolID == 1 ? false : true);

	}

	/**
	 * compare items in pools, returning which pool has the highest
	 * priority (i.e) which pool need to deliver its item first
	 * 
	 * @return a number indicating the which pool to fetch. E.g. pool(1)/pairPool(2)/triplePool(3), the
	 *         number also indicates how many robots are required to deliver the
	 *         item
	 */
	private int choosePool() {
		// iterator all the pools, if there is an item inside, retrieve the
		// items and put them in the map
		TreeMap<Item, Integer> map = new TreeMap<>(new ItemComparator());
		for (int i = 0; i < poolList.size(); i++) {
			if (poolList.get(i).size() > 0) {
				Item item = poolList.get(i).element();
				map.put(item, i + 1);
			}
		}

		if (!map.isEmpty()) {
			return map.get(map.firstKey());	// an number shows which pool to use
		}

		return 1;	// nothing need to be delivered, all the pools are empty

	}

	@Override
	public void registerWaiting(Robot robot) { // assumes won't be there already
		robots.add(robot);
	}

	/**
	 * called when robots need to work to deliver an item, if the teamState
	 * is false, then the robot works individually
	 * 
	 * @param poolID
	 *            : a number that indicates how many robots are required for
	 *            this item
	 * @param i
	 *            : the iterator of the linkedList<Robot>
	 * @param teamState
	 *            : which teamState the robot need to change to
	 * @throws ItemTooHeavyException
	 */
	public void groupRobots(int poolID, ListIterator<Robot> i, boolean teamState) throws ItemTooHeavyException {
		// checks if there are enough robots in total to carry a heavy item
		if (poolID > this.nrobots) {
			throw new ItemTooHeavyException();
		}

		// if we have enough robots and there is an item in the pool, processing loading procedure
		if (robots.size() >= poolID && poolList.get(poolID - 1).size() > 0) {
			ListIterator<Item> iterator = poolList.get(poolID - 1).listIterator();
			MailItem item = iterator.next().mailItem;

			// if (item.getWeight() > TRIPLE_MAX_WEIGHT) {
			// throw new ItemTooHeavyException();
			// }
			iterator.remove();
			// assigns robots to an item, based on the amount of robots needed to carry it
			for (int k = 0; k < poolID; k++) {
				try {
					Robot robot = i.next();
					assert (robot.isEmpty());
					robot.setTeamState(teamState);// sets the robot to work in a team
					robot.setNumTeamMembers(poolID); // sets the number of members in a team
					robot.addToHand(item);

					// if the robot is fetching the normal pool, load an other item in its tube
					if (poolID == 1 && poolList.get(0).size() > 0) {
						robot.addToTube(iterator.next().mailItem);
						iterator.remove();
					}

					robot.dispatch();
					i.remove();
				} catch (Exception e) {
					throw e;
				}
			}
		} else { // wait for more robots to come, iterate the next robot
			Robot robot = i.next();
			assert (robot.isEmpty());
		}
	}
}
