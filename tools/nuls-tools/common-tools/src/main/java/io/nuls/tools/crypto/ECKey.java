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
package io.nuls.tools.crypto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.primitives.UnsignedBytes;
import io.nuls.tools.data.ObjectUtils;
import io.nuls.tools.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.asn1.ASN1InputStream;
import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.DERSequenceGenerator;
import org.spongycastle.asn1.DLSequence;
import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.crypto.AsymmetricCipherKeyPair;
import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.ec.CustomNamedCurves;
import org.spongycastle.crypto.generators.ECKeyPairGenerator;
import org.spongycastle.crypto.params.ECDomainParameters;
import org.spongycastle.crypto.params.ECKeyGenerationParameters;
import org.spongycastle.crypto.params.ECPrivateKeyParameters;
import org.spongycastle.crypto.params.ECPublicKeyParameters;
import org.spongycastle.crypto.signers.ECDSASigner;
import org.spongycastle.crypto.signers.HMacDSAKCalculator;
import org.spongycastle.math.ec.ECPoint;
import org.spongycastle.math.ec.FixedPointCombMultiplier;
import org.spongycastle.math.ec.FixedPointUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Comparator;

/**
 * 椭圆曲线加密
 *
 * @author tag
 */

public class ECKey {
    private static final Logger log = LoggerFactory.getLogger(ECKey.class);
    private static final X9ECParameters CURVE_PARAMS = CustomNamedCurves.getByName("secp256k1");
    public static final ECDomainParameters CURVE;
    public static final BigInteger HALF_CURVE_ORDER;
    /**
     * 随机种子
     */
    private static final SecureRandom SECURE_RANDOM;

    static {
        if (HexUtil.isAndroidRuntime()) {
            new LinuxSecureRandom();
        }

        FixedPointUtil.precompute(CURVE_PARAMS.getG(), 12);
        CURVE = new ECDomainParameters(CURVE_PARAMS.getCurve(), CURVE_PARAMS.getG(), CURVE_PARAMS.getN(),
                CURVE_PARAMS.getH());
        HALF_CURVE_ORDER = CURVE_PARAMS.getN().shiftRight(1);
        SECURE_RANDOM = new SecureRandom();
    }

    /**
     * 私匙
     **/
    protected final BigInteger priv;
    /**
     * 公匙
     **/
    private final ECPoint pub;

    protected EncryptedData encryptedPrivateKey;

    protected long creationTimeSeconds;

    public ECKey() {
        this(SECURE_RANDOM);
    }

    /**
     * 使用给定的{@link SecureRandom}对象生成一个全新的密钥对。点压缩是这样使用的
     * 结果公钥将是33个字节（32个用于坐标，1个字节用于表示y位）。
     **/
    public ECKey(SecureRandom secureRandom) {
        ECKeyPairGenerator generator = new ECKeyPairGenerator();
        ECKeyGenerationParameters keygenParams = new ECKeyGenerationParameters(CURVE, secureRandom);
        generator.init(keygenParams);
        AsymmetricCipherKeyPair keypair = generator.generateKeyPair();
        ECPrivateKeyParameters privParams = (ECPrivateKeyParameters) keypair.getPrivate();
        ECPublicKeyParameters pubParams = (ECPublicKeyParameters) keypair.getPublic();
        priv = privParams.getD();
        pub = pubParams.getQ();
        creationTimeSeconds = System.currentTimeMillis();
    }

    /**
     * 根据私匙和公匙创建
     *
     * @param priv 私钥
     * @param pub  公钥
     */
    private ECKey(BigInteger priv, ECPoint pub) {
        if (priv != null) {
            //私匙不应该是0和1
            HexUtil.checkState(!priv.equals(BigInteger.ZERO));
            HexUtil.checkState(!priv.equals(BigInteger.ONE));
        }
        this.priv = priv;
        this.pub = HexUtil.checkNotNull(pub);
        creationTimeSeconds = System.currentTimeMillis();
    }

