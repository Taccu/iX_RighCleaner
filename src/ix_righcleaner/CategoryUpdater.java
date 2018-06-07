/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_righcleaner;

import com.opentext.livelink.service.core.ChunkedOperationContext;
import com.opentext.livelink.service.docman.CategoryItemsUpgradeInfo;
import com.opentext.livelink.service.docman.DocumentManagement;

/**
 *
 * @author Taccu
 */
public class CategoryUpdater extends ContentServerTask{
    private final Integer batchSize;
    private final Long categoryId,categoryVersion;
    private Integer numItems = 0;
    public CategoryUpdater(Logger logger, String user, String password, Long categoryId, Long categoryVersion){
        super(logger, user, password);
        this.batchSize = 2000000;
        this.categoryId = categoryId;
        this.categoryVersion = categoryVersion;
    }
    @Override
    public String getNameOfTask(){
        return "Category-Updater";
    }
    
    @Override
    public void doWork(){
        DocumentManagement docManClient = getDocManClient();
        ChunkedOperationContext context = docManClient.upgradeCategoryItemsContext(categoryId, categoryVersion, false);
        context.setChunkSize(batchSize);
        CategoryItemsUpgradeInfo upgradeCategoryItems = docManClient.upgradeCategoryItems(context);
        CategoryItemsUpgradeInfo chunkIt = chunkIt(docManClient.upgradeCategoryItems(context));
        numItems += chunkIt.getUpgradedCount();
        logger.info("Sucessfully applied category upgrade to " + numItems + " child objects.");
        setProcessedItems(numItems);
    }
    private CategoryItemsUpgradeInfo chunkIt(CategoryItemsUpgradeInfo nrui){
        if(nrui.getUpgradedCount() > 0 ) {
            logger.debug("Updated " + nrui.getUpgradedCount() + " items...");
            numItems += nrui.getUpgradedCount();
            DocumentManagement docManClient = getDocManClient();
            ChunkedOperationContext context = nrui.getContext();
            context.setChunkSize(batchSize);
            chunkIt(docManClient.upgradeCategoryItems(context));
        }
            return nrui;
    }
}
