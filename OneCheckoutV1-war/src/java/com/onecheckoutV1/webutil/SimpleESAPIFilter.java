/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.webutil;

import java.io.IOException;
import java.util.Arrays;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;

/**
 *
 * @author hafizsjafioedin
 */
public class SimpleESAPIFilter  implements Filter {

	private static final Logger logger = ESAPI.getLogger( "SwingsetFilter" );

	private static final String[] ignore = { "password" };

	/**
	 * Called by the web container to indicate to a filter that it is being
	 * placed into service. The servlet container calls the init method exactly
	 * once after instantiating the filter. The init method must complete
	 * successfully before the filter is asked to do any filtering work.
	 *
	 * @param filterConfig
	 *            configuration object
	 */
	public void init(FilterConfig filterConfig) {
        if ( ESAPI.securityConfiguration().getResourceDirectory() == null ) {
            System.out.println( "====ESAPI.properties ready to define====");
            ESAPI.securityConfiguration().setResourceDirectory( "/apps/ESAPI/resources/");
        }
        else if (!ESAPI.securityConfiguration().getResourceDirectory().equals("/apps/ESAPI/resources/"))   {
            System.out.println( "====ESAPI.properties already define but different path ====");
            ESAPI.securityConfiguration().setResourceDirectory( "/apps/ESAPI/resources/");
        }
        else    {
            System.out.println( "====ESAPI.properties already define ====");
        }
	}

	/**
	 * The doFilter method of the Filter is called by the container each time a
	 * request/response pair is passed through the chain due to a client request
	 * for a resource at the end of the chain. The FilterChain passed in to this
	 * method allows the Filter to pass on the request and response to the next
	 * entity in the chain.
	 *
	 * @param request
	 *            Request object to be processed
	 * @param response
	 *            Response object
	 * @param chain
	 *            current FilterChain
	 * @exception IOException
	 *                if any occurs
	 * @throws ServletException
	 */
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) resp;

		try {
			// register request and response in ESAPI (usually done through login)
			ESAPI.httpUtilities().setCurrentHTTP(request, response);

			// log this request, obfuscating any parameter named password
			ESAPI.httpUtilities().logHTTPRequest(ESAPI.httpUtilities().getCurrentRequest(), logger, Arrays.asList(ignore));

			// forward this request on to the web application
			chain.doFilter(request, response);
		} catch (Exception e) {
			request.setAttribute("message", e.getMessage() );
		} finally {
			// VERY IMPORTANT
			// clear out the ThreadLocal variables in the authenticator
			// some containers could possibly reuse this thread without clearing the User
			ESAPI.authenticator().clearCurrent();
		}
	}

	/**
	 * Called by the web container to indicate to a filter that it is being
	 * taken out of service. This method is only called once all threads within
	 * the filter's doFilter method have exited or after a timeout period has
	 * passed. After the web container calls this method, it will not call the
	 * doFilter method again on this instance of the filter.
	 */
	public void destroy() {
		// finalize
	}
}
