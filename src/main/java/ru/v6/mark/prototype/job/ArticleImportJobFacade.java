package ru.v6.mark.prototype.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.v6.mark.prototype.service.GoodsService;
import ru.v6.mark.prototype.service.ImportService;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ArticleImportJobFacade extends JobFacade {

    @Autowired
    private ImportService importService;
    @Autowired
    private GoodsService goodsService;

    @Override
    public void doJob() {
        importService.importArticles();

        AtomicInteger count = new AtomicInteger(0);

/*
        logger.info("Started: ImportService.updateCrptInfo");
        goodsService.findAll().parallelStream().forEach(goods -> {
            logger.debug(Thread.currentThread().getName() + " | count: " + count.incrementAndGet());
            importService.updateCrptInfo(goods);
        });
        logger.info("Finished: ImportService.updateCrptInfo");
*/
    }

}
