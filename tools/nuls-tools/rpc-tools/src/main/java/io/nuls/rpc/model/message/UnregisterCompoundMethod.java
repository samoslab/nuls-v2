package io.nuls.rpc.model.message;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 取消订阅多个远程方法
 * Unsubscribe to multiple remote methods
 *
 * @author tangyi
 * @date 2018/11/15
 * @description
 */
@ToString
@NoArgsConstructor
public class UnregisterCompoundMethod {
    @Getter
    @Setter
    private String unregisterCompoundMethodName;

}
