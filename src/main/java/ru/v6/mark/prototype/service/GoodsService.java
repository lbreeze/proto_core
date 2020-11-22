package ru.v6.mark.prototype.service;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.v6.mark.prototype.domain.constant.KIZMarkStatus;
import ru.v6.mark.prototype.domain.constant.LogPerformance;
import ru.v6.mark.prototype.domain.constant.MarkType;
import ru.v6.mark.prototype.domain.constant.Status;
import ru.v6.mark.prototype.domain.criteria.GoodsCriteria;
import ru.v6.mark.prototype.exception.ApplicationException;
import ru.v6.mark.prototype.service.importer.ProductImportService;
import ru.v6.mark.prototype.web.context.CheckMarkInfo;
import ru.v6.mark.prototype.web.context.RequestWrapper;
import ru.v6.mark.prototype.web.context.Response;
import ru.v6.mark.prototype.domain.dao.*;
import ru.v6.mark.prototype.domain.entity.*;

import java.io.Serializable;

@Service
public class GoodsService extends EntityCriteriaService<Goods, GoodsCriteria> {

    @Autowired
    GoodsDao goodsDao;
    @Autowired
    GtinDao gtinDao;
    @Autowired
    KIZMarkDao kizMarkDao;
    @Autowired
    UserDao userDao;
    @Autowired
    ProductImportService productImportService;

    @Override
    protected BaseCriteriaDao<Goods, GoodsCriteria> getPrimaryCriteriaDao() {
        return goodsDao;
    }

    @LogPerformance
    public Article markControlCheckEan(String ean) {
        ean = completeEan(ean);
        Goods goods = getById(ean);
        if (goods != null) {
            Hibernate.initialize(goods.getArticleItem());
            Article result = goods.getArticleItem();
            if (result != null && result.getMarkType() != null && result.getMarkType().equals(MarkType.TYPE8)) {
                return result;
            } else {
                throw ApplicationException
                        .build("Штрихкод {0} не относится к типу продукции \"Обувь\".")
                        .parameters(ean)
                        .status(Status.ERROR_COMMON);
            }
        } else {
            throw ApplicationException
                    .build("Штрихкод {0} отсутствует в справочнике.")
                    .parameters(ean)
                    .status(Status.ERROR_COMMON);
        }
    }

    @LogPerformance
    public Response markControlCheckMark(CheckMarkInfo checkMarkInfo) {
        if (checkMarkInfo != null && checkMarkInfo.getMark() != null && checkMarkInfo.getMark().length() >= 31) {
            String markIdent = checkMarkInfo.getMark().substring(0, 31);
            KIZMark kizMark = kizMarkDao.findByMark(markIdent);
            if (kizMark != null) {
                if (KIZMarkStatus.RECEIVED.equals(kizMark.getStatus())) {
                    Gtin gtin = gtinDao.getById(kizMark.getPosition().getEan());
                    if (gtin != null) {
                        String ean = completeEan(checkMarkInfo.getEan());
                        Goods goods = goodsDao.getById(ean);
                        if (goods != null) {
                            Article eanArticle = goods.getArticleItem();
                            if (gtin.getMarkSubType().equals(eanArticle.getMarkSubType()) && gtin.getImported() != null && eanArticle.getImported() != null && gtin.getImported().equals(eanArticle.getImported())) {
                                return new Response(Status.OK);
                            } else {
                                throw ApplicationException
                                        .build("Марка {0} не соответствует типу обуви или месту производства, нанесение запрещено.")
                                        .parameters(markIdent)
                                        .status(Status.ERROR_COMMON);
                            }
                        } else {
                            throw ApplicationException
                                    .build("Штрихкод {0} отсутствует в справочнике.")
                                    .parameters(checkMarkInfo.getEan())
                                    .status(Status.ERROR_COMMON);
                        }

                    } else {
                        throw ApplicationException
                                .build("Для марки {0} отсутствует GTIN, нанесение запрещено.")
                                .parameters(markIdent)
                                .status(Status.ERROR_COMMON);
                    }
                } else {
                    throw ApplicationException
                            .build("Марка {0} была проверена или введена в оборот ранее.")
                            .parameters(markIdent)
                            .status(Status.ERROR_COMMON);
                }
            } else {
                throw ApplicationException
                        .build("Марка {0} не найдена, нанесение запрещено.")
                        .parameters(markIdent)
                        .status(Status.ERROR_COMMON);
            }

        } else {
            throw ApplicationException
                    .build("Некорректый код марки {0}. Пересканируйте марку.")
                    .parameters(checkMarkInfo.getMark())
                    .status(Status.ERROR_COMMON);
        }
    }

    public Goods getWithAttributes(Serializable id) {
        Goods result = getById(id);
        Hibernate.initialize(result.getAttributes());
        return result;
    }

    public Goods saveAsNew(RequestWrapper<Goods> entity) {
        Goods entityItem = entity.getEntity();
        completeEan(entityItem);
        Goods goods = getById(entityItem.getId());
        if (goods != null) {
            throw ApplicationException
                    .build("Такой штрих-код уже существует.\nПерепривязать к новому артикулу?")
                    .status(Status.CONFIRMATION_REQUIRED);
        } else {
            if (updateCrptInfo(entity)) {
                goods = goodsDao.save(entityItem);
            }
            return goods;
        }
    }

    public Goods saveArticle(Goods entity) {
        completeEan(entity);
        Goods goods = goodsDao.getById(entity.getId());
        if (goods != null) {
            goods.setArticle(entity.getArticle());
            entity = goodsDao.save(goods);
        }
        return entity;
    }

    @Override
    public Goods save(Goods entity) {
        completeEan(entity);
        return goodsDao.save(entity);
    }

    private boolean updateCrptInfo(RequestWrapper<Goods> entity) {
        Goods entityItem = entity.getEntity();

        User user = userDao.getById(entity.getCurrentUser());
        return productImportService.importGoods(entityItem);
    }

    public void completeEan(Goods entity) {
        entity.setEan(completeEan(entity.getEan()));
    }

    public String completeEan(String ean) {
        if (ean != null && !ean.isEmpty()) {
            StringBuilder sb = new StringBuilder(ean);
            while (sb.length() < Goods.EAN_MAX_LENGTH) {
                sb.insert(0, "0");
            }
            return sb.toString();
        } else {
            throw ApplicationException.build("Штрих-код не задан!");
        }
    }
}
