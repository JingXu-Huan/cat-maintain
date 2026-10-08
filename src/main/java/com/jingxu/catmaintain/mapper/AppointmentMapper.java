package com.jingxu.catmaintain.mapper;

import com.jingxu.catmaintain.domain.appointment.Appointment;
import com.jingxu.catmaintain.domain.appointment.AppointmentStatus;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AppointmentMapper {

    @Select("""
            SELECT id, account_id, store_id, order_id, appointment_time, status,
                   vehicle_plate, vehicle_model, remark, checked_in_at, created_at, updated_at
            FROM appointments WHERE id = #{id}
            """)
    Appointment findById(@Param("id") Long id);

    @Select("SELECT * FROM appointments WHERE id = #{id} FOR UPDATE")
    Appointment findByIdForUpdate(@Param("id") Long id);

    @Select("SELECT * FROM appointments WHERE order_id = #{orderId} ORDER BY appointment_time DESC, id DESC")
    List<Appointment> findByOrderId(@Param("orderId") Long orderId);

    @Update("""
            UPDATE appointments SET checked_in_at = CURRENT_TIMESTAMP
            WHERE id = #{id} AND account_id = #{accountId} AND store_id = #{storeId}
              AND status = 'CONFIRMED' AND checked_in_at IS NULL
            """)
    int checkIn(@Param("id") Long id, @Param("accountId") Long accountId, @Param("storeId") Long storeId);

    @Select("""
            <script>
            SELECT id, account_id, store_id, order_id, appointment_time, status,
                   vehicle_plate, vehicle_model, remark, checked_in_at, created_at, updated_at
            FROM appointments
            <where>
                <if test="accountId != null">account_id = #{accountId}</if>
                <if test="storeId != null">store_id = #{storeId}</if>
            </where>
            ORDER BY appointment_time DESC, id DESC
            LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<Appointment> findPage(@Param("accountId") Long accountId, @Param("storeId") Long storeId,
                               @Param("offset") int offset, @Param("size") int size);

    @Select("""
            <script>
            SELECT COUNT(*) FROM appointments
            <where>
                <if test="accountId != null">account_id = #{accountId}</if>
                <if test="storeId != null">store_id = #{storeId}</if>
            </where>
            </script>
            """)
    long count(@Param("accountId") Long accountId, @Param("storeId") Long storeId);

    @Insert("""
            INSERT INTO appointments
                (account_id, store_id, order_id, appointment_time, status, vehicle_plate, vehicle_model, remark)
            VALUES (#{accountId}, #{storeId}, #{orderId}, #{appointmentTime}, #{status}, #{vehiclePlate}, #{vehicleModel}, #{remark})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Appointment appointment);

    @Update("""
            UPDATE appointments SET status = 'CANCELLED'
            WHERE id = #{id} AND account_id = #{accountId} AND status IN ('PENDING', 'CONFIRMED')
              AND checked_in_at IS NULL
            """)
    int cancel(@Param("id") Long id, @Param("accountId") Long accountId);

    @Update("""
            UPDATE appointments SET status = 'CONFIRMED'
            WHERE id = #{id} AND store_id = #{storeId} AND status = 'PENDING'
            """)
    int confirm(@Param("id") Long id, @Param("storeId") Long storeId);

    @Update("""
            UPDATE appointments SET status = 'REJECTED', remark = #{reason}
            WHERE id = #{id} AND store_id = #{storeId} AND status = 'PENDING'
            """)
    int reject(@Param("id") Long id, @Param("storeId") Long storeId, @Param("reason") String reason);

    @Update("""
            UPDATE appointments SET status = 'IN_PROGRESS'
            WHERE id = #{id} AND status = 'CONFIRMED'
            """)
    int startService(@Param("id") Long id);

    @Update("""
            UPDATE appointments SET status = 'COMPLETED'
            WHERE id = #{id} AND status = 'IN_PROGRESS'
            """)
    int complete(@Param("id") Long id);
}
