package com.hfing.tonadmin.services;

import com.hfing.tonadmin.dto.request.StockTransferRequest;
import com.hfing.tonadmin.entities.StockTransfer;
import com.hfing.tonadmin.entities.StockTransferItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.BindingResult;

import java.util.List;

public interface StockTransferService {

    Page<StockTransfer> getTransfers(Pageable pageable);

    StockTransfer getTransferById(String id);

    List<StockTransferItem> getTransferItems(String transferId);

    StockTransfer createTransfer(StockTransferRequest request, BindingResult bindingResult);
}