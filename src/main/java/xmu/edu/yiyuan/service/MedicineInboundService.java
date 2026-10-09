package xmu.edu.yiyuan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xmu.edu.yiyuan.entity.Medicine;
import xmu.edu.yiyuan.entity.MedicineInbound;
import xmu.edu.yiyuan.entity.MedicineStock;
import xmu.edu.yiyuan.repository.MedicineInboundRepository;
import xmu.edu.yiyuan.repository.MedicineRepository;
import xmu.edu.yiyuan.repository.MedicineStockRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MedicineInboundService {

    @Autowired
    private MedicineInboundRepository inboundRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private MedicineStockRepository stockRepository;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;

    public Optional<MedicineInbound> findById(Long id) {
        return inboundRepository.findById(id);
    }

    public List<MedicineInbound> findAll() {
        return inboundRepository.findAll();
    }

    public List<MedicineInbound> findByMedicineId(Long medicineId) {
        return inboundRepository.findByMedicineId(medicineId);
    }

    public List<MedicineInbound> findByStatus(Integer status) {
        return inboundRepository.findByStatus(status);
    }

    @Transactional
    public MedicineInbound save(MedicineInbound inbound) {
        validate(inbound);
        // 计算总金额
        if (inbound.getPurchasePrice() != null && inbound.getQuantity() != null) {
            BigDecimal total = inbound.getPurchasePrice().multiply(new BigDecimal(inbound.getQuantity()));
            inbound.setTotalAmount(total);
        }

        // 设置默认值
        if (inbound.getLocation() == null || inbound.getLocation().isEmpty()) {
            inbound.setLocation("药房");
        }
        if (inbound.getInboundDate() == null) {
            inbound.setInboundDate(LocalDateTime.now());
        }
        if (inbound.getStatus() == null) {
            inbound.setStatus(0); // 待审核
        }

        return inboundRepository.save(inbound);
    }

    @Transactional
    public void approveInbound(Long inboundId, String operator) {
        jdbc.queryForList("SELECT id FROM medicine_inbound WHERE id=? FOR UPDATE", Long.class, inboundId);
        MedicineInbound inbound = inboundRepository.findById(inboundId).orElse(null);
        if (inbound == null || inbound.getStatus() != 0) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "入库记录不存在或已审核");
        }
        validate(inbound);
        jdbc.queryForList("SELECT id FROM medicine WHERE id=? FOR UPDATE", Long.class, inbound.getMedicineId());

        // 更新入库记录状态为已审核
        inbound.setStatus(1);
        inbound.setOperator(operator);
        inboundRepository.update(inbound);

        // 创建或更新库存批次
        MedicineStock stock = stockRepository.findByBatchNumber(inbound.getBatchNumber())
                .stream()
                .filter(s -> s.getMedicineId().equals(inbound.getMedicineId()))
                .findFirst()
                .orElse(null);

        if (stock == null) {
            // 创建新批次
            stock = new MedicineStock();
            stock.setMedicineId(inbound.getMedicineId());
            stock.setBatchNumber(inbound.getBatchNumber());
            stock.setProductionDate(inbound.getProductionDate());
            stock.setExpiryDate(inbound.getExpiryDate());
            stock.setQuantity(inbound.getQuantity());
            stock.setPurchasePrice(inbound.getPurchasePrice());
            stock.setLocation(inbound.getLocation());
            stock.setStatus(1); // 正常
            stockRepository.save(stock);
        } else {
            // 更新现有批次数量
            stock.setQuantity(stock.getQuantity() + inbound.getQuantity());
            stock.setStatus(1); // 确保状态为正常
            stockRepository.update(stock);
        }

        // 更新药品总库存
        Medicine medicine = medicineRepository.findById(inbound.getMedicineId()).orElse(null);
        if (medicine != null) {
            int currentStock = medicine.getStock() != null ? medicine.getStock() : 0;
            medicine.setStock(currentStock + inbound.getQuantity());
            medicineRepository.update(medicine);
        }
    }

    public void update(MedicineInbound inbound) {
        validate(inbound);
        if (inbound.getPurchasePrice() != null && inbound.getQuantity() != null) {
            BigDecimal total = inbound.getPurchasePrice().multiply(new BigDecimal(inbound.getQuantity()));
            inbound.setTotalAmount(total);
        }
        inboundRepository.update(inbound);
    }

    public void deleteById(Long id) {
        inboundRepository.deleteById(id);
    }

    @Transactional
    public MedicineInbound receive(MedicineInbound inbound, String operator) {
        MedicineInbound saved = save(inbound);
        approveInbound(saved.getId(), operator);
        return saved;
    }

    private void validate(MedicineInbound inbound) {
        BusinessValidation.text(inbound.getBatchNumber(), 50, "批次号");
        BusinessValidation.require(inbound.getQuantity() != null && inbound.getQuantity() > 0 && inbound.getQuantity() <= 1000000, "入库数量必须为1至1000000");
        BusinessValidation.money(inbound.getPurchasePrice(), "采购价");
        BusinessValidation.require(inbound.getExpiryDate() != null && !inbound.getExpiryDate().isBefore(java.time.LocalDate.now()), "有效期不能早于今天");
        BusinessValidation.require(inbound.getProductionDate() == null || (!inbound.getProductionDate().isAfter(java.time.LocalDate.now()) && !inbound.getProductionDate().isAfter(inbound.getExpiryDate())), "生产日期无效");
        BusinessValidation.require(inbound.getMedicineId() != null && medicineRepository.findById(inbound.getMedicineId()).isPresent(), "药品不存在");
    }
}
