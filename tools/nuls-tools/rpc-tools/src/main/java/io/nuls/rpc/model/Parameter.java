package io.nuls.rpc.model;

/**
 * @author tangyi
 * @date 2018/11/19
 * @description
 */
public class Parameter {
    private String parameterName;
    private String parameterType;
    private String parameterValidRange;
    private String parameterValidRegExp;

    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    public String getParameterType() {
        return parameterType;
    }

    public void setParameterType(String parameterType) {
        this.parameterType = parameterType;
    }

    public String getParameterValidRange() {
        return parameterValidRange;
    }

    public void setParameterValidRange(String parameterValidRange) {
        this.parameterValidRange = parameterValidRange;
    }

    public String getParameterValidRegExp() {
        return parameterValidRegExp;
    }

    public void setParameterValidRegExp(String parameterValidRegExp) {
        this.parameterValidRegExp = parameterValidRegExp;
    }
}
