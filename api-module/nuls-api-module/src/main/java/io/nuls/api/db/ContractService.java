package io.nuls.api.db;

import com.mongodb.client.model.*;
import io.nuls.api.constant.MongoTableConstant;
import io.nuls.api.model.po.db.ContractInfo;
import io.nuls.api.model.po.db.ContractResultInfo;
import io.nuls.api.model.po.db.ContractTxInfo;
import io.nuls.api.model.po.db.PageInfo;
import io.nuls.api.utils.DocumentTransferTool;
import io.nuls.tools.core.annotation.Autowired;
import io.nuls.tools.core.annotation.Component;
import io.nuls.tools.parse.JSONUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.nuls.api.constant.MongoTableConstant.*;

@Component
public class ContractService {

    @Autowired
    private MongoDBService mongoDBService;
    @Autowired
    private AccountService accountService;

    public ContractInfo getContractInfo(int chainId, String contractAddress) {
        Document document = mongoDBService.findOne(CONTRACT_TABLE + chainId, Filters.eq("_id", contractAddress));
        if (document == null) {
            return null;
        }
        ContractInfo contractInfo = ContractInfo.toInfo(document);
        return contractInfo;
    }

    public ContractInfo getContractInfoByHash(int chainId, String txHash) {
        Document document = mongoDBService.findOne(CONTRACT_TABLE + chainId, Filters.eq("createTxHash", txHash));
        if (document == null) {
            return null;
        }
        ContractInfo tokenInfo = DocumentTransferTool.toInfo(document, "contractAddress", ContractInfo.class);
//        tokenInfo.setMethods(JSONUtils.json2list(tokenInfo.getMethodStr(), ContractMethod.class));
//        tokenInfo.setMethodStr(null);
        return tokenInfo;
    }

    public void saveContractInfos(int chainId, Map<String, ContractInfo> contractInfoMap) {
        if (contractInfoMap.isEmpty()) {
            return;
        }
        List<WriteModel<Document>> modelList = new ArrayList<>();
        for (ContractInfo contractInfo : contractInfoMap.values()) {
            Document document = contractInfo.toDocument();
            if (contractInfo.isNew()) {
                modelList.add(new InsertOneModel(document));
            } else {
                modelList.add(new ReplaceOneModel<>(Filters.eq("_id", contractInfo.getContractAddress()), document));
            }
        }
        mongoDBService.bulkWrite(CONTRACT_TABLE + chainId, modelList);
    }

    public void saveContractTxInfos(int chainId, List<ContractTxInfo> contractTxInfos) {
        if (contractTxInfos.isEmpty()) {
            return;
        }
        List<Document> documentList = new ArrayList<>();
        for (ContractTxInfo txInfo : contractTxInfos) {
            Document document = DocumentTransferTool.toDocument(txInfo);
            documentList.add(document);
        }
        mongoDBService.insertMany(CONTRACT_TX_TABLE + chainId, documentList);
    }

    public void saveContractResults(int chainId, List<ContractResultInfo> contractResultInfos) {
        if (contractResultInfos.isEmpty()) {
            return;
        }
        List<Document> documentList = new ArrayList<>();
        for (ContractResultInfo resultInfo : contractResultInfos) {
            Document document = resultInfo.toDocument();
            documentList.add(document);
        }
        mongoDBService.insertMany(CONTRACT_RESULT_TABLE + chainId, documentList);
    }

    public PageInfo<ContractTxInfo> getContractTxList(int chainId, String contractAddress, int type, int pageNumber, int pageSize) {
        Bson filter;
        if (type == 0) {
            filter = Filters.eq("contractAddress", contractAddress);
        } else {
            filter = Filters.and(Filters.eq("contractAddress", contractAddress), Filters.eq("type", type));
        }
        Bson sort = Sorts.descending("time");
        List<Document> docsList = this.mongoDBService.pageQuery(CONTRACT_TX_TABLE + chainId, filter, sort, pageNumber, pageSize);
        List<ContractTxInfo> contractTxInfos = new ArrayList<>();
        long totalCount = mongoDBService.getCount(CONTRACT_TX_TABLE + chainId, filter);
        for (Document document : docsList) {
            contractTxInfos.add(DocumentTransferTool.toInfo(document, ContractTxInfo.class));
        }
        PageInfo<ContractTxInfo> pageInfo = new PageInfo<>(pageNumber, pageSize, totalCount, contractTxInfos);
        return pageInfo;
    }

    public PageInfo<ContractInfo> getContractList(int chainId, int pageNumber, int pageSize, boolean onlyNrc20, boolean isHidden) {
        Bson filter = null;
        if (onlyNrc20) {
            filter = Filters.eq("isNrc20", 1);
        } else if (isHidden) {
            filter = Filters.ne("isNrc20", 1);
        }
        Bson sort = Sorts.descending("createTime");
        List<Document> docsList = this.mongoDBService.pageQuery(CONTRACT_TABLE + chainId, filter, sort, pageNumber, pageSize);
        List<ContractInfo> contractInfos = new ArrayList<>();
        long totalCount = mongoDBService.getCount(CONTRACT_TABLE + chainId, filter);

        for (Document document : docsList) {
            ContractInfo contractInfo = ContractInfo.toInfo(document);
            contractInfos.add(contractInfo);
        }
        PageInfo<ContractInfo> pageInfo = new PageInfo<>(pageNumber, pageSize, totalCount, contractInfos);
        return pageInfo;
    }

    public ContractResultInfo getContractResultInfo(int chainId, String txHash) throws Exception {
        Document document = mongoDBService.findOne(CONTRACT_RESULT_TABLE + chainId, Filters.eq("_id", txHash));
        if (document == null) {
            return null;
        }
        ContractResultInfo contractResultInfo = ContractResultInfo.toInfo(document);
        return contractResultInfo;
    }
}