    /**
     * 根据私匙创建密码器
     * @param privKey    private key
     * @return ECKey
     */
    public static ECKey fromPrivate(BigInteger privKey) {
        return fromPrivate(privKey, true);
    }

    /**
     * 根据私匙创建密码器，并选择是否压缩公匙
     *
     * @param privKey    private key
     * @param compressed compressed
     * @return ECKey
     */
    public static ECKey fromPrivate(BigInteger privKey, boolean compressed) {
        ECPoint point = publicPointFromPrivate(privKey);
        return new ECKey(privKey, getPointWithCompression(point, compressed));
    }


    /**
     * 创建一个不能用于签名的ECKey，仅从给定的编码点验证签名,pub的压缩状态将被保留。
     * @param pubKey    public key
     * @return ECKey
     */
    public static ECKey fromPublicOnly(byte[] pubKey) {
        return new ECKey(null, CURVE.getCurve().decodePoint(pubKey));
    }

    /**
     * 根据ECPoint(公钥)创建密码器
     * @param pub    ECPoint公钥
     * @return ECKey
     */
    public static ECKey fromPublicOnly(ECPoint pub) {
        return new ECKey(null, pub);
    }

    /**
     * 根据私钥生成ECPoint公钥
     * @param privKey    private key
     * @return ECPoint
     */
    public static ECPoint publicPointFromPrivate(BigInteger privKey) {
        if (privKey.bitLength() > CURVE.getN().bitLength()) {
            privKey = privKey.mod(CURVE.getN());
        }
        return new FixedPointCombMultiplier().multiply(CURVE.getG(), privKey);
    }

    /**
     * 根据EncryptedData和公钥生成ECKey
     * @param encryptedPrivateKey    私钥封装类
     * @param pubKey                 公钥
     * @return ECPoint
     */
    public static ECKey fromEncrypted(EncryptedData encryptedPrivateKey, byte[] pubKey) {
        ECKey key = fromPublicOnly(pubKey);
        ObjectUtils.canNotEmpty(encryptedPrivateKey, "encryptedPrivateKey can not null!");
        key.encryptedPrivateKey = encryptedPrivateKey;
        return key;
    }

    /**
     * 获取公钥的byte[],指定是否压缩
     * @param compressed  是否压缩
     * @return  byte[]
     * */
    protected byte[] getPubKey(boolean compressed) {
        return pub.getEncoded(compressed);
    }

    /**
     * 获取公匙内容,默认的公匙是压缩的
     *
     * @return byte[]
     */
    public byte[] getPubKey() {
        return getPubKey(true);
    }

    /**
     * 获取私匙对应的随机数
     *
     * @return BigInteger
     */
    @JsonIgnore
    public BigInteger getPrivKey() {
        if (priv == null) {
            throw new MissingPrivateKeyException();
        }
        return priv;
    }

    /**
     * 获取私匙的内容
     *
     * @return byte[]
     */
    @JsonIgnore
    public byte[] getPrivKeyBytes() {
        return getPrivKey().toByteArray();
    }

    /**
     * 获取私匙转16进制后的字符串
     *
     * @return String
     */
    @JsonIgnore
    public String getPrivateKeyAsHex() {
        return HexUtil.encode(getPrivKeyBytes());
    }

    /**
     * 获取公匙转16进制后的字符串，压缩过的
     *
     * @return String
     */
    public String getPublicKeyAsHex() {
        return getPublicKeyAsHex(false);
    }

    /**
     * 获取公匙转16进制后的字符串，指定是否压缩
     * @param compressed  是否压缩
     * @return String
     * */
    public String getPublicKeyAsHex(boolean compressed) {
        return HexUtil.encode(getPubKey(compressed));
    }

