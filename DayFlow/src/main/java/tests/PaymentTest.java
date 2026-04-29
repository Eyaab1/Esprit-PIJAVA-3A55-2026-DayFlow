package tests;

import enums.PaymentStatus;
import model.payment.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class PaymentTest {

    private Payment testPayment;

    @BeforeEach
    public void setUp() {
        testPayment = new Payment();
        testPayment.setId(1);
        testPayment.setUserId(1);
        testPayment.setCoachId(2);
        testPayment.setCoachingRequestId(5);
        testPayment.setAmount(new BigDecimal("99.99"));
        testPayment.setCurrency("EUR");
        testPayment.setStatus(PaymentStatus.PENDING);
    }

    @Test
    public void testDefaultValues() {
        Payment fresh = new Payment();
        assertEquals(PaymentStatus.PENDING, fresh.getStatus());
        assertEquals("EUR", fresh.getCurrency());
        assertNotNull(fresh.getCreatedAt());
    }

    @Test
    public void testPaymentCreation() {
        assertEquals(1, testPayment.getId());
        assertEquals(1, testPayment.getUserId());
        assertEquals(2, testPayment.getCoachId());
        assertEquals(new BigDecimal("99.99"), testPayment.getAmount());
        assertEquals("EUR", testPayment.getCurrency());
        assertEquals(PaymentStatus.PENDING, testPayment.getStatus());
    }

    @Test
    public void testUserIdValidation() {
        assertThrows(IllegalArgumentException.class, () -> testPayment.setUserId(0));
        assertThrows(IllegalArgumentException.class, () -> testPayment.setUserId(-1));
        assertDoesNotThrow(() -> testPayment.setUserId(null));
    }

    @Test
    public void testAmountValidation() {
        assertThrows(IllegalArgumentException.class, () -> testPayment.setAmount(new BigDecimal("-0.01")));
        assertDoesNotThrow(() -> testPayment.setAmount(BigDecimal.ZERO));
        assertDoesNotThrow(() -> testPayment.setAmount(null));
    }

    @Test
    public void testCurrencyValidation() {
        assertThrows(IllegalArgumentException.class, () -> testPayment.setCurrency("EU"));
        assertThrows(IllegalArgumentException.class, () -> testPayment.setCurrency("EURO"));
        assertDoesNotThrow(() -> testPayment.setCurrency("USD"));
    }

    @Test
    public void testCurrencyUppercased() {
        testPayment.setCurrency("eur");
        assertEquals("EUR", testPayment.getCurrency());
    }

    @Test
    public void testStatusNullValidation() {
        assertThrows(IllegalArgumentException.class, () -> testPayment.setStatus(null));
    }

    @Test
    public void testPaidAtSetOnSuccess() {
        assertNull(testPayment.getPaidAt());
        testPayment.setStatus(PaymentStatus.SUCCEEDED);
        assertNotNull(testPayment.getPaidAt());
    }

    @Test
    public void testCanBeCancelled() {
        testPayment.setStatus(PaymentStatus.PENDING);
        assertTrue(testPayment.canBeCancelled());

        testPayment.setStatus(PaymentStatus.SUCCEEDED);
        assertFalse(testPayment.canBeCancelled());
    }

    @Test
    public void testCanBeRefunded() {
        testPayment.setStatus(PaymentStatus.SUCCEEDED);
        assertTrue(testPayment.canBeRefunded());

        testPayment.setStatus(PaymentStatus.PENDING);
        assertFalse(testPayment.canBeRefunded());
    }

    @Test
    public void testFormattedAmount() {
        testPayment.setAmount(new BigDecimal("99.99"));
        testPayment.setCurrency("EUR");
        assertEquals("99.99 EUR", testPayment.getFormattedAmount());
    }

    @Test
    public void testFormattedAmountWhenNull() {
        testPayment.setAmount(null);
        assertEquals("0.00 EUR", testPayment.getFormattedAmount());
    }
}
