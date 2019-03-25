package io.nuls.contract.util;

import io.nuls.base.basic.AddressTool;
import io.nuls.contract.model.dto.BlockHeaderDto;

import java.math.BigInteger;
import java.util.List;

public class VMContextTest extends VMContext {

    @Override
    public BlockHeaderDto getBlockHeader(int chainId, String hash) {
        return newDto(chainId);
    }

    private BlockHeaderDto newDto(int chainId) {
        BlockHeaderDto dto = new BlockHeaderDto();
        dto.setHash("00204ed6f9ea133cf5e40edc6c9e9a6a69a4e5e0045bba008b6f157c4765f3b87ce4");
        dto.setPreHash("002079d03c0ae201f3d56714f3df6d27e7015c143dfea019f02830d9e651c8de460e");
        dto.setHeight(chainId);
        dto.setTime(1552988615800L);
        dto.setPackingAddress(AddressTool.getAddress("5MR_2Cj9tfgQpdeF7nDy5wyaGG6MZ35H3rA"));
        return dto;
    }

    @Override
    public BlockHeaderDto getBlockHeader(int chainId, long height) {
        return newDto(chainId);
    }

    @Override
    public BlockHeaderDto getNewestBlockHeader(int chainId) {
        return newDto(chainId);
    }

    @Override
    public BlockHeaderDto getCurrentBlockHeader(int chainId) {
        return newDto(chainId);
    }

    public static void main(String[] args) {
        System.out.println(System.currentTimeMillis());
    }

    @Override
    public BigInteger getBalance(int chainId, byte[] address) {
        return BigInteger.valueOf(chainId);
    }

    @Override
    public BigInteger getTotalBalance(int chainId, byte[] address) {
        return BigInteger.valueOf(chainId);
    }

    @Override
    public long getBestHeight(int chainId) {
        return chainId;
    }

    @Override
    public String getRandomSeed(long endHeight, int count, String algorithm) {
        return super.getRandomSeed(endHeight, count, algorithm);
    }

    @Override
    public String getRandomSeed(long startHeight, long endHeight, String algorithm) {
        return super.getRandomSeed(startHeight, endHeight, algorithm);
    }

    @Override
    public List<byte[]> getRandomSeedList(long endHeight, int seedCount) {
        return super.getRandomSeedList(endHeight, seedCount);
    }

    @Override
    public List<byte[]> getRandomSeedList(long startHeight, long endHeight) {
        return super.getRandomSeedList(startHeight, endHeight);
    }

    @Override
    public long getCustomMaxViewGasLimit(int chainId) {
        return chainId;
    }
}