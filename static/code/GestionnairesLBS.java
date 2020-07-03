package fr.su.loginsmicrostrat.services.business.impl;

import fr.su.loginsmicrostrat.objects.business.be.ActiviteBE;
import fr.su.loginsmicrostrat.objects.business.be.CentraleBE;
import fr.su.loginsmicrostrat.objects.business.be.DroitUtilisateurBE;
import fr.su.loginsmicrostrat.objects.business.be.GestionnaireBE;
import fr.su.loginsmicrostrat.services.business.itf.GestionnairesILBS;
import fr.su.loginsmicrostrat.services.data.itf.GestionnairesIDAO;
import fr.su.suapi.authentication.refutilisateurappli.xbe.UtilisateurConnecteXBE;
import fr.su.suapi.exception.BusinessException;
import fr.su.suapi.exception.EntityNotFoundBusinessException;
import fr.su.suapi.exception.util.ErrorDOBuilder;
import fr.su.suapi.exception.util.ErrorDOBuilderFactory;
import fr.su.suapi.objects.error.ErrorDO;
import fr.su.suapi.validation.BEAnnotationsValidator;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static fr.su.suapi.exception.util.ErrorConstants.NOT_FOUND_ENTITY;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;

@Service
public class GestionnairesLBS implements GestionnairesILBS {

    private final GestionnairesIDAO gestionnairesDAO;
    private final ErrorDOBuilderFactory builderFactory;
    private final BEAnnotationsValidator<GestionnaireBE> validator;

    public GestionnairesLBS(GestionnairesIDAO gestionnairesDAO, ErrorDOBuilderFactory builderFactory,
            BEAnnotationsValidator<GestionnaireBE> validator) {
        this.gestionnairesDAO = gestionnairesDAO;
        this.builderFactory = builderFactory;
        this.validator = validator;
    }

    @Override
    public List<GestionnaireBE> findGestionnaires(UtilisateurConnecteXBE utilisateurConnecte) throws BusinessException {
        checkGestionnaireAdmin(utilisateurConnecte);
        return gestionnairesDAO.findAll();
    }

    @Override
    public GestionnaireBE readGestionnaire(String id) throws EntityNotFoundBusinessException {
        return gestionnairesDAO.findById(id).orElseThrow(() -> new EntityNotFoundBusinessException(
                builderFactory.getBuilder().appendCodeAndLabel(NOT_FOUND_ENTITY, id).getOne()));
    }

    @Override
    public GestionnaireBE createGestionnaire(UtilisateurConnecteXBE utilisateurConnecte, GestionnaireBE gestionnaire)
            throws BusinessException {
        checkGestionnaireAdmin(utilisateurConnecte);
        validator.validateAndThrow(gestionnaire);
        return gestionnairesDAO.save(gestionnaire);
    }

    @Override
    public GestionnaireBE updateGestionnaire(UtilisateurConnecteXBE utilisateurConnecte, String idGestionnaire,
            GestionnaireBE gestionnaire) throws BusinessException {
        checkGestionnaireAdmin(utilisateurConnecte);
        gestionnaire.setId(idGestionnaire);
        return this.createGestionnaire(utilisateurConnecte, gestionnaire);
    }

    @Override
    public Boolean checkGestionnaireAdmin(UtilisateurConnecteXBE utilisateurConnecte) throws BusinessException {
        final GestionnaireBE gestionnaire = this.readGestionnaire(utilisateurConnecte.getUid());
        if (!Boolean.TRUE.equals(gestionnaire.getTypeGestionnaire().getAdminDroit())) {
            throw new BusinessException(builderFactory.getBuilder()
                    .appendCodeAndLabel("gestionnaire.non.administrateur", gestionnaire.getNom(),
                            gestionnaire.getPrenom())
                    .appendCurrentAndLimitValue(new Object[] { gestionnaire.getNom(), gestionnaire.getPrenom() }, null)
                    .getOne());
        }
        return Boolean.TRUE;
    }