    /**
     * 压缩公匙
     * @param point      压缩前的公钥public key
     * @param compressed 是否压缩
     * @return   ECPoint 压缩后的公钥
     **/
    private static ECPoint getPointWithCompression(ECPoint point, boolean compressed) {
        if (point.isCompressed() == compressed) {
            return point;
        }
        point = point.normalize();
        BigInteger x = point.getAffineXCoord().toBigInteger();
        BigInteger y = point.getAffineYCoord().toBigInteger();
        return CURVE.getCurve().createPoint(x, y, compressed);
    }

    /**
     * 验证数据签名
     * @param data        需验证的数据
     * @param signature   签名
     * @param pub         公钥
     * @return boolean        验证是否通过
     * */
    public static boolean verify(byte[] data, ECDSASignature signature, byte[] pub) {
        ECDSASigner signer = new ECDSASigner();
        ECPublicKeyParameters params = new ECPublicKeyParameters(CURVE.getCurve().decodePoint(pub), CURVE);
        signer.init(false, params);
        try {
            return signer.verifySignature(data, signature.r, signature.s);
        } catch (NullPointerException e) {
            log.error("Caught NPE inside bouncy castle", e);
            return false;
        }
    }


    /**
     * 验证数据签名
     * @param data        需验证的数据
     * @param signature   签名
     * @param pub         公钥
     * @return boolean        验证是否通过
     * */
    public static boolean verify(byte[] data, byte[] signature, byte[] pub) {
        return verify(data, ECDSASignature.decodeFromDER(signature), pub);
    }

    /**
     * 用当前ECKey公钥验证签名
     * @param hash        需验证的数据
     * @param signature   签名
     * @return boolean    验证是否通过
     * */
    public boolean verify(byte[] hash, byte[] signature) {
        return ECKey.verify(hash, signature, getPubKey());
    }

    public static class MissingPrivateKeyException extends RuntimeException {
        private static final long serialVersionUID = 2789844760773725676L;
    }

    /**
     * 静态内部类 签名封装类
     * */
    public static class ECDSASignature {
        public final BigInteger r, s;

        public ECDSASignature(BigInteger r, BigInteger s) {
            this.r = r;
            this.s = s;
        }

