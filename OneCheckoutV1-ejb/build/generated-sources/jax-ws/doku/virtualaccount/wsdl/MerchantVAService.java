
package doku.virtualaccount.wsdl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.1.3-b02-
 * Generated source version: 2.1
 * 
 */
@WebServiceClient(name = "MerchantVAService", targetNamespace = "http://wsdl.virtualaccount.doku/", wsdlLocation = "file:/apps/onecheckoutv1/wsdl/merchantVa.wsdl")
public class MerchantVAService
    extends Service
{

    private final static URL MERCHANTVASERVICE_WSDL_LOCATION;
    private final static Logger logger = Logger.getLogger(doku.virtualaccount.wsdl.MerchantVAService.class.getName());

    static {
        URL url = null;
        try {
            URL baseUrl;
            baseUrl = doku.virtualaccount.wsdl.MerchantVAService.class.getResource(".");
            url = new URL(baseUrl, "file:/apps/onecheckoutv1/wsdl/merchantVa.wsdl");
        } catch (MalformedURLException e) {
            logger.warning("Failed to create URL for the wsdl Location: 'file:/apps/onecheckoutv1/wsdl/merchantVa.wsdl', retrying as a local file");
            logger.warning(e.getMessage());
        }
        MERCHANTVASERVICE_WSDL_LOCATION = url;
    }

    public MerchantVAService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public MerchantVAService() {
        super(MERCHANTVASERVICE_WSDL_LOCATION, new QName("http://wsdl.virtualaccount.doku/", "MerchantVAService"));
    }

    /**
     * 
     * @return
     *     returns MerchantVA
     */
    @WebEndpoint(name = "MerchantVAPort")
    public MerchantVA getMerchantVAPort() {
        return super.getPort(new QName("http://wsdl.virtualaccount.doku/", "MerchantVAPort"), MerchantVA.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns MerchantVA
     */
    @WebEndpoint(name = "MerchantVAPort")
    public MerchantVA getMerchantVAPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://wsdl.virtualaccount.doku/", "MerchantVAPort"), MerchantVA.class, features);
    }

}
