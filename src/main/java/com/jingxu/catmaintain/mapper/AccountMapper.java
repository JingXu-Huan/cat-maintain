package com.jingxu.catmaintain.mapper;

import com.jingxu.catmaintain.domain.account.Account;
import com.jingxu.catmaintain.domain.account.AccountStatus;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AccountMapper {

    @Select("""
            SELECT id, username, password_hash, phone, role, status, created_at, updated_at
            FROM accounts
            WHERE username = #{username}
            """)
    Account findByUsername(@Param("username") String username);

    @Select("""
            SELECT id, username, password_hash, phone, role, status, created_at, updated_at
            FROM accounts
            WHERE id = #{id}
            """)
    Account findById(@Param("id") Long id);

    @Insert("""
            INSERT INTO accounts (username, password_hash, phone, role, status)
            VALUES (#{username}, #{passwordHash}, #{phone}, #{role}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Account account);

    @Update("""
            UPDATE accounts
            SET status = #{targetStatus}
            WHERE id = #{id} AND status = #{expectedStatus}
            """)
    int updateStatusIfCurrent(
            @Param("id") Long id,
            @Param("expectedStatus") AccountStatus expectedStatus,
            @Param("targetStatus") AccountStatus targetStatus
    );
}
