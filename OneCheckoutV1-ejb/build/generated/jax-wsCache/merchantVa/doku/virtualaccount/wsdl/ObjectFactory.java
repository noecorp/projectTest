
package doku.virtualaccount.wsdl;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the doku.virtualaccount.wsdl package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _GetTransaction_QNAME = new QName("http://wsdl.virtualaccount.doku/", "GetTransaction");
    private final static QName _GetVirtualAccountResponse_QNAME = new QName("http://wsdl.virtualaccount.doku/", "GetVirtualAccountResponse");
    private final static QName _RequestVirtualAccount_QNAME = new QName("http://wsdl.virtualaccount.doku/", "RequestVirtualAccount");
    private final static QName _GetVirtualAccount_QNAME = new QName("http://wsdl.virtualaccount.doku/", "GetVirtualAccount");
    private final static QName _GetTransactionResponse_QNAME = new QName("http://wsdl.virtualaccount.doku/", "GetTransactionResponse");
    private final static QName _RequestVirtualAccountResponse_QNAME = new QName("http://wsdl.virtualaccount.doku/", "RequestVirtualAccountResponse");
    private final static QName _RegisterVirtualAccountResponse_QNAME = new QName("http://wsdl.virtualaccount.doku/", "RegisterVirtualAccountResponse");
    private final static QName _RegisterVirtualAccount_QNAME = new QName("http://wsdl.virtualaccount.doku/", "RegisterVirtualAccount");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: doku.virtualaccount.wsdl
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link RegisterVirtualAccountResponse }
     * 
     */
    public RegisterVirtualAccountResponse createRegisterVirtualAccountResponse() {
        return new RegisterVirtualAccountResponse();
    }

    /**
     * Create an instance of {@link GetVirtualAccount }
     * 
     */
    public GetVirtualAccount createGetVirtualAccount() {
        return new GetVirtualAccount();
    }

    /**
     * Create an instance of {@link GetTransactionResponse }
     * 
     */
    public GetTransactionResponse createGetTransactionResponse() {
        return new GetTransactionResponse();
    }

    /**
     * Create an instance of {@link RegisterVirtualAccount }
     * 
     */
    public RegisterVirtualAccount createRegisterVirtualAccount() {
        return new RegisterVirtualAccount();
    }

    /**
     * Create an instance of {@link GetVirtualAccountResponse }
     * 
     */
    public GetVirtualAccountResponse createGetVirtualAccountResponse() {
        return new GetVirtualAccountResponse();
    }

    /**
     * Create an instance of {@link RequestVirtualAccount }
     * 
     */
    public RequestVirtualAccount createRequestVirtualAccount() {
        return new RequestVirtualAccount();
    }

    /**
     * Create an instance of {@link GetTransaction }
     * 
     */
    public GetTransaction createGetTransaction() {
        return new GetTransaction();
    }

    /**
     * Create an instance of {@link RequestVirtualAccountResponse }
     * 
     */
    public RequestVirtualAccountResponse createRequestVirtualAccountResponse() {
        return new RequestVirtualAccountResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetTransaction }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://wsdl.virtualaccount.doku/", name = "GetTransaction")
    public JAXBElement<GetTransaction> createGetTransaction(GetTransaction value) {
        return new JAXBElement<GetTransaction>(_GetTransaction_QNAME, GetTransaction.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetVirtualAccountResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://wsdl.virtualaccount.doku/", name = "GetVirtualAccountResponse")
    public JAXBElement<GetVirtualAccountResponse> createGetVirtualAccountResponse(GetVirtualAccountResponse value) {
        return new JAXBElement<GetVirtualAccountResponse>(_GetVirtualAccountResponse_QNAME, GetVirtualAccountResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RequestVirtualAccount }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://wsdl.virtualaccount.doku/", name = "RequestVirtualAccount")
    public JAXBElement<RequestVirtualAccount> createRequestVirtualAccount(RequestVirtualAccount value) {
        return new JAXBElement<RequestVirtualAccount>(_RequestVirtualAccount_QNAME, RequestVirtualAccount.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetVirtualAccount }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://wsdl.virtualaccount.doku/", name = "GetVirtualAccount")
    public JAXBElement<GetVirtualAccount> createGetVirtualAccount(GetVirtualAccount value) {
        return new JAXBElement<GetVirtualAccount>(_GetVirtualAccount_QNAME, GetVirtualAccount.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetTransactionResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://wsdl.virtualaccount.doku/", name = "GetTransactionResponse")
    public JAXBElement<GetTransactionResponse> createGetTransactionResponse(GetTransactionResponse value) {
        return new JAXBElement<GetTransactionResponse>(_GetTransactionResponse_QNAME, GetTransactionResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RequestVirtualAccountResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://wsdl.virtualaccount.doku/", name = "RequestVirtualAccountResponse")
    public JAXBElement<RequestVirtualAccountResponse> createRequestVirtualAccountResponse(RequestVirtualAccountResponse value) {
        return new JAXBElement<RequestVirtualAccountResponse>(_RequestVirtualAccountResponse_QNAME, RequestVirtualAccountResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RegisterVirtualAccountResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://wsdl.virtualaccount.doku/", name = "RegisterVirtualAccountResponse")
    public JAXBElement<RegisterVirtualAccountResponse> createRegisterVirtualAccountResponse(RegisterVirtualAccountResponse value) {
        return new JAXBElement<RegisterVirtualAccountResponse>(_RegisterVirtualAccountResponse_QNAME, RegisterVirtualAccountResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RegisterVirtualAccount }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://wsdl.virtualaccount.doku/", name = "RegisterVirtualAccount")
    public JAXBElement<RegisterVirtualAccount> createRegisterVirtualAccount(RegisterVirtualAccount value) {
        return new JAXBElement<RegisterVirtualAccount>(_RegisterVirtualAccount_QNAME, RegisterVirtualAccount.class, null, value);
    }

}
