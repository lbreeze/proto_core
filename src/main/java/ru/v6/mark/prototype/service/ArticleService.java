package ru.v6.mark.prototype.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.v6.mark.prototype.domain.criteria.ArticleCriteria;
import ru.v6.mark.prototype.domain.dao.ArticleDao;
import ru.v6.mark.prototype.domain.dao.BaseCriteriaDao;
import ru.v6.mark.prototype.domain.entity.Article;

@Service
public class ArticleService extends EntityCriteriaService<Article, ArticleCriteria> {

    @Autowired
    ArticleDao articleDao;

    @Override
    protected BaseCriteriaDao<Article, ArticleCriteria> getPrimaryCriteriaDao() {
        return articleDao;
    }

}
