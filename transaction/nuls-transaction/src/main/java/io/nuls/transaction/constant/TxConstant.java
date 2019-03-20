package io.nuls.transaction.constant;

/**
 * @author: Charlie
 * @date: 2018/11/12
 */
public interface TxConstant {


    /** coinbase交易*/
    int TX_TYPE_COINBASE = 1;
    /** 转账交易*/
    int TX_TYPE_TRANSFER = 2;
    /** 设置别名*/
    int TX_TYPE_ALIAS = 3;
    /** 创建共识节点交易*/
    int TX_TYPE_REGISTER_AGENT = 4;
    /** 委托交易(加入共识)*/
    int TX_TYPE_JOIN_CONSENSUS = 5;
    /** 取消委托交易(退出共识)*/
    int TX_TYPE_CANCEL_DEPOSIT = 6;
    /** 黄牌惩罚*/
    int TX_TYPE_YELLOW_PUNISH = 7;
    /** 红牌惩罚*/
    int TX_TYPE_RED_PUNISH = 8;
    /** 停止节点(删除共识节点)*/
    int TX_TYPE_STOP_AGENT = 9;
    /** 跨链转账交易*/
    int TX_TYPE_CROSS_CHAIN_TRANSFER = 10;
    /** 注册链交易*/
    int TX_TYPE_REGISTER_CHAIN_AND_ASSET = 11;
    /** 销毁链*/
    int TX_TYPE_DESTROY_CHAIN_AND_ASSET = 12;
    /** 为链新增一种资产*/
    int TX_TYPE_ADD_ASSET_TO_CHAIN = 13;
    /** 删除链上资产*/
    int TX_TYPE_REMOVE_ASSET_FROM_CHAIN = 14;
    /** 创建智能合约交易*/
    int TX_TYPE_CREATE_CONTRACT = 100;
    /** 调用智能合约交易*/
    int TX_TYPE_CALL_CONTRACT = 101;
    /** 删除智能合约交易*/
    int TX_TYPE_DELETE_CONTRACT = 102;


    String LOG_TX = "tx/txChain";
    String LOG_NEW_TX_PROCESS = "tx/newTxProcess";
    String LOG_TX_MESSAGE = "tx/message";


    String TX_CMD_PATH = "io.nuls.transaction.rpc.cmd";

    /** system params */
    String SYS_ALLOW_NULL_ARRAY_ELEMENT = "protostuff.runtime.allow_null_array_element";
    String SYS_FILE_ENCODING = "file.encoding";

    String RPC_VERSION = "1.0";

    /** 新本地交易task,初始延迟值(秒) */
    int TX_TASK_INITIALDELAY = 5;
    /** 新本地交易task, 运行周期间隔(秒) */
    int TX_TASK_PERIOD = 3;

    /** 新跨链交易task,初始延迟值(秒) */
    int CTX_TASK_INITIALDELAY = 5;
    /** 新跨链交易task, 运行周期间隔(秒) */
    int CTX_TASK_PERIOD = 10;

    /** 未确认交易清理机制task,初始延迟值(秒) */
    int CLEAN_TASK_INITIALDELAY = 5;
    /** 未确认交易清理机制task, 运行周期间隔(分钟) */
    int CLEAN_TASK_PERIOD = 5;

    /**
     * 参数key
     */
    String KEY_CHAIN_ID ="chainId";
    String KEY_NODE_ID="nodeId";
    String KEY_MESSAGE_BODY="messageBody";

    /**
     * 创建多签交易时，返回map的key
     */
    String MULTI_TX_HASH = "txHash";
    String MULTI_TX_HEX = "txHex";

    /** 接收新交易的文件队列名**/
    String TX_UNVERIFIED_QUEUE_PREFIX = "tx_unverified_queue_";

    int PAGESIZE = 20;

    int PAGENUMBER = 1;

    /** DB config */
    String DB_CONFIG_NAME = "db_config.properties";

    /**
     * 交易hash最大长度
     */
    int TX_HASH_DIGEST_BYTE_MAX_LEN = 70;

    /**
     * 跨链交易固定为非解锁交易
     */
    byte CORSS_TX_LOCKED = 0;

    /**
     * Map初始值
     */
    int INIT_CAPACITY_16 = 16;
    int INIT_CAPACITY_8 = 8;
    int INIT_CAPACITY_4 = 4;
    int INIT_CAPACITY_2 = 2;

    /** H2 数据库交易表名前缀 */
    String H2_TX_TABLE_NAME_PREFIX = "transaction_";
    /** H2 数据库交易表索引前缀 */
    String H2_TX_TABLE_INDEX_NAME_PREFIX = "tx_index_";
    /** H2 数据库交易表唯一键前缀(暂取消未使用) */
    String H2_TX_TABLE_UNIQUE_NAME_PREFIX = "tx_unique_";

    /**
     * 跨链注册信息交易
     */
//    String TX_MODULE_VALIDATOR = "txProcess";
//    String CROSS_TRANSFER_VALIDATOR = "crossTxValidator";
//    String CROSS_TRANSFER_COMMIT = "crossTxCommit";
//    String CROSS_TRANSFER_ROLLBACK = "crossTxRollback";

//-----------------------------------------------------------------------------------------------------
    /** 单个交易最大2MB */
//    int TX_MAX_SIZE = 1024 * 1024 * 2;//=


    /** 接收新交易的文件队列最大容量**/
//    long TX_UNVERIFIED_QUEUE_MAXSIZE = 10000000L;//=

    /** 孤儿交易池最大容量**/
//    int ORPHAN_CONTAINER_MAX_SIZE = 200000;//=





//    String MODULE_CODE = "tx";//=
//    int NULS_CHAINID = 12345;//=
//    int NULS_CHAIN_ASSETID = 1;//=

//    int H2_TX_TABLE_NUMBER = 128;
    /**
     * 跨链交易打包确认后需要达到的最低阈值高度才生效
     */
//    long CTX_EFFECT_THRESHOLD = 30;//=


    /** 跨链验证通过率百分比, 跨链通过率 */
//    String CROSS_VERIFY_RESULT_PASS_RATE = "0.51";//=

    /** 链内通过率 */
//    String CHAIN_NODES_RESULT_PASS_RATE = "0.8";//=

//    /** 友链链内最近N个出块者阈值*/
//    int RECENT_PACKAGER_THRESHOLD = 30;//=

//    /** 未确认交易过期毫秒数-30分钟 */
//    long UNCONFIRMED_TX_EXPIRE_MS = 30 * 60 * 1000;//=

//    /** 本地计算nonce值的hash缓存有效时间 30秒*/
//    int HASH_TTL = 30000;//=
}
