package com.github.mfondo;

import com.google.common.base.Preconditions;
import junit.framework.TestCase;

import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: mikefriesen
 * Date: 10/31/14
 * Time: 5:15 PM
 */
public class CuckooHashSetTest extends TestCase {

    private long cuckooHashAddTime = 0L;
    private long hashAddTime = 0L;
    private long cuckooHashRemoveTime = 0L;
    private long hashRemoveTime = 0L;

    public void test1() {

        final Set<Integer> cuckooSet = new CuckooHashSet<Integer>(Integer.class, 100, 0.9f, new CuckooHashSet.HashFunction<Integer>() {
            @Override
            public int hash(Integer integer) {
                return integer;
            }
        }, new CuckooHashSet.HashFunction<Integer>() {
            @Override
            public int hash(Integer integer) {
                //from http://stackoverflow.com/questions/664014/what-integer-hash-function-are-good-that-accepts-an-integer-hash-key
                integer = ((integer >> 16) ^ integer) * 0x45d9f3b;
                integer = ((integer >> 16) ^ integer) * 0x45d9f3b;
                integer = ((integer >> 16) ^ integer);
                return integer;
            }
        });

        final Set<Integer> hashSet = new HashSet<Integer>();

        assertAdd(cuckooSet, hashSet, 1);
        assertTrue(cuckooSet.contains(1));
        assertFalse(cuckooSet.contains(2));

        assertAdd(cuckooSet, hashSet, 2);
        assertTrue(cuckooSet.contains(1));
        assertTrue(cuckooSet.contains(2));
        assertFalse(cuckooSet.contains(3));

        assertAdd(cuckooSet, hashSet, 1);
        assertRemove(cuckooSet, hashSet, 1);

        final int iterations = 1000;

        for(int i = 0; i < iterations; i++) {
            assertAdd(cuckooSet, hashSet, i);
        }
        for(int i = 0; i < iterations; i++) {
            assertRemove(cuckooSet, hashSet, i);
        }
        for(int i = 0; i < iterations; i++) {
            if(Math.random() > 0.25f) {
                int rand = (int) Math.random() * Integer.MAX_VALUE;
                assertAdd(cuckooSet, hashSet, rand);
            } else {
                int rand;
                if(hashSet.isEmpty()) {
                    rand = (int) Math.random() * Integer.MAX_VALUE;
                } else {
                    rand = hashSet.iterator().next();
                }
                assertRemove(cuckooSet, hashSet, rand);
            }
        }

        //just for info purposes - compare performance of HashSet vs CuckooHashSet
        System.out.println("Cuckoo Add Nanos:\t\t" + cuckooHashAddTime);
        System.out.println("HashSet Remove Nanos:\t" + hashAddTime);
        System.out.println("Cuckoo Remove Nanos:\t" + cuckooHashRemoveTime);
        System.out.println("HashSet Remove Nanos:\t" + hashRemoveTime);
    }

    private void assertAdd(Set<Integer> cuckooSet, Set<Integer> hashSet, int i) {
        Preconditions.checkArgument(cuckooSet instanceof CuckooHashSet && hashSet instanceof HashSet);
        long start = System.nanoTime();
        cuckooSet.add(i);
        cuckooHashAddTime += System.nanoTime() - start;
        start = System.nanoTime();
        hashSet.add(i);
        hashAddTime += System.nanoTime() - start;
        assertEquals(cuckooSet, hashSet);
    }

    private void assertRemove(Set<Integer> cuckooSet, Set<Integer> hashSet, int i) {
        Preconditions.checkArgument(cuckooSet instanceof CuckooHashSet && hashSet instanceof HashSet);
        long start = System.nanoTime();
        cuckooSet.remove(i);
        cuckooHashRemoveTime += System.nanoTime() - start;
        start = System.nanoTime();
        hashSet.remove(i);
        hashRemoveTime += System.nanoTime() - start;
        assertEquals(cuckooSet, hashSet);
    }
}
