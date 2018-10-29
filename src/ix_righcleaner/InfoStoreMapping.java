/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_righcleaner;

import java.util.Objects;

/**
 *
 * @author bho
 */
public class InfoStoreMapping {
    private Class srcType, dstType;
    private String src, dst;
    
    public boolean validate() {
        if(srcType != null)
        if(dstType != null)
        if(src != null && !src.isEmpty())
        return dst != null && !dst.isEmpty();
        else return false;
        else return false;
        else return false;
    }

    @Override
    public String toString() {
        return "InfoStoreMapping{" + "src=" + src + ", dst=" + dst + '}';
    }
   
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + Objects.hashCode(this.srcType);
        hash = 47 * hash + Objects.hashCode(this.dstType);
        hash = 47 * hash + Objects.hashCode(this.src);
        hash = 47 * hash + Objects.hashCode(this.dst);
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
        final InfoStoreMapping other = (InfoStoreMapping) obj;
        if (!Objects.equals(this.src, other.src)) {
            return false;
        }
        if (!Objects.equals(this.dst, other.dst)) {
            return false;
        }
        if (!Objects.equals(this.srcType, other.srcType)) {
            return false;
        }
        if (!Objects.equals(this.dstType, other.dstType)) {
            return false;
        }
        return true;
    }

    
    public Class getSrcType() {
        return srcType;
    }

    public void setSrcType(Class srcType) {
        this.srcType = srcType;
    }

    public Class getDstType() {
        return dstType;
    }

    public void setDstType(Class dstType) {
        this.dstType = dstType;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public String getDst() {
        return dst;
    }

    public void setDst(String dst) {
        this.dst = dst;
    }
    
    
}
