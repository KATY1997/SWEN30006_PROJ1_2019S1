package automail;

import exceptions.ExcessiveDeliveryException;
import exceptions.ItemTooHeavyException;
import strategies.IMailPool;
import java.util.Map;
import java.util.TreeMap;

/**
 * The robot delivers mail!
 */
public class Robot {

	static public final int INDIVIDUAL_MAX_WEIGHT = 2000;
	static public final int PAIR_MAX_WEIGHT = 2600;
	static public final int TRIPLE_MAX_WEIGHT = 3000;

	IMailDelivery delivery;
	protected final String id;

	/** Possible states the robot can be in */
	public enum RobotState {
		DELIVERING, WAITING, RETURNING
	}

	public RobotState current_state;
	private int current_floor;
	private int destination_floor;
	private IMailPool mailPool;
	private boolean receivedDispatch;

	public boolean team_state; // in default the robot is doing
								// individually
	public int numOfTeam; // 1 refers to one robot carry one item, 2 means they
							// work in pairs
//	private boolean normalSpeed; // true meand the robot is in normal speed,
									// otherwise it moves slower

	private MailItem deliveryItem = null;
	private MailItem tube = null;
	private int timer;

	private int deliveryCounter;

	/**
	 * Initiates the robot's location at the start to be at the mailroom also
	 * set it to be waiting for mail.
	 * 
	 * @param behaviour
	 *            governs selection of mail items for delivery and behaviour on
	 *            priority arrivals
	 * @param delivery
	 *            governs the final delivery
	 * @param mailPool
	 *            is the source of mail items
	 */
	public Robot(IMailDelivery delivery, IMailPool mailPool) {
		id = "R" + hashCode();
		// current_state = RobotState.WAITING;
		current_state = RobotState.RETURNING;
		current_floor = Building.MAILROOM_LOCATION;
		this.delivery = delivery;
		this.mailPool = mailPool;
		this.receivedDispatch = false;
		this.deliveryCounter = 0;
		this.team_state = false;
		this.numOfTeam = 1;
//		this.normalSpeed = true;
		this.timer = 1;
	}

	public void dispatch() {
		receivedDispatch = true;
	}

	/**
	 * This is called on every time step
	 * 
	 * @throws ExcessiveDeliveryException
	 *             if robot delivers more than the capacity of the tube without
	 *             refilling
	 */
	public void step() throws ExcessiveDeliveryException {
		switch (current_state) {
		/**
		 * This state is triggered when the robot is returning to the mailroom
		 * after a delivery
		 */
		case RETURNING:
			/**
			 * If its current position is at the mailroom, then the robot should
			 * change state
			 */
			if (current_floor == Building.MAILROOM_LOCATION) {
				if (tube != null) {
					mailPool.addToPool(tube);
					System.out.printf("T: %3d > old addToPool [%s]%n", Clock.Time(), tube.toString());
					tube = null;
				}
				/** Tell the sorter the robot is ready */
//				if (getTeamState() == true) {
//					setTeamState(false);
//				}
//				this.numOfTeam = 1;
//				this.timer = 1;
				
				mailPool.registerWaiting(this);
				changeState(RobotState.WAITING);
			} else {
				/**
				 * If the robot is not at the mailroom floor yet, then move
				 * towards it!
				 */
				moveTowards(Building.MAILROOM_LOCATION);
				break;
			}
		case WAITING:
			/**
			 * If the StorageTube is ready and the Robot is waiting in the
			 * mailroom then start the delivery
			 */
			if (!isEmpty() && receivedDispatch) {
				receivedDispatch = false;
				deliveryCounter = 0; // reset delivery counter
				setRoute();
				changeState(RobotState.DELIVERING);
			}
			break;
		case DELIVERING:
			
			if (current_floor == destination_floor) { // If already here drop
														// off either way
				/** Delivery complete, report this to the simulator! */
				
				delivery.deliver(deliveryItem, numOfTeam);
				// now the robot is not working in groups
				if (getTeamState() == true) {
					setTeamState(false);
				}
				this.timer = 1;
				this.numOfTeam = 1;

				deliveryItem = null;
				deliveryCounter++;
				if (deliveryCounter > 2) { // Implies a simulation bug
					throw new ExcessiveDeliveryException();
				}
				/**
				 * Check if want to return, i.e. if there is no item in the tube
				 */
				if (tube == null || team_state == true) {
					changeState(RobotState.RETURNING);
				} else {
					/**
					 * If there is another item, set the robot's route to the
					 * location to deliver the item
					 */
					deliveryItem = tube;
					tube = null;
					setRoute();
					changeState(RobotState.DELIVERING);
				}
			} else {
				/** The robot is not at the destination yet, move towards it! */
				moveTowards(destination_floor);
			}
			break;
		}
	}

	private boolean getTeamState() {
		return this.team_state;
	}

	/**
	 * Sets the route for the robot
	 */
	private void setRoute() {
		/** Set the destination floor */
		destination_floor = deliveryItem.getDestFloor();
	}

	/**
	 * Generic function that moves the robot towards the destination
	 * 
	 * @param destination
	 *            the floor towards which the robot is moving
	 */
	private void moveTowards(int destination) {

		if (!team_state|| this.timer == 3) {
			timer = 1;
			if (current_floor < destination) {
				current_floor++;
			} else {
				current_floor--;
			}
		} else {
			timer++;
		}

	}

	private String getIdTube() {
		return String.format("%s(%1d)", id, (tube == null ? 0 : 1));
	}

	/**
	 * Prints out the change in state
	 * 
	 * @param nextState
	 *            the state to which the robot is transitioning
	 */
	private void changeState(RobotState nextState) {
		assert (!(deliveryItem == null && tube != null));
		if (current_state != nextState) {
			System.out.printf("T: %3d > %7s changed from %s to %s%n", Clock.Time(), getIdTube(), current_state,
					nextState);
		}
		current_state = nextState;
		if (nextState == RobotState.DELIVERING) {
			System.out.printf("T: %3d > %7s-> [%s]%n", Clock.Time(), getIdTube(), deliveryItem.toString());
		}
	}

	public MailItem getTube() {
		return tube;
	}

	static private int count = 0;
	static private Map<Integer, Integer> hashMap = new TreeMap<Integer, Integer>();

	@Override
	public int hashCode() {
		Integer hash0 = super.hashCode();
		Integer hash = hashMap.get(hash0);
		if (hash == null) {
			hash = count++;
			hashMap.put(hash0, hash);
		}
		return hash;
	}

	public boolean isEmpty() {
		return (deliveryItem == null && tube == null);
	}

	public void addToHand(MailItem mailItem) throws ItemTooHeavyException {
		assert (deliveryItem == null);
		deliveryItem = mailItem;
		if (deliveryItem.weight > INDIVIDUAL_MAX_WEIGHT && team_state == false)
			throw new ItemTooHeavyException();
	}

	public void addToTube(MailItem mailItem) throws ItemTooHeavyException {
		assert (tube == null);
		tube = mailItem;
		if (tube.weight > INDIVIDUAL_MAX_WEIGHT)
			throw new ItemTooHeavyException();
	}

	public void setTeamState(boolean state) {
		this.team_state = state;
	}

//	public boolean isNormalSpeed() {
//		return normalSpeed;
//	}
//
//	public void setNormalSpeed(boolean normalSpeed) {
//		this.normalSpeed = normalSpeed;
//	}

}
