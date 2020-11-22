package ru.v6.mark.prototype.service.converter;

import org.springframework.stereotype.Component;
import ru.v6.mark.prototype.domain.entity.Goods;
import ru.v6.mark.prototype.exception.ApplicationException;
import ru.v6.mark.prototype.service.util.JSONUtil;

import java.util.*;

@Component
public class ProductConverter {

    private static final List<String> primaryAttributes = Arrays.asList("id", "productGroupId", "productType", "name", "country", "inn", "gtin");

    public boolean convert(String externalJson, Goods goods) {

        boolean result = false;
        if (externalJson != null) {
            Map<String, String> results = (LinkedHashMap) JSONUtil.getObject("results", externalJson).get(0);
            result = true;
            String id = StringUtil.getValue(results,"id");
            String groupId = StringUtil.getValue(results,"productGroupId");
            String productType = StringUtil.getValue(results,"productType");
            String name = StringUtil.getValue(results,"name");
            String country = StringUtil.getValue(results,"country");
            String inn = StringUtil.getValue(results,"inn");
            if (id != null) {
                try {
                    goods.setCrptIdentity(Long.parseLong(id));
                } catch (NumberFormatException e) {
                    throw ApplicationException.build(e, "Нечисловой идентификатор ЦРПТ для продукции с GTIN {0}").parameters(goods.getEan());
                }
            }
            goods.setCrptGroup(groupId);
            goods.setCrptType(productType);
            goods.setName(name == null ? null : (name.length() > 250 ? name.substring(0, 250) : name));
            //goods.setProducerName();
            goods.setProducerInn(inn);
            goods.setProducerCountry(country);

            Set<String> keys = results.keySet();
            if (keys != null) {
                Map<String, String> attributes = new HashMap<>();
                for (String key : keys) {
                    if (!primaryAttributes.contains(key)) {
                        String value = StringUtil.getValue(results, key);
                        attributes.put(key, value);
                    }
                }
                if (!attributes.isEmpty())
                    goods.setAttributes(attributes);
            }
        }
        return result;
    }

}
