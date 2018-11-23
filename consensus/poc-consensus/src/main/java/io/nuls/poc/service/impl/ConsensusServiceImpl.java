package io.nuls.poc.service.impl;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.TransactionFeeCalculator;
import io.nuls.base.data.*;
import io.nuls.poc.constant.ConsensusConstant;
import io.nuls.poc.constant.ConsensusErrorCode;
import io.nuls.poc.model.bo.round.MeetingMember;
import io.nuls.poc.model.bo.round.MeetingRound;
import io.nuls.poc.model.bo.tx.CancelDepositTransaction;
import io.nuls.poc.model.bo.tx.CreateAgentTransaction;
import io.nuls.poc.model.bo.tx.DepositTransaction;
import io.nuls.poc.model.bo.tx.StopAgentTransaction;
import io.nuls.poc.model.bo.tx.txdata.Agent;
import io.nuls.poc.model.bo.tx.txdata.CancelDeposit;
import io.nuls.poc.model.bo.tx.txdata.Deposit;
import io.nuls.poc.model.bo.tx.txdata.StopAgent;
import io.nuls.poc.model.dto.input.CreateAgentDTO;
import io.nuls.poc.model.dto.input.CreateDepositDTO;
import io.nuls.poc.model.dto.input.WithdrawDTO;
import io.nuls.poc.model.dto.output.*;
import io.nuls.poc.model.po.AgentPo;
import io.nuls.poc.model.po.DepositPo;
import io.nuls.poc.model.po.PunishLogPo;
import io.nuls.poc.service.ConsensusService;
import io.nuls.poc.storage.AgentStorageService;
import io.nuls.poc.storage.DepositStorageService;
import io.nuls.poc.storage.PunihStorageService;
import io.nuls.poc.utils.manager.ConfigManager;
import io.nuls.poc.utils.manager.ConsensusManager;
import io.nuls.poc.utils.manager.RoundManager;
import io.nuls.poc.utils.util.ConsensusUtil;
import io.nuls.poc.utils.util.PoConvertUtil;
import io.nuls.poc.utils.validator.ValidatorManager;
import io.nuls.rpc.model.CmdResponse;
import io.nuls.tools.basic.VarInt;
import io.nuls.tools.core.annotation.Autowired;
import io.nuls.tools.core.annotation.Service;
import io.nuls.tools.crypto.HexUtil;
import io.nuls.tools.data.ByteUtils;
import io.nuls.tools.data.ObjectUtils;
import io.nuls.tools.data.StringUtils;
import io.nuls.tools.exception.NulsException;
import io.nuls.tools.exception.NulsRuntimeException;
import io.nuls.tools.log.Log;
import io.nuls.tools.parse.JSONUtils;
import io.nuls.tools.thread.TimeService;

import java.io.IOException;
import java.util.*;

@Service
public class ConsensusServiceImpl implements ConsensusService {
    @Autowired
    private AgentStorageService agentService;
    @Autowired
    private DepositStorageService depositService;
    @Autowired
    private PunihStorageService publishService;
    @Autowired
    private ValidatorManager validatorManager;

