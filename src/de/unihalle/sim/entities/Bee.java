package de.unihalle.sim.entities;

import de.unihalle.sim.main.BeeSimulation;
import de.unihalle.sim.util.MovementUtil;
import de.unihalle.sim.util.Position;
import de.unihalle.sim.util.TimeUtil;

public class Bee extends PositionedEntity {

	private static final double MOVEMENT_SPEED = MovementUtil.metersPerSecond(1);
	private static double INITIAL_TIME_TO_LIVE = TimeUtil.minutes(2);
	private static int MAX_CAPACITY = 3;
	private double _timeToLive = INITIAL_TIME_TO_LIVE;
	private int _capacity = MAX_CAPACITY;

	private BeeHive _home;

	private Bee(Position position, BeeHive home) {
		_position = position;
		_home = home;
	}

	/**
	 * Create a new <tt>Bee</tt> instance belonging to specified <tt>BeeHive</tt> home. The bee will spawn at the
	 * location of its home.
	 * 
	 * @param home
	 *            hive the <tt>Bee</tt> belongs to
	 * @return a new <tt>Bee</tt> instance linked to and located at the specified <tt>BeeHive</tt> instance
	 */
	public static Bee create(BeeHive home) {
		return new Bee(home.getPosition(), home);
	}

	/**
	 * Create a new <tt>Bee</tt> instance belonging to specified <tt>BeeHive</tt> home at the specified
	 * <tt>Position</tt> position.
	 * 
	 * @param home
	 *            hive the <tt>Bee</tt> belongs to
	 * @param position
	 *            the <tt>Bee</tt> will spawn at
	 * @return a new <tt>Bee</tt> instance linked to the specified home and located at the specified position
	 */
	public static Bee createAtPosition(Position position, BeeHive home) {
		return new Bee(position, home);
	}

	@Event
	public void collectNectarAtFlower(Flower flower) {
		_capacity -= flower.harvestMaxNectar(_capacity);
		infoWithPosition("Collecting nectar (" + (MAX_CAPACITY - _capacity) + " / " + MAX_CAPACITY + ").");
		if (_capacity == 0) {
			scheduleIfNotDead("flyBack", TimeUtil.seconds(2));
		} else {
			scheduleIfNotDead("flyToFlower", TimeUtil.seconds(2), BeeSimulation.getEnvironment().getRandomFlower());
		}
	}

	@Event
	public void flyBack() {
		infoWithPosition("Flying back to the hive.");
		double distance = _position.distance(_home.getPosition());
		double movementTime = MovementUtil.calculateMovementTime(distance, MOVEMENT_SPEED);
		scheduleIfNotDead("storeNectar", movementTime);
		moveTo(_home.getPosition());
	}

	@Event
	public void storeNectar() {
		_home.storeNectar(MAX_CAPACITY - _capacity);
		infoWithPosition("Storing nectar (" + _home.getStoredNectar() + ").");
		_capacity = MAX_CAPACITY;
		scheduleIfNotDead("flyToFlower", TimeUtil.seconds(2), BeeSimulation.getEnvironment().getRandomFlower());
	}

	@Event
	public void flyToFlower(Flower destination) {
		infoWithPosition("Flying to flower.");
		double movementTime = MovementUtil.calculateMovementTime(_position.distance(destination.getPosition()),
				MOVEMENT_SPEED);
		scheduleIfNotDead("collectNectarAtFlower", movementTime, destination);
		moveTo(destination.getPosition());
	}

	@Event
	public void die() {
		infoWithPosition("I am dead.");
		_home.reportDead();
		BeeSimulation.getEnvironment().removeBee(this);
	}

	@Override
	public void initialize() {
		infoWithPosition("I am alive!");
		BeeSimulation.getEnvironment().addBee(this);
		scheduleIfNotDead("flyToFlower", TimeUtil.seconds(2), BeeSimulation.getEnvironment().getRandomFlower());
	}

	private boolean willBeAliveIn(double seconds) {
		return (_timeToLive > seconds);
	}

	private void scheduleIfNotDead(String event, double time, Object... arguments) {
		if (willBeAliveIn(time)) {
			_timeToLive -= time;
			schedule(event, time, arguments);
		} else {
			schedule("die", _timeToLive);
		}
	}

	private void moveTo(Position pos) {
		_position.x = pos.x;
		_position.y = pos.y;
	}

	public boolean isAtHome(BeeHive home) {
		return _home.equals(home);
	}

}