package fr.su.loginsmicrostrat.services.business.impl;

import fr.su.loginsmicrostrat.objects.business.be.DemandeSynchroEnum;
import fr.su.loginsmicrostrat.objects.business.be.SynchronisationContextDO;
import fr.su.loginsmicrostrat.objects.business.be.TypeSynchroEnum;
import fr.su.loginsmicrostrat.objects.business.be.UtilisateurBE;
import fr.su.loginsmicrostrat.services.business.itf.GestionnairesILBS;
import fr.su.loginsmicrostrat.services.business.itf.UtilisateursILBS;
import fr.su.loginsmicrostrat.services.data.itf.UtilisateursIDAO;
import fr.su.loginsmicrostrat.services.external.itf.MicrostratIXBS;
import fr.su.suapi.exception.BusinessException;
import fr.su.suapi.exception.EntityNotFoundBusinessException;
import fr.su.suapi.exception.util.ErrorDOBuilderFactory;
import fr.su.suapi.validation.BEAnnotationsValidator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Arrays;
import java.util.List;

import static fr.su.suapi.exception.util.ErrorConstants.NOT_FOUND_ENTITY;
import static java.util.stream.Collectors.toList;

@Service
public class UtilisateursLBS implements UtilisateursILBS {

    private static final String STAR = "*";
    private static final String PERCENT = "%";
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final UtilisateursIDAO utilisateursDAO;
    private final MicrostratIXBS microstratXBS;
    private final GestionnairesILBS gestionnairesLBS;
    private final ErrorDOBuilderFactory builderFactory;
    private final BEAnnotationsValidator<UtilisateurBE> validator;
    private final AnomalieSynchronisationLBS anomalieSynchronisationLBS;

    public UtilisateursLBS(UtilisateursIDAO utilisateursDAO, MicrostratIXBS microstratXBS,
            GestionnairesILBS gestionnairesLBS, ErrorDOBuilderFactory builderFactory,
            BEAnnotationsValidator<UtilisateurBE> validator,
            AnomalieSynchronisationLBS anomalieSynchronisationLBS) {
        this.utilisateursDAO = utilisateursDAO;
        this.microstratXBS = microstratXBS;
        this.builderFactory = builderFactory;
        this.gestionnairesLBS = gestionnairesLBS;
        this.validator = validator;
        this.anomalieSynchronisationLBS = anomalieSynchronisationLBS;
    }

    @Override
    public List<UtilisateurBE> findUtilisateurs(String login) {
        if (login == null) {
            return utilisateursDAO.findAll();
        }

        final String likeLogin = login.replace(STAR, PERCENT);
        return utilisateursDAO.findByLoginLikeIgnoreCase(likeLogin);
    }

    @Override
    public UtilisateurBE readUtilisateur(String idMicrostrat) throws EntityNotFoundBusinessException {
        return utilisateursDAO.findById(idMicrostrat).orElseThrow(() -> new EntityNotFoundBusinessException(
                builderFactory.getBuilder().appendCodeAndLabel(NOT_FOUND_ENTITY).getOne()));
    }

    @Override
    public UtilisateurBE createUtilisateur(UtilisateurBE utilisateur) throws BusinessException {
        final UtilisateurBE utilisateurMstr = microstratXBS.insertUtilisateurInMicrostrat(utilisateur);
        return utilisateursDAO.save(utilisateurMstr);
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void synchroniserUtilisateurs(SynchronisationContextDO context) {
        final DemandeSynchroEnum demandeSynchro = context.getRequest().getDemandeSynchro();
        if (Arrays.asList(DemandeSynchroEnum.COMPLETE, DemandeSynchroEnum.UTILISATEURS_DROITS,
                DemandeSynchroEnum.UTILISATEURS).contains(demandeSynchro)) {
            log.info("lancement synchroniserUtilisateurs");
            try {
                gestionnairesLBS.checkGestionnaireAdmin(context.getUtilisateurConnecte());
                final List<String> idsUtilisateursDb = this.findUtilisateurs(null).stream()
                        .map(UtilisateurBE::getId)
                        .collect(toList());
                final List<UtilisateurBE> aInserer = microstratXBS.findUtilisateursMicrostrat().stream()
                        .filter(uti -> !idsUtilisateursDb.contains(uti.getId())).collect(toList());
                validator.validateAndThrow(aInserer);
                utilisateursDAO.saveAll(aInserer);
            } catch (BusinessException be) {
                log.error(be.getMessage(), be);
                anomalieSynchronisationLBS
                        .ajouterAnomalie(context.getIdDemande(), be.getErreurs(), TypeSynchroEnum.UTILISATEURS);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                anomalieSynchronisationLBS.ajouterAnomalie(context.getIdDemande(),
                        StringUtils.defaultIfBlank(e.getMessage(), e.toString()), TypeSynchroEnum.UTILISATEURS);
            }
        }
    }
}
