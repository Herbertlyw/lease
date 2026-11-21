package com.atguigu.lease.web.admin.schedule;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:schedule;MODE=MySQL",
        "spring.datasource.username=sa", "spring.datasource.password="})
@Transactional
class ScheduleTasksTest {
    @Autowired ScheduleTasks schedule;
    @Autowired JdbcTemplate db;

    @Test
    void expiresOnlyActiveAgreementsPastTheirEndDate() {
        db.execute("CREATE TABLE lease_agreement(id INT PRIMARY KEY, status INT, lease_end_date DATE, is_deleted INT DEFAULT 0, update_time TIMESTAMP)");
        db.update("INSERT INTO lease_agreement(id,status,lease_end_date) VALUES(1,2,'2020-01-01'),(2,1,'2020-01-01'),(3,2,'2099-01-01')");
        schedule.checkLeaseStatus();
        assertEquals(java.util.List.of(4, 1, 2), db.queryForList("SELECT status FROM lease_agreement ORDER BY id", Integer.class));
    }
}
