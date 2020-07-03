package fr.su.loginsmicrostrat.services.business.impl;

import fr.su.loginsmicrostrat.objects.business.be.DroitMicrostratBE;
import fr.su.loginsmicrostrat.objects.business.be.DroitUtilisateurBE;
import fr.su.loginsmicrostrat.objects.business.be.EtatEnum;
import fr.su.loginsmicrostrat.objects.business.be.GestionnaireBE;
import fr.su.loginsmicrostrat.objects.business.be.SynchronisationContextDO;
import fr.su.loginsmicrostrat.objects.business.be.TypeActionEnum;
import fr.su.loginsmicrostrat.objects.business.be.TypeSynchroEnum;
import fr.su.loginsmicrostrat.objects.business.be.UtilisateurBE;
import fr.su.loginsmicrostrat.services.business.itf.ActionsILBS;
import fr.su.loginsmicrostrat.services.business.itf.DroitsMicrostratILBS;
import fr.su.loginsmicrostrat.services.business.itf.DroitsUtilisateurILBS;
import fr.su.loginsmicrostrat.services.business.itf.GestionnairesILBS;
import fr.su.loginsmicrostrat.services.business.itf.UtilisateursILBS;
import fr.su.loginsmicrostrat.services.data.itf.DroitsUtilisateurIDAO;
import fr.su.loginsmicrostrat.services.external.itf.MicrostratIXBS;
import fr.su.suapi.authentication.AuthenticationFacade;
import fr.su.suapi.authentication.refutilisateurappli.xbe.UtilisateurConnecteXBE;
import fr.su.suapi.exception.BusinessException;
import fr.su.suapi.exception.EntityNotFoundBusinessException;
import fr.su.suapi.exception.util.ErrorDOBuilderFactory;
import fr.su.suapi.objects.error.ErrorDO;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@Service
public class DroitsUtilisateurLBS implements DroitsUtilisateurILBS {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final DroitsUtilisateurIDAO droitsUtilisateurDAO;
    private final ActionsILBS actionsLBS;
    private final GestionnairesILBS gestionnairesLBS;
    private final DroitsMicrostratILBS droitsMicrostratLBS;
    private final MicrostratIXBS microstratXBS;
    private final UtilisateursILBS utilisateursLBS;
    private final ErrorDOBuilderFactory builderFactory;
    private final AuthenticationFacade authenticationFacade;
    private final AnomalieSynchronisationLBS anomalieSynchronisationLBS;

    @Value("${application.compte-ldap.suid:}")
    private String suidCompteLdapApplication;

    public DroitsUtilisateurLBS(DroitsUtilisateurIDAO droitsUtilisateurDAO, ActionsILBS actionsLBS,
            GestionnairesILBS gestionnairesLBS, DroitsMicrostratILBS droitsMicrostratLBS, MicrostratIXBS microstratXBS,
            UtilisateursILBS utilisateursLBS, ErrorDOBuilderFactory builderFactory, MeterRegistry meterRegistry,
            AuthenticationFacade authenticationFacade, AnomalieSynchronisationLBS anomalieSynchronisationLBS) {
        this.droitsUtilisateurDAO = droitsUtilisateurDAO;
        this.actionsLBS = actionsLBS;
        this.gestionnairesLBS = gestionnairesLBS;
        this.microstratXBS = microstratXBS;
        this.utilisateursLBS = utilisateursLBS;
        this.droitsMicrostratLBS = droitsMicrostratLBS;
        this.builderFactory = builderFactory;
        this.authenticationFacade = authenticationFacade;
        this.anomalieSynchronisationLBS = anomalieSynchronisationLBS;
    }

