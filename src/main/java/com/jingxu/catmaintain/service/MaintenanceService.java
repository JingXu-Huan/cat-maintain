package com.jingxu.catmaintain.service;

import com.jingxu.catmaintain.domain.account.Account;
import com.jingxu.catmaintain.domain.account.AccountRole;
import com.jingxu.catmaintain.domain.appointment.Appointment;
import com.jingxu.catmaintain.domain.appointment.AppointmentStatus;
import com.jingxu.catmaintain.domain.maintenance.MaintenanceRecord;
import com.jingxu.catmaintain.domain.order.Order;
import com.jingxu.catmaintain.domain.order.OrderStatus;
import com.jingxu.catmaintain.dto.maintenance.MaintenanceCompleteRequest;
import com.jingxu.catmaintain.dto.maintenance.MaintenancePageResponse;
import com.jingxu.catmaintain.dto.maintenance.MaintenanceResponse;
import com.jingxu.catmaintain.dto.maintenance.MaintenanceStartRequest;
import com.jingxu.catmaintain.exception.BusinessException;
import com.jingxu.catmaintain.mapper.MaintenanceRecordMapper;
import com.jingxu.catmaintain.mapper.MerchantStoreMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final MaintenanceRecordMapper maintenanceRecordMapper;
    private final AppointmentService appointmentService;
    private final MerchantStoreMapper merchantStoreMapper;
    private final OrderService orderService;
    private final SessionAccountService sessionAccountService;

    public MaintenancePageResponse listForUser(int page, int size, HttpSession session) {
        Account account = sessionAccountService.requireRole(session, AccountRole.USER);
        return page(account.getId(), null, page, size);
    }

    public MaintenancePageResponse listForStore(int page, int size, HttpSession session) {
        Account account = sessionAccountService.requireRole(session, AccountRole.STORE);
        return page(null, requireStore(account.getId()).getId(), page, size);
    }

    @Transactional
    public MaintenanceResponse start(Long appointmentId, MaintenanceStartRequest request, HttpSession session) {
        Long storeId = requireStore(requireStoreAccount(session).getId()).getId();
        Appointment appointment = appointmentService.requireStoreAppointment(appointmentId, storeId);
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new BusinessException(HttpStatus.CONFLICT, "APPOINTMENT_NOT_CONFIRMED", "只有已确认预约才能开始保养");
        }
        if (maintenanceRecordMapper.findByAppointmentId(appointmentId) != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "MAINTENANCE_ALREADY_STARTED", "该预约已经创建保养记录");
        }
        if (appointment.getOrderId() != null) {
            Order order = orderService.requireOrder(appointment.getOrderId());
            if (!order.getAccountId().equals(appointment.getAccountId()) || !order.getStoreId().equals(storeId)) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "MAINTENANCE_ACCESS_DENIED", "预约关联订单归属不一致");
            }
            if (order.getStatus() != OrderStatus.VERIFIED) {
                throw new BusinessException(HttpStatus.CONFLICT, "ORDER_NOT_VERIFIED", "关联订单尚未完成门店核销");
            }
        }
        appointmentService.startService(appointmentId, storeId);
        MaintenanceRecord record = new MaintenanceRecord();
        record.setAppointmentId(appointmentId);
        record.setOrderId(appointment.getOrderId());
        record.setAccountId(appointment.getAccountId());
        record.setStoreId(storeId);
        record.setServiceStartedAt(LocalDateTime.now());
        record.setContent(request.content().trim());
        try {
            maintenanceRecordMapper.insert(record);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "MAINTENANCE_ALREADY_STARTED", "该预约已经创建保养记录");
        }
        return MaintenanceResponse.from(requireRecord(record.getId()));
    }

    @Transactional
    public MaintenanceResponse complete(Long id, MaintenanceCompleteRequest request, HttpSession session) {
        Long storeId = requireStore(requireStoreAccount(session).getId()).getId();
        MaintenanceRecord record = requireRecord(id);
        if (!record.getStoreId().equals(storeId)) throw forbidden();
        if (record.getServiceCompletedAt() != null) return MaintenanceResponse.from(record);
        // MySQL DATETIME 按秒保存，允许完成操作与开始操作落在同一秒；只拒绝明显来自未来的脏数据。
        if (record.getServiceStartedAt().isAfter(LocalDateTime.now().plusSeconds(1))) {
            throw new BusinessException(HttpStatus.CONFLICT, "INVALID_SERVICE_TIME", "服务完成时间必须晚于开始时间");
        }
        if (maintenanceRecordMapper.complete(id, request.mileage(), request.content().trim(), normalize(request.remark())) == 0) {
            throw invalidState();
        }
        appointmentService.completeService(record.getAppointmentId(), storeId);
        if (record.getOrderId() != null) orderService.completeForMaintenance(record.getOrderId());
        return MaintenanceResponse.from(requireRecord(id));
    }

    private MaintenancePageResponse page(Long accountId, Long storeId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PAGE", "分页参数不合法，size 必须在 1 到 100 之间");
        }
        List<MaintenanceResponse> content = maintenanceRecordMapper.findPage(accountId, storeId, page * size, size)
                .stream().map(MaintenanceResponse::from).toList();
        return new MaintenancePageResponse(content, maintenanceRecordMapper.count(accountId, storeId), page, size);
    }

    private MaintenanceRecord requireRecord(Long id) {
        MaintenanceRecord record = maintenanceRecordMapper.findById(id);
        if (record == null) throw new BusinessException(HttpStatus.NOT_FOUND, "MAINTENANCE_NOT_FOUND", "保养记录不存在");
        return record;
    }

    private Account requireStoreAccount(HttpSession session) {
        return sessionAccountService.requireRole(session, AccountRole.STORE);
    }

    private com.jingxu.catmaintain.domain.store.MerchantStore requireStore(Long accountId) {
        var store = merchantStoreMapper.findByAccountId(accountId);
        if (store == null) throw new BusinessException(HttpStatus.FORBIDDEN, "STORE_PROFILE_NOT_FOUND", "当前加盟店资料不存在");
        return store;
    }

    private BusinessException forbidden() {
        return new BusinessException(HttpStatus.FORBIDDEN, "MAINTENANCE_ACCESS_DENIED", "无权访问该保养记录");
    }

    private BusinessException invalidState() {
        return new BusinessException(HttpStatus.CONFLICT, "INVALID_MAINTENANCE_STATE", "保养记录状态已发生变化，请刷新后重试");
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
