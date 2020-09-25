package sim.field.continuous;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import sim.engine.DSimState;
import sim.engine.DistributedIterativeRepeat;
import sim.engine.Stopping;
import sim.field.DAbstractGrid2D;
import sim.field.DGrid;
import sim.field.HaloGrid2D;
import sim.field.partitioning.PartitionInterface;
import sim.field.storage.ContStorage;
import sim.util.MPIParam;
import sim.util.*;

/**
 * A countinous field that contains lists of objects of type T. Analogous to
 * Mason's Continuous2D.
 * 
 * @param <T> Type of object stored in the field
 */
public class DContinuous2D<T extends Serializable> extends DAbstractGrid2D implements DGrid<T, Double2D> {

	private HaloGrid2D<T, Double2D, ContStorage<T>> halo;

	public DContinuous2D(final PartitionInterface ps, final int[] aoi, final double[] discretizations,
			final DSimState state) {

		super(ps);
		if (ps.getNumDim() != 2)
			throw new IllegalArgumentException("The number of dimensions is expected to be 2, got: " + ps.getNumDim());

		halo = new HaloGrid2D<T, Double2D, ContStorage<T>>(ps, aoi,
				new ContStorage<T>(ps.getPartition(), discretizations), state);

	}

	public NumberND getLocation(final T obj) {
		return halo.localStorage.getLocation(obj);
	}

	public List<T> get(final Double2D p) {
		if (!halo.inLocalAndHalo(p)) {
			System.out.println(String.format("PID %d get %s is out of local boundary, accessing remotely through RMI",
					halo.partition.getPid(), p.toString()));
			return ((List<T>) halo.getFromRemote(p));
		} else
			return getLocal(p);
	}

	public void addLocal(final Double2D p, final T t) {
		halo.localStorage.addToLocation(t, p);
	}

	public void removeLocal(final Double2D p, final T t) {
		// TODO: Remove from just p
		halo.localStorage.removeObject(t);
	}

	public void removeLocal(final Double2D p) {
		halo.localStorage.removeObjects(p);
	}

	public List<T> getNearestNeighbors(final T obj, final int k) {
		return halo.localStorage.getNearestNeighbors(obj, k);
	}

	public List<T> getNeighborsWithin(final T obj, final double r) {
		return halo.localStorage.getNeighborsWithin(obj, r);
	}

	// TODO refactor this after new pack/unpack is introduced in Storage
	@SuppressWarnings("unchecked")
	public final List<T> getAllObjects() {
		Serializable data = null;

		data = halo.localStorage.pack(new MPIParam(halo.origPart, halo.haloPart, halo.MPIBaseType));

		final List<T> objs = ((ArrayList<ArrayList<T>>) data).get(0);

		return IntStream.range(0, objs.size())
				.filter(n -> n % 2 == 0)
				.mapToObj(objs::get)
				.collect(Collectors.toList());
	}

	public ArrayList<T> getLocal(final Double2D p) {
		return halo.localStorage.getObjects(p);
	}

	public void addAgent(final Double2D p, final T t) {
		halo.addAgent(p, t);
	}

	public void moveAgent(final Double2D fromP, final Double2D toP, final T t) {
		halo.moveAgent(fromP, toP, t);
	}

	// Re-implementing this because
	// add also moves the objects in this field
	public void move(final Double2D fromP, final Double2D toP, final T t) {
		final int fromPid = halo.partition.toPartitionId(fromP);
		final int toPid = halo.partition.toPartitionId(fromP);

		if (fromPid == toPid) {
			if (fromPid != halo.partition.pid)
				try {
					halo.proxy.getField(fromPid).addRMI(toP, t);
				} catch (final RemoteException e) {
					throw new RuntimeException(e);
				}
			else
				add(toP, t);
		} else {
			// move cannot be with a single call
			// Since, the fromP and toP are different Remove from one partition &
			// add to another
			remove(fromP, t);
			add(toP, t);
		}
	}

	public void add(Double2D p, T t) {
		halo.add(p, t);
	}

	public void remove(Double2D p, T t) {
		halo.remove(p, t);
	}

	public void remove(Double2D p) {
		halo.remove(p);
	}

	public void addAgent(Double2D p, T t, int ordering, double time) {
		halo.addAgent(p, t, ordering, time);
	}

	// Re-implementing this because
	// add also moves the objects in this field
	public void moveAgent(Double2D fromP, Double2D toP, T t, int ordering, double time) {
		if (!halo.inLocal(fromP)) {
			// System.out.println("pid " + halo.partition.pid + " agent" + t);
			// System.out.println("partitioning " + halo.partition.getPartition());
			throw new IllegalArgumentException("fromP must be local");
		}

		if (!(t instanceof Stopping))
			throw new IllegalArgumentException("t must be a Stopping");

		final Stopping agent = (Stopping) t;

		if (halo.inLocal(toP)) {
			// no need to remove for local moves
			add(toP, t);
			halo.getState().schedule.scheduleOnce(agent);
		} else {
			remove(fromP, t);
			halo.getState().getTransporter().migrateAgent(agent,
					halo.partition.toPartitionId(toP), toP, halo.fieldIndex);
		}
	}

	public void addRepeatingAgent(Double2D p, T t, double time, int ordering, double interval) {
		halo.addRepeatingAgent(p, t, time, ordering, interval);
	}

	public void addRepeatingAgent(Double2D p, T t, int ordering, double interval) {
		halo.addRepeatingAgent(p, t, ordering, interval);
	}

	public void removeAndStopRepeatingAgent(Double2D p, T t) {
		halo.removeAndStopRepeatingAgent(p, t);
	}

	public void removeAndStopRepeatingAgent(Double2D p, DistributedIterativeRepeat iterativeRepeat) {
		halo.removeAndStopRepeatingAgent(p, iterativeRepeat);
	}

	// Re-implementing this because
	// add also moves the objects in this field
	public void moveRepeatingAgent(Double2D fromP, Double2D toP, T t) {
		if (!halo.inLocal(fromP))
			throw new IllegalArgumentException("fromP must be local");

		// TODO: Should we remove the instanceOf check and assume that the
		// pre-conditions are always met?
		if (halo.inLocal(toP))
			add(toP, t);
		else if (!(t instanceof Stopping))
			throw new IllegalArgumentException("t must be a Stopping");
		else {
			remove(fromP, t);
			final Stopping stopping = (Stopping) t;
			// TODO: check type cast here
			final DistributedIterativeRepeat iterativeRepeat = (DistributedIterativeRepeat) stopping.getStoppable();
			halo.getState().getTransporter()
					.migrateRepeatingAgent(iterativeRepeat, halo.partition.toPartitionId(toP), toP, halo.fieldIndex);
			iterativeRepeat.stop();
		}
	}

	// Re-implementing this because
	// add also moves the objects in this field
	public void moveRepeatingAgent(Double2D fromP, Double2D toP, DistributedIterativeRepeat iterativeRepeat) {
		if (!halo.inLocal(fromP))
			throw new IllegalArgumentException("fromP must be local");

		// We cannot use checked cast for generics because of erasure
		// TODO: is there a safe way of doing this?
		final T t = (T) iterativeRepeat.getSteppable();

		if (halo.inLocal(toP))
			add(toP, t);
		else {
			remove(fromP, t);
			halo.getState().getTransporter()
					.migrateRepeatingAgent(iterativeRepeat, halo.partition.toPartitionId(toP), toP, halo.fieldIndex);
			iterativeRepeat.stop();
		}
	}
}
