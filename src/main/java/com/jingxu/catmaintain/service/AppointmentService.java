package com.jingxu.catmaintain.service;

import com.jingxu.catmaintain.domain.account.Account;
import com.jingxu.catmaintain.domain.account.AccountRole;
import com.jingxu.catmaintain.domain.appointment.Appointment;
import com.jingxu.catmaintain.domain.appointment.AppointmentStatus;
import com.jingxu.catmaintain.domain.order.Order;
import com.jingxu.catmaintain.domain.order.OrderStatus;
import com.jingxu.catmaintain.domain.store.MerchantStore;
import com.jingxu.catmaintain.dto.appointment.AppointmentCreateRequest;
import com.jingxu.catmaintain.dto.appointment.AppointmentDecisionRequest;
import com.jingxu.catmaintain.dto.appointment.AppointmentPageResponse;
import com.jingxu.catmaintain.dto.appointment.AppointmentResponse;
import com.jingxu.catmaintain.exception.BusinessException;
import com.jingxu.catmaintain.mapper.AppointmentMapper;
import com.jingxu.catmaintain.mapper.MerchantStoreMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentMapper appointmentMapper;
    private final MerchantStoreMapper merchantStoreMapper;
    private final OrderService orderService;
    private final SessionAccountService sessionAccountService;

    @Transactional
    public AppointmentResponse create(AppointmentCreateRequest request, HttpSession session) {
        Account account = sessionAccountService.requireRole(session, AccountRole.USER);
        if (!request.appointmentTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "APPOINTMENT_TIME_IN_PAST", "预约时间必须晚于当前时间");
        }
        MerchantStore store = merchantStoreMapper.findActiveById(request.storeId());
        if (store == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "STORE_NOT_FOUND", "目标加盟店不存在或未启用");
        }
        if (request.orderId() != null) {
            Order order = orderService.requireOrderForUpdate(request.orderId());
            if (!order.getAccountId().equals(account.getId()) || !order.getStoreId().equals(store.getId())) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "APPOINTMENT_ORDER_ACCESS_DENIED", "订单不属于当前用户或门店");
            }
            if (!EnumSet.of(OrderStatus.APPROVED, OrderStatus.DELIVERED, OrderStatus.VERIFIED).contains(order.getStatus())) {
                throw new BusinessException(HttpStatus.CONFLICT, "ORDER_NOT_READY_FOR_APPOINTMENT", "订单当前状态不能预约");
            }
            if (appointmentMapper.findByOrderId(order.getId()).stream().anyMatch(existing ->
                    existing.getStatus() != AppointmentStatus.CANCELLED && existing.getStatus() != AppointmentStatus.REJECTED)) {
                throw new BusinessException(HttpStatus.CONFLICT, "ORDER_ALREADY_APPOINTED", "该订单已有有效预约，请先取消原预约");
            }
        }
        Appointment appointment = new Appointment();
        appointment.setAccountId(account.getId());
        appointment.setStoreId(store.getId());
        appointment.setOrderId(request.orderId());
        appointment.setAppointmentTime(request.appointmentTime());
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setVehiclePlate(request.vehiclePlate().trim());
        appointment.setVehicleModel(normalize(request.vehicleModel()));
        appointment.setRemark(normalize(request.remark()));
        appointmentMapper.insert(appointment);
        return AppointmentResponse.from(requireAppointment(appointment.getId()));
    }

    public AppointmentPageResponse listForUser(int page, int size, HttpSession session) {
        Account account = sessionAccountService.requireRole(session, AccountRole.USER);
        return page(account.getId(), null, page, size);
    }

    public AppointmentPageResponse listForStore(int page, int size, HttpSession session) {
        Account account = sessionAccountService.requireRole(session, AccountRole.STORE);
        MerchantStore store = requireStore(account.getId());
        return page(null, store.getId(), page, size);
    }

    public AppointmentPageResponse listForAdmin(int page, int size, HttpSession session) {
        sessionAccountService.requireRole(session, AccountRole.ADMIN);
        return page(null, null, page, size);
    }

    @Transactional
    public AppointmentResponse cancel(Long id, HttpSession session) {
        Account account = sessionAccountService.requireRole(session, AccountRole.USER);
        Appointment appointment = requireAppointment(id);
        if (!appointment.getAccountId().equals(account.getId())) {
            throw forbidden();
        }
        if (appointmentMapper.cancel(id, account.getId()) == 0) {
            throw invalidState();
        }
        return AppointmentResponse.from(requireAppointment(id));
    }

    @Transactional
    public AppointmentResponse confirm(Long id, HttpSession session) {
        Account account = requireStoreAccount(session);
        Long storeId = requireStore(account.getId()).getId();
        requireStoreAppointment(id, storeId);
        if (appointmentMapper.confirm(id, storeId) == 0) throw invalidState();
        return AppointmentResponse.from(requireAppointment(id));
    }

    @Transactional
    public AppointmentResponse reject(Long id, AppointmentDecisionRequest request, HttpSession session) {
        Account account = requireStoreAccount(session);
        Long storeId = requireStore(account.getId()).getId();
        requireStoreAppointment(id, storeId);
        String reason = request == null || request.reason() == null || request.reason().isBlank()
                ? "门店暂时无法接待该预约" : request.reason().trim();
        if (appointmentMapper.reject(id, storeId, reason) == 0) throw invalidState();
        return AppointmentResponse.from(requireAppointment(id));
    }

    public Appointment requireAppointment(Long id) {
        Appointment appointment = appointmentMapper.findById(id);
        if (appointment == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "APPOINTMENT_NOT_FOUND", "预约不存在");
        }
        return appointment;
    }

    public Appointment requireStoreAppointment(Long id, Long storeId) {
        Appointment appointment = requireAppointment(id);
        if (!appointment.getStoreId().equals(storeId)) throw forbidden();
        return appointment;
    }

    @Transactional
    public void startService(Long id, Long storeId) {
        Appointment appointment = requireStoreAppointment(id, storeId);
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) throw invalidState();
        if (appointmentMapper.startService(id) == 0) throw invalidState();
    }

    @Transactional
    public void completeService(Long id, Long storeId) {
        Appointment appointment = requireStoreAppointment(id, storeId);
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) return;
        if (appointmentMapper.complete(id) == 0) throw invalidState();
    }

    private AppointmentPageResponse page(Long accountId, Long storeId, int page, int size) {
        validatePage(page, size);
        List<AppointmentResponse> content = appointmentMapper.findPage(accountId, storeId, page * size, size)
                .stream().map(AppointmentResponse::from).toList();
        return new AppointmentPageResponse(content, appointmentMapper.count(accountId, storeId), page, size);
    }

    private Account requireStoreAccount(HttpSession session) {
        return sessionAccountService.requireRole(session, AccountRole.STORE);
    }

    private MerchantStore requireStore(Long accountId) {
        MerchantStore store = merchantStoreMapper.findByAccountId(accountId);
        if (store == null) throw new BusinessException(HttpStatus.FORBIDDEN, "STORE_PROFILE_NOT_FOUND", "当前加盟店资料不存在");
        return store;
    }

    private BusinessException forbidden() {
        return new BusinessException(HttpStatus.FORBIDDEN, "APPOINTMENT_ACCESS_DENIED", "无权访问该预约");
    }

    private BusinessException invalidState() {
        return new BusinessException(HttpStatus.CONFLICT, "INVALID_APPOINTMENT_STATE", "预约状态已发生变化，请刷新后重试");
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PAGE", "分页参数不合法，size 必须在 1 到 100 之间");
        }
    }
}
