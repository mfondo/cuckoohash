package com.github.mfondo;

import com.google.common.collect.AbstractIterator;

import java.lang.reflect.Array;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;

/**
 * http://en.wikipedia.org/wiki/Cuckoo_hashing
 * Implementation of http://www.cs.tau.ac.il/~shanir/advanced-seminar-data-structures-2007/bib/pagh01cuckoo.pdf
 */
public class CuckooHashSet<T> extends AbstractSet<T> {

    private static final int DEFAULT_INITIAL_SIZE = 8;

    private final Class<T> valueClazz;
    private final HashFunction<T> hashFunction1;
    private final HashFunction<T> hashFunction2;
    private final int maxInsertLoops;
    private final float loadFactor;

    private T[] values1;
    private T[] values2;
    private int size = 0;

    public CuckooHashSet(Class<T> valueClazz, int maxInsertLoops, float loadFactor, HashFunction<T> hashFunction1, HashFunction<T> hashFunction2) {
        this.valueClazz = valueClazz;
        this.maxInsertLoops = maxInsertLoops;
        this.loadFactor = loadFactor;
        this.hashFunction1 = hashFunction1;
        this.hashFunction2 = hashFunction2;
        values1 = (T[])Array.newInstance(valueClazz, DEFAULT_INITIAL_SIZE);
        values2 = (T[])Array.newInstance(valueClazz, DEFAULT_INITIAL_SIZE);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size() < 1;
    }

    @Override
    public boolean contains(Object o) {
        return getHolderAndPosition((T)o) != null;
    }

    private ValuesAndPosition<T> getHolderAndPosition(T val) {
        int pos = hashFunction1.hash(val) % values1.length;
        T t = values1[pos];
        T[] values = values1;
        if(t == null || !t.equals(val)) {
            pos = hashFunction2.hash(val) % values2.length;
            t = values2[pos];
            values = values2;
        }
        ValuesAndPosition<T> ret;
        if(t != null) {
            ret = t.equals(val) ? new ValuesAndPosition<T>(values, pos) : null;
        } else {
            ret = null;
        }
        return ret;
    }

    @Override
    public boolean add(T t) {
        if(contains(t)) {
            return false;
        }
        if(((float)size) > (values1.length * loadFactor)) {
            int newSize = size < 1 ? 16 : size * 2;
            T[] tmp1 = (T[])Array.newInstance(valueClazz, newSize);
            T[] tmp2 = (T[])Array.newInstance(valueClazz, newSize);
            addValues(values1, tmp1, tmp2);
            addValues(values2, tmp1, tmp2);
            values1 = tmp1;
            values2 = tmp2;
        }
        add(values1, values2, t);
        size++;
        return true;
    }

    private void addValues(T[] from, T[] tmp1, T[] tmp2) {
        if(from != null) {
            for(T t : from) {
                if(t != null) {
                    add(tmp1, tmp2, t);
                }
            }
        }
    }

    private T add(T[] values1, T[] values2, T t) {
        T ret = null;
        int hash;
        int loops;
        for(loops = 0; loops < maxInsertLoops; loops++) {
            hash = hashFunction1.hash(t) % values1.length;
            ret = values1[hash];
            if(ret == null) {
                values1[hash] = t;
                break;
            }
            values1[hash] = t;
            t = ret;
            hash = hashFunction2.hash(t) % values2.length;
            ret = values2[hash];
            if(ret == null) {
                values2[hash] = t;
                break;
            }
            values2[hash] = t;
            t = ret;
        }
        if(loops >= maxInsertLoops) {
            throw new IllegalStateException();//todo resize
        }
        return ret;
    }

    @Override
    public Iterator<T> iterator() {
        return new AbstractIterator<T>() {

            private boolean inValues1 = true;
            private int currentPos = 0;

            @Override
            protected T computeNext() {
                T ret = null;
                T t;
                if(inValues1) {
                    for(; currentPos < values1.length; currentPos++) {
                        t = values1[currentPos];
                        if(t != null) {
                            ret = t;
                            break;
                        }
                    }
                    if(currentPos > values1.length - 1) {
                        inValues1 = false;
                        currentPos = 0;
                    }
                }
                if(!inValues1) {
                    for(; currentPos < values2.length; currentPos++) {
                        t = values2[currentPos];
                        if(t != null) {
                            ret = t;
                            break;
                        }
                    }
                }
                currentPos++;
                if(ret == null) {
                    ret = endOfData();
                }
                return ret;
            }
        };
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException();//todo
    }

    @Override
    public <T1 extends Object> T1[] toArray(T1[] a) {
        throw new UnsupportedOperationException();//todo
    }

    @Override
    public boolean remove(Object o) {
        ValuesAndPosition<T> hop = getHolderAndPosition((T)o);
        boolean ret;
        if(hop != null) {
            hop.values[hop.pos] = null;
            ret = true;
        } else {
            ret = false;
        }
        size--;
        return ret;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException();//todo
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();//todo
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();//todo
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();//todo
    }

    public static interface HashFunction<K> {
        int hash(K k);
    }

    private static class ValuesAndPosition<T> {
        private final T[] values;
        private final int pos;

        private ValuesAndPosition(T[] values, int pos) {
            this.values = values;
            this.pos = pos;
        }
    }
}