        public byte[] encodeToDER() {
            try {
                return derByteStream().toByteArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public static ECDSASignature decodeFromDER(byte[] bytes) {
            ASN1InputStream decoder = null;
            try {
                decoder = new ASN1InputStream(bytes);
                DLSequence seq = (DLSequence) decoder.readObject();
                if (seq == null) {
                    throw new RuntimeException("Reached past end of ASN.1 stream.");
                }
                ASN1Integer r, s;
                try {
                    r = (ASN1Integer) seq.getObjectAt(0);
                    s = (ASN1Integer) seq.getObjectAt(1);
                } catch (ClassCastException e) {
                    throw new IllegalArgumentException(e);
                }
                // OpenSSL deviates from the DER spec by interpreting these values as unsigned, though they should not be
                // Thus, we always use the positive versions. See: http://r6.ca/blog/20111119T211504Z.html
                return new ECDSASignature(r.getPositiveValue(), s.getPositiveValue());
            } catch (IOException e) {
                Log.error(e);
                throw new RuntimeException(e);
            } finally {
                if (decoder != null) {
                    try {
                        decoder.close();
                    } catch (IOException x) {
                    }
                }
            }
        }

        protected ByteArrayOutputStream derByteStream() throws IOException {
            // Usually 70-72 bytes.
            ByteArrayOutputStream bos = new ByteArrayOutputStream(72);
            DERSequenceGenerator seq = new DERSequenceGenerator(bos);
            seq.addObject(new ASN1Integer(r));
            seq.addObject(new ASN1Integer(s));
            seq.close();
            return bos;
        }

        public boolean isCanonical() {
            return s.compareTo(HALF_CURVE_ORDER) <= 0;
        }

        /**
         * Will automatically adjust the S component to be less than or equal to half the curve order, if necessary.
         * This is required because for every signature (r,s) the signature (r, -s (mod N)) is a valid signature of
         * the same validator. However, we dislike the ability to modify the bits of a Bitcoin transaction after it's
         * been signed, as that violates various assumed invariants. Thus in future only one of those forms will be
         * considered legal and the other will be banned.
         *
         * @return ECDSASignature
         */
        public ECDSASignature toCanonicalised() {
            if (!isCanonical()) {
                // The order of the curve is the number of valid points that exist on that curve. If S is in the upper
                // half of the number of valid points, then bring it back to the lower half. Otherwise, imagine that
                //    N = 10
                //    s = 8, so (-8 % 10 == 2) thus both (r, 8) and (r, 2) are valid solutions.
                //    10 - 8 == 2, giving us always the latter solution, which is canonical.
                return new ECDSASignature(r, CURVE.getN().subtract(s));
            } else {
                return this;
            }
        }
    }

    /**
     * 用私钥对数据进行签名
     * @param hash    需签名数据
     * @return byte[] 签名
     * */
    public byte[] sign(byte[] hash) {
        return sign(hash, null);
    }

    /**
     * 用私钥对数据进行签名
     * @param hash    需签名数据
     * @param aesKey  私钥
     * @return byte[] 签名
     * */
    public byte[] sign(Sha256Hash hash, BigInteger aesKey) {
        return doSign(hash.getBytes(), priv);
    }

    /**
     * 用私钥对数据进行签名
     * @param hash    需签名数据
     * @param aesKey  私钥
     * @return byte[] 签名
     * */
    public byte[] sign(byte[] hash, BigInteger aesKey) {
        return doSign(hash, priv);
    }

    /**
     * 用私钥对数据进行签名
     * @param input    需签名数据
     * @param privateKeyForSigning  私钥
     * @return byte[] 签名
     * */
    protected byte[] doSign(byte[] input, BigInteger privateKeyForSigning) {
        HexUtil.checkNotNull(privateKeyForSigning);
        ECDSASigner signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
        ECPrivateKeyParameters privKey = new ECPrivateKeyParameters(privateKeyForSigning, CURVE);
        signer.init(true, privKey);
        BigInteger[] components = signer.generateSignature(input);
        return new ECDSASignature(components[0], components[1]).toCanonicalised().encodeToDER();
    }

    /**
     * 该ECKey对象是否有私钥
     * @return  boolean 有私钥返回true，否则返回false
     * */
    public boolean hasPrivKey() {
        return priv != null;
    }

    /**
     * 该ECKey对象是否已经压缩过
     * @return  boolean 已压缩返回true，否则返回false
     * */
    public boolean isCompressed() {
        return pub.isCompressed();
    }

    public void setCreationTimeSeconds(long creationTimeSeconds) {
        this.creationTimeSeconds = creationTimeSeconds;
    }

    public long getCreationTimeSeconds() {
        return creationTimeSeconds;
    }

    public EncryptedData getEncryptedPrivateKey() {
        return encryptedPrivateKey;
    }

    public void setEncryptedPrivateKey(EncryptedData encryptedPrivateKey) {
        this.encryptedPrivateKey = encryptedPrivateKey;
    }

    /**
     * 验证16进制私钥字符串是否正确
     * @return  boolean 正确返回true,否则返回false
     * */
    public static boolean isValidPrivteHex(String privateHex) {
        int len = privateHex.length();
        if (len % 2 == 1) {
            return false;
        }

        if (len < 60 || len > 66) {
            return false;
        }
        return true;
    }

    /**
     * ECKey比较器（按公钥比较）
     * */
    public static final Comparator<ECKey> PUBKEY_COMPARATOR = new Comparator<ECKey>() {
        private Comparator<byte[]> comparator = UnsignedBytes.lexicographicalComparator();

        @Override
        public int compare(ECKey k1, ECKey k2) {
            return comparator.compare(k1.getPubKey(), k2.getPubKey());
        }
    };
}
