package sim.field.storage;

import java.io.Serializable;
import java.util.*;
import mpi.*;
import sim.util.*;

public class IntGridStorage extends GridStorage<Integer> {
	private static final long serialVersionUID = 1L;

	public int[] storage;

	public IntGridStorage(final IntRect2D shape) {
		super(shape);
		baseType = MPI.INT;
		clear();
	}

	public byte[] pack(MPIParam mp) throws MPIException {
		byte[] buf = new byte[MPI.COMM_WORLD.packSize(mp.size, baseType)];
		MPI.COMM_WORLD.pack(MPI.slice((int[]) storage, mp.idx), 1, mp.type, buf, 0);
		return buf;
	}

	public int unpack(MPIParam mp, Serializable buf) throws MPIException {
		return MPI.COMM_WORLD.unpack((byte[]) buf, 0, MPI.slice((int[]) storage, mp.idx), 1, mp.type);
	}

	public String toString() {
		int width = shape.getWidth();
		int height = shape.getHeight();
		int[] array = (int[]) storage;
		StringBuffer buf = new StringBuffer(String.format("IntGridStorage-%s\n", shape));

		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++)
				buf.append(String.format(" %4d ", array[i * height + j]));
			buf.append("\n");
		}

		return buf.toString();
	}

	public void set(Int2D p, int t) {
		storage[getFlatIdx((Int2D) p)] = t;
	}

	public void addObject(NumberND p, Integer t) {
		Int2D local_p = toLocalPoint((Int2D) p);

		set(local_p, t);
	}

	public Integer getObject(NumberND p, long id) {
		Int2D local_p = toLocalPoint((Int2D) p);

		return storage[getFlatIdx(local_p)];
	}

	// Don't call this method, it'd be foolish
	public ArrayList<Integer> getAllObjects(NumberND p) {
		
		Int2D local_p = toLocalPoint((Int2D) p);

		ArrayList<Integer> list = new ArrayList<Integer>();
		list.add(storage[getFlatIdx(local_p)]);
		return list;
	}

	public boolean removeObject(NumberND p, long id) {
		Int2D local_p = toLocalPoint((Int2D) p);

		set(local_p, 0);
		return true;
	}

	public void clear(NumberND p) {
		Int2D local_p = toLocalPoint((Int2D) p);

		set(local_p, 0);
	}

	public void clear() {
		storage = new int[shape.getArea()];
	}
}