    @Override
    public List<DroitUtilisateurBE> findAllDroitsUtilisateur(String idUtilisateurMicrostrat)
            throws EntityNotFoundBusinessException {
        List<DroitUtilisateurBE> listeDroits;
        final DroitUtilisateurBE example = new DroitUtilisateurBE();
        example.setUtilisateur(new UtilisateurBE());
        example.getUtilisateur().setId(idUtilisateurMicrostrat);
        final ExampleMatcher matcher = ExampleMatcher.matching();
        listeDroits = droitsUtilisateurDAO.findAll(Example.of(example, matcher));
        if (SecurityContextHolder.getContext().getAuthentication() != null
                && !suidCompteLdapApplication.equals(authenticationFacade.getUtilisateurConnecte().getSuId())) {
            //on ne fait pas ce filtre si on lance une synchro depuis automator
            GestionnaireBE gestionnaire = gestionnairesLBS
                    .readGestionnaire(authenticationFacade.getUtilisateurConnecte().getUid());
            listeDroits = listeDroits.stream()
                    .filter(it -> gestionnaire.getCentrales().contains(it.getDroitMicrostrat().getCentrale())
                            && gestionnaire.getActivites().contains(it.getDroitMicrostrat().getActivite()))
                    .collect(Collectors.toList());
        }
        return listeDroits;
    }

    @Override
    public List<DroitUtilisateurBE> findDroitsUtilisateurNonSupprimes(String idUtilisateurMicrostrat)
            throws EntityNotFoundBusinessException {
        return this.findAllDroitsUtilisateur(idUtilisateurMicrostrat).stream()
                .filter(droit -> !droit.getEtat().equals(EtatEnum.SUPPRIME)).collect(toList());
    }

    @Override
    public List<DroitUtilisateurBE> findDroitsUtilisateurOfGestionnaire(UtilisateurConnecteXBE utilisateurConnecte,
            List<EtatEnum> etats) throws EntityNotFoundBusinessException {
        final String idGestionnaire = utilisateurConnecte.getUid();
        // Vérification que le gestionnaire existe
        gestionnairesLBS.readGestionnaire(idGestionnaire);
        final List<DroitUtilisateurBE> droits = droitsUtilisateurDAO.findByGestionnaireId(idGestionnaire);
        if (etats == null) {
            return droits;
        }

        return droits.stream().filter(droit -> etats.contains(droit.getEtat())).collect(toList());
    }

    @Override
    public List<DroitUtilisateurBE> enregistrerDemandeModificationGroupeDroitsUtilisateur(
            UtilisateurConnecteXBE utilisateurConnecte, String idUtilisateur,
            Map<EtatEnum, List<String>> idsMicrostratParEtat) throws BusinessException {
        List<ErrorDO> erreurs = new ArrayList<>(Collections.emptyList());

        List<DroitUtilisateurBE> resultats = new ArrayList<>();
        final List<DroitUtilisateurBE> droitsUtiExistants;
        // Vérification que l'utilisateur existe
        utilisateursLBS.readUtilisateur(idUtilisateur);
        // Récupération des DroitUtilisateurBE existants de l'utilisateur
        droitsUtiExistants = this.findAllDroitsUtilisateur(idUtilisateur);

        // maj, creation ou suppression des droits utilisateurs par etat
        if (MapUtils.isNotEmpty(idsMicrostratParEtat)) {
            idsMicrostratParEtat.forEach(
                    (etat, identifiantsMicrostrat) -> resultats
                            .addAll(enregistrerDemandeModificationGroupeDroitsUtilisateurParEtat(
                                    erreurs, utilisateurConnecte, idUtilisateur, droitsUtiExistants, etat,
                                    // Récupèration des droits Microstrat associés aux identifiants
                                    droitsMicrostratLBS.findByIdMicrostratIn(identifiantsMicrostrat))));
        }
        if (CollectionUtils.isNotEmpty(erreurs)) {
            throw new BusinessException(erreurs);
        }
        return resultats;
    }

