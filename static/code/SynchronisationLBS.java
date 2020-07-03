package fr.su.loginsmicrostrat.services.business.impl;

import fr.su.loginsmicrostrat.objects.business.be.DemandeSynchroEnum;
import fr.su.loginsmicrostrat.objects.business.be.EtatSynchroEnum;
import fr.su.loginsmicrostrat.objects.business.be.GestionnaireBE;
import fr.su.loginsmicrostrat.objects.business.be.SynchronisationBE;
import fr.su.loginsmicrostrat.objects.business.be.SynchronisationContextDO;
import fr.su.loginsmicrostrat.objects.business.be.TypeSynchroEnum;
import fr.su.loginsmicrostrat.objects.business.be.UtilisateurBE;
import fr.su.loginsmicrostrat.services.business.itf.DroitsMicrostratILBS;
import fr.su.loginsmicrostrat.services.business.itf.DroitsUtilisateurILBS;
import fr.su.loginsmicrostrat.services.business.itf.SynchronisationILBS;
import fr.su.loginsmicrostrat.services.business.itf.UtilisateursILBS;
import fr.su.loginsmicrostrat.services.data.itf.SynchronisationIDAO;
import fr.su.suapi.exception.BusinessException;
import fr.su.suapi.exception.util.ErrorDOBuilderFactory;
import fr.su.suapi.validation.BEAnnotationsValidator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.transaction.Transactional;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@Service
public class SynchronisationLBS implements SynchronisationILBS {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final SynchronisationIDAO synchronisationDAO;
    private final BEAnnotationsValidator<SynchronisationBE> validator;
    private final DroitsMicrostratILBS droitsMicrostratLBS;
    private final UtilisateursILBS utilisateursLBS;
    private final DroitsUtilisateurILBS droitsUtilisateurLBS;
    private final MeterRegistry meterRegistry;
    private final GestionnairesLBS gestionnairesLBS;
    private final AnomalieSynchronisationLBS anomalieSynchronisationLBS;
    private final TransactionTemplate transactionTplReqNew;
    private final ErrorDOBuilderFactory errorDOBuilderFactory;

    protected boolean synchronisationEnCours = false;

    public SynchronisationLBS(SynchronisationIDAO synchronisationDAO,
            BEAnnotationsValidator<SynchronisationBE> validator,
            DroitsMicrostratILBS droitsMicrostratLBS,
            UtilisateursILBS utilisateursLBS,
            DroitsUtilisateurILBS droitsUtilisateurLBS, GestionnairesLBS gestionnairesLBS,
            MeterRegistry meterRegistry,
            AnomalieSynchronisationLBS anomalieSynchronisationLBS,
            PlatformTransactionManager transactionManager, ErrorDOBuilderFactory errorDOBuilderFactory) {
        this.synchronisationDAO = synchronisationDAO;
        this.validator = validator;
        this.droitsMicrostratLBS = droitsMicrostratLBS;
        this.utilisateursLBS = utilisateursLBS;
        this.droitsUtilisateurLBS = droitsUtilisateurLBS;
        this.gestionnairesLBS = gestionnairesLBS;
        this.meterRegistry = meterRegistry;
        this.anomalieSynchronisationLBS = anomalieSynchronisationLBS;
        this.transactionTplReqNew = new TransactionTemplate(transactionManager);
        this.transactionTplReqNew.setPropagationBehavior(PROPAGATION_REQUIRES_NEW);
        this.errorDOBuilderFactory = errorDOBuilderFactory;
    }

    @Override
    public Optional<SynchronisationBE> creerDemande(SynchronisationContextDO context) throws BusinessException {
        Validate.notNull(context);
        Validate.notNull(context.getRequest());
        Validate.notNull(context.getUtilisateurConnecte());

        final GestionnaireBE gestionnaire = gestionnairesLBS
                .readGestionnaire(context.getUtilisateurConnecte().getUid());

        SynchronisationBE synchronisationBE = new SynchronisationBE();
        synchronisationBE.setDemandeSynchro(context.getRequest().getDemandeSynchro());
        if (StringUtils.isNotBlank(context.getRequest().getIdUtilisateurMicrostrat())) {
            final UtilisateurBE utilisateur = utilisateursLBS
                    .readUtilisateur(context.getRequest().getIdUtilisateurMicrostrat());
            synchronisationBE.setUtilisateurMicrostrat(utilisateur);
        }
        synchronisationBE.setDemandeur(gestionnaire);
        synchronisationBE.setEtatSynchro(EtatSynchroEnum.CREATED);
        validator.validateAndThrow(synchronisationBE);

        return Optional.of(synchronisationDAO.save(synchronisationBE));
    }

