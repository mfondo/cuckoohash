package com.github.mfondo;

import junit.framework.TestCase;

import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: mikefriesen
 * Date: 12/5/14
 * Time: 4:10 PM
 */
public class CuckooFilterTest extends TestCase {

    public void testGetBits() {
        final int[] data = new int[2];
        data[0] = fromBitString("01111111111111111111111111111100");
        data[1] = fromBitString("00011111111111111111111111110000");

        //test bits within one int
        assertEquals(fromBitString("00000000000000000000000000000000"), CuckooFilter.getBits(data, 0, 1));
        assertEquals(fromBitString("10000000000000000000000000000000"), CuckooFilter.getBits(data, 1, 1));
        assertEquals(fromBitString("10000000000000000000000000000000"), CuckooFilter.getBits(data, 2, 1));
        assertEquals(fromBitString("10000000000000000000000000000000"), CuckooFilter.getBits(data, 3, 1));
        assertEquals(fromBitString("10000000000000000000000000000000"), CuckooFilter.getBits(data, 29, 1));
        assertEquals(fromBitString("00000000000000000000000000000000"), CuckooFilter.getBits(data, 30, 1));
        assertEquals(fromBitString("00000000000000000000000000000000"), CuckooFilter.getBits(data, 31, 1));

        assertEquals(fromBitString("01000000000000000000000000000000"), CuckooFilter.getBits(data, 0, 2));
        assertEquals(fromBitString("01100000000000000000000000000000"), CuckooFilter.getBits(data, 0, 3));
        assertEquals(fromBitString("01110000000000000000000000000000"), CuckooFilter.getBits(data, 0, 4));
        assertEquals(fromBitString("01111000000000000000000000000000"), CuckooFilter.getBits(data, 0, 5));
        assertEquals(fromBitString("01111111111111111111111111111100"), CuckooFilter.getBits(data, 0, 31));
        assertEquals(fromBitString("01111111111111111111111111111100"), CuckooFilter.getBits(data, 0, 32));

        assertEquals(fromBitString("11110000000000000000000000000000"), CuckooFilter.getBits(data, 1, 4));
        assertEquals(fromBitString("11110000000000000000000000000000"), CuckooFilter.getBits(data, 2, 4));

        assertEquals(fromBitString("00000000000000000000000000000000"), CuckooFilter.getBits(data, 32, 1));
        assertEquals(fromBitString("00010000000000000000000000000000"), CuckooFilter.getBits(data, 32, 4));
        assertEquals(fromBitString("00011111111111111111111111110000"), CuckooFilter.getBits(data, 32, 32));
        assertEquals(fromBitString("01100000000000000000000000000000"), CuckooFilter.getBits(data, 34, 3));

        //test bits across ints
        assertEquals(fromBitString("10000010000000000000000000000000"), CuckooFilter.getBits(data, 29, 7));
        assertEquals(fromBitString("11000001000000000000000000000000"), CuckooFilter.getBits(data, 28, 8));
    }

