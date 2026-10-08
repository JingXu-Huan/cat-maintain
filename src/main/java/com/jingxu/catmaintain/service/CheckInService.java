package com.jingxu.catmaintain.service;

import com.jingxu.catmaintain.domain.account.Account;
import com.jingxu.catmaintain.domain.account.AccountRole;
import com.jingxu.catmaintain.domain.appointment.Appointment;
import com.jingxu.catmaintain.domain.appointment.AppointmentStatus;
import com.jingxu.catmaintain.domain.store.MerchantStore;
import com.jingxu.catmaintain.dto.appointment.AppointmentResponse;
import com.jingxu.catmaintain.dto.appointment.CheckInRequest;
import com.jingxu.catmaintain.dto.appointment.CheckInResponse;
import com.jingxu.catmaintain.dto.order.OrderResponse;
import com.jingxu.catmaintain.dto.store.StoreResponse;
import com.jingxu.catmaintain.exception.BusinessException;
import com.jingxu.catmaintain.mapper.AppointmentMapper;
import com.jingxu.catmaintain.mapper.MerchantStoreMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckInService {

    private final AppointmentMapper appointmentMapper;
    private final MerchantStoreMapper merchantStoreMapper;
    private final OrderService orderService;
    private final SessionAccountService sessionAccountService;

    public StoreResponse storeProfile(HttpSession session) {
        Account account = sessionAccountService.requireRole(session, AccountRole.STORE);
        MerchantStore store = merchantStoreMapper.findByAccountId(account.getId());
        if (store == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "STORE_NOT_FOUND", "加盟店资料不存在");
        }
        return StoreResponse.from(store);
    }

    public List<AppointmentResponse> lookupAppointments(String code, HttpSession session) {
        OrderResponse order = orderService.lookupByCode(code, session);
        return appointmentMapper.findByOrderId(order.id()).stream().map(AppointmentResponse::from).toList();
    }

    @Transactional
    public CheckInResponse checkIn(Long storeId, CheckInRequest request, HttpSession session) {
        Account account = sessionAccountService.requireRole(session, AccountRole.USER);
        if (merchantStoreMapper.findActiveById(storeId) == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "STORE_NOT_FOUND", "门店不存在或未启用");
        }
        Appointment appointment = appointmentMapper.findByIdForUpdate(request.appointmentId());
        if (appointment == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "APPOINTMENT_NOT_FOUND", "预约不存在");
        }
        if (!appointment.getAccountId().equals(account.getId()) || !appointment.getStoreId().equals(storeId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "CHECK_IN_ACCESS_DENIED", "只能在预约门店登记自己的预约");
        }
        if (appointment.getOrderId() == null) {
            throw new BusinessException(HttpStatus.CONFLICT, "CHECK_IN_ORDER_REQUIRED", "该预约没有关联购买订单，请联系门店登记");
        }
        // 重复登记允许返回既有结果，但取消或拒绝的预约不可核销订单。
        boolean alreadyCheckedIn = appointment.getCheckedInAt() != null;
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED
                && !(alreadyCheckedIn && (appointment.getStatus() == AppointmentStatus.IN_PROGRESS
                || appointment.getStatus() == AppointmentStatus.COMPLETED))) {
            throw new BusinessException(HttpStatus.CONFLICT, "APPOINTMENT_NOT_CONFIRMED", "只有门店已确认的预约才能到店登记");
        }
        OrderResponse order = orderService.verifyForCheckIn(appointment.getOrderId(), account.getId(), storeId, request.verificationCode());
        if (!alreadyCheckedIn && "COMPLETED".equals(order.status())) {
            throw new BusinessException(HttpStatus.CONFLICT, "ORDER_ALREADY_COMPLETED", "该订单已完成保养，不能登记新的预约");
        }
        if (!alreadyCheckedIn && appointmentMapper.checkIn(appointment.getId(), account.getId(), storeId) == 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "INVALID_APPOINTMENT_STATE", "预约状态已变化，请刷新后重试");
        }
        return new CheckInResponse(AppointmentResponse.from(appointmentMapper.findById(appointment.getId())), order);
    }
}