    @Override
    public boolean isSynchronisationEnCours() {
        return synchronisationEnCours;
    }

    @Override
    public Optional<SynchronisationBE> changerEtatSynchro(UUID idDemande, EtatSynchroEnum etat) {
        return transactionTplReqNew.execute(transactionStatus -> {
            Optional<SynchronisationBE> synchronisation = synchronisationDAO.findById(idDemande);
            if (synchronisation.isPresent()) {
                synchronisation.get().setEtatSynchro(etat);
                if (Arrays.asList(EtatSynchroEnum.SUCCESS, EtatSynchroEnum.ERROR).contains(etat)) {
                    synchronisation.get().setDateFinTraitement(new Date());
                }
                return Optional.of(synchronisationDAO.save(synchronisation.get()));
            }
            return synchronisation;
        });
    }

    @Override
    @Async
    @Transactional(Transactional.TxType.REQUIRED)
    public void synchroniser(UUID idDemande, SynchronisationContextDO context) {
        if (!synchronisationEnCours) {
            synchronisationEnCours = true;
            try {
                changerEtatSynchro(idDemande, EtatSynchroEnum.RUNNING);

                final DemandeSynchroEnum demandeSynchro = context.getRequest().getDemandeSynchro();
                Timer timerComplete = meterRegistry
                        .timer("microstrat.synchronisation." + demandeSynchro);
                timerComplete.record(() -> {
                    context.setIdDemande(idDemande);
                    try {
                        droitsMicrostratLBS.synchroniserDroitsMicrostrat(context);
                        utilisateursLBS.synchroniserUtilisateurs(context);
                        synchroniserAffectationsDroitsutilisateur(context);

                        SynchronisationBE synchronisation = synchronisationDAO.getOne(idDemande);
                        EtatSynchroEnum etatFinal = CollectionUtils.isEmpty(synchronisation.getAnomalies()) ?
                                EtatSynchroEnum.SUCCESS :
                                EtatSynchroEnum.ERROR;
                        changerEtatSynchro(idDemande, etatFinal);
                    } catch (Exception e) {
                        anomalieSynchronisationLBS.ajouterAnomalie(context.getIdDemande(),
                                StringUtils.defaultIfBlank(e.getMessage(), e.toString()), null);
                        log.error(e.getMessage(), e);
                        changerEtatSynchro(idDemande, EtatSynchroEnum.ERROR);
                    }
                    log.info("fin synchronisations");
                });
            } finally {
                synchronisationEnCours = false;
            }
        } else {
            changerEtatSynchro(idDemande, EtatSynchroEnum.ABORTED);
        }
    }

    private void synchroniserAffectationsDroitsutilisateur(SynchronisationContextDO context) {
        Validate.notNull(context);
        Validate.notNull(context.getRequest());
        final DemandeSynchroEnum demandeSynchro = context.getRequest().getDemandeSynchro();
        if (Arrays.asList(DemandeSynchroEnum.COMPLETE, DemandeSynchroEnum.TOUTES_AFFECTATIONS)
                .contains(demandeSynchro)) {
            utilisateursLBS.findUtilisateurs(null).stream().map(UtilisateurBE::getId)//
                    .forEach(it -> {
                        log.info("lancement synchroniserAffectationsDroitsutilisateur utilisateur: {}",
                                it);
                        droitsUtilisateurLBS.synchroniserAffectationsDroitsutilisateur(context, it);
                    });
        } else if (DemandeSynchroEnum.AFFECTATIONS_UTILISATEUR.equals(demandeSynchro)) {
            try {
                Validate.notBlank(context.getRequest().getIdUtilisateurMicrostrat(),
                        "L''id utilisateur microstrat doit être renseigné");
                droitsUtilisateurLBS.synchroniserAffectationsDroitsutilisateur(context,
                        context.getRequest().getIdUtilisateurMicrostrat());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                anomalieSynchronisationLBS.ajouterAnomalie(context.getIdDemande(),
                        StringUtils.defaultIfBlank(e.getMessage(), e.toString()),
                        TypeSynchroEnum.AFFECTATION_DROITS_UTILISATEUR);
            }
        }
    }

    @Override
    public SynchronisationBE getResultat(UUID id) throws BusinessException {
        return synchronisationDAO.findById(id).orElseThrow(() ->
                new BusinessException(errorDOBuilderFactory.getBuilder()
                        .appendCodeAndLabel("droitmicrostrat.synchronisation.inexistante", id)
                        .appendCurrentAndLimitValue(id, null)
                        .getOne())
        );
    }

}
