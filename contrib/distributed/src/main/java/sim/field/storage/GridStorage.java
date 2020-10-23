package sim.field.storage;

import java.io.Serializable;

import mpi.Datatype;
import mpi.MPI;
import mpi.MPIException;
import sim.field.partitioning.IntHyperRect;
import sim.util.*;

/**
 * internal local storage for distributed grids.
 *
 * @param <T> Type of objects to store
 */
public abstract class GridStorage<T extends Serializable, P extends NumberND> implements java.io.Serializable {
	Object storage;
	IntHyperRect shape;
	Datatype baseType = MPI.BYTE;

	int stride;

	/* Abstract Method of generic storage based on N-dimensional Point */
	public abstract void addToLocation(final T obj, final P p);

//	public abstract P getLocation(final T obj);

//	public abstract void removeObject(final T obj);

	public abstract void removeObject(final T obj, P p);

	public abstract void removeObjects(final P p);

	public abstract Serializable getObjects(final P p);

	public GridStorage(final IntHyperRect shape) {
		super();
		this.shape = shape;
		stride = getStride(shape.getSize());
	}

	public GridStorage(final Object storage, final IntHyperRect shape) {
		super();
		this.storage = storage;
		this.shape = shape;
		stride = getStride(shape.getSize());
	}

	public GridStorage(final Object storage, final IntHyperRect shape, final Datatype baseType) {
		super();
		this.storage = storage;
		this.shape = shape;
		this.baseType = baseType;
		stride = getStride(shape.getSize());
	}

	public Object getStorage() {
		return storage;
	}

	public Datatype getMPIBaseType() {
		return baseType;
	}

	public IntHyperRect getShape() {
		return shape;
	}

	// Return a new instance of the subclass (IntStorage/DoubleStorage/etc...)
	public abstract GridStorage getNewStorage(IntHyperRect shape);

	public abstract String toString();

	public abstract Serializable pack(MPIParam mp) throws MPIException;

	public abstract int unpack(MPIParam mp, Serializable buf) throws MPIException;

	// Method that allocates an array of objects of desired type
	// This method will be called after the new shape has been set
	protected abstract Object allocate(int size);

	/**
	 * Reset the shape, stride, and storage w.r.t. newShape
	 * 
	 * @param newShape
	 */
	private void reload(final IntHyperRect newShape) {
		shape = newShape;
		stride = getStride(newShape.getSize());
		storage = allocate(newShape.getArea());
	}

	/**
	 * Reshapes HyperRect to a newShape
	 * 
	 * @param newShape
	 */
	public void reshape(final IntHyperRect newShape) {
		if (newShape.equals(shape))
			return;

		if (newShape.isIntersect(shape)) {

			final IntHyperRect overlap = newShape.getIntersection(shape);

			final MPIParam fromParam = new MPIParam(overlap, shape, baseType);
			final MPIParam toParam = new MPIParam(overlap, newShape, baseType);

			try {
				final Serializable buf = pack(fromParam);
				reload(newShape);
				unpack(toParam, buf);

				fromParam.type.free();
				toParam.type.free();
			} catch (final MPIException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		} else
			reload(newShape);
	}

	/**
	 * @param p
	 * 
	 * @return flattened index
	 */
	public int getFlatIdx(final Int2D p) {
		return getFlatIdx(p.x, p.y);
//		int sum = 0;
//		for (int i = 0; i < p.getNumDimensions(); i++) {
//			sum += p.c(i) * stride[i];
//		}
//		return sum;

//		return IntStream
//				.range(0, p.getNumDimensions())
//				.map(i -> p.c(i) * stride[i])
//				.sum();
	}

	/**
	 * @param p
	 * 
	 * @return flattened index
	 */
	public int getFlatIdx(int x, int y) {
		return x * stride + y;
	}

	/**
	 * @param p
	 * @param wrtSize
	 * 
	 * @return flattened index with respect to the given size
	 */
	public static int getFlatIdx(final Int2D p, final int[] wrtSize) {
		return p.c(0) * getStride(wrtSize) + p.c(1);

//		final int s = getStride(wrtSize);
//		int sum = 0;
//		for (int i = 0; i < p.getNumDimensions(); i++) {
//			sum += p.c(i) * s[i];
//		}
//		return sum;

//		return IntStream
//			.range(0, p.getNumDimensions())
//			.map(i -> p.c(i) * s[i])
//			.sum();
	}

	/**
	 * @param size
	 * @return stride
	 */
	protected static int getStride(final int[] size) {
		return size[1];
	}

//	protected static int[] getStride(final int[] size) {
//	final int[] ret = new int[size.length];
//
//	ret[size.length - 1] = 1;
//	for (int i = size.length - 2; i >= 0; i--)
//		ret[i] = ret[i + 1] * size[i + 1];
//
//	return ret;
//}

}
