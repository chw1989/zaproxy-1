/*
 *
 * Paros and its related class files.
 * 
 * Paros is an HTTP/HTTPS proxy for assessing web application security.
 * Copyright (C) 2003-2004 Chinotec Technologies Company
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Clarified Artistic License
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Clarified Artistic License for more details.
 * 
 * You should have received a copy of the Clarified Artistic License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
// ZAP: 2012/03/14 Changed to use the internationalised string.
// ZAP: 2013/03/03 Issue 546: Remove all template Javadoc comments
// ZAP: 2013/11/28 Issue 923: Allow individual rule thresholds and strengths to be set via GUI
package org.zaproxy.zap.extension.ascan;

import java.awt.Frame;
import java.awt.HeadlessException;
import java.util.ArrayList;
import java.util.List;

import org.parosproxy.paros.Constant;
import org.parosproxy.paros.control.Control;
import org.parosproxy.paros.core.scanner.Category;
import org.parosproxy.paros.core.scanner.ScannerParam;
import org.parosproxy.paros.model.Model;
import org.parosproxy.paros.model.OptionsParam;
import org.parosproxy.paros.view.AbstractParamDialog;
import org.parosproxy.paros.view.AbstractParamPanel;
import org.zaproxy.zap.extension.pscan.ExtensionPassiveScan;

public class PolicyDialog extends AbstractParamDialog {

    private static final long serialVersionUID = 1L;
    private static final String POLICY = Constant.messages.getString("ascan.policy.dialog.title"); // ZAP: i18n
    private static final String[] ROOT = {};
    private PolicyAllCategoryPanel policyAllCategoryPanel = null;
    private List<AbstractParamPanel> additionalPanels = new ArrayList<>();

    /**
     * 
     */
    public PolicyDialog() {
        super();
        initialize();

    }

    public PolicyDialog(Frame parent) throws HeadlessException {
        super(parent, true, POLICY, Constant.messages.getString("ascan.policy.title"));
        initialize();
    }

    /**
     * 
     */
    private void initialize() {
        this.setTitle(POLICY);
        this.setSize(750, 420); // TODO: This shouldn't be statically defined.
        addParamPanel(null, getPolicyAllCategoryPanel(), false);
        
        for (int i = 0; i < Category.getAllNames().length; i++) {
            addParamPanel(ROOT, Category.getName(i), 
            		new PolicyCategoryPanel(i, Control.getSingleton().getPluginFactory().getAllPlugin()), true);
        }
        
        getBtnCancel().setEnabled(false);
        this.setFooter(Constant.messages.getString("ascan.policy.dialog.footer"));
    }

    /**
     * 
     * @param panel 
     */
    public void addPolicyPanel(AbstractParamPanel panel) {
        this.additionalPanels.add(panel);
        addParamPanel(ROOT, panel.getName(), panel, true);
    }

    /**
     * This method initializes policyAllCategoryPanel
     *
     * @return org.parosproxy.paros.extension.scanner.PolicyAllCategoryPanel
     */
    public PolicyAllCategoryPanel getPolicyAllCategoryPanel() {
        if (policyAllCategoryPanel == null) {
            OptionsParam options = Model.getSingleton().getOptionsParam();
            ScannerParam param = (ScannerParam)options.getParamSet(ScannerParam.class);
            ExtensionPassiveScan pscan = 
            		(ExtensionPassiveScan) Control.getSingleton().getExtensionLoader().getExtension(
            				ExtensionPassiveScan.NAME);
        	
            policyAllCategoryPanel = 
            		new PolicyAllCategoryPanel(
            				options, param, Control.getSingleton().getPluginFactory(), pscan);
            policyAllCategoryPanel.setName(Constant.messages.getString("ascan.policy.title"));
        }
        
        return policyAllCategoryPanel;
    }
}
