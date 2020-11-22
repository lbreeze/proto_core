package ru.v6.mark.prototype.domain.dao;

import org.springframework.stereotype.Repository;
import ru.v6.mark.prototype.domain.entity.Protocol;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

@Repository
public class ProtocolDao extends BaseDao<Protocol> {

    public List<Protocol> findProtocols(String entity, String entityId) {
        return findList(new CriteriaQueryBuilder<Protocol>() {
            @Override
            public CriteriaQuery<Protocol> buildCriteria(CriteriaBuilder cb) {
                CriteriaQuery<Protocol> query = cb.createQuery(Protocol.class);
                Root<Protocol> root = query.from(Protocol.class);
                query.select(root);

                query.where(
                        cb.equal(root.<String>get("entity"), entity),
                        cb.equal(root.<String>get("entityId"), entityId)
                );

                query.orderBy(cb.desc(root.get("lastUpdate")));
                return query;
            }
        });
    }
}