package io.nuls.poc.storage.impl;

import io.nuls.base.data.NulsDigestData;
import io.nuls.db.model.Entry;
import io.nuls.db.service.RocksDBService;
import io.nuls.poc.constant.ConsensusConstant;
import io.nuls.poc.model.po.AgentPo;
import io.nuls.poc.storage.AgentStorageService;
import io.nuls.poc.utils.manager.ConsensusManager;
import io.nuls.poc.utils.util.PoConvertUtil;
import io.nuls.tools.core.annotation.Service;
import io.nuls.tools.log.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * 节点信息管理实现类
 * @author tag
 * 2018/11/06
 * */
@Service
public class AgentStorageServiceImpl implements AgentStorageService{

    @Override
    /**
     * 保存节点
     * @param  agentPo   节点对象
     * */
    public boolean save(AgentPo agentPo,int chainID) {
        if(agentPo == null || agentPo.getHash() == null){
            return false;
        }
        try {
            byte[] key = agentPo.getHash().serialize();
            byte[] value = agentPo.serialize();
            boolean dbSuccess = RocksDBService.put(ConsensusConstant.DB_NAME_CONSENSUS_AGENT+chainID,key,value);
            if(!dbSuccess){
                return false;
            }
            //更新缓存
            ConsensusManager.getInstance().addAgent(chainID,PoConvertUtil.poToAgent(agentPo));
            return true;
        }catch (Exception e){
            Log.error(e);
            return  false;
        }
    }

    @Override
    /**
     * 根据节点HASH查询节点
     * @param  hash   节点hash
     * */
    public AgentPo get(NulsDigestData hash,int chainID) {
        if(hash == null){
            return  null;
        }
        try {
            byte[] key = hash.serialize();
            byte[] value = RocksDBService.get(ConsensusConstant.DB_NAME_CONSENSUS_AGENT+chainID,key);
            if(value == null){
                return  null;
            }
            AgentPo agentPo = new AgentPo();
            agentPo.parse(value,0);
            agentPo.setHash(hash);
            return agentPo;
        }catch (Exception e){
            Log.error(e);
            return  null;
        }
    }

    @Override
    /**
     * 根据节点hash删除节点
     * @param hash  节点hash
     * */
    public boolean delete(NulsDigestData hash,int chainID) {
        if(hash == null){
            return  false;
        }
        try {
            byte[] key = hash.serialize();
            boolean dbSuccess = RocksDBService.delete(ConsensusConstant.DB_NAME_CONSENSUS_AGENT+chainID,key);
            if(!dbSuccess){
                return false;
            }
            //更新缓存
            ConsensusManager.getInstance().removeAgent(chainID,hash);
            return true;
        }catch (Exception e){
            Log.error(e);
            return  false;
        }
    }

    @Override
    /**
     * 获取所有节点信息
     * */
    public List<AgentPo> getList(int chainID) throws  Exception{
        try {
            List<Entry<byte[], byte[]>> list = RocksDBService.entryList(ConsensusConstant.DB_NAME_CONSENSUS_AGENT+chainID);
            List<AgentPo> agentList = new ArrayList<>();
            for (Entry<byte[], byte[]> entry:list) {
                AgentPo po = new AgentPo();
                po.parse(entry.getValue(),0);
                NulsDigestData hash = new NulsDigestData();
                hash.parse(entry.getKey(),0);
                po.setHash(hash);
                agentList.add(po);
            }
            return  agentList;
        }catch (Exception e){
            Log.error(e);
            throw e;
        }
    }

    @Override
    /**
     * 获取当前网络节点数量
     * */
    public int size(int chainID) {
        List<byte[]> keyList = RocksDBService.keyList(ConsensusConstant.DB_NAME_CONSENSUS_AGENT+chainID);
        if(keyList != null){
            return keyList.size();
        }
        return 0;
    }

    /*@Override
    public void afterPropertiesSet() throws NulsException {
        try {
            RocksDBService.createTable(ConsensusConstant.DB_NAME_CONSENSUS_AGENT);
        }catch (Exception e){
            Log.error(e);
            throw new NulsException(e);
        }
    }*/
}
