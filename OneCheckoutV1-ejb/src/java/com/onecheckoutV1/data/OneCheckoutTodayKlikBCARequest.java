/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.onecheckoutV1.data;

import com.onecheckoutV1.ejb.exception.OneCheckoutInvalidInputException;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import java.io.Serializable;

/**
 *
 * @author aditya
 */
public class OneCheckoutTodayKlikBCARequest  implements Serializable {

    private static final long serialVersionUID = 1L;

    private String USERIDKLIKBCA;


    public OneCheckoutTodayKlikBCARequest() {
        USERIDKLIKBCA = null;
    }

    /**
     * @return the USERIDKLIKBCA
     */
    public String getUSERIDKLIKBCA() {
        return USERIDKLIKBCA;
    }

    /**
     * @param USERIDKLIKBCA the USERIDKLIKBCA to set
     */
    public void setUSERIDKLIKBCA(String userIDKlikBCA) {

        try {
            if (userIDKlikBCA!=null)
                userIDKlikBCA = userIDKlikBCA.toUpperCase();
            this.USERIDKLIKBCA = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 12, "USERIDKLIKBCA", userIDKlikBCA, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);

        } catch (OneCheckoutInvalidInputException iv) {
            this.USERIDKLIKBCA = null;
        }
    }

}
