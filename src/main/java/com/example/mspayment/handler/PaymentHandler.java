package com.example.mspayment.handler;

import com.example.mspayment.models.dto.CreatePaymentWithAccountDTO;
import com.example.mspayment.models.dto.PaymentCreateDTO;
import com.example.mspayment.models.entities.*;
import com.example.mspayment.services.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Component
@Slf4j(topic = "PAYMENT_HANDLER")
public class PaymentHandler {
    private final IPaymentService paymentService;
    private final AcquisitionService acquisitionService;
    private final DebitService debitService;
    private final TransactionService transactionService;
    private final BillService billService;

    @Autowired
    public PaymentHandler(IPaymentService paymentService, AcquisitionService acquisitionService, DebitService debitService, TransactionService transactionService, BillService billService) {
        this.paymentService = paymentService;
        this.acquisitionService = acquisitionService;
        this.debitService = debitService;
        this.transactionService = transactionService;
        this.billService = billService;
    }


    public Mono<ServerResponse> findAll(ServerRequest request){
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                .body(paymentService.findAll(), Payment.class);
    }

    public Mono<ServerResponse> findById(ServerRequest request) {
        String id = request.pathVariable("id");
        return paymentService.findById(id).flatMap(p -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(p))
                .switchIfEmpty(Mono.error(new RuntimeException("Payment no found")));
    }

