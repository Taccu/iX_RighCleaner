/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_righcleaner;

/**
 *
 * @author bho
 */
public interface ILogger {
    void debug(String message);
    
    void error(String error);
    
    void info(String info);
    
    void warn(String Warning);
}
