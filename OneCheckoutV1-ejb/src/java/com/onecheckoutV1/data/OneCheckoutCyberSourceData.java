/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.onecheckoutV1.data;

import com.onecheckoutV1.ejb.util.OneCheckoutXPathReader;
import com.onecheckoutV1.type.OneCheckoutCyberSourceVerifyStatus;
import java.io.Serializable;
import javax.xml.xpath.XPathConstants;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 *
 * @author aditya
 */
public class OneCheckoutCyberSourceData implements Serializable {

    private static final long serialVersionUID = 1L;
    private String RequestID;
    private String InvoiceNumber;
    private String Reviewer;
    private String ReviewerComments;
    private String NotesDate;
    private String NoteAddedBy;
    private String Comment;
    private static final String root = "/CaseManagementOrderStatus/Update";
    private static final String OriginalDecisionPath = root + "/OriginalDecision";
    private static final String NewDecisionPath = root + "/NewDecision";
    private static final String ReviewerPath = root + "/Reviewer";
    private static final String ReviewerCommentsPath = root + "/ReviewerComments";

    private OneCheckoutCyberSourceVerifyStatus OriginalDecision;
    private OneCheckoutCyberSourceVerifyStatus NewDecision;

    private String CYBSXML;
//    static final String notes = "/CaseManagementOrderStatus/Update";
//    static final String root = "/CaseManagementOrderStatus/Update";
//    static final String root = "/CaseManagementOrderStatus/Update";
//    static final String root = "/CaseManagementOrderStatus/Update";

    public OneCheckoutCyberSourceData(String xml) {

        try {
            this.CYBSXML = xml;
            OneCheckoutXPathReader xpath = new OneCheckoutXPathReader(xml);

            Node n = (Node) xpath.query(OneCheckoutCyberSourceData.root,XPathConstants.NODE);
            NamedNodeMap m = n.getAttributes();
            Node merchantRefNode = m.getNamedItem("MerchantReferenceNumber");
            this.InvoiceNumber = merchantRefNode.getNodeValue();
            Node requestId = m.getNamedItem("RequestID");
            this.RequestID = requestId.getNodeValue();

            String oriDec = (String) xpath.query(OriginalDecisionPath, XPathConstants.STRING);
            this.setOriginalDecision(oriDec);

            String newDec = (String) xpath.query(NewDecisionPath, XPathConstants.STRING);
            this.setNewDecision(newDec);

            this.Reviewer = (String) xpath.query(ReviewerPath, XPathConstants.STRING);
            this.ReviewerComments = (String) xpath.query(ReviewerCommentsPath, XPathConstants.STRING);

        } catch (Throwable e) {
            e.printStackTrace();
            //return null;
        }

    }

    /**
     * @param RequestID the RequestID to set
     */
    public void setRequestID(String RequestID) {
        this.RequestID = RequestID;
    }

    /**
     * @param InvoiceNumber the InvoiceNumber to set
     */
    public void setInvoiceNumber(String InvoiceNumber) {
        this.InvoiceNumber = InvoiceNumber;
    }

    /**
     * @param OriginalDecision the OriginalDecision to set
     */
    private void setOriginalDecision(String cyberSourceStatus) {

        if (cyberSourceStatus==null || cyberSourceStatus.isEmpty())
            this.OriginalDecision = OneCheckoutCyberSourceVerifyStatus.UNDEFINED;
        else if (cyberSourceStatus.equalsIgnoreCase(OneCheckoutCyberSourceVerifyStatus.ACCEPT.name()))
            this.OriginalDecision = OneCheckoutCyberSourceVerifyStatus.ACCEPT;
        else if (cyberSourceStatus.equalsIgnoreCase(OneCheckoutCyberSourceVerifyStatus.REJECT.name()))
            this.OriginalDecision = OneCheckoutCyberSourceVerifyStatus.REJECT;
        else if (cyberSourceStatus.equalsIgnoreCase(OneCheckoutCyberSourceVerifyStatus.REVIEW.name()))
            this.OriginalDecision = OneCheckoutCyberSourceVerifyStatus.REVIEW;
        else
            this.OriginalDecision = OneCheckoutCyberSourceVerifyStatus.UNDEFINED;

    }

