package fr.su.loginsmicrostrat.services.business.impl;

import fr.su.loginsmicrostrat.objects.business.be.ActiviteBE;
import fr.su.loginsmicrostrat.objects.business.be.ApplicationBE;
import fr.su.loginsmicrostrat.objects.business.be.CentraleBE;
import fr.su.loginsmicrostrat.objects.business.be.DroitMicrostratBE;
import fr.su.loginsmicrostrat.objects.business.be.PrivilegeBE;
import fr.su.loginsmicrostrat.objects.business.be.RoleBE;
import fr.su.loginsmicrostrat.services.business.itf.ApplicationsILBS;
import fr.su.loginsmicrostrat.services.business.itf.CentralesILBS;
import fr.su.loginsmicrostrat.services.business.itf.PrivilegesILBS;
import fr.su.loginsmicrostrat.services.business.itf.RolesILBS;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.contains;

@Service
public class DroitsInfosExtractor {

    private final List<RoleBE> roles;
    private final List<PrivilegeBE> privileges;
    private final List<ApplicationBE> applications;
    private final List<CentraleBE> centrales;
    private Map<String, String> customMapping;
    private Map<String, String> reversedCustomMapping;
    private final String REGLE_EXT = "regle%d.externe";
    private final String REGLE_INT = "regle%d.interne";

    public DroitsInfosExtractor(RolesILBS rolesLBS, PrivilegesILBS privilegesLBS, ApplicationsILBS applicationsLBS,
            CentralesILBS centralesLBS, Environment env) {
        centrales = centralesLBS.findCentrales();
        applications = applicationsLBS.findApplications();
        privileges = privilegesLBS.findPrivileges();
        roles = rolesLBS.findRoles();
        initCustomMapping(env);
    }

    /**
     * Initialize custom mapping
     *
     * @param env the environment
     */
    private void initCustomMapping(Environment env) {
        customMapping = new HashMap<>();
        reversedCustomMapping = new HashMap<>();
        for (int cnt = 1; cnt < Integer.MAX_VALUE; cnt++) {
            final String ext = format(REGLE_EXT, cnt);
            final String intn = format(REGLE_INT, cnt);
            if (!env.containsProperty(ext) || !env.containsProperty(intn))
                break;

            customMapping.put(env.getProperty(ext), env.getProperty(intn));
            reversedCustomMapping.put(env.getProperty(intn), env.getProperty(ext));
        }
    }

    /**
     * Filtre les droits récupérés de Microstrat, les filtres pour ne récupérer que ceux gérés par l'api et
     * déduit l'application la centrale, le privilège et l'activité en fonction du label de groupe Microstrat
     *
     * @param droitsMicrostrat la liste des groupes Microstrat prérenseignés avec l'identifiant et le groupe Microstrat
     * @return La liste des droits filtrés et complétés
     */
    public List<DroitMicrostratBE> filterAndInferFields(List<DroitMicrostratBE> droitsMicrostrat) {
        return droitsMicrostrat.stream().peek(drt -> drt
                .setNomMicrostrat(customMapping.getOrDefault(drt.getNomMicrostrat(), drt.getNomMicrostrat())))
                .map(inferFieldsFromMicrostratGroup()).filter(Objects::nonNull)
                .peek(droit -> droit.setSupprime(Boolean.FALSE)).peek(droit -> droit.setRole(roles.get(0)))
                .peek(drt -> drt.setNomMicrostrat(
                        reversedCustomMapping.getOrDefault(drt.getNomMicrostrat(), drt.getNomMicrostrat())))
                .collect(toList());
    }

    private Function<DroitMicrostratBE, DroitMicrostratBE> inferFieldsFromMicrostratGroup() {
        final List<ActiviteBE> activites = new ArrayList<>();
        return droit -> {
            String nomMicrostrat = droit.getNomMicrostrat();

            final ApplicationBE app = getApplication(nomMicrostrat);
            if (app == null) {
                return null;
            }

            final PrivilegeBE privilege = getPrivilege(nomMicrostrat);
            if (privilege == null) {
                return null;
            }

            final CentraleBE centrale = getCentrale(nomMicrostrat);
            if (centrale == null) {
                return null;
            }

            nomMicrostrat = nomMicrostrat.replace(app.getNomMicrostrat(), EMPTY);
            nomMicrostrat = nomMicrostrat.replace(privilege.getNom(), EMPTY);
            nomMicrostrat = nomMicrostrat.replace(centrale.getNom(), EMPTY);

            nomMicrostrat = nomMicrostrat.replaceFirst("–", EMPTY);
            nomMicrostrat = nomMicrostrat.replaceFirst("\\p{Blank}+", EMPTY);
            nomMicrostrat = nomMicrostrat.matches("^\\p{Punct}.*$") ?
                    nomMicrostrat.replaceFirst("\\p{Punct}", EMPTY) :
                    nomMicrostrat;

            if (EMPTY.equals(nomMicrostrat)) {
                return null;
            }

            final String nomActivite = nomMicrostrat.trim();
            Optional<ActiviteBE> activite = activites.stream().filter(act -> act.getNom().equals(nomActivite))
                    .filter(act -> act.getApplication().getNomMicrostrat().equals(app.getNomMicrostrat())).findAny();
            if (!activite.isPresent()) {
                activite = Optional.of(new ActiviteBE(nomActivite, app));
                activites.add(activite.get());
            }

            droit.setActivite(activite.get());
            droit.setPrivilege(privilege);
            droit.setCentrale(centrale);

            return droit;
        };
    }

    private CentraleBE getCentrale(String nomMicrostrat) {
        return centrales.stream().filter(centrale -> contains(nomMicrostrat, centrale.getNom())).findAny().orElse(null);
    }

    private PrivilegeBE getPrivilege(String nomMicrostrat) {
        return privileges.stream().filter(privilege -> contains(nomMicrostrat, privilege.getNom())).findAny()
                .orElse(null);
    }

    private ApplicationBE getApplication(String nomMicrostrat) {
        return applications.stream().filter(app -> contains(nomMicrostrat, app.getNomMicrostrat())).findAny()
                .orElse(null);
    }

}
