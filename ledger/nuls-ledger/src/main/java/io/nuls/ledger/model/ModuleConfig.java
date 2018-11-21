package io.nuls.ledger.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Created by wangkun23 on 2018/11/19.
 */
@ToString
public class ModuleConfig {

    @Setter
    @Getter
    private String databaseDir;

    @Setter
    @Getter
    private Boolean databaseReset;

    @Setter
    @Getter
    protected Integer databaseVersion;
}
