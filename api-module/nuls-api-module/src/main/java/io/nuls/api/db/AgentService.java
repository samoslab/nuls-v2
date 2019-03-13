package io.nuls.api.db;

import com.mongodb.client.model.*;
import io.nuls.api.ApiContext;
import io.nuls.api.cache.ApiCache;
import io.nuls.api.constant.MongoTableConstant;
import io.nuls.api.manager.CacheManager;
import io.nuls.api.model.po.db.AgentInfo;
import io.nuls.api.model.po.db.AliasInfo;
import io.nuls.api.model.po.db.PageInfo;
import io.nuls.api.utils.DocumentTransferTool;
import io.nuls.tools.core.annotation.Autowired;
import io.nuls.tools.core.annotation.Component;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static io.nuls.api.constant.MongoTableConstant.AGENT_TABLE;

@Component
public class AgentService {

    @Autowired
    private MongoDBService mongoDBService;
    @Autowired
    private AliasService aliasService;

    public void initCache() {
        for (ApiCache apiCache : CacheManager.getApiCaches().values()) {
            List<Document> documentList = mongoDBService.query(AGENT_TABLE + apiCache.getChainInfo().getChainId());
            for (Document document : documentList) {
                AgentInfo agentInfo = DocumentTransferTool.toInfo(document, "txHash", AgentInfo.class);
                apiCache.addAgentInfo(agentInfo);
            }
        }
    }

    public AgentInfo getAgentByHash(int chainID, String agentHash) {
        AgentInfo agentInfo = CacheManager.getCache(chainID).getAgentInfo(agentHash);
        if (agentInfo == null) {
            Document document = mongoDBService.findOne(AGENT_TABLE + chainID, Filters.eq("_id", agentHash));
            agentInfo = DocumentTransferTool.toInfo(document, "txHash", AgentInfo.class);
            CacheManager.getCache(chainID).addAgentInfo(agentInfo);
        }
        return agentInfo;
    }

    public AgentInfo getAgentByPackingAddress(int chainID, String packingAddress) {
        Collection<AgentInfo> agentInfos = CacheManager.getCache(chainID).getAgentMap().values();
        AgentInfo info = null;
        for (AgentInfo agent : agentInfos) {
            if (!agent.getPackingAddress().equals(packingAddress)) {
                continue;
            }
            if (null == info || agent.getCreateTime() > info.getCreateTime()) {
                info = agent;
            }
        }
        return info;
    }

    public AgentInfo getAgentByAgentAddress(int chainID, String agentAddress) {
        Collection<AgentInfo> agentInfos = CacheManager.getCache(chainID).getAgentMap().values();
        AgentInfo info = null;
        for (AgentInfo agent : agentInfos) {
            if (!agentAddress.equals(agent.getAgentAddress())) {
                continue;
            }
            if (null == info || agent.getCreateTime() > info.getCreateTime()) {
                info = agent;
            }
        }
        return info;
    }

    public void saveAgentList(int chainID, List<AgentInfo> agentInfoList) {
        if (agentInfoList.isEmpty()) {
            return;
        }
        List<WriteModel<Document>> modelList = new ArrayList<>();
        for (AgentInfo agentInfo : agentInfoList) {
            Document document = DocumentTransferTool.toDocument(agentInfo, "txHash");

            if (agentInfo.isNew()) {
                modelList.add(new InsertOneModel(document));
                agentInfo.setNew(false);
                ApiCache cache = CacheManager.getCache(chainID);
                cache.addAgentInfo(agentInfo);
            } else {
                modelList.add(new ReplaceOneModel<>(Filters.eq("_id", agentInfo.getTxHash()), document));
            }
        }
        mongoDBService.bulkWrite(AGENT_TABLE + chainID, modelList);
    }

