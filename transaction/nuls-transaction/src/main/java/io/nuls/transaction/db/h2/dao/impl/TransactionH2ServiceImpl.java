package io.nuls.transaction.db.h2.dao.impl;

import com.github.pagehelper.PageHelper;
import io.nuls.base.data.Page;
import io.nuls.base.data.Transaction;
import io.nuls.h2.utils.SearchOperator;
import io.nuls.h2.utils.Searchable;
import io.nuls.tools.core.annotation.Service;
import io.nuls.tools.exception.NulsException;
import io.nuls.transaction.constant.TxConstant;
import io.nuls.transaction.db.h2.dao.TransactionH2Service;
import io.nuls.transaction.db.h2.dao.impl.mapper.TransactionMapper;
import io.nuls.transaction.model.po.TransactionsPO;
import io.nuls.transaction.model.split.TxTable;
import io.nuls.transaction.utils.TxUtil;
import org.apache.ibatis.session.SqlSession;
import org.h2.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: Charlie
 * @date: 2018/11/14
 */
@Service
public class TransactionH2ServiceImpl extends BaseService<TransactionMapper> implements TransactionH2Service {

    @Override
    public Page<TransactionsPO> getTxs(String address, Integer assetChainId, Integer assetId, Integer type, int pageNumber, int pageSize) {
        return getTxs(address, assetChainId, assetId, type, null, null, null, pageNumber, pageSize);
    }

    @Override
    public Page<TransactionsPO> getTxs(String address, Integer assetChainId, Integer assetId, Integer type, Integer state, Long startTime, Long endTime, int pageNumber, int pageSize) {

        Searchable searchable = new Searchable();
        if(!StringUtils.isNullOrEmpty(address)){
            searchable.addCondition("address", SearchOperator.eq, address);
        }
        if (null != assetChainId) {
            searchable.addCondition("assetChainId", SearchOperator.eq, assetChainId);
        }
        if (null != assetId) {
            searchable.addCondition("assetId", SearchOperator.eq, assetId);
        }
        if (null != startTime) {
            searchable.addCondition("time", SearchOperator.gte, startTime);
        }
        if (null != endTime) {
            searchable.addCondition("time", SearchOperator.lte, endTime);
        }
        if (null != type) {
            searchable.addCondition("type", SearchOperator.eq, type);
        }
        if (null != state) {
            searchable.addCondition("state", SearchOperator.eq, state);
        }
        SqlSession sqlSession = sqlSessionFactory.openSession();
        TransactionMapper mapper = sqlSession.getMapper(TransactionMapper.class);
        String tableName = getTableName(address);
        long count = mapper.queryCount(searchable, tableName);
        if (count < (pageNumber - 1) * pageSize) {
            return new Page<>(pageNumber, pageSize);
        }
        //开启分页
        PageHelper.startPage(pageNumber, pageSize);
        PageHelper.orderBy(" address asc, time desc, type asc ");
        List<TransactionsPO> list = mapper.getTxs(searchable, tableName);
        //sqlSession.commit();
        sqlSession.close();
        Page<TransactionsPO> page = new Page<>();
        if (pageSize > 0) {
            page.setPageNumber(pageNumber);
            page.setPageSize(pageSize);
        } else {
            page.setPageNumber(1);
            page.setPageSize((int) count);
        }
        page.setTotal(count);
        page.setList(list);

        return page;
    }

    /**
     * 根据地址获取对应存储位置的表名
     * @param address
     * @return
     */
    private String getTableName(String address){
        int tabNumber = (address.hashCode() & Integer.MAX_VALUE) % TxConstant.H2_TX_TABLE_NUMBER;
        return TxConstant.H2_TX_TABLE_NAME_PREFIX + tabNumber;
    }


    @Override
    public int saveTx(TransactionsPO txPo) {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        String tableName = TxConstant.H2_TX_TABLE_NAME_PREFIX + txPo.createTableIndex();
        int rs = sqlSession.getMapper(TransactionMapper.class).insert(txPo, tableName);
        sqlSession.commit();
        sqlSession.close();
        return rs;
    }


    private Map<String, List<TransactionsPO>> assembleMap(List<TransactionsPO> txPoList){
        Map<String, List<TransactionsPO>> map = new HashMap<>();
        for (TransactionsPO txPo : txPoList) {
            String tableName = TxConstant.H2_TX_TABLE_NAME_PREFIX + txPo.createTableIndex();
            if(!map.containsKey(tableName)){
                List<TransactionsPO> list = new ArrayList<>();
                list.add(txPo);
                map.put(tableName,list);
            }else{
                map.get(tableName).add(txPo);
            }
        }
        return map;
    }

    @Override
    public int saveTxsTables(List<TransactionsPO> txPoList) {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        Map<String, List<TransactionsPO>> map = assembleMap(txPoList);
        int rs = 0;
        for (Map.Entry<String, List<TransactionsPO>> entry : map.entrySet()){
            if(sqlSession.getMapper(TransactionMapper.class).batchInsert(entry.getValue(), entry.getKey()) == 1){
                rs+=entry.getValue().size();
            }
        }
        sqlSession.commit();
        sqlSession.close();
        return rs;
    }

    @Override
    public int saveTxs(List<TransactionsPO> txPoList) {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        int rs = 0;
        for (TransactionsPO txPo : txPoList){
            String tableName = TxConstant.H2_TX_TABLE_NAME_PREFIX + txPo.createTableIndex();
            if(sqlSession.getMapper(TransactionMapper.class).insert(txPo,tableName) == 1){
                rs++;
            }
        }
        sqlSession.commit();
        sqlSession.close();
        return rs;
    }

    @Override
    public int deleteTx(String address, String txhash) {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        int rs = sqlSession.getMapper(TransactionMapper.class).delete(address, txhash, getTableName(address));
        sqlSession.commit();
        sqlSession.close();
        return rs;
    }

    @Override
    public int deleteTx(Transaction tx) {
        int count = 0;
        try {
            List<TransactionsPO> list = TxUtil.tx2PO(tx);
            for (TransactionsPO transactionsPO : list){
                count += deleteTx(transactionsPO.getAddress(), transactionsPO.getHash());
            }
        } catch (NulsException e) {
            e.printStackTrace();
        }
        return count;
    }

    @Override
    public void createTxTablesIfNotExists(String tableName, String indexName, String uniqueName, int number) {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        TransactionMapper mapper = sqlSession.getMapper(TransactionMapper.class);
        List<TxTable> list = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            TxTable txTable = new TxTable(tableName + i, indexName + i, uniqueName + i);
            list.add(txTable);
        }
        mapper.createTxTables(list);
        sqlSession.commit();
        sqlSession.close();
    }
}
