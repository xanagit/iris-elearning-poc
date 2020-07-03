package fr.su.loginsmicrostrat.services.business.impl;

import fr.su.loginsmicrostrat.objects.business.be.PrivilegeBE;
import fr.su.loginsmicrostrat.services.business.itf.PrivilegesILBS;
import fr.su.loginsmicrostrat.services.data.itf.PrivilegesIDAO;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PrivilegesLBS implements PrivilegesILBS {

    private final PrivilegesIDAO privilegeDAO;

    public PrivilegesLBS(PrivilegesIDAO privilegeDAO) {
        this.privilegeDAO = privilegeDAO;
    }

    @Override
    public List<PrivilegeBE> findPrivileges() {
        return privilegeDAO.findAll(Sort.by(Sort.Direction.ASC, "ordre"));
    }
}