    public void testSetBits() {
        int[] data = new int[2];
        data[0] = fromBitString("01111111111111111111111111111100");
        data[1] = fromBitString("00011111111111111111111111110000");

        CuckooFilter.setBits(data, 0, 1, fromBitString("10000000000000000000000000000000"));
        assertEquals(fromBitString("11111111111111111111111111111100"), CuckooFilter.getBits(data, 0, 32));

        CuckooFilter.setBits(data, 0, 1, fromBitString("00000000000000000000000000000000"));
        assertEquals(fromBitString("01111111111111111111111111111100"), CuckooFilter.getBits(data, 0, 32));

        CuckooFilter.setBits(data, 0, 4, fromBitString("10100000000000000000000000000000"));
        assertEquals(fromBitString("10101111111111111111111111111100"), CuckooFilter.getBits(data, 0, 32));

        CuckooFilter.setBits(data, 1, 4, fromBitString("00100000000000000000000000000000"));
        assertEquals(fromBitString("10010111111111111111111111111100"), CuckooFilter.getBits(data, 0, 32));

        CuckooFilter.setBits(data, 31, 1, fromBitString("10000000000000000000000000000000"));
        assertEquals(fromBitString("10010111111111111111111111111101"), CuckooFilter.getBits(data, 0, 32));

        CuckooFilter.setBits(data, 30, 1, fromBitString("10000000000000000000000000000000"));
        assertEquals(fromBitString("10010111111111111111111111111111"), CuckooFilter.getBits(data, 0, 32));

        CuckooFilter.setBits(data, 29, 3, fromBitString("00000000000000000000000000000000"));
        assertEquals(fromBitString("10010111111111111111111111111000"), CuckooFilter.getBits(data, 0, 32));

        //test bits across ints
        CuckooFilter.setBits(data, 29, 4, fromBitString("10010000000000000000000000000000"));
        assertEquals(fromBitString("10010111111111111111111111111100"), CuckooFilter.getBits(data, 0, 32));
        assertEquals(fromBitString("10011111111111111111111111110000"), CuckooFilter.getBits(data, 32, 32));

        CuckooFilter.setBits(data, 29, 5, fromBitString("01101000000000000000000000000000"));
        assertEquals(fromBitString("10010111111111111111111111111011"), CuckooFilter.getBits(data, 0, 32));
        assertEquals(fromBitString("01011111111111111111111111110000"), CuckooFilter.getBits(data, 32, 32));

        CuckooFilter.setBits(data, 0, 32, fromBitString("00000000000000000000000000000000"));
        assertEquals(fromBitString("00000000000000000000000000000000"), CuckooFilter.getBits(data, 0, 32));

        CuckooFilter.setBits(data, 0, 32, fromBitString("11111111111111111111111111111111"));
        assertEquals(fromBitString("11111111111111111111111111111111"), CuckooFilter.getBits(data, 0, 32));

        data = new int[2];
        data[0] = 0;
        data[1] = 0;
        CuckooFilter.setBits(data, 40, 12, 51);
        assertEquals(51, CuckooFilter.getBits(data, 40, 12));
    }

    public void testFilter() {
        CuckooFilter<String> cuckooFilter = new CuckooFilter<String>(4, 100, 50, 12);

        //insert 1 item at a time
        for(int i = 0; i < 10; i++) {
            String val = Integer.toString(i);
            assertTrue(cuckooFilter.add(val));
            assertTrue(cuckooFilter.contains(val));
            cuckooFilter.remove(val);
            assertFalse(cuckooFilter.contains(val));
        }

        //insert 2 items at a time
        cuckooFilter = new CuckooFilter<String>(4, 100, 50, 12);
        for(int i = 0; i < 10; i++) {
            String val1 = Integer.toString((int) (Math.random() * 100));
            assertTrue(cuckooFilter.add(val1));
            String val2 = Integer.toString((int)(Math.random() * 100));
            assertTrue(cuckooFilter.add(val2));
            assertTrue(cuckooFilter.contains(val1));
            assertTrue(cuckooFilter.contains(val2));
            cuckooFilter.remove(val1);
            cuckooFilter.remove(val2);
            assertFalse(cuckooFilter.contains(val1));
            assertFalse(cuckooFilter.contains(val2));
        }

        //add all values at once
        //todo false positives can happen here, so this test is somewhat bogus
        cuckooFilter = new CuckooFilter<String>(4, 100, 50, 12);
        for(int i = 0; i < 10; i++) {
            String val = Integer.toString(i);
            assertTrue(cuckooFilter.add(val));
        }
        for(int i = 0; i < 10; i++) {
            String val = Integer.toString(i);
            assertTrue(cuckooFilter.contains(val));
        }

        cuckooFilter = new CuckooFilter<String>(4, 100, 50, 12);
        Set<String> vals = new HashSet<String>();
        for(int i = 0; i < 1000; i++) {
            String val = Integer.toString((int) (Math.random() * 1000));
            vals.add(val);
            cuckooFilter.add(val);
            assertTrue(cuckooFilter.contains(val));
        }
    }

    //reverse of CuckooFilter.toBitString()
    private static int fromBitString(String s) {
        if(s.length() != Integer.SIZE) {
            throw new IllegalArgumentException();
        }
        int ret = 0;
        char c;
        for(int i = 0; i < Integer.SIZE; i++) {
            c = s.charAt(i);
            if('0' == c) {
                //do nothing
            } else if('1' == c) {
                ret |= 1 << i;
            } else {
                throw new IllegalArgumentException("Invalid char " + c);
            }
        }
        return ret;
    }
}
