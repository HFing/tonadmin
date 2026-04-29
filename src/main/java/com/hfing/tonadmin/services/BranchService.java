package com.hfing.tonadmin.services;

import com.hfing.tonadmin.dto.request.BranchRequest;
import com.hfing.tonadmin.entities.Branch;
import org.springframework.validation.BindingResult;

import java.util.List;

public interface BranchService {

    List<Branch> getAllBranches();

    Branch getBranchById(String id);

    boolean createBranch(BranchRequest request, BindingResult bindingResult);

    boolean updateBranch(String id, BranchRequest request, BindingResult bindingResult);

    void toggleActive(String id);



}
