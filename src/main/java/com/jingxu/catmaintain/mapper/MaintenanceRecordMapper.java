package com.jingxu.catmaintain.mapper;

import com.jingxu.catmaintain.domain.maintenance.MaintenanceRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MaintenanceRecordMapper {

    @Select("""
            SELECT id, appointment_id, order_id, account_id, store_id, service_started_at,
                   service_completed_at, mileage, content, remark, created_at, updated_at
            FROM maintenance_records WHERE id = #{id}
            """)
    MaintenanceRecord findById(@Param("id") Long id);

    @Select("""
            SELECT id, appointment_id, order_id, account_id, store_id, service_started_at,
                   service_completed_at, mileage, content, remark, created_at, updated_at
            FROM maintenance_records WHERE appointment_id = #{appointmentId}
            """)
    MaintenanceRecord findByAppointmentId(@Param("appointmentId") Long appointmentId);

    @Select("""
            <script>
            SELECT id, appointment_id, order_id, account_id, store_id, service_started_at,
                   service_completed_at, mileage, content, remark, created_at, updated_at
            FROM maintenance_records
            <where>
                <if test="accountId != null">account_id = #{accountId}</if>
                <if test="storeId != null">store_id = #{storeId}</if>
            </where>
            ORDER BY created_at DESC, id DESC
            LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<MaintenanceRecord> findPage(@Param("accountId") Long accountId, @Param("storeId") Long storeId,
                                     @Param("offset") int offset, @Param("size") int size);

    @Select("""
            <script>
            SELECT COUNT(*) FROM maintenance_records
            <where>
                <if test="accountId != null">account_id = #{accountId}</if>
                <if test="storeId != null">store_id = #{storeId}</if>
            </where>
            </script>
            """)
    long count(@Param("accountId") Long accountId, @Param("storeId") Long storeId);

    @Insert("""
            INSERT INTO maintenance_records
                (appointment_id, order_id, account_id, store_id, service_started_at, content, remark)
            VALUES (#{appointmentId}, #{orderId}, #{accountId}, #{storeId}, #{serviceStartedAt}, #{content}, #{remark})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(MaintenanceRecord record);

    @Update("""
            UPDATE maintenance_records
            SET service_completed_at = CURRENT_TIMESTAMP, mileage = #{mileage}, content = #{content}, remark = #{remark}
            WHERE id = #{id} AND service_completed_at IS NULL
            """)
    int complete(@Param("id") Long id, @Param("mileage") Integer mileage,
                 @Param("content") String content, @Param("remark") String remark);
}
