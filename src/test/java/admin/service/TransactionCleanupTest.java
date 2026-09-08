package admin.service;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import shared.repository.TransactionRepository;
import user.repository.UserRepository;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionCleanupTest {
    @Test void deletesOnlyTheConfirmedLogWithoutTouchingBalances() throws Exception {
        var users = mock(UserRepository.class);
        var transactions = mock(TransactionRepository.class);
        when(transactions.deleteById(105L)).thenReturn(true, false);
        var service = new AdminAccountService(users, transactions);

        assertThrows(IllegalArgumentException.class, () -> service.deleteTransactionLog(0, "DELETE 0"));
        assertThrows(IllegalArgumentException.class, () -> service.deleteTransactionLog(105, "DELETE 106"));
        assertThrows(IllegalArgumentException.class, () -> service.deleteTransactionLog(105, null));
        verifyNoInteractions(transactions, users);

        assertTrue(service.deleteTransactionLog(105, "DELETE 105"));
        assertFalse(service.deleteTransactionLog(105, "DELETE 105"));
        verify(transactions, times(2)).deleteById(105L);
        verifyNoMoreInteractions(transactions);
        verifyNoInteractions(users);
    }

    @Test void databaseFailureIsNotReportedAsSuccessfulDeletion() throws Exception {
        var transactions = mock(TransactionRepository.class);
        when(transactions.deleteById(105L)).thenThrow(new SQLException("Unavailable"));
        var service = new AdminAccountService(mock(UserRepository.class), transactions);
        assertThrows(SQLException.class, () -> service.deleteTransactionLog(105, "DELETE 105"));
    }
}
