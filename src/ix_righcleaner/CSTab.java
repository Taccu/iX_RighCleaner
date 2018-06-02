/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_righcleaner;

/**
 *
 * @author Taccu
 */
public interface CSTab {
    public void run(ContentServerTask task);
    public boolean validateConfiguration();
}