    public Mono<ServerResponse> findByAcquisitionIban(ServerRequest request){
        String iban = request.pathVariable("iban");
        return acquisitionService.findByIban(iban).flatMap(p -> ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(p))
                        .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> findPaymentByIban(ServerRequest request){
        String iban = request.pathVariable("iban");
        return paymentService.findByAcquisition_Iban(iban).flatMap(p -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(p))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> findCreditCard(ServerRequest request){
        String creditCard = request.pathVariable("creditCard");
        return paymentService.findByAcquisition_CardNumber(creditCard)
                .flatMap(p -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(p))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> updateAcquisition(ServerRequest request){
        Mono<Acquisition> acquisition = request.bodyToMono(Acquisition.class);
        return acquisition
                .flatMap(acquisitionService::updateAcquisition)
                .flatMap(p -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(p))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> save(ServerRequest request){
        Mono<Payment> payment = request.bodyToMono(Payment.class);
        return payment.flatMap(paymentService::create)
                .flatMap(p -> ServerResponse.created(URI.create("/payment/".concat(p.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(p))
                .onErrorResume(error -> Mono.error(new RuntimeException(error.getMessage())));
    }

    private Mono<Payment> createPaymentCardLess(Mono<CreatePaymentWithAccountDTO> tuple) {
        return tuple
                .zipWhen(paymentRequest ->  {
            log.info("ACCOUNT, {}", paymentRequest.getAccountNumber());
            return billService.findByAccountNumber(paymentRequest.getAccountNumber());
                })
                .zipWhen(result -> {
                    Transaction transaction = new Transaction();
                    transaction.setTransactionType("DEPOSIT");
                    transaction.setTransactionAmount(result.getT1().getAmount());
                    transaction.setDescription(result.getT1().getDescription());
                    Bill bill = result.getT2();
                    bill.setBalance(bill.getBalance() - result.getT1().getAmount());
                    transaction.setBill(bill);
                    return transactionService.createTransaction(transaction);
                })
                .zipWhen(data -> paymentService.findByAcquisition_Bill_AccountNumber(data.getT1().getT1().getAccountNumber()))
                .zipWhen(res -> {
                    res.getT1().getT1().getT2().setBalance(res.getT2().getAcquisition().getBill().getBalance());
                    return billService.updateBill(res.getT1().getT1().getT2());
                })
                .flatMap(response -> Mono.just(response.getT1().getT2()));

    }

    private Mono<Payment> createPaymentWithCard(Mono<Tuple2<CreatePaymentWithAccountDTO, Debit>> tuple) {
        return tuple
                .zipWhen(data -> debitService.findByCardNumber(data.getT2().getCardNumber()))
                .zipWhen(result -> {
                    Transaction transaction = new Transaction();
                    transaction.setTransactionType("PAGO");
                    transaction.setTransactionAmount(result.getT1().getT1().getAmount());
                    transaction.setDescription(result.getT1().getT1().getDescription());

                    if (result.getT1().getT1().getAmount() > result.getT2().getPrincipal().getBill().getBalance()) {
                        List<Acquisition> acquisitions = result.getT2().getAssociations();
                        Acquisition acquisition = acquisitions.stream().filter(acq -> acq.getBill().getBalance() > result.getT1().getT1().getAmount()).findFirst().orElseThrow(() -> new RuntimeException("The retire amount exceeds the available balance in yours accounts"));
                        Bill bill = acquisition.getBill();
                        bill.setBalance(bill.getBalance() - result.getT1().getT1().getAmount());
                        transaction.setBill(bill);
                    } else {
                        result.getT2().getPrincipal().getBill().setBalance(result.getT2().getPrincipal().getBill().getBalance() - result.getT1().getT1().getAmount());
                        transaction.setBill(result.getT2().getPrincipal().getBill());
                    }
                    return transactionService.createTransaction(transaction);
                })
                .zipWhen(updateDebit -> {
                    //update list
                    List<Acquisition> acquisitions = updateDebit.getT1().getT2().getAssociations().stream().peek(rx -> {
                        if (Objects.equals(rx.getBill().getAccountNumber(), updateDebit.getT2().getBill().getAccountNumber())) {
                            rx.setBill(updateDebit.getT2().getBill());
                        }
                    }).collect(Collectors.toList());
                    //validate is principal
                    Acquisition currentAcq = acquisitions.stream().filter(acquisition -> Objects.equals(acquisition.getBill().getAccountNumber(), updateDebit.getT2().getBill().getAccountNumber())).findFirst().orElse(null);
                    Boolean isPrincipal = updateDebit.getT1().getT2().getPrincipal().getIban().equals(currentAcq.getIban());
                    if (Boolean.TRUE.equals(isPrincipal)) {
                        updateDebit.getT1().getT2().getPrincipal().setBill(updateDebit.getT2().getBill());
                    }
                    Debit debit = new Debit();
                    debit.setAssociations(acquisitions);
                    debit.setPrincipal(updateDebit.getT1().getT2().getPrincipal());
                    debit.setCardNumber(updateDebit.getT1().getT2().getCardNumber());
                    return debitService.updateDebit(debit);
                })
                .checkpoint("after debit update")
                .flatMap(payment -> {
                    String creditCard = payment.getT1().getT1().getT1().getT1().getCreditCard();
                    if (Objects.equals(creditCard, "")) {
                        return Mono.error(() -> new RuntimeException("the credit card is invalid"));
                    }
                    return paymentService.findByAcquisition_CardNumber(creditCard);
                })
                .zipWhen(data -> billService.findByAccountNumber(data.getAcquisition().getBill().getAccountNumber()))
                .zipWhen(bill -> {
                    Bill billUpdate = bill.getT2();
                    billUpdate.setBalance(bill.getT1().getCreditLine());
                    billUpdate.setAcquisition(billUpdate.getAcquisition());
                    return billService.updateBill(billUpdate);
                })
                .zipWhen(updateAcq -> acquisitionService.updateAcquisition(updateAcq.getT2().getAcquisition()))
                .flatMap(response -> {
                    Payment payment = response.getT1().getT1().getT1();
                    //Bill bill = response.getT1().getT2();
                    Acquisition acquisition = response.getT2();
                    acquisition.setBill(response.getT1().getT2());
                    payment.setAmount(0.0);
                    payment.setAcquisition(acquisition);
                    payment.setExpirationDate(LocalDateTime.now().plusDays(30));
                    return paymentService.update(payment);
                });
    }
    public Mono<ServerResponse> makePaymentAccountNumber(ServerRequest request) {
        Mono<CreatePaymentWithAccountDTO> paymentCreateDTOMono = request.bodyToMono(CreatePaymentWithAccountDTO.class);
        return paymentCreateDTOMono
                .zipWhen(paymentRequest -> debitService.findByAccountNumber(paymentRequest.getAccountNumber())
                        .switchIfEmpty(Mono.defer(() -> {
                    return Mono.just(new Debit());
                })))
                .flatMap(data -> {
                    log.info("DEBIT_DATA. {}", data.getT2());
                    if(data.getT2().getCardNumber() == null){
                        return Mono.just(data.getT1()).as(this::createPaymentCardLess);
                    }
                    return Mono.just(Tuples.of(data.getT1(), data.getT2())).as(this::createPaymentWithCard);
                })
                .flatMap(payment ->
                        ServerResponse.created(URI.create("/payment/".concat(payment.getId())))
                                .contentType(APPLICATION_JSON)
                                .bodyValue(payment))
                .log()
                .onErrorResume(e -> Mono.error(new RuntimeException(e.getMessage())));
    }

    public Mono<ServerResponse> makePaymentWithCardNumber(ServerRequest request) {
        Mono<PaymentCreateDTO> paymentCreateDTOMono = request.bodyToMono(PaymentCreateDTO.class);
        Mono<Transaction> transactionCreate = Mono.just(new Transaction());
        return Mono.zip(paymentCreateDTOMono
                        , transactionCreate)
                .zipWhen(data -> debitService.findByCardNumber(data.getT1().getCardNumber()))
                .zipWhen(result -> {
                    Transaction transaction = result.getT1().getT2();
                    transaction.setTransactionType("PAGO");
                    transaction.setTransactionAmount(result.getT1().getT1().getAmount());
                    transaction.setDescription(result.getT1().getT1().getDescription());

                    if (result.getT1().getT1().getAmount() > result.getT2().getPrincipal().getBill().getBalance()) {
                        List<Acquisition> acquisitions = result.getT2().getAssociations();
                        Acquisition acquisition = acquisitions.stream().filter(acq -> acq.getBill().getBalance() > result.getT1().getT1().getAmount()).findFirst().orElseThrow(() -> new RuntimeException("The retire amount exceeds the available balance in yours accounts"));
                        Bill bill = acquisition.getBill();
                        bill.setBalance(bill.getBalance() - result.getT1().getT1().getAmount());
                        transaction.setBill(bill);
                    } else {
                        result.getT2().getPrincipal().getBill().setBalance(result.getT2().getPrincipal().getBill().getBalance() - result.getT1().getT1().getAmount());
                        transaction.setBill(result.getT2().getPrincipal().getBill());
                    }
                    return transactionService.createTransaction(transaction);
                })
                .zipWhen(updateDebit -> {
                    //update list
                    List<Acquisition> acquisitions = updateDebit.getT1().getT2().getAssociations().stream().peek(rx -> {
                        if (Objects.equals(rx.getBill().getAccountNumber(), updateDebit.getT2().getBill().getAccountNumber())) {
                            rx.setBill(updateDebit.getT2().getBill());
                        }
                    }).collect(Collectors.toList());
                    //validate is principal
                    Acquisition currentAcq = acquisitions.stream().filter(acquisition -> Objects.equals(acquisition.getBill().getAccountNumber(), updateDebit.getT2().getBill().getAccountNumber())).findFirst().orElse(null);
                    Boolean isPrincipal = updateDebit.getT1().getT2().getPrincipal().getIban().equals(currentAcq.getIban());
                    if (Boolean.TRUE.equals(isPrincipal)) {
                        updateDebit.getT1().getT2().getPrincipal().setBill(updateDebit.getT2().getBill());
                    }
                    Debit debit = new Debit();
                    debit.setAssociations(acquisitions);
                    debit.setPrincipal(updateDebit.getT1().getT2().getPrincipal());
                    debit.setCardNumber(updateDebit.getT1().getT2().getCardNumber());
                    return debitService.updateDebit(debit);
                })
                .zipWhen(data2 -> paymentService.findByAcquisition_Iban(data2.getT2().getPrincipal().getIban()))
                .flatMap(response -> {
                    response.getT2().setAmount(0.0);
                    response.getT2().getAcquisition().getBill().setBalance(response.getT2().getCreditLine());
                    response.getT2().setExpirationDate(LocalDateTime.now().plusDays(30));
                    return paymentService.update(response.getT2());
                })
                .zipWhen(payment -> billService.findByIban(payment.getAcquisition().getIban()))
                .zipWhen(res -> {
                    res.getT2().setBalance(res.getT1().getAcquisition().getBill().getBalance());
                    return billService.updateBill(res.getT2());
                })
                .flatMap(retireCreate ->
                        ServerResponse.created(URI.create("/payment/".concat(retireCreate.getT1().getT1().getId())))
                                .contentType(APPLICATION_JSON)
                                .bodyValue(retireCreate))
                .log()
                .onErrorResume(e -> Mono.error(new RuntimeException(e.getMessage())));
    }

    public Mono<ServerResponse> makePayment(ServerRequest request) {
        Mono<Payment> payment = request.bodyToMono(Payment.class);
        Payment paymentDto = new Payment();

        return payment
                .flatMap(paymentRequest -> {
                    paymentDto.setDescription(paymentRequest.getDescription());
                    paymentDto.setAmount(paymentRequest.getAmount());
                    return acquisitionService.findByBillAccountNumber(paymentRequest.getAcquisition().getBill().getAccountNumber());
                }).checkpoint("after consultation acquisition service web-client by account number")
                .flatMap(acquisition -> {
                    if (Objects.equals(acquisition.getBill().getBalance(), paymentDto.getCreditLine())){
                        return ServerResponse.created(URI.create("/payment/".concat(paymentDto.getId())))
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue("you are up to date on your payments");
                    }
                    acquisition.getBill().setBalance(paymentDto.getAmount() - acquisition.getBill().getBalance());
                    paymentDto.setAcquisition(acquisition);
                    paymentDto.setPaymentDate(LocalDateTime.now());
                    return paymentService.create(paymentDto);
                })
                .flatMap(p -> ServerResponse.created(URI.create("/payment/".concat(paymentDto.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(paymentDto));
    }

    public Mono<ServerResponse> update(ServerRequest request){
        Mono<Payment> payment = request.bodyToMono(Payment.class);

        return payment.zipWhen(paymentRequest -> paymentService.findByAcquisition_Iban(paymentRequest.getAcquisition().getIban()))
                .flatMap(paymentDB -> {
                    paymentDB.getT2().setAmount(paymentDB.getT1().getAmount());
                    paymentDB.getT2().setAcquisition(paymentDB.getT1().getAcquisition());
                    paymentDB.getT2().setExpirationDate(paymentDB.getT2().getExpirationDate().plusDays(30));
                    return paymentService.update(paymentDB.getT2());
                })
                .checkpoint("after update payment", true)
                .flatMap(p -> ServerResponse.created(URI.create("/payment/".concat(p.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(p))
                .onErrorResume(throwable -> Mono.error(new RuntimeException("update payment failed")));
    }
}
