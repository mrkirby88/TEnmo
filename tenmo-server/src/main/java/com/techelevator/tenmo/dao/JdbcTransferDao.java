package com.techelevator.tenmo.dao;

import com.techelevator.tenmo.model.Transfer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class JdbcTransferDao implements TransferDao {

@Autowired
private JdbcTemplate jdbcTemplate;
@Autowired
private AccountDao accountDao;

public JdbcTransferDao(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
}


    @Override
    public List<Transfer> getAllTransfers(long userID) {
    List<Transfer> transferList = new ArrayList<>();
//    String sql = "SELECT *, r.username AS senderFrom, s.username AS receiverTo FROM transfers " +
//            "JOIN accounts a ON transfers.account_from = a.account_id  " +
//            "JOIN accounts ac ON transfers.account_to = ac.account_id  " +
//            "JOIN users r ON a.user_id = r.user_id  " +
//            "JOIN users s ON ac.user_id = s.user_id " +
//            "WHERE a.user_id = ? OR ac.user_id = ?;";

        String sql = "SELECT t.*, u.username AS userFrom, v.username AS userTo FROM transfers t " +
                "JOIN accounts a ON t.account_from = a.account_id " +
                "JOIN accounts b ON t.account_to = b.account_id " +
                "JOIN users u ON a.user_id = u.user_id " +
                "JOIN users v ON b.user_id = v.user_id " +
                "WHERE a.user_id = ? OR b.user_id = ?";

        SqlRowSet results = jdbcTemplate.queryForRowSet(sql, userID, userID);
        while (results.next()) {
            Transfer getAllTransfers = mapRowToTransfer(results);
            transferList.add(getAllTransfers);
        }
        return transferList;
    }

    @Override
    public Transfer getTransferById(long transferID) {

        Transfer transfer = null;
        String sql = "SELECT t.transfer_id, s.username AS sender, r.username AS receiver, tt.transfer_type_desc, ts.transfer_status_desc, t.amount " +
                " FROM transfers t " +
                " JOIN transfer_types tt ON t.transfer_type_id = tt.transfer_type_id " +
                " JOIN transfer_statuses ts ON t.transfer_status_id = ts.transfer_status_id " +
                " JOIN accounts a ON a.account_id = t.account_from " +
                " JOIN users r ON  a.account_id = r.user_id " +
                " JOIN accounts b ON b.account_id = t.account_to " +
                " JOIN users s ON b.account_id = s.user_id " +
                " WHERE transfer_id = ?;";

        SqlRowSet results = jdbcTemplate.queryForRowSet(sql, transferID);
        if(results.next()) {
            transfer = mapRowToTransfer(results);
        } else {
//            throw new TransferNotFoundException();
        }
        return transfer;
    }
    @Override
    public String sendingMoneyTo(long accountFroms, long accountTos, BigDecimal amount) {
///        Transfer account = null;
//        String sql = "INSERT INTO transfers(transfer_type_id, transfer_status_id, account_from, account_to, amount) " +
//                "VALUES (2, 1, ?, ?, ?);";
//
//        //return jdbcTemplate.update(sql, userID, amount);
//            return jdbcTemplate.update(sql, accountFrom, accountTo, amount);
        if (accountFroms == accountTos) {
            return "You can not send money to yourself.";
        } else{

            String sql = "INSERT INTO transfers (transfer_type_id, transfer_status_id, account_from, account_to, amount) " +
                    "VALUES (2, 1, ?, ?, ?)";
            jdbcTemplate.update(sql, accountFroms, accountTos, amount);
            accountDao.increaseBalance(amount, accountTos);
            accountDao.decreaseBalance(amount, accountFroms);
            return "Transfer complete";
        }
   }
//    @Override
//    public int receivingMoneyFrom(long accountFrom, long accountTo, BigDecimal amount) {
//        Transfer transfer = null;
//        String sql = "INSERT INTO transfers(transfer_type_id, transfer_status_id, account_from, account_to, amount) " +
//                "VALUES (1, 1, ?, ?, ?);";
//
//        return jdbcTemplate.update(sql, accountFrom, accountTo, amount);
//    }
    @Override
    public String receivingMoneyFrom(long accountFroms, long accountTos, BigDecimal amount) {
        if (accountFroms == accountTos) {
            return "You can not request money from your self.";
        }
        if (amount.compareTo(new BigDecimal(0)) == 1) {
            String sql = "INSERT INTO transfers (transfer_type_id, transfer_status_id, account_from, account_to, amount) " +
                    "VALUES (1, 1, ?, ?, ?)";
            jdbcTemplate.update(sql, accountFroms, accountTos, amount);
            return "Request sent";
        } else {
            return "There was a problem sending the request";
        }
    }

    @Override
    public List<Transfer> pendingRequests(long transferID) {
        List<Transfer> requests = new ArrayList<>();
        String sql = "SELECT t.transfer_id, r.username AS receiver, ts.transfer_status_desc, t.amount " +
                "FROM transfers t " +
                "JOIN transfer_statuses ts ON t.transfer_status_id = ts.transfer_status_id " +
                "JOIN accounts a ON a.account_id = t.account_from " +
                "JOIN users r ON  a.account_id = r.user_id " +
                "WHERE ts.transfer_status_id = 1 AND t.transfer_id = ?;";

        SqlRowSet results = jdbcTemplate.queryForRowSet(sql);
        while (results.next()) {
            Transfer getAllPendingRequests = mapRowToTransfer(results);
            requests.add(getAllPendingRequests);
        }
        return requests;
    }
    @Override
    public boolean updatePendingRequests(int option) {

        String sql = "UPDATE transfers " +
                "SET transfer_status_id = ? " +
                "WHERE transfer_id = ?;"; // how do we tie this part with selected transferID from pendingRequests
        return jdbcTemplate.update(sql, option) == 1;
    }

//    @Override
//public Transfer sendingMoneyTo(long userID, BigDecimal amount) {
//return null;
//}
//    @Override
//    public Transfer receivingMoneyFrom(long userID, BigDecimal amount) {
//return null;
//    }


    private Transfer mapRowToTransfer(SqlRowSet rowSet) {
    Transfer transfer = new Transfer();
    transfer.setTransferID(rowSet.getLong("transfer_id"));
        transfer.setTransferTypeID(rowSet.getLong("transfer_type_id"));
        transfer.setTransferStatusID(rowSet.getLong("transfer_status_id"));
        transfer.setAccountFrom(rowSet.getLong("account_from"));
        transfer.setAccountTo(rowSet.getLong("account_to"));
        transfer.setAmount(rowSet.getBigDecimal("amount"));
        try {
            transfer.setAccountFroms(rowSet.getString("accountFroms"));
            transfer.setAccountTos(rowSet.getString("accountTos"));
        } catch (Exception e) {}
        try {
            transfer.setTransferTypeId(rowSet.getString("transferTypeId"));
            transfer.setTransferStatusId(rowSet.getString("transferStatusId"));
        } catch (Exception e) {}
        return transfer;
    }
}
