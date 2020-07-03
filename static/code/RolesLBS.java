package fr.su.loginsmicrostrat.services.business.impl;

import fr.su.loginsmicrostrat.objects.business.be.RoleBE;
import fr.su.loginsmicrostrat.services.business.itf.RolesILBS;
import fr.su.loginsmicrostrat.services.data.itf.RolesIDAO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RolesLBS implements RolesILBS {

    private final RolesIDAO roleDAO;

    public RolesLBS(RolesIDAO roleDAO) {
        this.roleDAO = roleDAO;
    }

    @Override
    public List<RoleBE> findRoles(){
        return roleDAO.findAll();
    }
}