    /**
     * @param NewDecision the NewDecision to set
     */
    private void setNewDecision(String cyberSourceStatus) {


        if (cyberSourceStatus==null || cyberSourceStatus.isEmpty())
            this.NewDecision = OneCheckoutCyberSourceVerifyStatus.UNDEFINED;
        else if (cyberSourceStatus.equalsIgnoreCase(OneCheckoutCyberSourceVerifyStatus.ACCEPT.name()))
            this.NewDecision = OneCheckoutCyberSourceVerifyStatus.ACCEPT;
        else if (cyberSourceStatus.equalsIgnoreCase(OneCheckoutCyberSourceVerifyStatus.REJECT.name()))
            this.NewDecision = OneCheckoutCyberSourceVerifyStatus.REJECT;
        else if (cyberSourceStatus.equalsIgnoreCase(OneCheckoutCyberSourceVerifyStatus.REVIEW.name()))
            this.NewDecision = OneCheckoutCyberSourceVerifyStatus.REVIEW;
        else
            this.NewDecision = OneCheckoutCyberSourceVerifyStatus.UNDEFINED;

    }

    /**
     * @param Reviewer the Reviewer to set
     */
    public void setReviewer(String Reviewer) {
        this.Reviewer = Reviewer;
    }

    /**
     * @param ReviewerComments the ReviewerComments to set
     */
    public void setReviewerComments(String ReviewerComments) {
        this.ReviewerComments = ReviewerComments;
    }

    /**
     * @param NotesDate the NotesDate to set
     */
    public void setNotesDate(String NotesDate) {
        this.NotesDate = NotesDate;
    }

    /**
     * @param NoteAddedBy the NoteAddedBy to set
     */
    public void setNoteAddedBy(String NoteAddedBy) {
        this.NoteAddedBy = NoteAddedBy;
    }

    /**
     * @param Comment the Comment to set
     */
    public void setComment(String Comment) {
        this.Comment = Comment;
    }

    /**
     * @return the CYBSXML
     */
    public String getCYBSXML() {
        return CYBSXML;
    }

    /**
     * @param CYBSXML the CYBSXML to set
     */
    public void setCYBSXML(String CYBSXML) {
        this.CYBSXML = CYBSXML;
    }

    /**
     * @return the RequestID
     */
    public String getRequestID() {
        return RequestID;
    }

    /**
     * @return the InvoiceNumber
     */
    public String getInvoiceNumber() {
        return InvoiceNumber;
    }

    /**
     * @return the OriginalDecision
     */
    public OneCheckoutCyberSourceVerifyStatus getOriginalDecision() {
        return OriginalDecision;
    }

    /**
     * @return the NewDecision
     */
    public OneCheckoutCyberSourceVerifyStatus getNewDecision() {
        return NewDecision;
    }

    /**
     * @return the Reviewer
     */
    public String getReviewer() {
        return Reviewer;
    }

    /**
     * @return the ReviewerComments
     */
    public String getReviewerComments() {
        return ReviewerComments;
    }

    /**
     * @return the NotesDate
     */
    public String getNotesDate() {
        return NotesDate;
    }

    /**
     * @return the NoteAddedBy
     */
    public String getNoteAddedBy() {
        return NoteAddedBy;
    }

    /**
     * @return the Comment
     */
    public String getComment() {
        return Comment;
    }

    /**
     * @return the root
     */
    public static String getRoot() {
        return root;
    }

    /**
     * @return the OriginalDecisionPath
     */
    public static String getOriginalDecisionPath() {
        return OriginalDecisionPath;
    }

    /**
     * @return the NewDecisionPath
     */
    public static String getNewDecisionPath() {
        return NewDecisionPath;
    }

    /**
     * @return the ReviewerPath
     */
    public static String getReviewerPath() {
        return ReviewerPath;
    }

    /**
     * @return the ReviewerCommentsPath
     */
    public static String getReviewerCommentsPath() {
        return ReviewerCommentsPath;
    }

}