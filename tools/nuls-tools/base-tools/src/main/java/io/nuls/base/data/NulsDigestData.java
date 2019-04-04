/*
 * MIT License
 *
 * Copyright (c) 2017-2018 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package io.nuls.base.data;


import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.tools.crypto.HexUtil;
import io.nuls.tools.crypto.Sha256Hash;
import io.nuls.tools.exception.NulsRuntimeException;
import io.nuls.tools.model.ByteUtils;
import io.nuls.tools.exception.NulsException;
import io.nuls.tools.log.Log;
import io.nuls.tools.parse.SerializeUtils;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @author facjas
 */
public class NulsDigestData extends BaseNulsData {

    public static final int HASH_LENGTH = 32;

    protected byte[] digestBytes;

    public NulsDigestData() {
    }

    public NulsDigestData(byte[] bytes) {
        this.digestBytes = bytes;
        if (bytes.length != HASH_LENGTH) {
            throw new RuntimeException("the length is not eq 32 byte");
        }
    }

    @Override
    public int size() {
        return HASH_LENGTH;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer buffer) throws IOException {
        buffer.write(digestBytes);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.digestBytes = byteBuffer.readBytes(HASH_LENGTH);
    }

    public String getDigestHex() {
        try {
            return HexUtil.encode(serialize());
        } catch (IOException e) {
            Log.error(e);
            return null;
        }
    }

    public static NulsDigestData fromDigestHex(String hex) throws NulsException {
        byte[] bytes = HexUtil.decode(hex);
        NulsDigestData hash = new NulsDigestData();
        hash.parse(bytes, 0);
        return hash;
    }

    public static boolean validHash(String hex) {
        try {
            fromDigestHex(hex);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static NulsDigestData calcDigestData(BaseNulsData data) {
        try {
            return calcDigestData(data.serialize());
        } catch (Exception e) {
            Log.error(e);
            return null;
        }
    }

    public byte[] getDigestBytes() {
        return digestBytes;
    }


    public static NulsDigestData calcDigestData(byte[] data) {
        NulsDigestData digestData = new NulsDigestData();
        byte[] content = Sha256Hash.hashTwice(data);
        digestData.digestBytes = content;
        return digestData;
    }

    public static NulsDigestData calcMerkleDigestData(List<NulsDigestData> ddList) {
        int levelOffset = 0;
        for (int levelSize = ddList.size(); levelSize > 1; levelSize = (levelSize + 1) / 2) {
            for (int left = 0; left < levelSize; left += 2) {
                int right = Math.min(left + 1, levelSize - 1);
                byte[] leftBytes = ByteUtils.reverseBytes(ddList.get(levelOffset + left).getDigestBytes());
                byte[] rightBytes = ByteUtils.reverseBytes(ddList.get(levelOffset + right).getDigestBytes());
                byte[] whole = new byte[leftBytes.length + rightBytes.length];
                System.arraycopy(leftBytes, 0, whole, 0, leftBytes.length);
                System.arraycopy(rightBytes, 0, whole, leftBytes.length, rightBytes.length);
                NulsDigestData digest = NulsDigestData.calcDigestData(whole);
                ddList.add(digest);
            }
            levelOffset += levelSize;
        }
        byte[] bytes = ddList.get(ddList.size() - 1).getDigestBytes();
        Sha256Hash merkleHash = Sha256Hash.wrap(bytes);
        NulsDigestData digestData = new NulsDigestData();
        digestData.digestBytes = merkleHash.getBytes();
        return digestData;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * <p>
     * The {@code equals} method implements an equivalence relation
     * on non-null object references:
     * <ul>
     * <li>It is <i>reflexive</i>: for any non-null reference value
     * {@code x}, {@code x.equals(x)} should return
     * {@code true}.
     * <li>It is <i>symmetric</i>: for any non-null reference values
     * {@code x} and {@code y}, {@code x.equals(y)}
     * should return {@code true} if and only if
     * {@code y.equals(x)} returns {@code true}.
     * <li>It is <i>transitive</i>: for any non-null reference values
     * {@code x}, {@code y}, and {@code z}, if
     * {@code x.equals(y)} returns {@code true} and
     * {@code y.equals(z)} returns {@code true}, then
     * {@code x.equals(z)} should return {@code true}.
     * <li>It is <i>consistent</i>: for any non-null reference values
     * {@code x} and {@code y}, multiple invocations of
     * {@code x.equals(y)} consistently return {@code true}
     * or consistently return {@code false}, provided no
     * information used in {@code equals} comparisons on the
     * objects is modified.
     * <li>For any non-null reference value {@code x},
     * {@code x.equals(null)} should return {@code false}.
     * </ul>
     * <p>
     * The {@code equals} method for class {@code Object} implements
     * the most discriminating possible equivalence relation on objects;
     * that is, for any non-null reference values {@code x} and
     * {@code y}, this method returns {@code true} if and only
     * if {@code x} and {@code y} refer to the same object
     * ({@code x == y} has the value {@code true}).
     * <p>
     * Note that it is generally necessary to override the {@code hashCode}
     * method whenever this method is overridden, so as to maintain the
     * general contract for the {@code hashCode} method, which states
     * that equal objects must have equal hash codes.
     *
     * @param obj the reference object with which to compare.
     * @return {@code true} if this object is the same as the obj
     * argument; {@code false} otherwise.
     * @see #hashCode()
     * @see HashMap
     */
    @Override
    public boolean equals(Object obj) {

        if (obj == null) {
            return false;
        }
        if (!(obj instanceof NulsDigestData)) {
            return false;
        }
        if(null == this.getDigestBytes() || null == ((NulsDigestData) obj).getDigestBytes()){
            return false;
        }
        return Arrays.equals(this.getDigestBytes(), ((NulsDigestData) obj).getDigestBytes());
    }

    @Override
    public String toString() {
        return getDigestHex();
    }

    /**
     * Returns a hash code value for the object. This method is
     * supported for the benefit of hash tables such as those provided by
     * {@link HashMap}.
     * <p>
     * The general contract of {@code hashCode} is:
     * <ul>
     * <li>Whenever it is invoked on the same object more than once during
     * an execution of a Java application, the {@code hashCode} method
     * must consistently return the same integer, provided no information
     * used in {@code equals} comparisons on the object is modified.
     * This integer need not remain consistent from one execution of an
     * application to another execution of the same application.
     * <li>If two objects are equal according to the {@code equals(Object)}
     * method, then calling the {@code hashCode} method on each of
     * the two objects must produce the same integer result.
     * <li>It is <em>not</em> required that if two objects are unequal
     * according to the {@link Object#equals(Object)}
     * method, then calling the {@code hashCode} method on each of the
     * two objects must produce distinct integer results.  However, the
     * programmer should be aware that producing distinct integer results
     * for unequal objects may improve the performance of hash tables.
     * </ul>
     * <p>
     * As much as is reasonably practical, the hashCode method defined by
     * class {@code Object} does return distinct integers for distinct
     * objects. (This is typically implemented by converting the internal
     * address of the object into an integer, but this implementation
     * technique is not required by the
     * Java&trade; programming language.)
     *
     * @return a hash code value for this object.
     * @see Object#equals(Object)
     * @see System#identityHashCode
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(this.getDigestBytes());
    }
}