    /**
     * 创建节点
     * */
    @Override
    public CmdResponse createAgent(Map<String,Object> params) {
        if(params == null){
            return new CmdResponse(1, ConsensusErrorCode.PARAM_ERROR.getCode(),ConsensusErrorCode.PARAM_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
        try {
            CreateAgentDTO dto = JSONUtils.map2pojo(params,CreateAgentDTO.class);
            ObjectUtils.canNotEmpty(dto);
            ObjectUtils.canNotEmpty(dto.getChainId(), "chainId can not be null");
            ObjectUtils.canNotEmpty(dto.getAgentAddress(), "agent address can not be null");
            ObjectUtils.canNotEmpty(dto.getCommissionRate(), "commission rate can not be null");
            ObjectUtils.canNotEmpty(dto.getDeposit(), "deposit can not be null");
            ObjectUtils.canNotEmpty(dto.getPackingAddress(), "packing address can not be null");
            //1.参数验证
            if (!AddressTool.isPackingAddress(dto.getPackingAddress(),(short)dto.getChainId()) || !AddressTool.validAddress((short)dto.getChainId(),dto.getAgentAddress())){
                throw new NulsRuntimeException(ConsensusErrorCode.ADDRESS_ERROR);
            }
            //todo 调用账户模块接口  验证账户是否正确
            /*boolean validResult = true;
            if(!validResult){
                return new CmdResponse(1, ConsensusErrorCode.ACCOUNT_INFO_ERROR.getCode(),ConsensusErrorCode.ACCOUNT_INFO_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
            }*/
            //3.组装创建节点交易
            CreateAgentTransaction tx = new CreateAgentTransaction();
            tx.setTime(TimeService.currentTimeMillis());
            //3.1.组装共识节点信息
            Agent agent = new Agent();
            agent.setAgentAddress(AddressTool.getAddress(dto.getAgentAddress()));
            agent.setPackingAddress(AddressTool.getAddress(dto.getPackingAddress()));
            if (StringUtils.isBlank(dto.getRewardAddress())) {
                agent.setRewardAddress(agent.getAgentAddress());
            } else {
                agent.setRewardAddress(AddressTool.getAddress(dto.getRewardAddress()));
            }
            agent.setDeposit(Na.valueOf(dto.getDeposit()));
            agent.setCommissionRate(dto.getCommissionRate());
            tx.setTxData(agent);
            //3.2.组装coinData(调用账户模块)
            CoinData coinData = new CoinData();
            List<Coin> toList = new ArrayList<>();
            toList.add(new Coin(agent.getAgentAddress(), agent.getDeposit(), ConsensusConstant.CONSENSUS_LOCK_TIME));
            coinData.setTo(toList);
            tx.setCoinData(coinData);
            //todo 4.交易签名

            //todo 5.将交易发送给交易管理模块

            return new CmdResponse(1, ConsensusErrorCode.SUCCESS.getCode(),ConsensusErrorCode.SUCCESS.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }catch (NulsRuntimeException e){
            Log.error(e);
            return new CmdResponse(1, e.getErrorCode().getCode(),e.getErrorCode().getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
    }

    /**
     * 停止节点
     * */
    @Override
    public CmdResponse stopAgent(Map<String,Object> params) {
        if(params == null || params.get("chain_id") == null || params.get("address") == null){
            return new CmdResponse(1, ConsensusErrorCode.PARAM_ERROR.getCode(),ConsensusErrorCode.PARAM_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
        try {
            int chainId = (Integer)params.get("chain_id");
            String address = (String) params.get("address");
            String password = null;
            if(params.get("password") != null){
                password = (String)params.get("password");
            }
            if (!AddressTool.validAddress((short)chainId,address)) {
                throw new NulsRuntimeException(ConsensusErrorCode.ADDRESS_ERROR);
            }
            //todo  验证账户正确性（账户模块）

            StopAgentTransaction tx = new StopAgentTransaction();
            StopAgent stopAgent = new StopAgent();
            stopAgent.setAddress(AddressTool.getAddress(address));
            List<Agent> agentList = ConsensusManager.getInstance().getAllAgentMap().get(chainId);
            Agent agent = null;
            for (Agent a : agentList) {
                if (a.getDelHeight() > 0) {
                    continue;
                }
                if (Arrays.equals(a.getAgentAddress(), AddressTool.getAddress(address))) {
                    agent = a;
                    break;
                }
            }
            if (agent == null || agent.getDelHeight() > 0) {
                return new CmdResponse(1, ConsensusErrorCode.AGENT_NOT_EXIST.getCode(),ConsensusErrorCode.AGENT_NOT_EXIST.getMsg(), ConsensusConstant.RPC_VERSION,null);
            }
            stopAgent.setCreateTxHash(agent.getTxHash());
            tx.setTxData(stopAgent);
            CoinData coinData = ConsensusUtil.getStopAgentCoinData(chainId, agent, TimeService.currentTimeMillis() + ConfigManager.config_map.get(chainId).getStopAgent_lockTime());
            tx.setCoinData(coinData);
            Na fee = TransactionFeeCalculator.getMaxFee(tx.size());
            coinData.getTo().get(0).setNa(coinData.getTo().get(0).getNa().subtract(fee));

            //todo 交易签名

            //todo 将交易传递给交易管理模块

            return new CmdResponse(1, ConsensusErrorCode.SUCCESS.getCode(),ConsensusErrorCode.SUCCESS.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }catch (NulsRuntimeException e){
            Log.error(e);
            return new CmdResponse(1, e.getErrorCode().getCode(),e.getErrorCode().getMsg(), ConsensusConstant.RPC_VERSION,null);
        }catch (IOException io){
            Log.error(io);
            return new CmdResponse(1, ConsensusErrorCode.DATA_PARSE_ERROR.getCode(),ConsensusErrorCode.DATA_PARSE_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
    }

    /**
     * 委托共识
     * */
    @Override
    public CmdResponse depositToAgent(Map<String,Object> params) {
        if(params == null){
            return new CmdResponse(1, ConsensusErrorCode.PARAM_ERROR.getCode(),ConsensusErrorCode.PARAM_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
        try {
            CreateDepositDTO dto = JSONUtils.map2pojo(params,CreateDepositDTO.class);
            ObjectUtils.canNotEmpty(dto);
            ObjectUtils.canNotEmpty(dto.getAddress());
            ObjectUtils.canNotEmpty(dto.getAgentHash());
            ObjectUtils.canNotEmpty(dto.getDeposit());
            if (!NulsDigestData.validHash(dto.getAgentHash())) {
                throw new NulsException(ConsensusErrorCode.AGENT_NOT_EXIST);
            }
            if (!AddressTool.validAddress((short)dto.getChainId(),dto.getAddress())) {
                throw new NulsException(ConsensusErrorCode.ADDRESS_ERROR);
            }
            //todo 账户验证（账户模块）
            DepositTransaction tx = new DepositTransaction();
            Deposit deposit = new Deposit();
            deposit.setAddress(AddressTool.getAddress(dto.getAddress()));
            deposit.setAgentHash(NulsDigestData.fromDigestHex(dto.getAgentHash()));
            deposit.setDeposit(Na.valueOf(dto.getDeposit()));
            tx.setTxData(deposit);
            CoinData coinData = new CoinData();
            List<Coin> toList = new ArrayList<>();
            toList.add(new Coin(deposit.getAddress(), deposit.getDeposit(), ConsensusConstant.CONSENSUS_LOCK_TIME));
            coinData.setTo(toList);
            tx.setCoinData(coinData);
            //todo 获取coindata （账本模块），处理返回结果

            //todo 交易签名

            //todo 将交易传递给交易管理模块

            return new CmdResponse(1, ConsensusErrorCode.SUCCESS.getCode(),ConsensusErrorCode.SUCCESS.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }catch (NulsException e){
            Log.error(e);
            return new CmdResponse(1, e.getErrorCode().getCode(),e.getErrorCode().getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
    }

    /**
     * 退出共识
     * */
    @Override
    public CmdResponse withdraw(Map<String,Object> params) {
        if(params == null){
            return new CmdResponse(1, ConsensusErrorCode.PARAM_ERROR.getCode(),ConsensusErrorCode.PARAM_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
        try {
            WithdrawDTO dto = JSONUtils.map2pojo(params,WithdrawDTO.class);
            if (!NulsDigestData.validHash(dto.getTxHash())) {
                throw new NulsException(ConsensusErrorCode.PARAM_ERROR);
            }
            if (!AddressTool.validAddress((short)dto.getChainId(),dto.getAddress())) {
                throw new NulsException(ConsensusErrorCode.ADDRESS_ERROR);
            }
            //todo 账户验证（账户模块）

            CancelDepositTransaction tx = new CancelDepositTransaction();
            CancelDeposit cancelDeposit = new CancelDeposit();
            NulsDigestData hash = NulsDigestData.fromDigestHex(dto.getTxHash());
            //todo 从交易模块获取委托交易（交易模块）+ 返回数据处理
            DepositTransaction depositTransaction = new DepositTransaction();

            cancelDeposit.setAddress(AddressTool.getAddress(dto.getAddress()));
            cancelDeposit.setJoinTxHash(hash);
            tx.setTxData(cancelDeposit);
            CoinData coinData = new CoinData();
            List<Coin> toList = new ArrayList<>();
            toList.add(new Coin(cancelDeposit.getAddress(), depositTransaction.getTxData().getDeposit(), 0));
            coinData.setTo(toList);

            List<Coin> fromList = new ArrayList<>();
            for (int index = 0; index < depositTransaction.getCoinData().getTo().size(); index++) {
                Coin coin = depositTransaction.getCoinData().getTo().get(index);
                if (coin.getLockTime() == -1L && coin.getNa().equals(depositTransaction.getTxData().getDeposit())) {
                    coin.setOwner(ByteUtils.concatenate(hash.serialize(), new VarInt(index).encode()));
                    fromList.add(coin);
                    break;
                }
            }
            if (fromList.isEmpty()) {
                throw new NulsException(ConsensusErrorCode.DATA_ERROR);
            }
            coinData.setFrom(fromList);
            tx.setCoinData(coinData);
            Na fee = TransactionFeeCalculator.getMaxFee(tx.size());
            coinData.getTo().get(0).setNa(coinData.getTo().get(0).getNa().subtract(fee));

            //todo 交易签名

            //todo 将交易传递给交易管理模块

            return new CmdResponse(1, ConsensusErrorCode.SUCCESS.getCode(),ConsensusErrorCode.SUCCESS.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }catch (NulsException e){
            Log.error(e);
            return new CmdResponse(1, e.getErrorCode().getCode(),e.getErrorCode().getMsg(), ConsensusConstant.RPC_VERSION,null);
        }catch (IOException io){
            Log.error(io);
            return new CmdResponse(1, ConsensusErrorCode.DATA_PARSE_ERROR.getCode(),ConsensusErrorCode.DATA_PARSE_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
    }

    /**
     * 获取节点列表信息
     * */
    @Override
    public CmdResponse getAgentList(Map<String,Object> params) {
        int pageNumber = 0;
        if(params.get("pageNumber") != null){
            pageNumber = (Integer) params.get("pageNumber");
        }
        int pageSize = 0;
        if(params.get("pageSize") != null){
            pageSize = (Integer) params.get("pageSize");
        }
        if (pageNumber == 0) {
            pageNumber = 1;
        }
        if (pageSize == 0) {
            pageSize = 10;
        }
        if (pageNumber < 0 || pageSize < 0 || pageSize > 100 || params.get("chain_id") == null) {
            return new CmdResponse(1, ConsensusErrorCode.PARAM_ERROR.getCode(),ConsensusErrorCode.PARAM_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
        int chain_id = (Integer) params.get("chain_id");
        List<Agent> agentList = ConsensusManager.getInstance().getAllAgentMap().get(chain_id);
        List<Agent> handleList = new ArrayList<>();
        String keyword = null;
        if(params.get("keyword")!=null){
            keyword = (String)params.get("keyword");
        }
        //todo 从区块管理模块获取本地最新高度
        long startBlockHeight = 100;
        for (Agent agent:agentList) {
            if (agent.getDelHeight() != -1L && agent.getDelHeight() <= startBlockHeight) {
                continue;
            }
            if(agent.getBlockHeight() > startBlockHeight || agent.getBlockHeight() < 0L){
                continue;
            }
            if(StringUtils.isNotBlank(keyword)){
                keyword = keyword.toUpperCase();
                String agentAddress = AddressTool.getStringAddressByBytes(agent.getAgentAddress()).toUpperCase();
                String packingAddress = AddressTool.getStringAddressByBytes(agent.getPackingAddress()).toUpperCase();
                String agentId = PoConvertUtil.getAgentId(agent.getTxHash()).toUpperCase();
                //todo
                //从账户模块获取账户别名
                String alias = "";
                boolean b = agentId.indexOf(keyword) >= 0;
                b = b || agentAddress.equals(keyword) || packingAddress.equals(keyword);
                if (StringUtils.isNotBlank(alias)) {
                    b = b || alias.toUpperCase().indexOf(keyword) >= 0;
                    agent.setAlais(alias);
                }
                if (!b) {
                    continue;
                }
            }
            handleList.add(agent);
        }
        int start = pageNumber * pageSize - pageSize;
        Page<AgentDTO> page = new Page<>(pageNumber, pageSize, handleList.size());
        if (start >= page.getTotal()) {
            return new CmdResponse(1, ConsensusErrorCode.SUCCESS.getCode(),ConsensusErrorCode.SUCCESS.getMsg(), ConsensusConstant.RPC_VERSION,page);
        }
        fillAgentList(chain_id, handleList, null);
        //todo 是否要添加排序功能
        List<AgentDTO> resultList = new ArrayList<>();
        for (int i = start; i < handleList.size() && i < (start + pageSize); i++) {
            AgentDTO agentDTO = new AgentDTO(handleList.get(i));
            resultList.add(agentDTO);
        }
        page.setList(resultList);
        return new CmdResponse(1, ConsensusErrorCode.SUCCESS.getCode(),ConsensusErrorCode.SUCCESS.getMsg(), ConsensusConstant.RPC_VERSION,page);

    }

    /**
     * 获取指定节点信息
     * */
    @Override
    public CmdResponse getAgentInfo(Map<String,Object> params) {
        if(params.get("agentHash") == null || params.get("chain_id") == null ){
            return new CmdResponse(1, ConsensusErrorCode.PARAM_ERROR.getCode(),ConsensusErrorCode.PARAM_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
        String agentHash = (String)params.get("agentHash");
        if (!NulsDigestData.validHash(agentHash)) {
            return new CmdResponse(1, ConsensusErrorCode.AGENT_NOT_EXIST.getCode(),ConsensusErrorCode.AGENT_NOT_EXIST.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
        try {
            NulsDigestData agentHashData = NulsDigestData.fromDigestHex(agentHash);
            int chain_id = (Integer)params.get("chain_id");
            List<Agent> agentList = ConsensusManager.getInstance().getAllAgentMap().get(chain_id);
            for (Agent agent:agentList) {
                if (agent.getTxHash().equals(agentHashData)) {
                    MeetingRound round = RoundManager.getInstance().getCurrentRound(chain_id);
                    this.fillAgent(chain_id, agent, round, null);
                    AgentDTO dto = new AgentDTO(agent);
                    return new CmdResponse(1, ConsensusErrorCode.SUCCESS.getCode(),ConsensusErrorCode.SUCCESS.getMsg(), ConsensusConstant.RPC_VERSION,dto);
                }
            }
        }catch (Exception e){
            Log.error(e);
        }
        return new CmdResponse(1, ConsensusErrorCode.AGENT_NOT_EXIST.getCode(),ConsensusErrorCode.AGENT_NOT_EXIST.getMsg(), ConsensusConstant.RPC_VERSION,null);
    }

    /**
     * 获取惩罚信息
     * */
    @Override
    public CmdResponse getPublishList(Map<String,Object> params) {
        if(params.get("chain_id") == null){
            return new CmdResponse(1, ConsensusErrorCode.PARAM_ERROR.getCode(),ConsensusErrorCode.PARAM_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
        int chain_id = (Integer)params.get("chain_id");
        String address = null;
        if(params.get("address") != null){
            address = (String)params.get("address");
        }
        int type = 0;
        if(params.get("type") != null){
            type = (Integer)params.get("type");
        }
        List<PunishLogDTO> yellowPunishList = null;
        List<PunishLogDTO> redPunishList = null;
        //查询红牌交易
        if(type != 1){
            for (PunishLogPo po:ConsensusManager.getInstance().getRedPunishMap().get(chain_id)) {
                if(StringUtils.isNotBlank(address) && !ByteUtils.arrayEquals(po.getAddress(),AddressTool.getAddress(address))){
                    continue;
                }
                redPunishList.add(new PunishLogDTO(po));
            }
        }else if(type != 2){
            for (PunishLogPo po:ConsensusManager.getInstance().getYellowPunishMap().get(chain_id)) {
                if(StringUtils.isNotBlank(address) && !ByteUtils.arrayEquals(po.getAddress(),AddressTool.getAddress(address))){
                    continue;
                }
                yellowPunishList.add(new PunishLogDTO(po));
            }
        }
        Map<String,List<PunishLogDTO>> resultMap = new HashMap<>();
        resultMap.put("redPunish",redPunishList);
        resultMap.put("yellowPunish",yellowPunishList);
        return new CmdResponse(1, ConsensusErrorCode.SUCCESS.getCode(),ConsensusErrorCode.SUCCESS.getMsg(), ConsensusConstant.RPC_VERSION,resultMap);
    }

    /**
     * 获取委托列表信息
     * */
    @Override
    public CmdResponse getDepositList(Map<String,Object> params) {
        int pageNumber = 0;
        if(params.get("pageNumber") != null){
            pageNumber = (Integer) params.get("pageNumber");
        }
        int pageSize = 0;
        if(params.get("pageSize") != null){
            pageSize = (Integer) params.get("pageSize");
        }
        if (pageNumber == 0) {
            pageNumber = 1;
        }
        if (pageSize == 0) {
            pageSize = 10;
        }
        if (pageNumber < 0 || pageSize < 0 || pageSize > 100 || params.get("chain_id") == null) {
            return new CmdResponse(1, ConsensusErrorCode.PARAM_ERROR.getCode(),ConsensusErrorCode.PARAM_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
        int chain_id = (Integer) params.get("chain_id");
        String address = null;
        if(params.get("address") != null){
            address = (String)params.get("address");
        }
        String agentHash = null;
        if(params.get("agentHash") != null){
            agentHash = (String)params.get("agentHash");
        }
        List<Deposit> depositList = ConsensusManager.getInstance().getAllDepositMap().get(chain_id);
        List<Deposit> handleList = new ArrayList<>();
        //todo 从区块管理模块获取本地最新高度
        long startBlockHeight = 0;
        byte[] addressBytes = null;
        if(StringUtils.isNotBlank(address)){
            addressBytes=AddressTool.getAddress(address);
        }
        for (Deposit deposit:depositList) {
            if (deposit.getDelHeight() != -1L && deposit.getDelHeight() <= startBlockHeight) {
                continue;
            }
            if (deposit.getBlockHeight() > startBlockHeight || deposit.getBlockHeight() < 0L) {
                continue;
            }
            if (addressBytes != null && !Arrays.equals(deposit.getAddress(), addressBytes)) {
                continue;
            }
            if (agentHash != null && !deposit.getAgentHash().getDigestHex().equals(agentHash)) {
                continue;
            }
            handleList.add(deposit);
        }
        int start = pageNumber * pageSize - pageSize;
        Page<DepositDTO> page = new Page<>(pageNumber, pageSize, handleList.size());
        if (start >= handleList.size()) {
            return new CmdResponse(1, ConsensusErrorCode.SUCCESS.getCode(),ConsensusErrorCode.SUCCESS.getMsg(), ConsensusConstant.RPC_VERSION,page);
        }
        List<DepositDTO> resultList = new ArrayList<>();
        for (int i = start; i < depositList.size() && i < (start + pageSize); i++) {
            Deposit deposit = depositList.get(i);
            List<Agent> agentList = ConsensusManager.getInstance().getAllAgentMap().get(chain_id);
            Agent agent = null;
            for (Agent a : agentList) {
                if (a.getTxHash().equals(deposit.getAgentHash())) {
                    agent = a;
                    break;
                }
            }
            deposit.setStatus(agent == null ? 0 : agent.getStatus());
            resultList.add(new DepositDTO(deposit, agent));
        }
        page.setList(resultList);
        return new CmdResponse(1, ConsensusErrorCode.SUCCESS.getCode(),ConsensusErrorCode.SUCCESS.getMsg(), ConsensusConstant.RPC_VERSION,page);
    }

    /**
     * 获取全网信息
     * */
    @Override
    public CmdResponse getWholeInfo(Map<String,Object> params) {
        if (params.get("chain_id") == null) {
            return new CmdResponse(1, ConsensusErrorCode.PARAM_ERROR.getCode(),ConsensusErrorCode.PARAM_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
        int chain_id = (Integer) params.get("chain_id");
        WholeNetConsensusInfoDTO dto = new WholeNetConsensusInfoDTO();
        List<Agent> agentList = ConsensusManager.getInstance().getAllAgentMap().get(chain_id);
        if(agentList == null ){
            return new CmdResponse(1, ConsensusErrorCode.DATA_NOT_EXIST.getCode(),ConsensusErrorCode.DATA_NOT_EXIST.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
        List<Agent> handleList = new ArrayList<>();
        //todo 从区块管理模块获取本地最新高度
        long startBlockHeight = 100;
        for (Agent agent:agentList) {
            if (agent.getDelHeight() != -1L && agent.getDelHeight() <= startBlockHeight) {
                continue;
            } else if (agent.getBlockHeight() > startBlockHeight || agent.getBlockHeight() < 0L) {
                continue;
            }
            handleList.add(agent);
        }
        MeetingRound round = RoundManager.getInstance().getCurrentRound(chain_id);
        long totalDeposit = 0;
        int packingAgentCount = 0;
        if (null != round) {
            for (MeetingMember member : round.getMemberList()) {
                totalDeposit += (member.getAgent().getTotalDeposit().getValue() + member.getAgent().getDeposit().getValue());
                if (member.getAgent() != null) {
                    packingAgentCount++;
                }
            }
        }
        dto.setAgentCount(handleList.size());
        dto.setTotalDeposit(totalDeposit);
        dto.setConsensusAccountNumber(handleList.size());
        dto.setPackingAgentCount(packingAgentCount);
        return new CmdResponse(1, ConsensusErrorCode.SUCCESS.getCode(),ConsensusErrorCode.SUCCESS.getMsg(), ConsensusConstant.RPC_VERSION,dto);
    }

    /**
     * 获取指定账户信息
     * */
    @Override
    public CmdResponse getInfo(Map<String,Object> params) {
        if (params.get("chain_id") == null || params.get("address")==null) {
            return new CmdResponse(1, ConsensusErrorCode.PARAM_ERROR.getCode(),ConsensusErrorCode.PARAM_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
        int chain_id = (Integer) params.get("chain_id");
        String address = (String)params.get("address");
        AccountConsensusInfoDTO dto = new AccountConsensusInfoDTO();
        //todo 从共识模块获取本地最新高度
        long startBlockHeight = 100;
        int agentCount = 0;
        String agentHash = null;
        byte[] addressBytes = AddressTool.getAddress(address);
        List<Agent> agentList = ConsensusManager.getInstance().getAllAgentMap().get(chain_id);
        for (Agent agent:agentList) {
            if (agent.getDelHeight() != -1L && agent.getDelHeight() <= startBlockHeight) {
                continue;
            } else if (agent.getBlockHeight() > startBlockHeight || agent.getBlockHeight() < 0L) {
                continue;
            }
            if (Arrays.equals(agent.getAgentAddress(), addressBytes)) {
                //一个账户最多只能创建一个共识节点
                agentCount = 1;
                agentHash = agent.getTxHash().getDigestHex();
                break;
            }
        }
        List<Deposit> depositList = ConsensusManager.getInstance().getAllDepositMap().get(chain_id);
        Set<NulsDigestData> agentSet = new HashSet<>();
        long totalDeposit = 0;
        for (Deposit deposit : depositList) {
            if (deposit.getDelHeight() != -1L && deposit.getDelHeight() <= startBlockHeight) {
                continue;
            }
            if (deposit.getBlockHeight() > startBlockHeight || deposit.getBlockHeight() < 0L) {
                continue;
            }
            if (!Arrays.equals(deposit.getAddress(), addressBytes)) {
                continue;
            }
            agentSet.add(deposit.getAgentHash());
            totalDeposit += deposit.getDeposit().getValue();
        }
        dto.setAgentCount(agentCount);
        dto.setAgentHash(agentHash);
        dto.setJoinAgentCount(agentSet.size());
        //todo 统计账户奖励金
        //dto.setReward(this.rewardCacheService.getReward(address).getValue());
        //dto.setRewardOfDay(rewardCacheService.getRewardToday(address).getValue());
        dto.setTotalDeposit(totalDeposit);
        try {
            //todo 从账本模块获取账户可用余额
            //dto.setUsableBalance(accountLedgerService.getBalance(addressBytes).getData().getUsable().getValue());
        } catch (Exception e) {
            Log.error(e);
            dto.setUsableBalance(0L);
        }
        return new CmdResponse(1, ConsensusErrorCode.SUCCESS.getCode(),ConsensusErrorCode.SUCCESS.getMsg(), ConsensusConstant.RPC_VERSION,dto);
    }

    /**
     * 验证区块正确性
     * */
    @Override
    public CmdResponse validBlock(Map<String,Object> params) {
        if (params.get("chain_id") == null || params.get("block")==null) {
            return new CmdResponse(1, ConsensusErrorCode.PARAM_ERROR.getCode(),ConsensusErrorCode.PARAM_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
        try {
            int chain_id = (Integer) params.get("chain_id");
            String blockHex = (String)params.get("block");
            Block block = new Block();
            block.parse(new NulsByteBuffer(HexUtil.decode(blockHex)));
            //验证梅克尔哈希

            //验证轮次信息及打包人

            return new CmdResponse(1, ConsensusErrorCode.SUCCESS.getCode(),ConsensusErrorCode.SUCCESS.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }catch (NulsException e){
            Log.error(e);
            return new CmdResponse(1, ConsensusErrorCode.DATA_PARSE_ERROR.getCode(),ConsensusErrorCode.DATA_PARSE_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
    }

    /**
     * 批量验证共识模块交易
     * */
    @Override
    public CmdResponse batchValid(List<Object> params) {
        return null;
    }

    /**
     * 获取当前轮次信息
     * */
    @Override
    public CmdResponse getRoundInfo(List<Object> params) {
        if(params == null){
            return new CmdResponse(1, ConsensusErrorCode.PARAM_ERROR.getCode(),ConsensusErrorCode.PARAM_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
        MeetingRound round = RoundManager.getInstance().getOrResetCurrentRound((Integer)params.get(0),true);
        return new CmdResponse(1, ConsensusErrorCode.SUCCESS.getCode(),ConsensusErrorCode.SUCCESS.getMsg(), ConsensusConstant.RPC_VERSION,round);

    }

    /**
     * 获取指定节点状态
     * */
    @Override
    public CmdResponse getAgentStatus(List<Object> params) {
        //从数据库查询节点信息，返回节点状态
        if(params == null || params.size() != 2){
            return new CmdResponse(1, ConsensusErrorCode.PARAM_ERROR.getCode(),ConsensusErrorCode.PARAM_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
        Map<String,Integer> result = new HashMap<>();
        try {
            NulsDigestData agentHash = new NulsDigestData();
            agentHash.parse(new NulsByteBuffer(HexUtil.decode((String) params.get(0))));
            AgentPo agent = agentService.get(agentHash,(Integer)params.get(1));
            if(agent.getDelHeight() > 0){
                result.put("status",0);
            }else{
                result.put("status",1);
            }
        }catch (Exception e){
            Log.error(e);
        }
        return new CmdResponse(1, ConsensusErrorCode.SUCCESS.getCode(),ConsensusErrorCode.SUCCESS.getMsg(), ConsensusConstant.RPC_VERSION,result);
    }

    /**
     * 修改节点状态
     * */
    @Override
    public CmdResponse updateAgentStatus(List<Object> params) {
        if(params == null || params.size() != 1){
            return new CmdResponse(1, ConsensusErrorCode.PARAM_ERROR.getCode(),ConsensusErrorCode.PARAM_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
        int chain_id = (Integer)params.get(0);
        ConsensusManager.getInstance().getPacking_status().put(chain_id,true);
        return new CmdResponse(1, ConsensusErrorCode.SUCCESS.getCode(),ConsensusErrorCode.SUCCESS.getMsg(), ConsensusConstant.RPC_VERSION,null);
    }

    /**
     * 停止一条链
     * */
    @Override
    public CmdResponse stopChain(List<Object> params) {
        return null;
    }

    /**
     * 启动一条新链
     * */
    @Override
    public CmdResponse runChain(List<Object> params) {
        return null;
    }

    /**
     * 创建节点交易验证
     * */
    @Override
    public CmdResponse createAgentValid(Map<String, Object> params) {
        if (params.get("chain_id") == null || params.get("tx")==null) {
            return new CmdResponse(1, ConsensusErrorCode.PARAM_ERROR.getCode(),ConsensusErrorCode.PARAM_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
        try {
            int chain_id = (Integer) params.get("chain_id");
            String txHex = (String) params.get("tx");
            CreateAgentTransaction transaction = new CreateAgentTransaction();
            transaction.parse(HexUtil.decode(txHex),0);
            boolean result = validatorManager.validateCreateAgent(chain_id,transaction);
            if(!result){
                return new CmdResponse(1, ConsensusErrorCode.TX_DATA_VALIDATION_ERROR.getCode(),ConsensusErrorCode.TX_DATA_VALIDATION_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
            }
            return new CmdResponse(1, ConsensusErrorCode.SUCCESS.getCode(),ConsensusErrorCode.SUCCESS.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }catch (NulsException e){
            Log.error(e);
            return new CmdResponse(1, e.getErrorCode().getCode(),e.getErrorCode().getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
    }

    /**
     * 创建节点交易提交
     * */
    @Override
    public CmdResponse createAgentCommit(Map<String, Object> params) {
        if (params.get("chain_id") == null || params.get("tx")==null || params.get("blockHeader") == null) {
            return new CmdResponse(1, ConsensusErrorCode.PARAM_ERROR.getCode(),ConsensusErrorCode.PARAM_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
        try {
            int chain_id = (Integer) params.get("chain_id");
            String txHex = (String) params.get("tx");
            CreateAgentTransaction transaction = new CreateAgentTransaction();
            transaction.parse(HexUtil.decode(txHex),0);
            String headerHex = (String) params.get("blockHeader");
            BlockHeader blockHeader = new BlockHeader();
            blockHeader.parse(HexUtil.decode(headerHex),0);
            Agent agent = transaction.getTxData();
            agent.setTxHash(transaction.getHash());
            agent.setBlockHeight(blockHeader.getHeight());
            agent.setTime(transaction.getTime());
            AgentPo agentPo = PoConvertUtil.agentToPo(agent);
            //保存数据库和缓存
            if(!agentService.save(agentPo,chain_id)){
                return new CmdResponse(1, ConsensusErrorCode.SAVE_FAILED.getCode(),ConsensusErrorCode.SAVE_FAILED.getMsg(), ConsensusConstant.RPC_VERSION,null);
            }
            return new CmdResponse(1, ConsensusErrorCode.SUCCESS.getCode(),ConsensusErrorCode.SUCCESS.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }catch (NulsException e){
            Log.error(e);
            return new CmdResponse(1, e.getErrorCode().getCode(),e.getErrorCode().getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
    }

    /**
     * 创建节点交易回滚
     * */
    @Override
    public CmdResponse createAgentRollBack(Map<String, Object> params) {
        if (params.get("chain_id") == null || params.get("tx")==null) {
            return new CmdResponse(1, ConsensusErrorCode.PARAM_ERROR.getCode(),ConsensusErrorCode.PARAM_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
        try {
            int chain_id = (Integer) params.get("chain_id");
            String txHex = (String) params.get("tx");
            CreateAgentTransaction transaction = new CreateAgentTransaction();
            transaction.parse(HexUtil.decode(txHex),0);
            //删除数据库数据和缓存数据
            if(!agentService.delete(transaction.getHash(),chain_id)){
                return new CmdResponse(1, ConsensusErrorCode.ROLLBACK_FAILED.getCode(),ConsensusErrorCode.ROLLBACK_FAILED.getMsg(), ConsensusConstant.RPC_VERSION,null);
            }
            return new CmdResponse(1, ConsensusErrorCode.SUCCESS.getCode(),ConsensusErrorCode.SUCCESS.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }catch (NulsException e){
            Log.error(e);
            return new CmdResponse(1, e.getErrorCode().getCode(),e.getErrorCode().getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
    }

    /**
     * 停止节点交易验证
     * */
    @Override
    public CmdResponse stopAgentValid(Map<String, Object> params) {
        if (params.get("chain_id") == null || params.get("tx")==null) {
            return new CmdResponse(1, ConsensusErrorCode.PARAM_ERROR.getCode(),ConsensusErrorCode.PARAM_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
        try {
            int chain_id = (Integer) params.get("chain_id");
            String txHex = (String) params.get("tx");
            StopAgentTransaction transaction = new StopAgentTransaction();
            transaction.parse(HexUtil.decode(txHex),0);
            boolean result = validatorManager.validateStopAgent(chain_id,transaction);
            if(!result){
                return new CmdResponse(1, ConsensusErrorCode.TX_DATA_VALIDATION_ERROR.getCode(),ConsensusErrorCode.TX_DATA_VALIDATION_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
            }
            return new CmdResponse(1, ConsensusErrorCode.SUCCESS.getCode(),ConsensusErrorCode.SUCCESS.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }catch (NulsException e){
            Log.error(e);
            return new CmdResponse(1, e.getErrorCode().getCode(),e.getErrorCode().getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
    }

    /**
     * 停止节点交易提交
     * */
    @Override
    public CmdResponse stopAgentCommit(Map<String, Object> params) {
        if (params.get("chain_id") == null || params.get("tx")==null || params.get("blockHeader") == null) {
            return new CmdResponse(1, ConsensusErrorCode.PARAM_ERROR.getCode(),ConsensusErrorCode.PARAM_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
        try {
            int chain_id = (Integer) params.get("chain_id");
            String txHex = (String) params.get("tx");
            StopAgentTransaction transaction = new StopAgentTransaction();
            transaction.parse(HexUtil.decode(txHex),0);
            String headerHex = (String) params.get("blockHeader");
            BlockHeader blockHeader = new BlockHeader();
            blockHeader.parse(HexUtil.decode(headerHex),0);
            if (transaction.getTime() < (blockHeader.getTime() - 300000L)) {
                return new CmdResponse(1, ConsensusErrorCode.LOCK_TIME_NOT_REACHED.getCode(),ConsensusErrorCode.LOCK_TIME_NOT_REACHED.getMsg(), ConsensusConstant.RPC_VERSION,null);
            }
            //找到需要注销的节点信息
            AgentPo agentPo = agentService.get(transaction.getTxData().getCreateTxHash(),chain_id);
            if(agentPo == null || agentPo.getDelHeight() > 0){
                return new CmdResponse(1, ConsensusErrorCode.AGENT_NOT_EXIST.getCode(),ConsensusErrorCode.AGENT_NOT_EXIST.getMsg(), ConsensusConstant.RPC_VERSION,null);
            }
            //找到该节点的委托信息,并设置委托状态为退出
            List<DepositPo> depositPoList = depositService.getList(chain_id);
            for (DepositPo depositPo : depositPoList) {
                if (depositPo.getDelHeight() > -1L) {
                    continue;
                }
                if (!depositPo.getAgentHash().equals(agentPo.getHash())) {
                    continue;
                }
                depositPo.setDelHeight(transaction.getBlockHeight());
                depositService.save(depositPo,chain_id);
            }
            agentPo.setDelHeight(transaction.getBlockHeight());
            //保存数据库和缓存
            if(!agentService.save(agentPo,chain_id)){
                return new CmdResponse(1, ConsensusErrorCode.SAVE_FAILED.getCode(),ConsensusErrorCode.SAVE_FAILED.getMsg(), ConsensusConstant.RPC_VERSION,null);
            }
            return new CmdResponse(1, ConsensusErrorCode.SUCCESS.getCode(),ConsensusErrorCode.SUCCESS.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }catch (NulsException e){
            Log.error(e);
            return new CmdResponse(1, e.getErrorCode().getCode(),e.getErrorCode().getMsg(), ConsensusConstant.RPC_VERSION,null);
        }catch (Exception et){
            Log.error(et);
            return new CmdResponse(1, ConsensusErrorCode.DATA_ERROR.getCode(),ConsensusErrorCode.DATA_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
    }

    /**
     * 停止节点交易回滚
     * */
    @Override
    public CmdResponse stopAgentRollBack(Map<String, Object> params) {
        if (params.get("chain_id") == null || params.get("tx")==null) {
            return new CmdResponse(1, ConsensusErrorCode.PARAM_ERROR.getCode(),ConsensusErrorCode.PARAM_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
        try {
            int chain_id = (Integer) params.get("chain_id");
            String txHex = (String) params.get("tx");
            StopAgentTransaction transaction = new StopAgentTransaction();
            transaction.parse(HexUtil.decode(txHex),0);
            AgentPo agentPo = agentService.get(transaction.getTxData().getCreateTxHash(),chain_id);
            agentPo.setDelHeight(-1);
            //找到该节点的委托信息,并设置委托状态为退出
            List<DepositPo> depositPoList = depositService.getList(chain_id);
            for (DepositPo depositPo : depositPoList) {
                if (depositPo.getDelHeight() != transaction.getBlockHeight()) {
                    continue;
                }
                if (!depositPo.getAgentHash().equals(agentPo.getHash())) {
                    continue;
                }
                depositPo.setDelHeight(-1);
                depositService.save(depositPo,chain_id);
            }
            //保存数据库和缓存
            if(!agentService.save(agentPo,chain_id)){
                return new CmdResponse(1, ConsensusErrorCode.ROLLBACK_FAILED.getCode(),ConsensusErrorCode.ROLLBACK_FAILED.getMsg(), ConsensusConstant.RPC_VERSION,null);
            }
            return new CmdResponse(1, ConsensusErrorCode.SUCCESS.getCode(),ConsensusErrorCode.SUCCESS.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }catch (NulsException e){
            Log.error(e);
            return new CmdResponse(1, e.getErrorCode().getCode(),e.getErrorCode().getMsg(), ConsensusConstant.RPC_VERSION,null);
        }catch (Exception et){
            Log.error(et);
            return new CmdResponse(1, ConsensusErrorCode.DATA_ERROR.getCode(),ConsensusErrorCode.DATA_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
    }

    /**
     * 委托共识交易验证
     * */
    @Override
    public CmdResponse depositValid(Map<String, Object> params) {
        if (params.get("chain_id") == null || params.get("tx")==null) {
            return new CmdResponse(1, ConsensusErrorCode.PARAM_ERROR.getCode(),ConsensusErrorCode.PARAM_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
        try {
            int chain_id = (Integer) params.get("chain_id");
            String txHex = (String) params.get("tx");
            DepositTransaction transaction = new DepositTransaction();
            transaction.parse(HexUtil.decode(txHex),0);
            boolean result = validatorManager.validateDeposit(chain_id,transaction);
            if(!result){
                return new CmdResponse(1, ConsensusErrorCode.TX_DATA_VALIDATION_ERROR.getCode(),ConsensusErrorCode.TX_DATA_VALIDATION_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
            }
            return new CmdResponse(1, ConsensusErrorCode.SUCCESS.getCode(),ConsensusErrorCode.SUCCESS.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }catch (NulsException e){
            Log.error(e);
            return new CmdResponse(1, e.getErrorCode().getCode(),e.getErrorCode().getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
    }

    /**
     * 委托共识交易提交
     * */
    @Override
    public CmdResponse depositCommit(Map<String, Object> params) {
        if (params.get("chain_id") == null || params.get("tx")==null || params.get("blockHeader") == null) {
            return new CmdResponse(1, ConsensusErrorCode.PARAM_ERROR.getCode(),ConsensusErrorCode.PARAM_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
        try {
            int chain_id = (Integer) params.get("chain_id");
            String txHex = (String) params.get("tx");
            DepositTransaction transaction = new DepositTransaction();
            transaction.parse(HexUtil.decode(txHex),0);
            String headerHex = (String) params.get("blockHeader");
            BlockHeader blockHeader = new BlockHeader();
            blockHeader.parse(HexUtil.decode(headerHex),0);
            Deposit deposit = transaction.getTxData();
            deposit.setTxHash(transaction.getHash());
            deposit.setTime(transaction.getTime());
            deposit.setBlockHeight(blockHeader.getHeight());
            DepositPo depositPo = PoConvertUtil.depositToPo(deposit);
            if(!depositService.save(depositPo,chain_id)){
                return new CmdResponse(1, ConsensusErrorCode.SAVE_FAILED.getCode(),ConsensusErrorCode.SAVE_FAILED.getMsg(), ConsensusConstant.RPC_VERSION,null);
            }
            return new CmdResponse(1, ConsensusErrorCode.SUCCESS.getCode(),ConsensusErrorCode.SUCCESS.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }catch (NulsException e){
            Log.error(e);
            return new CmdResponse(1, e.getErrorCode().getCode(),e.getErrorCode().getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
    }

    /**
     * 委托共识交易回滚
     * */
    @Override
    public CmdResponse depositRollBack(Map<String, Object> params) {
        if (params.get("chain_id") == null || params.get("tx")==null) {
            return new CmdResponse(1, ConsensusErrorCode.PARAM_ERROR.getCode(),ConsensusErrorCode.PARAM_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
        try {
            int chain_id = (Integer) params.get("chain_id");
            String txHex = (String) params.get("tx");
            DepositTransaction transaction = new DepositTransaction();
            transaction.parse(HexUtil.decode(txHex),0);
            if(!depositService.delete(transaction.getHash(),chain_id)){
                return new CmdResponse(1, ConsensusErrorCode.ROLLBACK_FAILED.getCode(),ConsensusErrorCode.ROLLBACK_FAILED.getMsg(), ConsensusConstant.RPC_VERSION,null);
            }
            return new CmdResponse(1, ConsensusErrorCode.SUCCESS.getCode(),ConsensusErrorCode.SUCCESS.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }catch (NulsException e){
            Log.error(e);
            return new CmdResponse(1, e.getErrorCode().getCode(),e.getErrorCode().getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
    }

    /**
     * 退出共识交易验证
     * */
    @Override
    public CmdResponse withdrawValid(Map<String, Object> params) {
        if (params.get("chain_id") == null || params.get("tx")==null) {
            return new CmdResponse(1, ConsensusErrorCode.PARAM_ERROR.getCode(),ConsensusErrorCode.PARAM_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
        try {
            int chain_id = (Integer) params.get("chain_id");
            String txHex = (String) params.get("tx");
            CancelDepositTransaction transaction = new CancelDepositTransaction();
            transaction.parse(HexUtil.decode(txHex),0);
            boolean result = validatorManager.validateWithdraw(chain_id,transaction);
            if(!result){
                return new CmdResponse(1, ConsensusErrorCode.TX_DATA_VALIDATION_ERROR.getCode(),ConsensusErrorCode.TX_DATA_VALIDATION_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
            }
            return new CmdResponse(1, ConsensusErrorCode.SUCCESS.getCode(),ConsensusErrorCode.SUCCESS.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }catch (NulsException e){
            Log.error(e);
            return new CmdResponse(1, e.getErrorCode().getCode(),e.getErrorCode().getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
    }

    /**
     * 退出共识交易提交
     * */
    @Override
    public CmdResponse withdrawCommit(Map<String, Object> params) {
        if (params.get("chain_id") == null || params.get("tx")==null) {
            return new CmdResponse(1, ConsensusErrorCode.PARAM_ERROR.getCode(),ConsensusErrorCode.PARAM_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
        try {
            int chain_id = (Integer) params.get("chain_id");
            String txHex = (String) params.get("tx");
            CancelDepositTransaction transaction = new CancelDepositTransaction();
            transaction.parse(HexUtil.decode(txHex),0);
            //获取该笔交易对应的加入共识委托交易
            DepositPo po = depositService.get(transaction.getTxData().getJoinTxHash(),chain_id);
            //委托交易不存在
            if(po == null){
                return new CmdResponse(1, ConsensusErrorCode.DATA_NOT_EXIST.getCode(),ConsensusErrorCode.DATA_NOT_EXIST.getMsg(), ConsensusConstant.RPC_VERSION,null);
            }
            //委托交易已退出
            if(po.getDelHeight() > 0){
                return new CmdResponse(1, ConsensusErrorCode.DEPOSIT_WAS_CANCELED.getCode(),ConsensusErrorCode.DEPOSIT_WAS_CANCELED.getMsg(), ConsensusConstant.RPC_VERSION,null);
            }
            //设置退出共识高度
            po.setDelHeight(transaction.getBlockHeight());
            if(!depositService.save(po,chain_id)){
                return new CmdResponse(1, ConsensusErrorCode.SAVE_FAILED.getCode(),ConsensusErrorCode.SAVE_FAILED.getMsg(), ConsensusConstant.RPC_VERSION,null);
            }
            return new CmdResponse(1, ConsensusErrorCode.SUCCESS.getCode(),ConsensusErrorCode.SUCCESS.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }catch (NulsException e){
            Log.error(e);
            return new CmdResponse(1, e.getErrorCode().getCode(),e.getErrorCode().getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
    }

    /**
     * 退出共识交易回滚
     * */
    @Override
    public CmdResponse withdrawRollBack(Map<String, Object> params) {
        if (params.get("chain_id") == null || params.get("tx")==null) {
            return new CmdResponse(1, ConsensusErrorCode.PARAM_ERROR.getCode(),ConsensusErrorCode.PARAM_ERROR.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }try {
            int chain_id = (Integer) params.get("chain_id");
            String txHex = (String) params.get("tx");
            CancelDepositTransaction transaction = new CancelDepositTransaction();
            transaction.parse(HexUtil.decode(txHex),0);
            //获取该笔交易对应的加入共识委托交易
            DepositPo po = depositService.get(transaction.getTxData().getJoinTxHash(),chain_id);
            //委托交易不存在
            if(po == null){
                return new CmdResponse(1, ConsensusErrorCode.DATA_NOT_EXIST.getCode(),ConsensusErrorCode.DATA_NOT_EXIST.getMsg(), ConsensusConstant.RPC_VERSION,null);
            }
            if(po.getDelHeight() != transaction.getBlockHeight()){
                return new CmdResponse(1, ConsensusErrorCode.DEPOSIT_NEVER_CANCELED.getCode(),ConsensusErrorCode.DEPOSIT_NEVER_CANCELED.getMsg(), ConsensusConstant.RPC_VERSION,null);
            }
            po.setDelHeight(-1L);
            if(!depositService.save(po,chain_id)){
                return new CmdResponse(1, ConsensusErrorCode.ROLLBACK_FAILED.getCode(),ConsensusErrorCode.ROLLBACK_FAILED.getMsg(), ConsensusConstant.RPC_VERSION,null);
            }
            return new CmdResponse(1, ConsensusErrorCode.SUCCESS.getCode(),ConsensusErrorCode.SUCCESS.getMsg(), ConsensusConstant.RPC_VERSION,null);
        }catch (NulsException e){
            Log.error(e);
            return new CmdResponse(1, e.getErrorCode().getCode(),e.getErrorCode().getMsg(), ConsensusConstant.RPC_VERSION,null);
        }
    }

    private void fillAgentList(int chain_id, List<Agent> agentList, List<Deposit> depositList) {
        MeetingRound round = RoundManager.getInstance().getCurrentRound(chain_id);
        for (Agent agent : agentList) {
            fillAgent(chain_id, agent, round, depositList);
        }
    }

    private void fillAgent(int chain_id, Agent agent, MeetingRound round, List<Deposit> depositList) {
        if (null == depositList || depositList.isEmpty()) {
            depositList = ConsensusManager.getInstance().getAllDepositMap().get(chain_id);
        }
        Set<String> memberSet = new HashSet<>();
        Na total = Na.ZERO;
        for (int i = 0; i < depositList.size(); i++) {
            Deposit deposit = depositList.get(i);
            if (!agent.getTxHash().equals(deposit.getAgentHash())) {
                continue;
            }
            if (deposit.getDelHeight() >= 0) {
                continue;
            }
            total = total.add(deposit.getDeposit());
            memberSet.add(AddressTool.getStringAddressByBytes(deposit.getAddress()));
        }
        agent.setMemberCount(memberSet.size());
        agent.setTotalDeposit(total);
        if (round == null) {
            return;
        }
        MeetingMember member = round.getMember(agent.getPackingAddress());
        if (null == member) {
            agent.setStatus(0);
            return;
        }
        agent.setStatus(1);
        agent.setCreditVal(member.getAgent().getCreditVal());
    }
}
