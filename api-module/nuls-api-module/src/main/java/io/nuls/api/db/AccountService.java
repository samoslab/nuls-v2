package io.nuls.api.db;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.*;
import io.nuls.api.cache.ApiCache;
import io.nuls.api.constant.MongoTableConstant;
import io.nuls.api.manager.CacheManager;
import io.nuls.api.model.po.db.AccountInfo;
import io.nuls.api.model.po.db.PageInfo;
import io.nuls.api.model.po.db.TxRelationInfo;
import io.nuls.api.utils.DocumentTransferTool;
import io.nuls.tools.core.annotation.Autowired;
import io.nuls.tools.core.annotation.Component;
import io.nuls.tools.model.BigIntegerUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static io.nuls.api.constant.MongoTableConstant.ACCOUNT_TABLE;
import static io.nuls.api.constant.MongoTableConstant.TX_RELATION_TABLE;

@Component
public class AccountService {

    @Autowired
    private MongoDBService mongoDBService;

    public void initCache() {
        for (ApiCache apiCache : CacheManager.getApiCaches().values()) {
            List<Document> documentList = mongoDBService.query(ACCOUNT_TABLE + apiCache.getChainInfo().getChainId());
            for (Document document : documentList) {
                AccountInfo accountInfo = DocumentTransferTool.toInfo(document, "address", AccountInfo.class);
                apiCache.addAccountInfo(accountInfo);
            }
        }
    }

    public AccountInfo getAccountInfo(int chainId, String address) {
        return CacheManager.getCache(chainId).getAccountInfo(address);
    }

    public void saveAccounts(int chainId, Map<String, AccountInfo> accountInfoMap) {
        if (accountInfoMap.isEmpty()) {
            return;
        }
        List<WriteModel<Document>> modelList = new ArrayList<>();
        for (AccountInfo accountInfo : accountInfoMap.values()) {
            Document document = DocumentTransferTool.toDocument(accountInfo, "address");
            document.put("totalBalance", BigIntegerUtils.bigIntegerToString(accountInfo.getTotalBalance(), 32));
            if (accountInfo.isNew()) {
                modelList.add(new InsertOneModel(document));
                accountInfo.setNew(false);
                ApiCache apiCache = CacheManager.getCache(chainId);
                apiCache.addAccountInfo(accountInfo);
            } else {
                modelList.add(new ReplaceOneModel<>(Filters.eq("_id", accountInfo.getAddress()), document));
            }
        }
        mongoDBService.bulkWrite(ACCOUNT_TABLE + chainId, modelList);
    }

    public PageInfo<AccountInfo> pageQuery(int chainId, int pageNumber, int pageSize) {
        List<Document> docsList = this.mongoDBService.pageQuery(ACCOUNT_TABLE + chainId, pageNumber, pageSize);
        List<AccountInfo> accountInfoList = new ArrayList<>();
        long totalCount = mongoDBService.getCount(ACCOUNT_TABLE + chainId);
        for (Document document : docsList) {
            accountInfoList.add(DocumentTransferTool.toInfo(document, "address", AccountInfo.class));
        }
        PageInfo<AccountInfo> pageInfo = new PageInfo<>(pageNumber, pageSize, totalCount, accountInfoList);
        return pageInfo;
    }

    public PageInfo<TxRelationInfo> getAccountTxs(int chainId, String address, int pageIndex, int pageSize, int type, boolean isMark) {
        Bson filter = null;
        Bson addressFilter = Filters.eq("address", address);

        if (type == 0 && isMark) {
            filter = Filters.and(addressFilter, Filters.ne("type", 1));
        } else if (type > 0) {
            filter = Filters.and(addressFilter, Filters.eq("type", type));
        } else {
            filter = addressFilter;
        }
//        long start = System.currentTimeMillis();
        long totalCount = mongoDBService.getCount(TX_RELATION_TABLE + chainId, filter);
//        Log.info("count use:{}ms",System.currentTimeMillis()-start);
//        start = System.currentTimeMillis();
        List<Document> docsList = this.mongoDBService.pageQuery(TX_RELATION_TABLE + chainId, filter, Sorts.descending("height", "createTime"), pageIndex, pageSize);
//        Log.info("query use:{}ms",System.currentTimeMillis()-start);
        List<TxRelationInfo> txRelationInfoList = new ArrayList<>();
        for (Document document : docsList) {
            txRelationInfoList.add(DocumentTransferTool.toInfo(document, TxRelationInfo.class));
        }
        PageInfo<TxRelationInfo> pageInfo = new PageInfo<>(pageIndex, pageSize, totalCount, txRelationInfoList);
        return pageInfo;
    }

    public PageInfo<AccountInfo> getCoinRanking(int pageIndex, int pageSize, int sortType, int chainId) {
        Bson sort;
        if (sortType == 0) {
            sort = Sorts.descending("totalBalance");
        } else {
            sort = Sorts.ascending("totalBalance");
        }
        List<AccountInfo> accountInfoList = new ArrayList<>();
        Bson filter = Filters.ne("totalBalance", 0);
        List<Document> docsList = this.mongoDBService.pageQuery(ACCOUNT_TABLE + chainId, filter, sort, pageIndex, pageSize);
        long totalCount = mongoDBService.getCount(ACCOUNT_TABLE + chainId, filter);
        for (Document document : docsList) {
            AccountInfo accountInfo = DocumentTransferTool.toInfo(document, "address", AccountInfo.class);
//            List<Output> outputs = utxoService.getAccountUtxos(accountInfo.getAddress());
//            CalcUtil.calcBalance(accountInfo, outputs, blockHeaderService.getBestBlockHeight());
            accountInfoList.add(accountInfo);
        }
        PageInfo<AccountInfo> pageInfo = new PageInfo<>(pageIndex, pageSize, totalCount, accountInfoList);
        return pageInfo;
    }

    public BigInteger getAllAccountBalance(int chainId) {
        List<Document> documentList = mongoDBService.query(ACCOUNT_TABLE + chainId);

        BigInteger totalBalance = BigInteger.ZERO;
        for (Document document : documentList) {
            totalBalance = totalBalance.add(new BigInteger(document.getString("totalBalance")));
        }
        return totalBalance;
    }

    public BigInteger getAccountTotalBalance(int chainId, String address) {
        AccountInfo accountInfo = getAccountInfo(chainId, address);
        if (accountInfo == null) {
            return BigInteger.ZERO;
        }
        return accountInfo.getTotalBalance();
    }
}
