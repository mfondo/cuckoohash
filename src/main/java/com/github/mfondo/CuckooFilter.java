package com.github.mfondo;

import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: mikefriesen
 * Date: 11/20/14
 * Time: 4:37 PM
 *
 * http://www.eecs.harvard.edu/~michaelm/postscripts/cuckoo-conext2014.pdf
 * http://www.cs.cmu.edu/~binfan/papers/login_cuckoofilter.pdf
 *
 * semi-sorting buckets, as described in the paper has not been implemented here yet
 */
public class CuckooFilter<T> {

    private static final int ALL_ONE_BITS;

    static {
        ALL_ONE_BITS = createMask(Integer.SIZE);
    }

    private final Random rand;
    private final int numBuckets;
    private final int bucketEntries;
    private final int maxInsertLoops;
    private final int[] data;
    private final int fingerprintBits;
    private final int fingerprintMask;
    private final int bucketBits;

    /**
     * @param bucketEntries number of entries per bucket
     * @param numBuckets number of buckets
     * @param maxInsertLoops maximum number of loop iterations on insert before giving up
     * @param fingerprintBits number of bits in fingerprint
     */
    public CuckooFilter(int bucketEntries, int numBuckets, int maxInsertLoops, int fingerprintBits) {
        if(bucketEntries < 1 || !isPowerOf2(bucketEntries)) {
            throw new IllegalArgumentException("Invalid bucket entries");
        }
        if(maxInsertLoops < 1) {
            throw new IllegalArgumentException("Invalid max insert loops");
        }
        //fingerprintBits must be < 32 bits to fit in an int
        if(fingerprintBits < 1 || fingerprintBits > Integer.SIZE) {
            throw new IllegalArgumentException("Invalid fingerprint bits");
        }
        this.fingerprintBits = fingerprintBits;
        rand = new Random();
        this.bucketEntries = bucketEntries;
        this.numBuckets = numBuckets;
        this.maxInsertLoops = maxInsertLoops;
        bucketBits = ((fingerprintBits * bucketEntries) + bucketEntries);
        int dataSize = bucketBits * numBuckets;
        dataSize += (dataSize % 8);//so that division rounds up
        data = new int[dataSize / 8];
        fingerprintMask = createMask(fingerprintBits);
    }

