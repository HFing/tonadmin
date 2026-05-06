package com.hfing.tonadmin.services;

import com.hfing.tonadmin.dto.request.CancelSalesOrderRequest;
import com.hfing.tonadmin.dto.request.PaymentUpdateRequest;
import com.hfing.tonadmin.dto.request.SalesOrderRequest;
import com.hfing.tonadmin.dto.request.SalesOrderSearchRequest;
import com.hfing.tonadmin.entities.SalesOrder;
import com.hfing.tonadmin.entities.SalesOrderItem;
import com.hfing.tonadmin.entities.SalesPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.BindingResult;

import java.util.List;

public interface SalesOrderService {

    Page<SalesOrder> getSalesOrders(SalesOrderSearchRequest search, Pageable pageable);

    SalesOrder getSalesOrderById(String id);

    List<SalesOrderItem> getSalesOrderItems(String salesOrderId);

    List<SalesPayment> getSalesPayments(String salesOrderId);

    SalesOrder createSalesOrder(SalesOrderRequest request, BindingResult bindingResult);

    boolean updatePayment(String salesOrderId, PaymentUpdateRequest request, BindingResult bindingResult);

    boolean cancelSalesOrder(String salesOrderId, CancelSalesOrderRequest request, BindingResult bindingResult);
}
