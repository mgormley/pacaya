package edu.jhu.util.collections;

import java.util.Arrays;

public class PDoubleArrayList {

    /** The internal array representing this list. */
    private double[] elements;
    /** The number of elements in the list. */
    private int size;
    
    public PDoubleArrayList() {
        this(8);
    }
    
    public PDoubleArrayList(int initialCapacity) {
        elements = new double[initialCapacity];
        size = 0;
    }
    
    /**
     * Adds the value to the end of the list.
     * @param value The value to add.
     */
    public void add(double value) {
        ensureCapacity(size + 1);
        elements[size] = value;
        size++;
    }
    
    /**
     * Gets the i'th element of the array list.
     * @param i The index of the element to get.
     * @return The value of the element to get.
     */
    public double get(int i) {
        if (i < 0 || i >= size) {
            throw new IndexOutOfBoundsException();
        }
        return elements[i];
    }

    /**
     * Adds all the elements in the given array to the array list.
     * @param values The values to add to the array list.
     */
    public void add(double[] values) {
        ensureCapacity(size + values.length);
        for (double element : values) {
            this.add(element);
        }
    }

    /**
     * Gets a NEW array containing all the elements in this array list.
     * @return The new array containing the elements in this list.
     */
    public double[] toNativeArray() {
        return Arrays.copyOf(elements, size);
    }
    
    /**
     * Trims the internal array to the size of the array list and then return
     * the internal array backing this array list. CAUTION: this should not be
     * called without carefully handling the result.
     * 
     * @return The internal array representing this array list, trimmed to the
     *         correct size.
     */
    // TODO: rename to getElements.
    public double[] elements() {
        this.trimToSize();
        return elements;
    }
    
    /**
     * Trims the internal array to exactly the size of the list.
     */
    public void trimToSize() {
        if (size != elements.length) { 
            elements = Arrays.copyOf(elements, size);
        }
    }

    /**
     * Ensure that the internal array has space to contain the specified number of elements.
     * @param size The number of elements. 
     */
    private void ensureCapacity(int size) {
        if (size > elements.length) {
            double[] tmp = new double[size*2];
            System.arraycopy(elements, 0, tmp, 0, elements.length);
            elements = tmp;
        }
    }

    /**
     * Gets the number of elements in the list.
     * @return The size of the list.
     */
    public int size() {
        return size;
    }
    
    /**
     * Removes all elements from this array list.
     */
    public void clear() {
        size = 0;
    }
    
}
