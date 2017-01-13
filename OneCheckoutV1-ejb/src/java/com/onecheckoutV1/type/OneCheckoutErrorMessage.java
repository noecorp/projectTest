/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.type;

/**
 *
 * @author hafizsjafioedin
 */
public enum OneCheckoutErrorMessage {
    UNKNOWN("5555"),

    PAYMENTCHANNEL_NOT_REGISTERED("5501"),
    PAYMENTCHANNEL_MERCHANT_DISABLED("5502"),
    MAXIMUM_3_TIMES_ATTEMPT("5503"),
    WORDS_DOES_NOT_MATCH("5504"),
    ERROR_VALIDATE_INPUT("5505"),
    NOTIFY_FAILED("5506"),
    PAYMENT_HAS_NOT_BEEN_PROCCED("5507"),
    RE_ENTER_TRANSACTION("5508"),//payment has not been initiated;
    EXPIRED_TIME_EXCEED("5509"),
    CANCEL_BY_USER("5510"),
    NOT_YET_PAID("5511"),
    INSUFFICIENT_PARAMS("5512"),
    VOID_BY_COREPAYMENT("5513"),
    HIGHRISK_OR_REJECT("5514"),
    DUPLICATE_PNR("5515"),
    TRANSACTION_NOT_FOUND("5516"),
    ERROR_CONNECT_TO_CORE("5517"),
    ERROR_PARSING_RESPONSE("5518"),
    NOT_CONTINUE_FROM_ACS("5519"),
    FAILED_REGISTER_ACCOUNT_BILLING("5520"),
    INVALID_MERCHANT("5521"),
    RATE_NOT_FOUND("5522"),
    CANNOT_GET_CHECKSTATUS("5523"),
    CANNOT_GET_REVERSAL_TO_CORE("5524"),
    TRANSACTION_CAN_NOT_BE_PROCCED("5525"),
    NOT_CONTINUE_FROM_ACQUIRER("5526"),
    OFF_US_INSTALLMENT_PROCESS("5527"),
    SUCCESS("0000"),
    FAILED_REFUND("5568"),
    AMOUNT_REFUND_NOT_VALID("5569"),
    INVALID_PARAMETER("5548"),
    DOMPETKU_SUCCESS_PROCESS("0"),
    INIT_VA("INIT"),
    //  REVERSAL("5513");

    FAILED_TRANSACTION_VIA_SCHEDULER("5520"),
    OFF_US_REWARDS_PROCESS("5554"),
    OFF_US_INSTALLMENT_AND_REWARDS_PROCESS("5555"),
    TIMEOUT("5536"),
    INTERNAL_SERVER_ERROR("5530");
    String type;
    private static final long serialVersionUID = 1L;

    OneCheckoutErrorMessage(String type) {
        this.type = type;
    }

    public String value() {
        return type;
    }

    public static OneCheckoutErrorMessage findType(String c) {
        OneCheckoutErrorMessage[] types = OneCheckoutErrorMessage.values();
        for (OneCheckoutErrorMessage ttype : types) {
            if (ttype.value().equals(c)) {
                return ttype;
            }
        }
        return null;
    }
        
}
