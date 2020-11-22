package ru.v6.mark.prototype.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.v6.mark.prototype.domain.criteria.VendorCriteria;
import ru.v6.mark.prototype.domain.dao.BaseCriteriaDao;
import ru.v6.mark.prototype.domain.dao.VendorDao;
import ru.v6.mark.prototype.domain.entity.Vendor;

@Service
public class VendorService extends EntityCriteriaService<Vendor, VendorCriteria> {

    @Autowired
    VendorDao vendorDao;

    @Override
    protected BaseCriteriaDao<Vendor, VendorCriteria> getPrimaryCriteriaDao() {
        return vendorDao;
    }
}
