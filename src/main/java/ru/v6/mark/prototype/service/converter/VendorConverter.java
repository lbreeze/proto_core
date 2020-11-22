package ru.v6.mark.prototype.service.converter;

import org.springframework.stereotype.Component;
import ru.v6.mark.xsd.УчастникТип;
import ru.v6.mark.prototype.domain.entity.Vendor;

@Component
public class VendorConverter {

    public Vendor convert(УчастникТип участникТип) {
        Vendor vendor = new Vendor();
        vendor.setInn(участникТип.getИдСв().getСвЮЛУч().getИННЮЛ());
        vendor.setOkpo(участникТип.getОКПО());
        vendor.setKpp(участникТип.getИдСв().getСвЮЛУч().getКПП());
        vendor.setName(участникТип.getИдСв().getСвЮЛУч().getНаимОрг());
        if (участникТип.getАдрес() != null && участникТип.getАдрес().getАдрРФ() != null) {
            vendor.setAddress(участникТип.getАдрес().getАдрРФ().getИндекс()
                    + " " + участникТип.getАдрес().getАдрРФ().getГород()
                    + " " + участникТип.getАдрес().getАдрРФ().getУлица()
                    + " " + участникТип.getАдрес().getАдрРФ().getДом());
        }
        return vendor;
    }
}
