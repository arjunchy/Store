package com.ecommerce.backend.service.payment;

import com.ecommerce.backend.enums.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PaymentGatewayFactory {

    private final Map<PaymentMethod, PaymentGateway> gatewayMap;
    private final SimulatedPaymentGateway simulatedGateway;

    @Autowired
    public PaymentGatewayFactory(List<PaymentGateway> gateways, SimulatedPaymentGateway simulatedGateway) {
        this.simulatedGateway = simulatedGateway;
        this.gatewayMap = gateways.stream()
                .collect(Collectors.toMap(
                        PaymentGateway::getMethod,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));
        log.info("Initialized PaymentGatewayFactory with gateways: {}", gatewayMap.keySet());
    }

    public PaymentGateway getGateway(PaymentMethod method) {
        if (method == null) {
            log.debug("No payment method provided, using simulated gateway");
            return simulatedGateway;
        }
        PaymentGateway gateway = gatewayMap.get(method);
        if (gateway != null) {
            log.debug("Found gateway for method: {}", method);
            return gateway;
        }
        throw new IllegalArgumentException("Unsupported payment method: " + method);
    }
}
