package fr.su.loginsmicrostrat.services.business.impl;

import fr.su.loginsmicrostrat.objects.business.be.ActiviteBE;
import fr.su.loginsmicrostrat.objects.business.be.DemandeSynchroEnum;
import fr.su.loginsmicrostrat.objects.business.be.DroitMicrostratBE;
import fr.su.loginsmicrostrat.objects.business.be.SynchronisationContextDO;
import fr.su.loginsmicrostrat.objects.business.be.TypeSynchroEnum;
import fr.su.loginsmicrostrat.services.business.itf.DroitsMicrostratILBS;
import fr.su.loginsmicrostrat.services.business.itf.GestionnairesILBS;
import fr.su.loginsmicrostrat.services.data.itf.ActivitesIDAO;
import fr.su.loginsmicrostrat.services.data.itf.DroitsMicrostratIDAO;
import fr.su.loginsmicrostrat.services.external.itf.MicrostratIXBS;
import fr.su.suapi.exception.BusinessException;
import fr.su.suapi.validation.BEAnnotationsValidator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
public class DroitsMicrostratLBS implements DroitsMicrostratILBS {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final MicrostratIXBS microstratXBS;
    private final DroitsMicrostratIDAO droitsMicrostratDAO;
    private final ActivitesIDAO activitesDAO;
    private final GestionnairesILBS gestionnairesLBS;
    private final BEAnnotationsValidator<DroitMicrostratBE> validator;
    private final AnomalieSynchronisationLBS anomalieSynchronisationLBS;

    public DroitsMicrostratLBS(MicrostratIXBS microstratXBS, DroitsMicrostratIDAO droitsMicrostratDAO,
            ActivitesIDAO activitesDAO, GestionnairesILBS gestionnairesLBS, BEAnnotationsValidator validator,
            AnomalieSynchronisationLBS anomalieSynchronisationLBS) {
        this.microstratXBS = microstratXBS;
        this.droitsMicrostratDAO = droitsMicrostratDAO;
        this.activitesDAO = activitesDAO;
        this.gestionnairesLBS = gestionnairesLBS;
        this.validator = validator;
        this.anomalieSynchronisationLBS = anomalieSynchronisationLBS;
    }

    @Override
    public List<DroitMicrostratBE> findAllDroitsMicrostrat() {
        return droitsMicrostratDAO.findAll();
    }

    @Override
    public List<DroitMicrostratBE> findDroitsMicrostratNonSupprimes() {
        return this.findAllDroitsMicrostrat().stream().filter(droit -> !droit.getSupprime()).collect(toList());
    }

    @Override
    public List<DroitMicrostratBE> findByIdMicrostratIn(List<String> idMicrostratList) {
        return droitsMicrostratDAO.findByIdIn(idMicrostratList);
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void synchroniserDroitsMicrostrat(SynchronisationContextDO context) {
        final DemandeSynchroEnum demandeSynchro = context.getRequest().getDemandeSynchro();
        if (Arrays.asList(DemandeSynchroEnum.COMPLETE, DemandeSynchroEnum.UTILISATEURS_DROITS,
                DemandeSynchroEnum.DROITS).contains(demandeSynchro)) {
            log.info("lancement synchroniserDroitsMicrostrat");
            try {
                // Vérifie si le gestionnaire est admin
                gestionnairesLBS.checkGestionnaireAdmin(context.getUtilisateurConnecte());
                // Récupération des droits dans Microstrat
                final List<DroitMicrostratBE> droitsMstr = microstratXBS.findDroitsMicrostrat();
                // Récupération des droits en base
                final List<DroitMicrostratBE> droitsDb = droitsMicrostratDAO.findAll();
                final List<String> idsDroitMstr = droitsMstr.stream().map(DroitMicrostratBE::getId)
                        .collect(toList());
                final List<String> idsDroitDb = droitsDb.stream().map(DroitMicrostratBE::getId).collect(toList());

                // Calcul des droits ajoutés dans Microstrat
                final List<DroitMicrostratBE> addedDroits = droitsMstr.stream()
                        .filter(droit -> !idsDroitDb.contains(droit.getId())).collect(toList());
                // Calcul des droits mis à jour dans Microstrat
                final List<DroitMicrostratBE> updatedDroits = droitsDb.stream()
                        .filter(droit -> idsDroitMstr.contains(droit.getId())).collect(toList());
                // Calcul des droits supprimés dans Microstrat
                final List<DroitMicrostratBE> removedDroits = droitsDb.stream()
                        .filter(droit -> !idsDroitMstr.contains(droit.getId()))
                        .peek(droit -> droit.setSupprime(Boolean.TRUE)).collect(toList());

                // Mise à jour des nom des activités
                for (DroitMicrostratBE droitDb : updatedDroits) {
                    for (DroitMicrostratBE droitMstr : droitsMstr) {
                        if (droitDb.getId().equals(droitMstr.getId())) {
                            droitDb.getActivite().setNom(droitMstr.getActivite().getNom());
                            droitDb.setNomMicrostrat(droitMstr.getNomMicrostrat());
                            droitDb.setSupprime(Boolean.FALSE);
                        }
                    }
                }

                // Insersion de l'activité, rattachement de l'activité au droit puis enregistrement en base des droits
                for (DroitMicrostratBE droitDb : addedDroits) {
                    final ActiviteBE activite = activitesDAO.save(droitDb.getActivite());
                    droitDb.setActivite(activite);
                    droitDb.setSupprime(Boolean.FALSE);
                }

                // Concaténation des listes
                final List<DroitMicrostratBE> saveList = new ArrayList<>(removedDroits);
                saveList.addAll(updatedDroits);
                saveList.addAll(addedDroits);
                validator.validateAndThrow(saveList);
                droitsMicrostratDAO.saveAll(saveList);
            } catch (BusinessException be) {
                log.error(be.getMessage(), be);
                anomalieSynchronisationLBS
                        .ajouterAnomalie(context.getIdDemande(), be.getErreurs(), TypeSynchroEnum.DROITS);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                anomalieSynchronisationLBS.ajouterAnomalie(context.getIdDemande(),
                        StringUtils.defaultIfBlank(e.getMessage(), e.toString()), TypeSynchroEnum.DROITS);
            }
        }
    }
}