    protected List<DroitUtilisateurBE> enregistrerDemandeModificationGroupeDroitsUtilisateurParEtat(
            List<ErrorDO> erreurs,
            UtilisateurConnecteXBE utilisateurConnecte, String idUtilisateur,
            List<DroitUtilisateurBE> droitsUtiExistants, EtatEnum nouvelEtat,
            List<DroitMicrostratBE> droitsMicrostrat) {
        List<DroitUtilisateurBE> resultats = new ArrayList<>();
        if (CollectionUtils.isEmpty(droitsMicrostrat)) {
            return resultats;
        }
        List<DroitUtilisateurBE> listeAnnulationSuppression = new ArrayList<>(Collections.emptyList());
        List<DroitUtilisateurBE> listeAnnulationAjout = new ArrayList<>(Collections.emptyList());
        List<DroitUtilisateurBE> listeAAjouter = new ArrayList<>(Collections.emptyList());
        List<DroitUtilisateurBE> listeASupprimer = new ArrayList<>(Collections.emptyList());
        try {
            droitsMicrostrat.forEach(droitMicrostrat -> {
                Optional<DroitUtilisateurBE> droitUtilisateur = Optional.ofNullable(droitsUtiExistants)
                        .orElse(Collections.emptyList()).stream()
                        .filter(it -> it.getDroitMicrostrat().equals(droitMicrostrat)).findFirst();
                if (droitUtilisateur.isPresent()) {
                    //le droit est enregistré, on doit mettre a jour son statut
                    if (EtatEnum.VALIDE.equals(nouvelEtat) //
                            && EtatEnum.EN_ATTENTE_SUPPRESSION.equals(droitUtilisateur.get().getEtat())) {
                        //si le nouvel etat est valide, on ne fait rien sauf si son ancien état
                        // était en attente de suppression, on doit le remettre a valide
                        listeAnnulationSuppression.add(droitUtilisateur.get());
                    } else if (EtatEnum.SUPPRIME.equals(nouvelEtat) //
                            && EtatEnum.EN_ATTENTE_AJOUT.equals(droitUtilisateur.get().getEtat())) {
                        //si le nouvel etat est supprimé, on ne fait rien sauf si son ancien état
                        // était en attente d'ajout, on doit le remettre a supprimé
                        listeAnnulationAjout.add(droitUtilisateur.get());
                    } else if (EtatEnum.EN_ATTENTE_AJOUT.equals(nouvelEtat) //
                            && !EtatEnum.EN_ATTENTE_AJOUT.equals(droitUtilisateur.get().getEtat())) {
                        //si le nouvel etat est en attente d'ajout et qu'il n'était pas déjà a ce statut
                        listeAAjouter.add(droitUtilisateur.get());
                    } else if (EtatEnum.EN_ATTENTE_SUPPRESSION.equals(nouvelEtat)//
                            && !EtatEnum.EN_ATTENTE_SUPPRESSION.equals(droitUtilisateur.get().getEtat())) {
                        //si le nouvel etat est en attente de suppression et qu'il n'était pas déjà a ce statut
                        listeASupprimer.add(droitUtilisateur.get());
                    }
                } else {
                    //nouvelle demande de droit pour l'utilisateur, il ne peut s'agir que d'une demande d'ajout
                    //sinon on peut ignorer la demande qui n'a pas lieu d'être
                    if (EtatEnum.EN_ATTENTE_AJOUT.equals(nouvelEtat)) {
                        listeAAjouter.add(initialiserDroitUtilisateurBE(idUtilisateur, droitMicrostrat));
                    }
                }
            });
            resultats.addAll(updateStatutsDemandeDroitsUtilisateur(utilisateurConnecte, listeAnnulationSuppression,
                    listeAnnulationAjout,
                    listeAAjouter, listeASupprimer));

        } catch (EntityNotFoundBusinessException e) {
            erreurs.addAll(e.getErreurs());
            log.error(e.getMessage(), e);
        }
        return resultats;
    }

