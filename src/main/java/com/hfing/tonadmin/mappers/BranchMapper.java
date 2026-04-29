package com.hfing.tonadmin.mappers;

import com.hfing.tonadmin.dto.request.BranchRequest;
import com.hfing.tonadmin.entities.Branch;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface BranchMapper {

    Branch toBranch(BranchRequest request);

    BranchRequest toBranchRequest(Branch branch);

    void updateBranchFromRequest(BranchRequest request, @MappingTarget Branch branch);
}