    /**
     * @param t element to add
     * @return true if the element was successfully added
     */
    public boolean add(T t) {
        if(t == null) {
            throw new IllegalArgumentException();
        }
        int fingerprint = fingerprint(t);
        int i1 = hash(t);
        int i2 = i1 ^ hashFingerprint(fingerprint);
        if(addToBucket(i1, fingerprint)) {
            return true;
        }
        if(addToBucket(i2, fingerprint)) {
            return true;
        }
        int i = rand.nextBoolean() ? i1 : i2;
        int bucketBitOffset;
        int entryBitOffset;
        int entry;
        int tmpFingerprint;
        for(int n = 0; n < maxInsertLoops; n++) {
            bucketBitOffset = getBucketBitOffset(i);
            entry = rand.nextInt(bucketEntries);
            entryBitOffset = getEntryBitOffset(bucketBitOffset, entry);
            tmpFingerprint = getFingerprint(entryBitOffset);
            storeFingerprint(entryBitOffset, fingerprint);
            fingerprint = tmpFingerprint;
            i = i ^ hashFingerprint(fingerprint);
            if(addToBucket(i, tmpFingerprint)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param t element to remove
     * @return true if the element was contained in the CuckooFilter
     */
    public boolean remove(T t) {
        return containsOrRemove(t, true);
    }

    /**
     * @param t T
     * @return true if the CuckooFilter possibly contains T, false if it definitely does not contain T
     */
    public boolean contains(T t) {
        return containsOrRemove(t, false);
    }

    private boolean containsOrRemove(T t, boolean remove) {
        int fingerprint = fingerprint(t);
        int i1 = hash(t);
        if(bucketContainsOrRemove(i1, fingerprint, remove)) {
            return true;
        }
        int i2 = i1 ^ hashFingerprint(fingerprint);
        if(bucketContainsOrRemove(i2, fingerprint, remove)) {
            return true;
        }
        return false;
    }

    private static boolean isPowerOf2(int i) {
        return ((i & (i - 1)) == 0);
    }

    private int getBucketBitOffset(int bucketNbr) {
        return bucketBits * bucketNbr;
    }

    private int getEntryBitOffset(int bucketBitOffset, int entryNbr) {
        return bucketBitOffset + (bucketEntries) + (entryNbr * fingerprintBits);
    }

    //return -1 if could not find one
    private int getFirstEmptyEntryBitOffsetAndMarkPopulated(int bucketBitOffset) {
        int entriesPopulatedBits = getBits(data, bucketBitOffset, bucketEntries);
        int entriesPopulatedMask;
        for(int i = 0; i < bucketEntries; i++) {
            entriesPopulatedMask = 1 << i;
            if((entriesPopulatedMask & entriesPopulatedBits) == 0) {
                setBits(data, bucketBitOffset + i, 1, 1);
                return bucketBitOffset + bucketEntries + (i * fingerprintBits);
            }
        }
        return -1;
    }

    private int getFingerprint(int entryBitOffset) {
        return getBits(data, entryBitOffset, fingerprintBits);
    }

    //this does not mark the entry as populated - caller is responsible for that
    private void storeFingerprint(int entryBitOffset, int fingerprint) {
        setBits(data, entryBitOffset, fingerprintBits, fingerprint);
    }

    private int fingerprint(T t) {
        //lower bits of t.hashCode()
        return t.hashCode() & fingerprintMask;
    }

    private int hash(T t) {
        //upper bits of t.hashCode()
        return Math.abs((t.hashCode() >>> (Integer.SIZE - fingerprintBits))) % numBuckets;
    }

    private int hashFingerprint(int fingerprint) {
        //from http://stackoverflow.com/questions/664014/what-integer-hash-function-are-good-that-accepts-an-integer-hash-key
        fingerprint = ((fingerprint >> 16) ^ fingerprint) * 0x45d9f3b;
        fingerprint = ((fingerprint >> 16) ^ fingerprint) * 0x45d9f3b;
        fingerprint = ((fingerprint >> 16) ^ fingerprint);
        return fingerprint % numBuckets;
    }

    //returns true if i was added to the bucket
    private boolean addToBucket(int bucket, int fingerprint) {
        int bucketBitOffset = getBucketBitOffset(bucket);
        int entryBitOffset = getFirstEmptyEntryBitOffsetAndMarkPopulated(bucketBitOffset);
        boolean added;
        if(entryBitOffset >= 0) {
            storeFingerprint(entryBitOffset, fingerprint);
            added = true;
        } else {
            added = false;
        }
        return added;
    }

    /**
     * @param bucket bucket number
     * @param fingerprint fingerprint
     * @param remove if true, then the fingerprint will be removed and the entry marked as empty
     * @return true of the bucket contains fingerprint
     */
    private boolean bucketContainsOrRemove(int bucket, int fingerprint, boolean remove) {
        int bucketBitOffset = getBucketBitOffset(bucket);
        int entriesPopulatedBits = getBits(data, bucketBitOffset, bucketEntries);
        int entryBitOffset = bucketBitOffset + bucketEntries;
        int storedFingerprint;
        int entriesPopulatedMask;
        for(int i = 0; i < bucketEntries; i++) {
            entriesPopulatedMask = 1 << i;
            if((entriesPopulatedMask & entriesPopulatedBits) != 0) {
                storedFingerprint = getBits(data, entryBitOffset, fingerprintBits);
                if(fingerprint == storedFingerprint) {
                    if(remove) {
                        //just mark the entry as empty - no need to overwrite the fingerprint value
                        setBits(data, bucketBitOffset + i, 1, 0);
                    }
                    return true;
                }
            }
            entryBitOffset += fingerprintBits;
        }
        return false;
    }

    //default access for unit testing
    static int getBits(int data[], int bitOffset, int numBits) {
        if(numBits > Integer.SIZE) {
            throw new IllegalArgumentException("Invalid number of bits");
        }
        int dataIndex = bitOffset / Integer.SIZE;
        int startIndex = bitOffset % Integer.SIZE;
        int endIndex = startIndex + numBits;
        boolean checkNextInt;
        int nextIntNumBits;
        if(endIndex > Integer.SIZE) {
            checkNextInt = true;
            numBits -= (endIndex - Integer.SIZE);
            nextIntNumBits = endIndex - Integer.SIZE;
        } else {
            checkNextInt = false;
            nextIntNumBits = 0;//not used
        }
        int tmp = data[dataIndex];
        tmp >>>= startIndex;
        tmp &= ALL_ONE_BITS >>> (Integer.SIZE - numBits);
        int ret = tmp;

        if(checkNextInt) {
            dataIndex++;
            tmp = data[dataIndex];
            tmp &= ALL_ONE_BITS >>> (Integer.SIZE - nextIntNumBits);
            tmp <<= numBits;
            ret |= tmp;
        }
        return ret;
    }

    //default access for unit testing
    static void setBits(int data[], int bitOffset, int numBits, int bits) {
        if(numBits > Integer.SIZE) {
            throw new IllegalArgumentException("Invalid number of bits");
        }
        int dataIndex = bitOffset / Integer.SIZE;
        int tmpOffset = (bitOffset % Integer.SIZE) - 1;
        //todo this is not the most efficient way to do this
        //todo maybe somethinglike this would work? https://graphics.stanford.edu/~seander/bithacks.html#ConditionalSetOrClearBitsWithoutBranching
        int tmp = data[dataIndex];
        boolean b;
        for(int i = 0; i < numBits; i++) {
            if(++tmpOffset >= Integer.SIZE) {
                data[dataIndex] = tmp;
                tmp = data[++dataIndex];
                tmpOffset = 0;
            }
            b = ((1 << i) & bits) != 0;
            if(b) {
                tmp |= 1 << tmpOffset;
            } else {
                tmp &= ~(ALL_ONE_BITS & (1 << tmpOffset));
            }
        }
        data[dataIndex] = tmp;
    }

    private static int createMask(int numOneBits) {
        int ret = 0;
        for(int i = 0; i < numOneBits; i++) {
            ret |= (1 << i);
        }
        return ret;
    }

    //useful for debugging and testing
    static String toBitString(int i) {
        StringBuilder sb = new StringBuilder();
        for(int j = 0; j < Integer.SIZE; j++) {
            sb.append((i & (1 << j)) != 0 ? '1' : '0');
        }
        return sb.toString();
    }
}
