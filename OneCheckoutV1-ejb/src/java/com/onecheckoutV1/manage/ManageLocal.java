/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.manage;

import javax.ejb.Local;

/**
 *
 * @author Opiks
 */
@Local
public interface ManageLocal {

    public com.onechekoutv1.dto.MerchantActivity startMerchantActivity(com.onecheckoutV1.ejb.helper.RequestHelper requestHelper, com.onechekoutv1.dto.Merchants merchants, com.onecheckoutV1.enums.EActivity eActivity, java.util.HashMap<String, String> data);

    public void endMerchantActtivity(com.onecheckoutV1.ejb.helper.RequestHelper requestHelper, boolean status);

    public <T extends com.onecheckoutV1.ejb.helper.RequestHelper> boolean manageMerchantActivity(T requestHelper, com.onechekoutv1.dto.MerchantActivity merchantActivity);

    public void processTemplate(com.onecheckoutV1.ejb.helper.RequestHelper requestHelper, java.io.Writer out);

    public com.onecheckoutV1.ejb.helper.RequestHelper readRequestHelperFile(java.lang.String fullPathFileName);

    public <T extends com.onecheckoutV1.ejb.helper.RequestHelper> boolean createRequestHelperFile(java.lang.String fullPathFileName, T requestHelper);
}
