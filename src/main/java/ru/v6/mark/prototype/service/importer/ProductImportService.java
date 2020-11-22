package ru.v6.mark.prototype.service.importer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.v6.mark.prototype.domain.criteria.GoodsCriteria;
import ru.v6.mark.prototype.domain.entity.Goods;
import ru.v6.mark.prototype.service.CachedDataReceiver;
import ru.v6.mark.prototype.service.ClientService;
import ru.v6.mark.prototype.service.GoodsService;
import ru.v6.mark.prototype.service.converter.ProductConverter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductImportService {

    @Autowired
    ClientService clientService;

    @Autowired
    CachedDataReceiver cachedDataReceiver;

    @Autowired
    ProductConverter productConverter;

    @Autowired
    GoodsService goodsService;

    public List<Goods> getUnboundGoods(GoodsCriteria criteria) {
        List<Goods> goods = goodsService.findByCriteria(criteria);
        for (Goods good : goods) {
            if (importGoods(good))
                goodsService.save(good);
        }
        return goods;
    }

    public boolean importGoods(Goods entity) {
        Map<String, String> params = new HashMap<>();
        params.put("gtins", entity.getEan());
        String product = clientService.getProduct(cachedDataReceiver.getTokenById(null, false).getValue(), params);
        return productConverter.convert(product, entity);
    }
}
