package sim.field.storage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import mpi.*;

import sim.field.partitioning.IntHyperRect;
import sim.util.*;

public class IntGridStorage extends GridStorage<Integer> 
	{
	final int initVal;

	public IntGridStorage(IntHyperRect shape, int initVal) {
		super(shape);
		baseType = MPI.INT;
		storage = allocate(shape.getArea());
		this.initVal = initVal;
		Arrays.fill((int[]) storage, initVal);
	}

	public GridStorage getNewStorage(IntHyperRect shape) {
		return new IntGridStorage(shape, 0);
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
		int[] size = shape.getSize();
		int[] array = (int[]) storage;
		StringBuffer buf = new StringBuffer(String.format("IntGridStorage-%s\n", shape));

		if (shape.getNd() == 2)
			for (int i = 0; i < size[0]; i++) {
				for (int j = 0; j < size[1]; j++)
					buf.append(String.format(" %4d ", array[i * size[1] + j]));
				buf.append("\n");
			}

		return buf.toString();
	}

	protected Object allocate(int size) {
		return new int[size];
	}

	public int[] getStorageArray() {
		return (int[]) getStorage();
	}

	public void addToLocation(Integer t, NumberND p) {
		getStorageArray()[getFlatIdx((Int2D) p)] = t;
	}

	public void addToLocation(int t, NumberND p) {
		getStorageArray()[getFlatIdx((Int2D) p)] = t;
	}

	public void removeObject(Integer t, NumberND p) {
		addToLocation(initVal, p);
	}

	public void removeObject(int t, NumberND p) {
		addToLocation(initVal, p);
	}

	public void removeObjects(NumberND p) {
		addToLocation(initVal, p);
	}

	public Serializable getObjects(NumberND p) {
		return getStorageArray()[getFlatIdx((Int2D) p)];
	}

}