    protected List<DroitUtilisateurBE> updateStatutsDemandeDroitsUtilisateur(UtilisateurConnecteXBE utilisateurConnecte,
            List<DroitUtilisateurBE> listeAnnulationSuppression, List<DroitUtilisateurBE> listeAnnulationAjout,
            List<DroitUtilisateurBE> listeAAjouter, List<DroitUtilisateurBE> listeASupprimer)
            throws EntityNotFoundBusinessException {
        List<DroitUtilisateurBE> resultats = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(listeAnnulationSuppression)) {
            //modification non repercuté dans microstrat,
            // on peut donc annuler la demande et remettre l'état précédent
            resultats.addAll(saveDroitsUtilisateur(utilisateurConnecte, listeAnnulationSuppression, EtatEnum.VALIDE,
                    TypeActionEnum.ANNULATION_SUPPRESSION));
        }
        if (CollectionUtils.isNotEmpty(listeAnnulationAjout)) {
            //modification non repercuté dans microstrat,
            // on peut donc annuler la demande et remettre l'état précédent
            resultats.addAll(saveDroitsUtilisateur(utilisateurConnecte, listeAnnulationAjout, EtatEnum.SUPPRIME,
                    TypeActionEnum.ANNULATION_AJOUT));
        }
        if (CollectionUtils.isNotEmpty(listeAAjouter)) {
            resultats.addAll(saveDroitsUtilisateur(utilisateurConnecte, listeAAjouter, EtatEnum.EN_ATTENTE_AJOUT,
                    TypeActionEnum.DEMANDE_AJOUT));
        }
        if (CollectionUtils.isNotEmpty(listeASupprimer)) {
            resultats
                    .addAll(saveDroitsUtilisateur(utilisateurConnecte, listeASupprimer, EtatEnum.EN_ATTENTE_SUPPRESSION,
                            TypeActionEnum.DEMANDE_SUPPRESION));
        }
        return resultats;
    }

    private DroitUtilisateurBE initialiserDroitUtilisateurBE(String idUtilisateur, DroitMicrostratBE droitMicrostrat) {
        UtilisateurBE utilisateurBE = new UtilisateurBE();
        utilisateurBE.setId(idUtilisateur);
        DroitUtilisateurBE droit = new DroitUtilisateurBE(droitMicrostrat);
        droit.setUtilisateur(utilisateurBE);
        return droit;
    }

    @Override
    public List<DroitUtilisateurBE> deleteGroupeDroitsUtilisateur(UtilisateurConnecteXBE utilisateurConnecte,
            String idUtilisateur, List<String> idsMicrostrat) throws BusinessException {
        List<DroitUtilisateurBE> listeDroitsSupprimes = new ArrayList<>();

        // Vérification que l'utilisateur existe
        UtilisateurBE utilisateurBE = utilisateursLBS.readUtilisateur(idUtilisateur);
        // Récupèration des droits Microstrat associés aux identifiants
        final List<DroitMicrostratBE> idDroitsMstr = droitsMicrostratLBS.findByIdMicrostratIn(idsMicrostrat);

        // Récupération des DroitUtilisateurBE existants de l'utilisateur
        final List<DroitUtilisateurBE> droitsUtiExistants = this.findAllDroitsUtilisateur(idUtilisateur);

        //verifie que les droits a supprimer existent
        verifierExistanceDroitsASupprimer(utilisateurBE, idDroitsMstr, droitsUtiExistants);

        //annule la demande de création des droits
        listeDroitsSupprimes.addAll(annulerAjoutDroitsUtilisateur(utilisateurConnecte, droitsUtiExistants));

        // demande la suppression des droits existant
        listeDroitsSupprimes.addAll(demanderSuppressionDroitsPourUtilisateur(utilisateurConnecte, idsMicrostrat,
                droitsUtiExistants));

        return listeDroitsSupprimes;
    }

    /**
     * demande la suppression de droits existant
     * repasse le statut de VALIDE a EN_ATTENTE_SUPPRESSION
     *
     * @param idsMicrostrat      liste des id des droits microstrat a supprimer
     * @param droitsUtiExistants droits existants
     * @return la liste des droits supprimes
     * @throws EntityNotFoundBusinessException entité non trouvée
     */
    private List<DroitUtilisateurBE> demanderSuppressionDroitsPourUtilisateur(
            UtilisateurConnecteXBE utilisateurConnecte, List<String> idsMicrostrat,
            List<DroitUtilisateurBE> droitsUtiExistants) throws EntityNotFoundBusinessException {
        // Construction de la liste des droits à supprimer
        final List<DroitUtilisateurBE> aSupprimer = droitsUtiExistants.stream()
                .filter(droit -> droit.getEtat().equals(EtatEnum.VALIDE))
                .filter(droit -> idsMicrostrat.contains(droit.getDroitMicrostrat().getId())).collect(toList());
        return this.saveDroitsUtilisateur(utilisateurConnecte, aSupprimer, EtatEnum.EN_ATTENTE_SUPPRESSION,
                TypeActionEnum.DEMANDE_SUPPRESION);
    }

    /**
     * annule la demande de création des droits
     * repasse le statut de EN_ATTENTE_AJOUT a SUPPRIME
     *
     * @param droitsUtiExistants droits utilisateurs existants
     * @return la liste des droits supprimes
     * @throws EntityNotFoundBusinessException entité non trouvée
     */
    private List<DroitUtilisateurBE> annulerAjoutDroitsUtilisateur(UtilisateurConnecteXBE utilisateurConnecte,
            List<DroitUtilisateurBE> droitsUtiExistants) throws EntityNotFoundBusinessException {
        //recupération des droit EN_ATTENTE_AJOUT dont on veut annuler l'ajout
        final List<DroitUtilisateurBE> droitsEnAttenteAjout = droitsUtiExistants.stream()
                .filter(droit -> droit.getEtat().equals(EtatEnum.EN_ATTENTE_AJOUT)).collect(toList());
        return this.saveDroitsUtilisateur(utilisateurConnecte, droitsEnAttenteAjout, EtatEnum.SUPPRIME,
                TypeActionEnum.ANNULATION_AJOUT);
    }

    /**
     * verifier que les droits utilisateurs a supprimer existent
     *
     * @param utilisateurBE      utilisateur traité
     * @param idDroitsMstr       liste des id des droits microstrat a supprimer
     * @param droitsUtiExistants droit existant a supprimer
     * @throws BusinessException exception metier
     */
    private void verifierExistanceDroitsASupprimer(UtilisateurBE utilisateurBE, List<DroitMicrostratBE> idDroitsMstr,
            List<DroitUtilisateurBE> droitsUtiExistants) throws BusinessException {
        final List<String> idDroitsUtiExistants = droitsUtiExistants.stream()
                .map(DroitUtilisateurBE::getDroitMicrostrat).map(DroitMicrostratBE::getId).collect(toList());

        // Créé un ErrorDO par id Microstrat non déjà affecté à l'utilisateur
        final List<ErrorDO> errors = idDroitsMstr.stream()
                .filter(droit -> !idDroitsUtiExistants.contains(droit.getId())).map(droit -> builderFactory.getBuilder()
                        .appendCodeAndLabel("droitmicrostrat.affectation.inexistante", droit.getNomMicrostrat(),
                                utilisateurBE.getNomComplet()).appendCurrentAndLimitValue(
                                new Object[] { droit.getNomMicrostrat(), utilisateurBE.getNomComplet() }, null)
                        .getOne()).collect(toList());
        if (!errors.isEmpty()) {
            throw new BusinessException(errors);
        }
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void synchroniserAffectationsDroitsutilisateur(SynchronisationContextDO context,
            String idUtilisateurMicrostrat) {
        try {
            gestionnairesLBS.checkGestionnaireAdmin(context.getUtilisateurConnecte());
            // Récupère la liste des droits utilisateur en base
            final List<DroitUtilisateurBE> droitUtilisateurDb = this
                    .findAllDroitsUtilisateur(idUtilisateurMicrostrat);
            final List<String> idsDroitsUtilisateurDb = droitUtilisateurDb.stream()
                    .map(DroitUtilisateurBE::getDroitMicrostrat).map(DroitMicrostratBE::getId).collect(toList());

            final UtilisateurBE utilisateur = utilisateursLBS.readUtilisateur(idUtilisateurMicrostrat);
            // Récupère la liste des droits utilisateur dans Microstrat
            final List<DroitUtilisateurBE> droitsMicrostrat = microstratXBS.findDroitsUtilisateur(utilisateur);
            final List<String> idsDroitsMicrostrat = droitsMicrostrat.stream()
                    .map(DroitUtilisateurBE::getDroitMicrostrat).map(DroitMicrostratBE::getId).collect(toList());

            // Droits non assignés à l'utilisateur dans Microstrat et à l'état validé en base
            // (pas en attente d'ajout ou de suppression)
            List<DroitUtilisateurBE> aDesaffecter = droitUtilisateurDb.stream()
                    .filter(droit -> !idsDroitsMicrostrat.contains(droit.getDroitMicrostrat().getId()))
                    .filter(droit -> droit.getEtat().equals(EtatEnum.VALIDE)).collect(toList());

            // Droits non assignés à l'utilisateur dans la base
            List<DroitUtilisateurBE> supprimeaAffecter = droitUtilisateurDb.stream()
                    .filter(droit -> droit.getEtat().equals(EtatEnum.SUPPRIME))
                    .filter(droit -> idsDroitsMicrostrat.contains(droit.getDroitMicrostrat().getId()))
                    .collect(toList());
            List<DroitUtilisateurBE> aAffecter = droitsMicrostrat.stream()
                    .filter(droit -> !idsDroitsUtilisateurDb.contains(droit.getDroitMicrostrat().getId()))
                    .collect(toList());
            aAffecter.addAll(supprimeaAffecter);

            // Droits attachés à l'utilisateur mais avec un statut en attente d'ajout)
            List<DroitUtilisateurBE> validerAjout = droitUtilisateurDb.stream()
                    .filter(droit -> idsDroitsMicrostrat.contains(droit.getDroitMicrostrat().getId()))
                    .filter(droit -> droit.getEtat().equals(EtatEnum.EN_ATTENTE_AJOUT)).collect(toList());

            // Droits non attachés à l'utilisateur mais avec un statut en attente de suppression
            List<DroitUtilisateurBE> validerSuppression = droitUtilisateurDb.stream()
                    .filter(droit -> !idsDroitsMicrostrat.contains(droit.getDroitMicrostrat().getId()))
                    .filter(droit -> droit.getEtat().equals(EtatEnum.EN_ATTENTE_SUPPRESSION)).collect(toList());

            this.saveDroitsUtilisateur(context.getUtilisateurConnecte(), aDesaffecter, EtatEnum.SUPPRIME,
                    TypeActionEnum.SUPPRESSION_SYNC_MICROSTRAT);
            this.saveDroitsUtilisateur(context.getUtilisateurConnecte(), aAffecter, EtatEnum.VALIDE,
                    TypeActionEnum.AJOUT_SYNC_MICROSTRAT);
            this.saveDroitsUtilisateur(context.getUtilisateurConnecte(), validerAjout, EtatEnum.VALIDE,
                    TypeActionEnum.AJOUT_SYNC_MICROSTRAT);
            this.saveDroitsUtilisateur(context.getUtilisateurConnecte(), validerSuppression, EtatEnum.SUPPRIME,
                    TypeActionEnum.SUPPRESSION_SYNC_MICROSTRAT);

        } catch (BusinessException be) {
            log.error(be.getMessage(), be);
            anomalieSynchronisationLBS.ajouterAnomalie(context.getIdDemande(), be.getErreurs(),
                    TypeSynchroEnum.AFFECTATION_DROITS_UTILISATEUR);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            anomalieSynchronisationLBS.ajouterAnomalie(context.getIdDemande(),
                    StringUtils.defaultIfBlank(e.getMessage(), e.toString()),
                    TypeSynchroEnum.AFFECTATION_DROITS_UTILISATEUR);
        }
    }

    @Override
    public List<DroitUtilisateurBE> saveDroitsUtilisateur(UtilisateurConnecteXBE utilisateurConnecte,
            List<DroitUtilisateurBE> droitsUtilisateur, EtatEnum newEtat, TypeActionEnum typeAction)
            throws EntityNotFoundBusinessException {
        if (CollectionUtils.isEmpty(droitsUtilisateur)) {
            return new ArrayList<>();
        }

        List<DroitUtilisateurBE> list = droitsUtilisateur.stream().peek(droit -> droit.setEtat(newEtat))
                .collect(toList());
        list = actionsLBS.addOneActionPerDroitUtilisateur(utilisateurConnecte, list, typeAction);
        return droitsUtilisateurDAO.saveAll(list);
    }

    @Override
    public List<DroitUtilisateurBE> validerDroitsUtilisateur(UtilisateurConnecteXBE utilisateurConnecte,
            List<UUID> idsDroitsUtilisateur) throws BusinessException {
        // Récupération des droits utilisateur grace à leurs identifiants
        final List<DroitUtilisateurBE> droitsUtilisateur = droitsUtilisateurDAO.findAllById(idsDroitsUtilisateur);
        // Vérification des droits du gestionnaire
        gestionnairesLBS.checkGestionnaireApprobationAuthorizationOn(utilisateurConnecte, droitsUtilisateur);

        // Récupération des droits en attente de validation ou d'ajout
        final List<DroitUtilisateurBE> attenteAjout = droitsUtilisateur.stream()
                .filter(droit -> droit.getEtat().equals(EtatEnum.EN_ATTENTE_AJOUT)).collect(toList());
        final List<DroitUtilisateurBE> attenteSuppr = droitsUtilisateur.stream()
                .filter(droit -> droit.getEtat().equals(EtatEnum.EN_ATTENTE_SUPPRESSION)).collect(toList());

        // Groupement des validations à réaliser par utilisateur
        final Map<String, List<DroitUtilisateurBE>> ajoutsMstrPerUser = attenteAjout.stream()
                .collect(groupingBy(drt -> drt.getUtilisateur().getId()));
        final Map<String, List<DroitUtilisateurBE>> supprMstrPerUser = attenteSuppr.stream()
                .collect(groupingBy(drt -> drt.getUtilisateur().getId()));

        // Appel à Microstrat pour l'ajout des droits à chaque utilisateur
        for (Map.Entry<String, List<DroitUtilisateurBE>> entryAjoutsMstr : ajoutsMstrPerUser.entrySet()) {
            String idUtilisateur = entryAjoutsMstr.getKey();
            List<DroitUtilisateurBE> listeDroits = entryAjoutsMstr.getValue();
            List<String> idsDroitsMstr = listeDroits.stream().map(drt -> drt.getDroitMicrostrat().getId())
                    .collect(toList());
            microstratXBS.addDroitsUtilisateur(idUtilisateur, idsDroitsMstr);
        }
        // Appel à Microstrat pour la suppression des droits à chaque utilisateur
        for (Map.Entry<String, List<DroitUtilisateurBE>> entrySupprMstr : supprMstrPerUser.entrySet()) {
            String idUtilisateur = entrySupprMstr.getKey();
            List<DroitUtilisateurBE> listeDroits = entrySupprMstr.getValue();
            List<String> idsDroitsMstr = listeDroits.stream().map(drt -> drt.getDroitMicrostrat().getId())
                    .collect(toList());
            microstratXBS.removeDroitsUtilisateur(idUtilisateur, idsDroitsMstr);
        }

        // Mise à jour des états, création des actions associées puis enregistrement en base
        final List<DroitUtilisateurBE> validated = new ArrayList<>(this
                .saveDroitsUtilisateur(utilisateurConnecte, attenteAjout, EtatEnum.VALIDE,
                        TypeActionEnum.VALIDATION_AJOUT));
        validated
                .addAll(new ArrayList<>(this.saveDroitsUtilisateur(utilisateurConnecte, attenteSuppr, EtatEnum.SUPPRIME,
                        TypeActionEnum.VALIDATION_SUPPRESION)));
        return validated;
    }

    @Override
    public List<DroitUtilisateurBE> rejeterDroitsUtilisateur(UtilisateurConnecteXBE utilisateurConnecte,
            List<UUID> idsDroitsUtilisateur) throws BusinessException {
        // Récupération des droits utilisateur grace à leurs identifiants
        final List<DroitUtilisateurBE> droitsUtilisateur = droitsUtilisateurDAO.findAllById(idsDroitsUtilisateur);
        // Vérification des droits du gestionnaire
        gestionnairesLBS.checkGestionnaireApprobationAuthorizationOn(utilisateurConnecte, droitsUtilisateur);

        // Récupération des droits en attente de validation ou d'ajout
        final List<DroitUtilisateurBE> attenteAjout = droitsUtilisateur.stream()
                .filter(droit -> droit.getEtat().equals(EtatEnum.EN_ATTENTE_AJOUT)).collect(toList());
        final List<DroitUtilisateurBE> attenteSuppr = droitsUtilisateur.stream()
                .filter(droit -> droit.getEtat().equals(EtatEnum.EN_ATTENTE_SUPPRESSION)).collect(toList());

        // Mise à jour des états, création des actions associées puis enregistrement en base
        final List<DroitUtilisateurBE> refused = this
                .saveDroitsUtilisateur(utilisateurConnecte, attenteAjout, EtatEnum.SUPPRIME,
                        TypeActionEnum.REJET_AJOUT);
        refused.addAll(this.saveDroitsUtilisateur(utilisateurConnecte, attenteSuppr, EtatEnum.VALIDE,
                TypeActionEnum.REJET_SUPPRESSION));
        return refused;
    }

    public void setSuidCompteLdapApplication(String suidCompteLdapApplication) {
        this.suidCompteLdapApplication = suidCompteLdapApplication;
    }
}
