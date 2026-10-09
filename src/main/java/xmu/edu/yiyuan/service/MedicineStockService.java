package xmu.edu.yiyuan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xmu.edu.yiyuan.entity.MedicineStock;
import xmu.edu.yiyuan.repository.MedicineStockRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class MedicineStockService {

    @Autowired
    private MedicineStockRepository stockRepository;

    public Optional<MedicineStock> findById(Long id) {
        return stockRepository.findById(id);
    }

    public List<MedicineStock> findAll() {
        return stockRepository.findAll();
    }

    public List<MedicineStock> findByMedicineId(Long medicineId) {
        return stockRepository.findByMedicineId(medicineId);
    }

    public List<MedicineStock> findByLocation(String location) {
        return stockRepository.findByLocation(location);
    }

    public List<MedicineStock> findExpiringSoon(int days) {
        LocalDate threshold = LocalDate.now().plusDays(days);
        return stockRepository.findAll().stream()
                .filter(s -> s.getExpiryDate() != null
                        && s.getExpiryDate().isBefore(threshold)
                        && s.getExpiryDate().isAfter(LocalDate.now())
                        && s.getStatus() == 1)
                .toList();
    }

    public MedicineStock save(MedicineStock stock) {
        return stockRepository.save(stock);
    }

    public void update(MedicineStock stock) {
        stockRepository.update(stock);
    }

    public void deleteById(Long id) {
        stockRepository.deleteById(id);
    }
}
