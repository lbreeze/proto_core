package ru.v6.mark.prototype.job;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.UnexpectedRollbackException;
import ru.v6.mark.xsd.Файл;
import ru.v6.mark.prototype.domain.constant.AcceptanceStatus;
import ru.v6.mark.prototype.domain.constant.AperakStatus;
import ru.v6.mark.prototype.domain.constant.JobType;
import ru.v6.mark.prototype.domain.entity.Acceptance;
import ru.v6.mark.prototype.domain.entity.JobTask;
import ru.v6.mark.prototype.domain.entity.Protocol;
import ru.v6.mark.prototype.domain.entity.Vendor;
import ru.v6.mark.prototype.service.*;
import ru.v6.mark.prototype.service.converter.AcceptanceConverter;
import ru.v6.mark.prototype.service.util.ResultError;

import javax.xml.bind.DataBindingException;
import javax.xml.bind.JAXB;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AcceptanceJobFacade extends JobFacade {

    @Autowired
    private AcceptanceService acceptanceService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private FileService fileService;
    @Autowired
    AcceptanceConverter acceptanceConverter;
    @Autowired
    private ProtocolService protocolService;
    @Autowired
    private JobTaskService jobTaskService;

    @Override
    public void doJob() {
        JobTask task = jobTaskService.getByType(JobType.ACCEPTANCE);

        Protocol protocol = new Protocol();
        protocol.setEntity("JOBTASK");
        protocol.setEntityId(String.valueOf(task.getId()));
        protocol.setAction("Обработаны УПД");

        StringBuffer changeLog = new StringBuffer();

        Map<String, Vendor> vendors = new ConcurrentHashMap<>();
        List<File> files = fileService.loadFiles();
        files.parallelStream().forEach(file -> {
            try {
                Файл файл = JAXB.unmarshal(file, Файл.class);
                ResultError resultError = new ResultError();

//                Acceptance acceptance = acceptanceService.save(файл, FilenameUtils.removeExtension(file.getName()), vendors, resultError);

                Acceptance acceptance = new Acceptance();
                acceptance.setFileName(FilenameUtils.removeExtension(file.getName()));

                try {
                    acceptanceConverter.convert(файл, acceptance, vendors);

                    String order = acceptance.getOrder();

                    boolean accepted = acceptanceService.removeDuplicates(acceptance);

                    if (!accepted) {
                        acceptance = acceptanceService.validationCheck(acceptance, resultError);

                        synchronized (task) {
                            changeLog.append("\n\n").append("Заказ: ").append(order).append(", ").append(fileService.transferFile(file, acceptance != null));
                        }

                        if (acceptance != null && acceptance.getStatus() != null && AcceptanceStatus.PROHIBITED.equals(acceptance.getStatus())) {
                            acceptanceService.generateXml(acceptance, AperakStatus.ERR, resultError);
                        }
                    } else {
                        synchronized (task) {
                            changeLog.append("\n\n").append("Заказ: ").append(order).append(" в процессе приемки, ").append(fileService.transferFile(file, false));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }

            } catch (DataBindingException e) {
                logger.error("Can't read/parse file {}", file.getPath(), e);
                synchronized (task) {
                    changeLog.append("\n\n").append("Ошибка: ").append(e.getMessage()).append(", ").append(fileService.transferFile(file, false));
                }
            } catch (UnexpectedRollbackException ure) {
                logger.error("UnexpectedRollbackException", ure);
                synchronized (task) {
                    changeLog.append("\n\n").append("Ошибка: ").append(ure.getMessage()).append(", ").append(fileService.transferFile(file, false));
                }
            } catch (Exception ex) {
                logger.error("Exception", ex);
                synchronized (task) {
                    changeLog.append("\n\n").append("Ошибка: ").append(ex.getMessage()).append(", ").append(fileService.transferFile(file, false));
                }
            }
        });

        if (changeLog.length() > 0) {
            protocol.setExternalLink("");
            protocol = protocolService.save(protocol);
            protocolService.saveAsFile(protocol.getId(), changeLog.toString().substring(2)); // remove starting newline chars
        }
    }
}
