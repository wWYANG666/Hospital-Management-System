package xmu.edu.yiyuan.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Additive, repeatable migration; any unexpected DDL failure stops startup. */
@Component
@Order(0)
@ConditionalOnProperty(name = "hospital.bootstrap.enabled", havingValue = "true")
public class BusinessSchemaMigration implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    public BusinessSchemaMigration(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void run(ApplicationArguments args) {
        column("hospitalization", "appointment_id", "BIGINT NULL");
        column("hospitalization", "admission_reason", "TEXT NULL");
        column("hospitalization", "expected_days", "INT NULL");
        column("hospitalization", "request_department_id", "BIGINT NULL");
        column("hospitalization", "request_time", "DATETIME NULL");
        column("hospitalization", "request_status", "VARCHAR(20) NULL DEFAULT NULL");
        column("hospitalization", "discharge_diagnosis", "TEXT NULL");
        column("hospitalization", "discharge_notes", "TEXT NULL");
        column("hospitalization", "paid", "TINYINT(1) NOT NULL DEFAULT 0");
        column("hospitalization", "order_cost", "DECIMAL(14,2) NULL");
        column("hospitalization", "bed_cost", "DECIMAL(14,2) NULL");
        column("hospitalization", "settled_at", "DATETIME NULL");
        column("medical_order", "quantity", "INT NULL");
        column("medical_order", "cost", "DECIMAL(14,2) NULL");
        column("bed", "ward", "VARCHAR(50) NULL");
        column("report", "paid", "TINYINT(1) NULL");
        column("report", "paid_at", "DATETIME NULL");
        jdbc.execute("CREATE TABLE IF NOT EXISTS payment_record ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY, patient_id BIGINT NOT NULL, "
                + "business_type VARCHAR(30) NOT NULL, business_id BIGINT NOT NULL, "
                + "amount DECIMAL(14,2) NOT NULL, paid_at DATETIME NOT NULL, "
                + "UNIQUE KEY uk_payment_business (business_type,business_id), "
                + "KEY idx_payment_patient (patient_id,paid_at)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        // Preserve historical amounts; never recompute already discharged episodes.
        jdbc.update("UPDATE hospitalization SET order_cost=COALESCE(total_cost,0) WHERE order_cost IS NULL");
        jdbc.update("UPDATE hospitalization SET settled_at=COALESCE(updated_at,created_at,NOW()) "
                + "WHERE status='DISCHARGED' AND settled_at IS NULL");
        jdbc.update("UPDATE hospitalization h SET paid=1 WHERE paid=0 AND EXISTS "
                + "(SELECT 1 FROM medical_order o WHERE o.hospitalization_id=h.id AND o.order_type='OTHER' "
                + "AND o.status='COMPLETED' AND o.order_content LIKE '%住院费用已支付%')");
        jdbc.update("UPDATE hospitalization SET request_status='APPROVED' "
                + "WHERE request_status IS NULL AND bed_id IS NOT NULL");
        jdbc.update("INSERT INTO payment_record (patient_id,business_type,business_id,amount,paid_at) "
                + "SELECT p.patient_id,'PRESCRIPTION',p.id,COALESCE((SELECT SUM(i.total_price) FROM prescription_item i WHERE i.prescription_id=p.id),0),COALESCE(p.paid_at,p.created_at,NOW()) "
                + "FROM prescription p WHERE p.paid=1 AND NOT EXISTS (SELECT 1 FROM payment_record r WHERE r.business_type='PRESCRIPTION' AND r.business_id=p.id)");
        jdbc.update("INSERT INTO payment_record (patient_id,business_type,business_id,amount,paid_at) "
                + "SELECT r.patient_id,'EXAM_REPORT',r.id,COALESCE(e.price,0),COALESCE(r.paid_at,r.report_date,NOW()) FROM report r JOIN examination e ON r.examination_id=e.id "
                + "WHERE r.paid=1 AND NOT EXISTS (SELECT 1 FROM payment_record p WHERE p.business_type='EXAM_REPORT' AND p.business_id=r.id)");
        jdbc.update("INSERT INTO payment_record (patient_id,business_type,business_id,amount,paid_at) "
                + "SELECT h.patient_id,'HOSPITALIZATION',h.id,COALESCE(h.total_cost,0),COALESCE(h.updated_at,h.created_at,NOW()) FROM hospitalization h "
                + "WHERE h.paid=1 AND NOT EXISTS (SELECT 1 FROM payment_record p WHERE p.business_type='HOSPITALIZATION' AND p.business_id=h.id)");
    }

    private void column(String table, String name, String definition) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_schema=DATABASE() AND table_name=? AND column_name=?", Integer.class, table, name);
        if (count == null || count == 0) jdbc.execute("ALTER TABLE `" + table + "` ADD COLUMN `" + name + "` " + definition);
    }
}
