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

	public MailPool(int nrobots) {
		// Start empty
		pool = new LinkedList<Item>();
		pairPool = new LinkedList<Item>();
		triplePool = new LinkedList<Item>();
		robots = new LinkedList<Robot>();
	}

	private void sortPool(LinkedList<Item> pool) {
		ListIterator<Item> j = pool.listIterator();
		if (pool.size() > 0) {

			MailItem mail = j.next().mailItem;
			while (mail.getWeight() > 2000) {

				if ((mail.getWeight() > 2000) && (mail.getWeight() <= 2600)) {
					Item item = new Item(mail);
					pairPool.add(item);
					pairPool.sort(new ItemComparator());
					j.remove();

				} else if ((mail.getWeight() > 2600) && (mail.getWeight() <= 3000)) {
					Item item = new Item(mail);
					triplePool.add(item);
					triplePool.sort(new ItemComparator());
					j.remove();
				}

			}
		}
	}

	public void addToPool(MailItem mailItem) {
		Item item = new Item(mailItem);
		pool.add(item);
		pool.sort(new ItemComparator());
	}

	@Override
	public void step() throws ItemTooHeavyException {
		try {
			ListIterator<Robot> i = robots.listIterator();
			while (i.hasNext())
				loadRobot(i);
		} catch (Exception e) {
			throw e;
		}
	}

	private void loadRobot(ListIterator<Robot> i) throws ItemTooHeavyException {
		
		// System.out.printf("P: %3d%n", pool.size());
		sortPool(pool);

		if (pool.size() > 0 || pairPool.size() > 0 || triplePool.size() > 0) {
			System.out.println("-----");
			try {
				int poolID = getHighestProityItem();
				switch (poolID) {
				case 1:
					Robot robot = i.next();
					assert (robot.isEmpty());
					if (robot.get_team_status() == true) {
						robot.switch_team_status();
					}
					robot.addToHand(pool.removeLast().mailItem); // hand first
																	// as we
																	// want
																	// higher
					// priority delivered first
					if (pool.size() > 0) {
						MailItem tubeItem = null;
						for (int j = 0; j < pool.size(); j++) {
							if (pool.get(j).mailItem.getWeight() <= 2000) {
								tubeItem = pool.get(j).mailItem;
							}
						}
						robot.addToTube(tubeItem);
					}
					robot.dispatch(); // send the robot off if it has any items
										// to deliver
					i.remove(); // remove from mailPool queue
					break;
				case 2:
					if (pairPool.size() > 0 && robots.size() >= 2) {

						ListIterator<Item> pair = pairPool.listIterator();
						for (int k = 0; k < 2; k++) {
							Robot pairRobot = i.next();
							assert (pairRobot.isEmpty());
							if (pairRobot.get_team_status() == false) {
								pairRobot.switch_team_status();
							}

							assert (pairRobot.isEmpty());
							pairRobot.addToHand(pairPool.getLast().mailItem);

							pairRobot.dispatch(); // send the robot off if it
													// has any
													// items to deliver
							i.remove(); // remove from mailPool queue
						}
						pair.remove();
					}

					break;
				case 3:
					if (triplePool.size() > 0 && robots.size() >= 3) {

						ListIterator<Item> triple = triplePool.listIterator();
						for (int k = 0; k < 3; k++) {
							Robot tripleRobot = i.next();
							assert (tripleRobot.isEmpty());
							if (tripleRobot.get_team_status() == false) {
								tripleRobot.switch_team_status();
							}

							assert (tripleRobot.isEmpty());
							tripleRobot.addToHand(triplePool.getLast().mailItem);

							tripleRobot.dispatch(); // send the robot off if it
													// has any
							// items to deliver
							i.remove(); // remove from mailPool queue
						}
						triple.remove();
					}
					break;
				}

			} catch (Exception e) {
				throw e;
			}
		}

	}

	private int getHighestProityItem() {
		int poolItem = 0;
		int pariItem = 0;
		int tripleItem = 0;
		if (pool.size() > 0) {
			poolItem = pool.getFirst().priority;
		}
		if (pairPool.size() > 0) {
			pariItem = pairPool.getFirst().priority;
		}
		if (triplePool.size() > 0) {
			tripleItem = triplePool.getFirst().priority;
		}

		int highest = Math.max(poolItem, Math.max(poolItem, tripleItem));

		if (highest == poolItem) {
			return 1;
		} else if (highest == pariItem) {
			return 2;
		} else if (highest == tripleItem) {
			return 3;
		}
		return 1;
	}

	@Override
	public void registerWaiting(Robot robot) { // assumes won't be there already
		robots.add(robot);
	}

}
