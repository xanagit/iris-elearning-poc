package fr.su.loginsmicrostrat.services.business.impl;

import fr.su.loginsmicrostrat.objects.business.be.TypeGestionnaireBE;
import fr.su.loginsmicrostrat.services.business.itf.TypeGestionnairesILBS;
import fr.su.loginsmicrostrat.services.data.itf.TypeGestionnairesIDAO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TypeGestionnairesLBS implements TypeGestionnairesILBS {

    private final TypeGestionnairesIDAO typeGestionnairesDAO;

    public TypeGestionnairesLBS(TypeGestionnairesIDAO typeGestionnairesDAO) {
        this.typeGestionnairesDAO = typeGestionnairesDAO;
    }

    @Override
    public List<TypeGestionnaireBE> findTypeGestionnaires() {
        return typeGestionnairesDAO.findAll();
    }
}
