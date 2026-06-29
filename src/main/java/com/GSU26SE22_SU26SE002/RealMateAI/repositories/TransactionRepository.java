package com.GSU26SE22_SU26SE002.RealMateAI.repositories;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.TransactionTypeEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
    Optional<Transaction> findByTransactionCode(String transactionCode);
    Optional<Transaction> findByTransactionTypeAndDocnoId(TransactionTypeEnum type, Integer docnoId);

}
