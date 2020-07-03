package fr.su.loginsmicrostrat.services.business.impl;

import fr.su.loginsmicrostrat.objects.business.be.ExportActionsDO;
import fr.su.loginsmicrostrat.services.business.impl.export.HistoriqueMicrostratExport;
import fr.su.loginsmicrostrat.services.business.itf.ExportActionsILBS;
import fr.su.loginsmicrostrat.services.data.itf.ExportActionsIDAO;
import fr.su.suapi.exception.BusinessException;
import fr.su.suapi.exception.util.ErrorDOBuilder;
import fr.su.suapi.exception.util.ErrorDOBuilderFactory;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class ExportActionsLBS implements ExportActionsILBS {

    private static final int ZERO = 0;
    private static final int LAST_DAY_HOUR = 23;
    private static final int LAST_DAY_MINUTES_SECONDS = 59;

    private final ExportActionsIDAO exportActionsDAO;
    private final HistoriqueMicrostratExport historiqueMicrostratExport;
    private final ErrorDOBuilderFactory errorDOBuilderFactory;

    public ExportActionsLBS(ExportActionsIDAO exportActionsDAO, HistoriqueMicrostratExport historiqueMicrostratExport,
            ErrorDOBuilderFactory errorDOBuilderFactory) {
        this.exportActionsDAO = exportActionsDAO;
        this.historiqueMicrostratExport = historiqueMicrostratExport;
        this.errorDOBuilderFactory = errorDOBuilderFactory;
    }

    @Override
    public Resource readExportHistoriqueMicrostrat(Date debut, Date fin) throws BusinessException, IOException {
        final ErrorDOBuilder builder = errorDOBuilderFactory.getBuilder();
        if (debut == null || fin == null) {
            builder.appendCodeAndLabel("export.dates.vides");
            throw new BusinessException(builder.getAll());
        } else if (!debut.before(fin) && !debut.equals(fin)) {
            builder.appendCodeAndLabel("export.dates.incoherence");
            throw new BusinessException(builder.getAll());
        }

        debut = setHourMinutesSeconds(debut, ZERO, ZERO, ZERO);
        fin = setHourMinutesSeconds(fin, LAST_DAY_HOUR, LAST_DAY_MINUTES_SECONDS, LAST_DAY_MINUTES_SECONDS);
        List<ExportActionsDO> exportActions = exportActionsDAO.findAllExportActionsBetween(debut, fin);

        return historiqueMicrostratExport.readExportHistoriqueMicrostrat(exportActions);
    }

    /**
     * Set hour, minutes and seconds of date
     * @param date the date
     * @param hour hour
     * @param minutes minutes
     * @param seconds seconds
     * @return dates set with requested parameters
     */
    private Date setHourMinutesSeconds(Date date, int hour, int minutes, int seconds) {
        Calendar calendar = DateUtils.toCalendar(date);
        calendar.set(Calendar.HOUR, hour);
        calendar.set(Calendar.MINUTE, minutes);
        calendar.set(Calendar.SECOND, seconds);

        return calendar.getTime();
    }
}
