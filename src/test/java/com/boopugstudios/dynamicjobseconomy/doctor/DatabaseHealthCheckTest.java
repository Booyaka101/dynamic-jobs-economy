package com.boopugstudios.dynamicjobseconomy.doctor;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import com.boopugstudios.dynamicjobseconomy.database.DatabaseManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseHealthCheckTest {

    @Mock
    DynamicJobsEconomy plugin;

    @Mock
    DatabaseManager databaseManager;

    @Mock
    Connection connection;

    @Mock
    PreparedStatement preparedStatement;

    @Test
    void run_returnsOkWhenSelectOneExecutes() throws Exception {
        when(plugin.getDatabaseManager()).thenReturn(databaseManager);
        when(databaseManager.getDatabaseType()).thenReturn("sqlite");
        when(databaseManager.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.execute()).thenReturn(true);

        DatabaseHealthCheck check = new DatabaseHealthCheck(plugin);
        DatabaseHealthCheck.Result result = check.run();

        assertTrue(result.isOk(), "Expected DB health to be OK");
        assertEquals("sqlite", result.getDbType());
        assertTrue(result.getLatencyMs() >= 0);
        assertNull(result.getErrorMessage());

        verify(connection).prepareStatement("SELECT 1");
        verify(preparedStatement).execute();
    }

    @Test
    void run_handlesMissingDatabaseManager() {
        when(plugin.getDatabaseManager()).thenReturn(null);

        DatabaseHealthCheck check = new DatabaseHealthCheck(plugin);
        DatabaseHealthCheck.Result result = check.run();

        assertFalse(result.isOk());
        assertEquals("unknown", result.getDbType());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    void run_handlesSQLException() throws Exception {
        when(plugin.getDatabaseManager()).thenReturn(databaseManager);
        when(databaseManager.getDatabaseType()).thenReturn("mysql");
        when(databaseManager.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("boom"));

        DatabaseHealthCheck check = new DatabaseHealthCheck(plugin);
        DatabaseHealthCheck.Result result = check.run();

        assertFalse(result.isOk());
        assertEquals("mysql", result.getDbType());
        assertEquals("boom", result.getErrorMessage());
    }
}
