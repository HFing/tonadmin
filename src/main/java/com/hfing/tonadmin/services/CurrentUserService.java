package com.hfing.tonadmin.services;

import com.hfing.tonadmin.entities.Branch;
import com.hfing.tonadmin.entities.User;

public interface CurrentUserService {

    User getCurrentUser();

    boolean isAdmin();

    boolean isBranchStaff();

    Branch getCurrentUserBranch();

    boolean canAccessBranch(String branchId);
}