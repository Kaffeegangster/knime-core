/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * Created on Apr 22, 2013 by Berthold
 */
package org.knime.workbench.editor2;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.knime.core.node.AbstractNodeView.ViewableModel;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.web.WebTemplate;
import org.knime.core.node.web.WebViewContent;
import org.knime.core.node.wizard.AbstractWizardNodeView;
import org.knime.core.node.wizard.WizardNode;
import org.knime.core.node.wizard.WizardViewCreator;
import org.knime.core.wizard.SubnodeViewableModel;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.editor2.ElementRadioSelectionDialog.RadioItem;

/**
 * Standard implementation for interactive views which are launched on the client side via an integrated browser. They
 * only have indirect access to the NodeModel via get and setViewContent methods and therefore simulate the behavior of
 * the same view in the WebPortal.
 *
 * @author B. Wiswedel, M. Berthold, Th. Gabriel, C. Albrecht
 * @param <T> requires a {@link NodeModel} implementing {@link WizardNode} as well
 * @param <REP> the {@link WebViewContent} implementation used
 * @param <VAL>
 * @since 2.9
 */
public final class WizardNodeView<T extends ViewableModel & WizardNode<REP, VAL>,
        REP extends WebViewContent, VAL extends WebViewContent>
        extends AbstractWizardNodeView<T, REP, VAL> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(WizardNodeView.class);

    private Shell m_shell;

    private Browser m_browser;
    private BrowserFunction m_viewRequestCallback;
    private BrowserFunction m_updateRequestStatusCallback;
    private BrowserFunction m_cancelRequestCallback;
    private BrowserFunction m_isPushSupportedCallback;
    private boolean m_viewSet = false;
    private boolean m_initialized = false;
    private String m_title;

    /**
     * @param nodeModel the underlying model
     * @since 2.10
     */
    public WizardNodeView(final T nodeModel) {
        super(nodeModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void modelChanged() {
        Display display = getDisplay();
        if (display == null) {
            // view most likely disposed
            return;
        }
        display.asyncExec(new Runnable() {

            @Override
            public void run() {
                if (m_browser != null && !m_browser.isDisposed()) {
                    synchronized (m_browser) {
                        setBrowserURL();
                    }
                }
            }
        });
    }

    private Display getDisplay() {
        //Display display = new Display();
        Display display = Display.getCurrent();
        if (display == null && m_browser != null && !m_browser.isDisposed()) {
            display = m_browser.getDisplay();
        }
        return display;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void callOpenView(final String title) {
        callOpenView(title, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void callOpenView(final String title, final Rectangle knimeWindowBounds) {
        m_title = (title == null ? "View" : title);

        Display display = getDisplay();
        m_shell = new Shell(display, SWT.SHELL_TRIM);
        m_shell.setText(m_title);

        m_shell.setImage(ImageRepository.getIconImage(SharedImages.KNIME));
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        m_shell.setLayout(layout);

        m_browser = new Browser(m_shell, SWT.NONE);
        m_browser.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
        m_browser.setText(getViewCreator().createMessageHTML("Loading view..."), true);

        Composite buttonComposite = new Composite(m_shell, SWT.NONE);
        buttonComposite.setLayoutData(new GridData(GridData.END, GridData.END, false, false));
        buttonComposite.setLayout(new RowLayout());

        ToolBar toolBar = new ToolBar(buttonComposite, SWT.BORDER | SWT.HORIZONTAL);
        ToolItem resetButton = new ToolItem(toolBar, SWT.PUSH);
        resetButton.setText("Reset");
        resetButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                // Could only call if actual settings have been changed,
                // however there might be things in views that one can change
                // which do not get saved, then it's nice to trigger the event anyways.
                /*if (checkSettingsChanged()) {*/
                    modelChanged();
                /*}*/
            }
        });
        new ToolItem(toolBar, SWT.SEPARATOR);
        ToolItem applyButton = new ToolItem(toolBar, SWT.DROP_DOWN);
        applyButton.setText("Apply");
        applyButton.setToolTipText("Applies the current settings and triggers a re-execute of the node.");
        DropdownSelectionListener applyListener = new DropdownSelectionListener(applyButton);
        String aTTooltip = "Applies the current settings and triggers a re-execute of the node.";
        applyListener.add("Apply temporarily", aTTooltip, new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                applyTriggered(false);
            }
        });
        String nDTooltip = "Applies the current settings as the node default settings and triggers a re-execute of the node.";
        applyListener.add("Apply as new default", nDTooltip, new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                applyTriggered(true);
            }
        });
        applyButton.addSelectionListener(applyListener);

        applyButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (e.detail != SWT.ARROW) {
                    if (checkSettingsChanged()) {
                        showApplyDialog();
                    }
                }
            }
        });

        new ToolItem(toolBar, SWT.SEPARATOR);

        ToolItem closeButton = new ToolItem(toolBar, SWT.DROP_DOWN);
        closeButton.setText("Close");
        closeButton.setToolTipText("Closes the view.");
        DropdownSelectionListener closeListener = new DropdownSelectionListener(closeButton);
        String cDTooltip = "Closes the view and discards any changes made.";
        closeListener.add("Close && Discard", cDTooltip, new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                m_shell.dispose();
            }
        });
        String cATooltip = "Closes the view, applies the current settings and triggers a re-execute of the node.";
        closeListener.add("Close && Apply temporarily", cATooltip, new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (applyTriggered(false)) {
                    m_shell.dispose();
                }
            }
        });
        String cTTooltip = "Closes the view, applies the current settings as node defaults and triggers a re-execute of the node.";
        closeListener.add("Close && Apply as new default", cTTooltip, new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (applyTriggered(true)) {
                    m_shell.dispose();
                }
            }
        });
        closeButton.addSelectionListener(closeListener);

        closeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (e.detail != SWT.ARROW) {
                    if (checkSettingsChanged()) {
                        /*MessageDialogWithToggle dialog =
                            MessageDialogWithToggle.openOkCancelConfirm(m_browser.getShell(), "Discard Settings",
                                "View settings have changed and will be lost. Do you want to continue?",
                                "Do not ask again", false, null, null);*/
                        if  (!showCloseDialog()) {
                            return;
                        }
                    }
                    m_shell.dispose();
                }
            }
        });

        m_shell.addListener(SWT.Close, new Listener() {

            @Override
            public void handleEvent(final Event event) {
                if (checkSettingsChanged()) {
                    event.doit = showCloseDialog();
                }
            }
        });

        //TODO: make initial size dynamic
        m_shell.setSize(1024, 768);

        Point middle = new Point(knimeWindowBounds.width / 2, knimeWindowBounds.height / 2);
        // Left upper point for window
        Point newLocation = new Point(middle.x - (m_shell.getSize().x / 2) + knimeWindowBounds.x,
                                      middle.y - (m_shell.getSize().y / 2) + knimeWindowBounds.y);
        m_shell.setLocation(newLocation.x, newLocation.y);
        m_shell.addDisposeListener(new org.eclipse.swt.events.DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                callCloseView();
            }
        });
        m_shell.open();

        display.asyncExec(new Runnable() {

            @Override
            public void run() {
                m_browser.addProgressListener(new ProgressListener() {

                    @Override
                    public void completed(final ProgressEvent event) {
                        if (m_viewSet && !m_initialized) {
                            WizardNode<REP, VAL> model = getModel();
                            WizardViewCreator<REP, VAL> creator = getViewCreator();
                            String initCall =
                                creator.createInitJSViewMethodCall(model.getViewRepresentation(), model.getViewValue());
                            initCall = creator.wrapInTryCatch(initCall);
                            //The execute call might fire the completed event again in some browsers!
                            m_initialized = true;
                            m_browser.execute(initCall);
                        }
                    }

                    @Override
                    public void changed(final ProgressEvent event) {
                        // do nothing
                    }
                });
                setBrowserURL();
                m_viewRequestCallback = new ViewRequestFunction(m_browser, "knimeViewRequest");
                m_updateRequestStatusCallback = new UpdateRequestStatusFunction(m_browser, "knimeUpdateRequestStatus");
                m_cancelRequestCallback = new CancelRequestFunction(m_browser, "knimeCancelRequest");
                m_isPushSupportedCallback = new PushSupportedFunction(m_browser, "knimePushSupported");
            }
        });

    }

    class DropdownSelectionListener extends SelectionAdapter {

        private Menu menu;

        public DropdownSelectionListener(final ToolItem drop) {
          menu = new Menu(drop.getParent().getShell(), SWT.POP_UP);
        }

        public void add(final String text, final String tooltip, final SelectionAdapter selectionListener) {
          //MenuItem menuItem = new MenuItem(menu, SWT.NONE);
          MenuItem menuItem = new MenuItem(menu, SWT.CASCADE);
          menuItem.setText(text);
          menuItem.setToolTipText(tooltip);
          menuItem.addSelectionListener(selectionListener);
        }

        @Override
        public void widgetSelected(final SelectionEvent event) {
          if (event.detail == SWT.ARROW) {
            ToolItem item = (ToolItem) event.widget;
            org.eclipse.swt.graphics.Rectangle rect = item.getBounds();
            org.eclipse.swt.graphics.Point pt = item.getParent().toDisplay(new org.eclipse.swt.graphics.Point(rect.x, rect.y));
            menu.setLocation(pt.x, pt.y + rect.height);
            menu.setVisible(true);
          }
        }
      }

    private void setBrowserURL() {
        try {
            m_initialized = false;
            File src = getViewSource();
            if (src != null && src.exists()) {
                m_browser.setUrl(getViewSource().getAbsolutePath());
                m_viewSet = true;
            } else {
                m_browser.setText(getViewCreator().createMessageHTML("No data to display"));
                m_viewSet = false;
            }
        } catch (Exception e) {
            m_browser.setText(getViewCreator().createMessageHTML(e.getMessage()));
            m_viewSet = false;
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void closeView() {
        if (m_viewRequestCallback != null && !m_viewRequestCallback.isDisposed()) {
            m_viewRequestCallback.dispose();
        }
        if (m_updateRequestStatusCallback != null && !m_updateRequestStatusCallback.isDisposed()) {
            m_updateRequestStatusCallback.dispose();
        }
        if (m_cancelRequestCallback != null && !m_cancelRequestCallback.isDisposed()) {
            m_cancelRequestCallback.dispose();
        }
        if (m_isPushSupportedCallback != null && !m_isPushSupportedCallback.isDisposed()) {
            m_isPushSupportedCallback.dispose();
        }
        if (m_shell != null && !m_shell.isDisposed()) {
            m_shell.dispose();
        }
        m_shell = null;
        m_browser = null;
        m_viewRequestCallback = null;
        m_updateRequestStatusCallback = null;
        m_cancelRequestCallback = null;
        m_isPushSupportedCallback = null;
        m_viewSet = false;
        // do instanceof check here to avoid a public discard method in the ViewableModel interface
        if (getViewableModel() instanceof SubnodeViewableModel) {
            ((SubnodeViewableModel)getViewableModel()).discard();
        }
    }

     /**
     * {@inheritDoc}
     */
    @Override
    protected boolean showApplyOptionsDialog(final boolean showDiscardOption, final String title, final String message) {
        ElementRadioSelectionDialog dialog = new ElementRadioSelectionDialog(m_browser.getShell());
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setSize(60, showDiscardOption ? 14 : 11);
        RadioItem discardOption =
            new RadioItem(DISCARD_LABEL, null, DISCARD_DESCRIPTION);
        RadioItem applyOption = new RadioItem(APPLY_LABEL, null,
            String.format(APPLY_DESCRIPTION_FORMAT, showDiscardOption ? ", closes the view" : ""));
        RadioItem newDefaultOption = new RadioItem(APPLY_DEFAULT_LABEL, null,
            String.format(APPLY_DEFAULT_DESCRIPTION_FORMAT, showDiscardOption ? ", closes the view" : ""));
        if (showDiscardOption) {
            dialog.setElements(new RadioItem[]{discardOption, applyOption, newDefaultOption});
            dialog.setInitialSelectedElement(discardOption);
        } else {
            dialog.setElements(new RadioItem[]{applyOption, newDefaultOption});
            dialog.setInitialSelectedElement(applyOption);
        }
        dialog.open();
        if (dialog.getReturnCode() != IDialogConstants.OK_ID) {
            return false;
        }
        RadioItem selectedItem = dialog.getSelectedElement();
        if (applyOption.equals(selectedItem)) {
            return applyTriggered(false);
        }
        if (newDefaultOption.equals(selectedItem)) {
            return applyTriggered(true);
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean viewInteractionPossible() {
        return m_viewSet;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean validateCurrentValueInView() {
        boolean valid = true;
        WizardViewCreator<REP, VAL> creator = getViewCreator();
        WebTemplate template = creator.getWebTemplate();
        String validateMethod = template.getValidateMethodName();
        if (validateMethod != null && !validateMethod.isEmpty()) {
            String evalCode = creator
                .wrapInTryCatch("return JSON.stringify(" + creator.getNamespacePrefix() + validateMethod + "());");
            String jsonString = (String)m_browser.evaluate(evalCode);
            valid = Boolean.parseBoolean(jsonString);
        }
        return valid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String retrieveCurrentValueFromView() {
        WizardViewCreator<REP, VAL> creator = getViewCreator();
        WebTemplate template = creator.getWebTemplate();
        String pullMethod = template.getPullViewContentMethodName();
        String ns = creator.getNamespacePrefix();
        String jsonString = null;
        if (ns != null && !ns.isEmpty() && pullMethod != null && !pullMethod.isEmpty()) {
            String evalCode = creator.wrapInTryCatch("if (typeof " + ns.substring(0, ns.length() - 1)
                + " != 'undefined') { return JSON.stringify(" + ns + pullMethod + "());}");
            jsonString = (String)m_browser.evaluate(evalCode);
        }
        return jsonString;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void showValidationErrorInView(final String error) {
        WizardViewCreator<REP, VAL> creator = getViewCreator();
        WebTemplate template = creator.getWebTemplate();
        String showErrorMethod = template.getSetValidationErrorMethodName();
        String escapedError = error.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ");
        String showErrorCall = creator.wrapInTryCatch(creator.getNamespacePrefix() + showErrorMethod + "('" + escapedError + "');");
        m_browser.execute(showErrorCall);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void respondToViewRequest(final String response) {
        Display display = getDisplay();
        if (display == null) {
            // view most likely disposed
            return;
        }
        display.asyncExec(new Runnable() {

            @Override
            public void run() {
                LOGGER.debug("Sending response: " + response);
                String call = "KnimeInteractivity.respondToViewRequest(JSON.parse('" + response + "'));";
                WizardViewCreator<REP, VAL> creator = getViewCreator();
                call = creator.wrapInTryCatch(call);
                if (m_browser != null && !m_browser.isDisposed()) {
                    m_browser.execute(call);
                }
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void pushRequestUpdate(final String monitor) {
        Display display = getDisplay();
        if (display == null) {
            // view most likely disposed
            return;
        }
        display.asyncExec(new Runnable() {

            @Override
            public void run() {
                String call = "KnimeInteractivity.updateResponseMonitor(JSON.parse('" + monitor + "'));";
                WizardViewCreator<REP, VAL> creator = getViewCreator();
                call = creator.wrapInTryCatch(call);
                if (m_browser != null && !m_browser.isDisposed()) {
                    m_browser.execute(call);
                }
            }
        });
    }

    private class ViewRequestFunction extends BrowserFunction {

        /**
         * @param browser
         * @param name
         */
        public ViewRequestFunction(final Browser browser, final String name) {
            super(browser, name);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object function(final Object[] arguments) {
            if (arguments == null || arguments.length < 1) {
                return false;
            }
            return handleViewRequest((String)arguments[0]);
        }

    }

    private class UpdateRequestStatusFunction extends BrowserFunction {

        /**
         * @param browser
         * @param name
         */
        public UpdateRequestStatusFunction(final Browser browser, final String name) {
            super(browser, name);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object function(final Object[] arguments) {
            if (arguments == null || arguments.length < 1) {
                return false;
            }
            return updateRequestStatus((String)arguments[0]);
        }
    }

    private class CancelRequestFunction extends BrowserFunction {

        /**
         * @param browser
         * @param name
         */
        public CancelRequestFunction(final Browser browser, final String name) {
            super(browser, name);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object function(final Object[] arguments) {
            if (arguments == null || arguments.length < 1) {
                return false;
            }
            cancelRequest((String)arguments[0]);
            return null;
        }
    }

    private class PushSupportedFunction extends BrowserFunction {

        /**
         * @param browser
         * @param name
         */
        public PushSupportedFunction(final Browser browser, final String name) {
            super(browser, name);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object function(final Object[] arguments) {
            return isPushEnabled();
        }

    }

}
