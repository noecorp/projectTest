/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.util;

import java.io.ByteArrayInputStream;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

/**
 *
 * @author hafizsjafioedin
 */
public class OneCheckoutXPathReader {

    private String xml = "";
    private Document doc = null;
    private XPath xPath = null;
    private XPathExpression xPathExpression = null;
    private Log log = LogFactory.getLog(getClass());

    public OneCheckoutXPathReader(String xml){
        this.xml = xml;
        initObject();
    }

    private void initObject(){

        try{

            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes()));
            xPath = XPathFactory.newInstance().newXPath();

        }catch(Exception e){
            log.info("Error " + e.toString());
        }
    }

    public Object query(String expression, QName returnType){

        try{

            xPathExpression = xPath.compile(expression);

            return xPathExpression.evaluate(doc,returnType);

        }catch(Exception e){
            log.info("Error " + e.toString());
            return null;
        }
    }        
    
}
