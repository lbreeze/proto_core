package ru.v6.mark.prototype.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.v6.mark.prototype.domain.entity.KIZPosition;
import ru.v6.mark.prototype.service.KIZMarkService;
import ru.v6.mark.prototype.service.KIZPositionService;

import java.util.List;

/**
 * Created by Michael on 21.12.2019.
 */
@Component
public class MarkToTurnJobFacade extends JobFacade {

    @Autowired
    KIZPositionService kizPositionService;

    @Autowired
    KIZMarkService kizMarkService;

    @Override
    public void doJob() {
        List<KIZPosition> positions = kizPositionService.findPositionsToTurn();
        positions.parallelStream().forEach(position -> {
            kizMarkService.sendMarksToTurn(position);
        });
    }
}
