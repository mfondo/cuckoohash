package com.github.mfondo;

import com.google.common.collect.AbstractIterator;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;

/**
 * http://en.wikipedia.org/wiki/Cuckoo_hashing
 * Implementation of http://www.cs.tau.ac.il/~shanir/advanced-seminar-data-structures-2007/bib/pagh01cuckoo.pdf
 */
public class CuckooHashSet<T> extends AbstractSet<T> {

    private static final int DEFAULT_INITIAL_SIZE = 8;

    private final HashFunction<T> hashFunction1;
    private final HashFunction<T> hashFunction2;
    private final int maxInsertLoops;
    private final float loadFactor;

    private Holder<T>[] values1 = new Holder[DEFAULT_INITIAL_SIZE];
    private Holder<T>[] values2 = new Holder[DEFAULT_INITIAL_SIZE];;
    private int size = 0;

    public CuckooHashSet(int maxInsertLoops, float loadFactor, HashFunction<T> hashFunction1, HashFunction<T> hashFunction2) {
        this.maxInsertLoops = maxInsertLoops;
        this.loadFactor = loadFactor;
        this.hashFunction1 = hashFunction1;
        this.hashFunction2 = hashFunction2;
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
        int hash = hashFunction1.hash((T)o) % values1.length;
        Holder<T> t = values1[hash];
        if(t == null) {
            hash = hashFunction2.hash((T)o) % values2.length;
            t = values2[hash];
        }
        boolean ret;
        if(t != null) {
            ret = t.t.equals(o);
        } else {
            ret = false;
        }
        return ret;
    }

    @Override
    public boolean add(T t) {
        if(contains(t)) {
            return false;
        }
        if(size > (values1.length * loadFactor)) {
            int newSize = size < 1 ? 16 : size * 2;
            Holder<T>[] tmp1 = new Holder[newSize];
            Holder<T>[] tmp2 = new Holder[newSize];
            addValues(values1, tmp1, tmp2);
            addValues(values2, tmp1, tmp2);
            values1 = tmp1;
            values2 = tmp2;
        }
        add(values1, values2, t);
        size++;
        return true;
    }

    private void addValues(Holder<T>[] from, Holder<T>[] tmp1, Holder<T>[] tmp2) {
        if(from != null) {
            for(Holder<T> h : from) {
                if(h != null) {
                    add(tmp1, tmp2, h.t);
                }
            }
        }
    }

    private T add(Holder<T>[] values1, Holder<T>[] values2, T t) {
        Holder<T> ret = null;
        int hash;
        for(int i = 0; i < maxInsertLoops; i++) {
            hash = hashFunction1.hash(t) % values1.length;
            ret = values1[hash];
            if(ret == null) {
                values1[hash] = new Holder<T>(t);
                break;
            }
            values1[hash] = new Holder<T>(t);
            t = ret.t;
            hash = hashFunction2.hash(t) % values2.length;
            ret = values2[hash];
            if(ret == null) {
                values2[hash] = new Holder<T>(t);
                break;
            }
            values2[hash] = new Holder<T>(t);
            t = ret.t;
        }
        return ret != null ? ret.t : null;
    }

    @Override
    public Iterator<T> iterator() {
        return new AbstractIterator<T>() {

            private boolean inValues1 = true;
            private int currentPos = 0;

            @Override
            protected T computeNext() {
                T ret = null;
                Holder<T> h;
                if(inValues1) {
                    for(; currentPos < values1.length; currentPos++) {
                        h = values1[currentPos];
                        if(h != null) {
                            ret = h.t;
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
                        h = values2[currentPos];
                        if(h != null) {
                            ret = h.t;
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
        size--;
        throw new UnsupportedOperationException();//todo
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        int containCnt = 0;
        for(Object a : c) {
            if(contains(a)) {
                containCnt++;
            }
        }
        return containCnt == c.size();
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

    //todo this makes things much less efficient to have to have the indirect pointer
    //todo maybe try Array.newInstance(c, s) and pass in the class to constructor?
    private static class Holder<T> {
        private T t;

        private Holder(T t) {
            this.t = t;
        }
    }
}
