package com.ecommerce.backend.service.payment;

import com.ecommerce.backend.enums.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentGatewayFactoryTest {

    private PaymentGatewayFactory factory;
    private SimulatedPaymentGateway simulatedGateway;
    private PaymentGateway creditCardGateway;

    @BeforeEach
    void setUp() {
        simulatedGateway = mock(SimulatedPaymentGateway.class);
        creditCardGateway = mock(PaymentGateway.class);
        when(creditCardGateway.getMethod()).thenReturn(PaymentMethod.CREDIT_CARD);
        when(simulatedGateway.getMethod()).thenReturn(PaymentMethod.SIMULATED);

        factory = new PaymentGatewayFactory(List.of(creditCardGateway, simulatedGateway), simulatedGateway);
    }

    @Test
    void getGateway_simulatedMethod_returnsSimulated() {
        PaymentGateway result = factory.getGateway(PaymentMethod.SIMULATED);

        assertThat(result).isEqualTo(simulatedGateway);
    }

    @Test
    void getGateway_creditCardMethod_returnsCreditCard() {
        PaymentGateway result = factory.getGateway(PaymentMethod.CREDIT_CARD);

        assertThat(result).isEqualTo(creditCardGateway);
    }

    @Test
    void getGateway_nullMethod_returnsSimulated() {
        PaymentGateway result = factory.getGateway(null);

        assertThat(result).isEqualTo(simulatedGateway);
    }

    @Test
    void getGateway_khalti_returnsSimulated() {
        PaymentGateway result = factory.getGateway(PaymentMethod.KHALTI);

        assertThat(result).isEqualTo(simulatedGateway);
    }

    @Test
    void getGateway_esewa_returnsSimulated() {
        PaymentGateway result = factory.getGateway(PaymentMethod.ESEWA);

        assertThat(result).isEqualTo(simulatedGateway);
    }
}
