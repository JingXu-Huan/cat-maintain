package com.jingxu.catmaintain.service;

import com.jingxu.catmaintain.domain.account.Account;
import com.jingxu.catmaintain.domain.account.AccountRole;
import com.jingxu.catmaintain.domain.account.AccountStatus;
import com.jingxu.catmaintain.domain.store.MerchantStoreApplication;
import com.jingxu.catmaintain.dto.admin.StoreApplicationResponse;
import com.jingxu.catmaintain.exception.BusinessException;
import com.jingxu.catmaintain.mapper.AccountMapper;
import com.jingxu.catmaintain.mapper.MerchantStoreMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreReviewService {

    private final AccountMapper accountMapper;
    private final MerchantStoreMapper merchantStoreMapper;
    private final SessionAccountService sessionAccountService;

    public List<StoreApplicationResponse> list(AccountStatus status, HttpSession session) {
        requireAdmin(session);
        return merchantStoreMapper.findApplicationsByStatus(status).stream()
                .map(StoreApplicationResponse::from)
                .toList();
    }

    @Transactional
    public StoreApplicationResponse approve(Long storeId, HttpSession session) {
        requireAdmin(session);
        MerchantStoreApplication application = requireApplication(storeId);
        ensurePending(application);
        updateAccountStatus(application, AccountStatus.ACTIVE);
        merchantStoreMapper.markReviewed(storeId, null);
        return StoreApplicationResponse.from(merchantStoreMapper.findApplicationById(storeId));
    }

    @Transactional
    public StoreApplicationResponse reject(Long storeId, String reason, HttpSession session) {
        requireAdmin(session);
        MerchantStoreApplication application = requireApplication(storeId);
        ensurePending(application);
        updateAccountStatus(application, AccountStatus.REJECTED);
        String reviewRemark = StringUtils.hasText(reason) ? reason.trim() : "平台拒绝了加盟店申请";
        merchantStoreMapper.markReviewed(storeId, reviewRemark);
        return StoreApplicationResponse.from(merchantStoreMapper.findApplicationById(storeId));
    }

    private Account requireAdmin(HttpSession session) {
        return sessionAccountService.requireRole(session, AccountRole.ADMIN);
    }

    private MerchantStoreApplication requireApplication(Long storeId) {
        MerchantStoreApplication application = merchantStoreMapper.findApplicationById(storeId);
        if (application == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "STORE_NOT_FOUND", "加盟店申请不存在");
        }
        return application;
    }

    private void ensurePending(MerchantStoreApplication application) {
        if (application.getStatus() != AccountStatus.PENDING) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "STORE_REVIEW_ALREADY_PROCESSED",
                    "该加盟店申请已经处理，不能重复审核"
            );
        }
    }

    private void updateAccountStatus(MerchantStoreApplication application, AccountStatus targetStatus) {
        int updated = accountMapper.updateStatusIfCurrent(
                application.getAccountId(),
                AccountStatus.PENDING,
                targetStatus
        );
        if (updated != 1) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "STORE_REVIEW_ALREADY_PROCESSED",
                    "该加盟店申请已经处理，不能重复审核"
            );
        }
    }
}
