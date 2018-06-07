/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_righcleaner;

import com.opentext.livelink.service.docman.NodePermissions;
import com.opentext.livelink.service.memberservice.Member;

/**
 *
 * @author bho
 */
public abstract class Entry {
    private NodePermissions perm;
    private Member rightOwner;
    
    public abstract void applyRights();
    public abstract boolean checkRights();
    
    final void setMember(Member rightOwner) {
        this.rightOwner = rightOwner;
    }
    
    public Member getMember() {
        if(rightOwner != null) return rightOwner;
        return null;
    }
    
    public void setPermissions(String string){
        perm = new NodePermissions();
        switch(string){
            case "11111111":
                perm.setEditPermissionsPermission(true);
            case "11111110":
                perm.setDeletePermission(true);
            case "11111100":
                perm.setDeleteVersionsPermission(true);
            case "11111000":
                perm.setReservePermission(true);
            case "11110000":
                perm.setEditAttributesPermission(true);
            case "11100000":
                perm.setModifyPermission(true);
            case "11000000":
                perm.setSeeContentsPermission(true);
            case "10000000":
                perm.setSeePermission(true);
                break;
            case "00000000":
                perm.setEditPermissionsPermission(false);
                perm.setDeletePermission(false);
                perm.setDeleteVersionsPermission(false);
                perm.setReservePermission(false);
                perm.setEditAttributesPermission(false);
                perm.setModifyPermission(false);
                perm.setSeeContentsPermission(false);
                perm.setSeePermission(false);
                break;
            default: //do nothing
        }
    }
    public NodePermissions getPermissions(){
        if(perm != null) return perm;
        return null;
    }
}
