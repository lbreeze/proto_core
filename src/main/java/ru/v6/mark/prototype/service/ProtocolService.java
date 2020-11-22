package ru.v6.mark.prototype.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.v6.mark.prototype.domain.dao.BaseDao;
import ru.v6.mark.prototype.domain.dao.ProtocolDao;
import ru.v6.mark.prototype.domain.entity.BaseEntity;
import ru.v6.mark.prototype.domain.entity.Protocol;

import java.io.Serializable;
import java.util.List;

@Service
public class ProtocolService extends EntityService<Protocol> {

    @Autowired
    FileService fileService;

    @Autowired
    ProtocolDao protocolDao;

    @Override
    protected BaseDao<Protocol> getPrimaryDao() {
        return protocolDao;
    }

    public <T extends BaseEntity> List<Protocol> findProtocols(T entity) {
        return protocolDao.findProtocols(entity.getClass().getSimpleName().toUpperCase(), entity.getId().toString());
    }

    public <T extends BaseEntity> List<Protocol> findProtocols(String entityName, Serializable id) {
        return protocolDao.findProtocols(entityName, id.toString());
    }

    @Override
    public Protocol saveAsNew(Protocol entity) {
        return protocolDao.save(entity);
    }

    @Transactional
    public String readFile(String protocolId) {
        return fileService.read(getPathArrayForId(protocolId));
    }

    @Transactional
    public void saveAsFile(Long protocolId, String changeLog) {
        fileService.save(changeLog, getPathArrayForId(String.valueOf(protocolId)));
    }

    private String[] getPathArrayForId(String protocolId) {
        StringBuilder id = new StringBuilder(protocolId);
        while (id.length() < 7) {
            id.insert(0, '0');
        }
        String folder34 = id.substring(id.length() - 4, id.length() - 2);
        String folder56 = id.substring(id.length() - 6, id.length() - 4);
        String folder7 = id.substring(0, id.length() - 6);

        return new String[]{"protocol", folder7, folder56, folder34, protocolId + ".txt"};
    }

}