    @Override
    public Boolean checkIfCentraleAdmin(UtilisateurConnecteXBE utilisateurConnecte,
            List<CentraleBE> centralesGestionnaire) throws BusinessException {
        final GestionnaireBE admin = this.readGestionnaire(utilisateurConnecte.getUid());
        final List<UUID> adminCentraleUUID = admin.getCentrales().stream().map(CentraleBE::getId).collect(toList());
        final List<CentraleBE> nonAdminCentrales = centralesGestionnaire.stream()
                .filter(centrale -> !adminCentraleUUID.contains(centrale.getId())).collect(toList());
        if (!nonAdminCentrales.isEmpty()) {
            final ErrorDOBuilder builder = builderFactory.getBuilder();
            for (CentraleBE centrale : nonAdminCentrales) {
                builder.appendNewErrorDO()
                        .appendCodeAndLabel("gestionnaire.parametrage.droitsinsuffisantCentrale", admin.getNom(),
                                admin.getPrenom(), centrale.getNom()).appendCurrentAndLimitValue(
                        new Object[] { admin.getNom(), admin.getPrenom(), centrale.getNom() }, null)
                        .appendPathAndField(centralesGestionnaire.get(centralesGestionnaire.indexOf(centrale)));
            }
            final List<ErrorDO> errors = builder.getAll();
            if (!isEmpty(errors)) {
                errors.remove(0);
                throw new BusinessException(errors);
            }
        }
        return Boolean.TRUE;
    }

    @Override
    public Boolean checkGestionnaireApprobationAuthorizationOn(UtilisateurConnecteXBE utilisateurConnecte,
            List<DroitUtilisateurBE> droitsUtilisateur) throws BusinessException {
        // Vérification de l'existance du gestionnaire
        final GestionnaireBE gestionnaire = this.readGestionnaire(utilisateurConnecte.getUid());
        if (!Boolean.TRUE.equals(gestionnaire.getTypeGestionnaire().getValiderDroit())) {
            throw new BusinessException(builderFactory.getBuilder()
                    .appendCodeAndLabel("gestionnaire.approbation.interdiction", gestionnaire.getNom(),
                            gestionnaire.getPrenom())
                    .appendCurrentAndLimitValue(new Object[] { gestionnaire.getNom(), gestionnaire.getPrenom() }, null)
                    .getOne());
        }
        final List<UUID> idsDroitsUtilisateur = droitsUtilisateur.stream().map(DroitUtilisateurBE::getId)
                .collect(toList());

        // Vérification que le gestionnaire peut gérer les centrales et activités de tous les droits
        final List<UUID> centralesGestionnaire = gestionnaire.getCentrales().stream().map(CentraleBE::getId)
                .collect(toList());
        final List<UUID> activitesGestionnaire = gestionnaire.getActivites().stream().map(ActiviteBE::getId)
                .collect(toList());
        final Stream<DroitUtilisateurBE> nonAdminCentrales = droitsUtilisateur.stream()
                .filter(droit -> !centralesGestionnaire.contains(droit.getDroitMicrostrat().getCentrale().getId()));
        final Stream<DroitUtilisateurBE> nonAdminActivites = droitsUtilisateur.stream()
                .filter(droit -> !activitesGestionnaire.contains(droit.getDroitMicrostrat().getActivite().getId()));
        final List<DroitUtilisateurBE> nonAdmin = Stream.concat(nonAdminActivites, nonAdminCentrales).collect(toList());
        if (!nonAdmin.isEmpty()) {
            final ErrorDOBuilder builder = builderFactory.getBuilder();
            for (DroitUtilisateurBE droit : nonAdmin) {
                builder.appendNewErrorDO()
                        .appendCodeAndLabel("gestionnaire.approbation.droitsinsufisant", gestionnaire.getNom(),
                                gestionnaire.getPrenom(), droit.getDroitMicrostrat()).appendCurrentAndLimitValue(
                        new Object[] { gestionnaire.getNom(), gestionnaire.getPrenom(), droit.getDroitMicrostrat() },
                        null).appendPathAndField(idsDroitsUtilisateur.indexOf(droit.getId()));
            }

            final List<ErrorDO> errors = builder.getAll();
            if (!isEmpty(errors)) {
                errors.remove(0);
                throw new BusinessException(errors);
            }
        }

        return Boolean.TRUE;
    }
}
