package ru.v6.mark.prototype.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import ru.v6.mark.prototype.domain.constant.LogPerformance;
import ru.v6.mark.prototype.domain.constant.MarkType;
import ru.v6.mark.prototype.domain.constant.Measure;
import ru.v6.mark.prototype.domain.dao.ArticleDao;
import ru.v6.mark.prototype.domain.dao.GoodsDao;
import ru.v6.mark.prototype.domain.entity.Article;
import ru.v6.mark.prototype.domain.entity.Goods;
import ru.v6.mark.prototype.service.importer.ProductImportService;

import javax.annotation.Resource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class ImportService extends BaseService {

    @Autowired
    ArticleDao articleDao;

    @Autowired
    GoodsDao goodsDao;
    @Autowired
    GoodsService goodsService;
    @Autowired
    ProductImportService productImportService;

    @Resource
    ImportService importService;

    @Autowired
    NamedParameterJdbcTemplate npjtBI;

    @LogPerformance
    public void importArticles() {
        logger.info("Started: ImportService.importArticles");
        List<Article> articleList = articleDao.findAll();
        List<Goods> goodsList = goodsDao.findAll();

        articleList.parallelStream().forEach(article -> article.setDeleted(Boolean.TRUE));
        goodsList.parallelStream().forEach(goods -> goods.setDeleted(Boolean.TRUE));

        npjtBI.query("select " +
                "EANFULL, " +
                "ARTILCE, " +
                "LABEL, " +
                "RAY, " +
                "SEG, " +
                "CAT, " +
                "FAM, " +
                "TYPE, " +
                "FLAG," +
                "PRODUCTION," +
                //"FLAG_SMTH," +
                "VES  " +
                "from DM.VET_NEW\n" +
                "where FLAG > 6 and FLAG < 14", new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet resultSet) throws SQLException {
                Goods newGoods = new Goods();
                newGoods.setEan(resultSet.getString(1));
                goodsService.completeEan(newGoods);

                int index = goodsList.indexOf(newGoods);
                Goods goods;
                if (index == -1) {
                    goods = newGoods;
                    goodsList.add(goods);
                } else {
                    goods = goodsList.get(index);
                    goods.setDeleted(Boolean.FALSE);
                }

                Article newArticle = new Article();
                newArticle.setArticle(resultSet.getInt(2));

                index = articleList.indexOf(newArticle);
                Article article;
                if (index == -1) {
                    article = newArticle;
                    articleList.add(article);
                } else {
                    article = articleList.get(index);
                    article.setDeleted(Boolean.FALSE);
                }
                article.setName(resultSet.getString(3));

                article.setMarket(resultSet.getString(4));
                article.setSegment(resultSet.getString(5));
                article.setCategory(resultSet.getString(6));
                article.setFamily(resultSet.getString(7));
                article.setMeasure(Measure.forValue(resultSet.getString(8)));
                article.setMarkType(MarkType.forValue("TYPE" + resultSet.getString(9)));
                String imported = resultSet.getString(10);
                article.setImported(imported != null && "92".equals(imported.trim()));
                //article.setPcb(resultSet.getInt(11));

                goods.setArticle(article.getArticle());
            }
        });

        articleDao.saveAll(articleList, true);
        goodsDao.saveAll(goodsList, true);

        logger.info("Finished: ImportService.importArticles");
    }

    @LogPerformance
    public void updateCrptInfo(Goods goodsItem) {

        Goods goods = goodsDao.getById(goodsItem.getId());

        if (goods.getDeleted() != null && !goods.getDeleted() && goods.getArticleItem() != null && MarkType.TYPE8.equals(goods.getArticleItem().getMarkType())) {
            try {
                productImportService.importGoods(goods);
                goodsDao.save(goods);
            } catch (Exception e) {
                logger.info("Error updateCrptInfo for ean " + goods.getEan());
            }
        }
    }
}
