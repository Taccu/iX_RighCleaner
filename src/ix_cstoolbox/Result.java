/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_cstoolbox;

import java.util.Date;
import java.util.Objects;

/**
 *
 * @author bho
 */
public class Result {
    private int result_type;
    private String var_String;
    private Date var_Date;
    private Integer var_Int;
    private Long var_Long;
    private String resultColumn;
    
    public int getResult_type() {
        return result_type;
    }
    
    public String getResultColumn() {
        return resultColumn;
    }

    public void setResultColumn(String resultColumn) {
        this.resultColumn = resultColumn;
    }

    public void setResult_type(int result_type) {
        this.result_type = result_type;
    }

    public String getVar_String() {
        return var_String;
    }

    public void setVar_String(String var_String) {
        this.var_String = var_String;
    }

    public Date getVar_Date() {
        return var_Date;
    }

    public void setVar_Date(Date var_Date) {
        this.var_Date = var_Date;
    }

    public Integer getVar_Int() {
        return var_Int;
    }

    public void setVar_Int(Integer var_Int) {
        this.var_Int = var_Int;
    }

    public Long getVar_Long() {
        return var_Long;
    }

    public void setVar_Long(Long var_Long) {
        this.var_Long = var_Long;
    }

    @Override
    public String toString() {
        switch(getResult_type()){
            case 0:
                return getVar_String();
            case 1:
                return getVar_Int().toString();
            case 2:
                return getVar_Long().toString();
            case 3:
                return getVar_Date().toString();
            default:
                return "";
        }
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + Objects.hashCode(this.resultColumn);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Result other = (Result) obj;
        if (!Objects.equals(this.resultColumn, other.resultColumn)) {
            return false;
        }
        return true;
    }
    
    
}