    public List<AgentInfo> getAgentList(int chainId, long startHeight) {
        ApiCache apiCache = CacheManager.getCache(chainId);
        Collection<AgentInfo> agentInfos = apiCache.getAgentMap().values();
        List<AgentInfo> resultList = new ArrayList<>();
        for (AgentInfo agent : agentInfos) {
            if (agent.getDeleteHash() != null && agent.getDeleteHeight() <= startHeight) {
                continue;
            }
            if (agent.getBlockHeight() > startHeight) {
                continue;
            }
            resultList.add(agent);
        }

//        Bson bson = Filters.and(Filters.lte("blockHeight", startHeight), Filters.or(Filters.eq("deleteHeight", 0), Filters.gt("deleteHeight", startHeight)));
//
//        List<Document> list = this.mongoDBService.query(MongoTableName.AGENT_INFO, bson);

//        for (Document document : list) {
//            AgentInfo agentInfo = DocumentTransferTool.toInfo(document, "txHash", AgentInfo.class);
//            AliasInfo alias = aliasService.getAliasByAddress(agentInfo.getAgentAddress());
//            if (alias != null) {
//                agentInfo.setAgentAlias(alias.getAlias());
//            }
//            resultList.add(agentInfo);
//        }

        return resultList;
    }

    public PageInfo<AgentInfo> getAgentList(int chainId, int type, int pageNumber, int pageSize) {
        Bson filter = null;
        Bson deleteFilter = Filters.eq("deleteHeight", 0);
        if (type == 1) {
            List list = new ArrayList<>(ApiContext.DEVELOPER_NODE_ADDRESS);
            list.addAll(ApiContext.AMBASSADOR_NODE_ADDRESS);
            filter = Filters.and(Filters.nin("agentAddress", list.toArray()), deleteFilter);
        } else if (type == 2) {
            filter = Filters.and(Filters.in("agentAddress", ApiContext.DEVELOPER_NODE_ADDRESS.toArray()), deleteFilter);
        } else if (type == 3) {
            filter = Filters.and(Filters.in("agentAddress", ApiContext.AMBASSADOR_NODE_ADDRESS.toArray()), deleteFilter);
        } else {
            filter = deleteFilter;
        }
        long totalCount = this.mongoDBService.getCount(AGENT_TABLE + chainId, filter);
        List<Document> docsList = this.mongoDBService.pageQuery(AGENT_TABLE + chainId, filter, Sorts.descending("createTime"), pageNumber, pageSize);
        List<AgentInfo> agentInfoList = new ArrayList<>();
        for (Document document : docsList) {
            AgentInfo agentInfo = DocumentTransferTool.toInfo(document, "txHash", AgentInfo.class);
            AliasInfo alias = aliasService.getAliasByAddress(chainId, agentInfo.getAgentAddress());
            if (alias != null) {
                agentInfo.setAgentAlias(alias.getAlias());
            }
            agentInfoList.add(agentInfo);
            if (agentInfo.getType() == 0 && null != agentInfo.getAgentAddress()) {
                if (ApiContext.DEVELOPER_NODE_ADDRESS.contains(agentInfo.getAgentAddress())) {
                    agentInfo.setType(2);
                } else if (ApiContext.AMBASSADOR_NODE_ADDRESS.contains(agentInfo.getAgentAddress())) {
                    agentInfo.setType(3);
                } else {
                    agentInfo.setType(1);
                }
            }
        }
        PageInfo<AgentInfo> pageInfo = new PageInfo<>(pageNumber, pageSize, totalCount, agentInfoList);
        return pageInfo;
    }

    public long agentsCount(int chainId, long startHeight) {
        ApiCache apiCache = CacheManager.getCache(chainId);
        Collection<AgentInfo> agentInfos = apiCache.getAgentMap().values();
        long count = 0;
        for (AgentInfo agent : agentInfos) {
            if (agent.getDeleteHash() != null && agent.getDeleteHeight() <= startHeight) {
                continue;
            }
            if (agent.getBlockHeight() > startHeight) {
                continue;
            }
            count++;
        }
        return count;
//        Bson bson = Filters.and(Filters.lte("blockHeight", startHeight), Filters.or(Filters.eq("deleteHeight", 0), Filters.gt("deleteHeight", startHeight)));
//        return this.mongoDBService.getCount(MongoTableName.AGENT_INFO, bson);
    }
}
